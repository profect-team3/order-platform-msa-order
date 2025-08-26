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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
	private final KafkaProducerService kafkaProducerService;

	/**
	 * 인증된 사용자로부터 장바구니를 기반으로 주문을 생성하고 주문 ID를 반환합니다.
	 *
	 * <p>동작 요약:
	 * - 인증 정보에서 사용자 ID를 추출하고 캐시된 장바구니를 조회합니다. 장바구니가 비어있거나
	 *   서로 다른 상점의 아이템이 섞여 있으면 예외를 던집니다.
	 * - 상점 존재 여부 및 메뉴 정보를 내부 스토어 서비스에서 검증하고, 장바구니 합계와 요청 총액이 일치하는지 확인합니다.
	 * - 재고를 감소시키는 외부 호출을 수행하여 재고가 충분한지 확인합니다.
	 * - 모든 검증이 완료되면 Orders 및 관련 OrderItem 엔티티를 저장하고, 주문 생성 이벤트를 Kafka로 발행합니다.
	 * - 환불 가능 상태 비활성화 작업을 예약합니다.
	 *
	 * @param authentication 인증된 사용자의 Spring Security Authentication 객체
	 * @param request 주문 생성 요청 데이터 (결제 방식, 총액, 배달 주소 등)
	 * @return 생성된 주문의 UUID
	 * @throws GeneralException 다음과 같은 ErrorStatus로 실패할 수 있습니다:
	 *         CART_NOT_FOUND, ORDER_DIFFERENT_STORE, STORE_NOT_FOUND, MENU_NOT_FOUND,
	 *         ORDER_PRICE_MISMATCH, OUT_OF_STOCK, _INTERNAL_SERVER_ERROR
	 */
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
		try{
			stockCheckResponse=decreaseStockWithCircuitBreaker(stockRequests);
		}catch (HttpServerErrorException|HttpClientErrorException e){
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

		kafkaProducerService.sendMessage("order-created-events", savedOrder);

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

	@CircuitBreaker(name = "test")
	public ApiResponse<Boolean> decreaseStockWithCircuitBreaker(List<StockRequest> stockRequests) {
		return internalStoreClient.decreaseStock(stockRequests);
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
