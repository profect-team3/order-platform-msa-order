package app.domain.order.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.commonSecurity.TokenPrincipalParser;
import app.domain.cart.model.dto.RedisCartItem;
import app.domain.cart.service.CartService;
import app.domain.order.client.InternalStoreClient;
import app.domain.order.model.dto.response.MenuInfoResponse;
import app.domain.order.model.dto.request.CreateOrderRequest;
import app.domain.order.model.dto.request.StockRequest;
import app.domain.order.model.dto.response.OrderDetailResponse;
import app.domain.order.model.dto.response.OrderResponse;
import app.domain.order.model.dto.response.UpdateOrderStatusResponse;
import app.domain.order.model.entity.OrderItem;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.repository.OrderItemRepository;
import app.domain.order.model.repository.OrdersRepository;
import app.domain.order.status.OrderErrorStatus;
import app.global.apiPayload.ApiResponse;
import app.global.apiPayload.PagedResponse;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.exception.GeneralException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

	private final OrdersRepository ordersRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartService cartService;
	private final OrderDelayService orderDelayService;
	private final ObjectMapper objectMapper;
	private final InternalStoreClient internalStoreClient;
	private final TokenPrincipalParser tokenPrincipalParser;
	private final CircuitBreakerRegistry circuitBreakerRegistry;

	@Transactional
	public UUID createOrder(Authentication authentication,CreateOrderRequest request) {
		String userIdStr = tokenPrincipalParser.getUserId(authentication);
		Long userId = Long.parseLong(userIdStr);
		List<RedisCartItem> cartItems = cartService.getCartFromCache(authentication);
		if (cartItems.isEmpty()) {
			throw new GeneralException(ErrorStatus.CART_NOT_FOUND);
		}
		UUID storeId = cartItems.get(0).getStoreId();
		boolean allSameStore = cartItems.stream().allMatch(item -> item.getStoreId().equals(storeId));
		if (!allSameStore) {
			throw new GeneralException(OrderErrorStatus.ORDER_DIFFERENT_STORE);
		}
		ApiResponse<Boolean> storeExistsResponse;
		try{
			storeExistsResponse = internalStoreClient.isStoreExists(storeId);
		} catch (HttpClientErrorException | HttpServerErrorException e){
			log.error("Store Service Error: {}", e.getResponseBodyAsString());
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}

		if (!storeExistsResponse.result()) {
			throw new GeneralException(ErrorStatus.STORE_NOT_FOUND);
		}



		List<UUID> menuIds = cartItems.stream()
			.map(RedisCartItem::getMenuId)
			.toList();

		ApiResponse<List<MenuInfoResponse>> menuInfoResponse;
		try {
			menuInfoResponse=internalStoreClient.getMenuInfoList(menuIds);
		} catch (HttpClientErrorException | HttpServerErrorException e){
			log.error("Store Service Error: {}", e.getResponseBodyAsString());
			throw new GeneralException(ErrorStatus.MENU_NOT_FOUND);
		}

		List<MenuInfoResponse> menuInfoResponseList=menuInfoResponse.result();
		Map<UUID, MenuInfoResponse> menuMap = menuInfoResponseList.stream()
			.collect(java.util.stream.Collectors.toMap(MenuInfoResponse::getMenuId, menu -> menu));

		Long calculatedTotalPrice = cartItems.stream()
			.mapToLong(item -> menuMap.get(item.getMenuId()).getPrice() * item.getQuantity())
			.sum();
		if (!calculatedTotalPrice.equals(request.getTotalPrice())) {
			throw new GeneralException(OrderErrorStatus.ORDER_PRICE_MISMATCH);
		}


		List<StockRequest> stockRequests = cartItems.stream()
			.map(StockRequest::from)
			.toList();

		ApiResponse<Boolean> stockCheckResponse;
		try {
			CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test");
			stockCheckResponse = circuitBreaker.executeSupplier(() -> internalStoreClient.decreaseStock(stockRequests));
		} catch (HttpServerErrorException | HttpClientErrorException e){
			log.error("Store Service Error: {}", e.getResponseBodyAsString());
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}

		if(!stockCheckResponse.result()){
			throw new GeneralException(OrderErrorStatus.OUT_OF_STOCK);
		}

		Orders order = Orders.builder()
			.userId(userId)
			.storeId(storeId)
			.paymentMethod(request.getPaymentMethod())
			.orderChannel(request.getOrderChannel())
			.receiptMethod(request.getReceiptMethod())
			.requestMessage(request.getRequestMessage())
			.totalPrice(request.getTotalPrice())
			.orderStatus(OrderStatus.PENDING)
			.deliveryAddress(request.getDeliveryAddress())
			.orderHistory(
				"pending:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
			.isRefundable(true)
			.build();

		Orders savedOrder = ordersRepository.save(order);

		for (RedisCartItem cartItem : cartItems) {
			MenuInfoResponse menu = menuMap.get(cartItem.getMenuId());

			OrderItem orderItem = OrderItem.builder()
				.orders(savedOrder)
				.menuName(menu.getName())
				.price(menu.getPrice())
				.quantity(cartItem.getQuantity())
				.build();
			orderItemRepository.save(orderItem);
		}

		orderDelayService.scheduleRefundDisable(savedOrder.getOrdersId());

		return savedOrder.getOrdersId();
	}

	public OrderDetailResponse getOrderDetail(UUID orderId) {
		Orders order = ordersRepository.findById(orderId)
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));

		List<OrderItem> orderItems = orderItemRepository.findByOrders(order);

		return OrderDetailResponse.from(order, orderItems);
	}

	@Transactional(readOnly = true)
	public PagedResponse<OrderDetailResponse> getCustomerOrderListById(Long userId, Pageable pageable) {

		Page<Orders> ordersPage = ordersRepository.findAllByUserIdAndDeliveryAddressIsNotNull(userId, pageable);

		Page<OrderDetailResponse> mapped = ordersPage.map(order -> {
			List<OrderItem> orderItems = orderItemRepository.findByOrders(order);
			return OrderDetailResponse.from(order, orderItems);
		});

		return PagedResponse.from(mapped);
	}

	private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
		OrderStatus.PENDING, EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.REJECTED, OrderStatus.REFUNDED),
		OrderStatus.ACCEPTED, EnumSet.of(OrderStatus.COOKING),
		OrderStatus.COOKING, EnumSet.of(OrderStatus.IN_DELIVERY),
		OrderStatus.IN_DELIVERY, EnumSet.of(OrderStatus.COMPLETED)
	);

	@Transactional
	public UpdateOrderStatusResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {

		Orders order = ordersRepository.findById(orderId)
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));

		validateOwnerUpdate(order, newStatus);

		String updatedHistory = appendToHistory(order.getOrderHistory(), newStatus);
		order.updateStatusAndHistory(newStatus, updatedHistory);

		return UpdateOrderStatusResponse.from(order);
	}

	private void validateOwnerUpdate(Orders order, OrderStatus newStatus) {
		ApiResponse<Boolean> response;
		try {
			response=internalStoreClient.isStoreOwner(order.getStoreId());
		} catch (HttpClientErrorException | HttpServerErrorException e){
			log.error("Store Service Error: {}", e.getResponseBodyAsString());
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}

		if (!response.result()) {
			throw new GeneralException(OrderErrorStatus.ORDER_ACCESS_DENIED);
		}

		Set<OrderStatus> allowedTransitions = VALID_TRANSITIONS.getOrDefault(order.getOrderStatus(),
			EnumSet.noneOf(OrderStatus.class));

		if (!allowedTransitions.contains(newStatus)) {
			throw new GeneralException(OrderErrorStatus.INVALID_ORDER_STATUS_TRANSITION);
		}
	}

	private String appendToHistory(String currentHistoryJson, OrderStatus newStatus) {
		TypeReference<Map<String, String>> typeRef = new TypeReference<>() {
		};

		Map<String, String> historyMap;
		if (currentHistoryJson == null || currentHistoryJson.isBlank() || currentHistoryJson.equals("{}")) {
			historyMap = new LinkedHashMap<>();
		} else {
			try {
				historyMap = objectMapper.readValue(currentHistoryJson, typeRef);
			} catch (JsonProcessingException e) {
				throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
			}
		}

		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		historyMap.put(newStatus.name(), timestamp);

		try {
			return objectMapper.writeValueAsString(historyMap);
		} catch (JsonProcessingException e) {
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> getCustomerOrders(Authentication authentication) {
		String userIdStr = tokenPrincipalParser.getUserId(authentication);
		Long userId = Long.parseLong(userIdStr);

		List<Orders> orders = ordersRepository.findByUserId(userId);
		if (orders.isEmpty()) {
			throw new GeneralException(ErrorStatus.ORDER_NOT_FOUND);
		}
		return orders.stream()
			.map(OrderResponse::of)
			.collect(Collectors.toList());

	}
}
