package app.domain.order.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.domain.cart.model.dto.RedisCartItem;
import app.domain.cart.service.CartService;
import app.domain.order.client.InternalStoreClient;
import app.domain.order.model.dto.response.MenuInfoResponse;
import app.domain.order.model.dto.request.CreateOrderRequest;
import app.domain.order.model.dto.response.OrderDetailResponse;
import app.domain.order.model.dto.response.UpdateOrderStatusResponse;
import app.domain.order.model.entity.OrderItem;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.repository.OrderItemRepository;
import app.domain.order.model.repository.OrdersRepository;
import app.domain.order.status.OrderErrorStatus;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.exception.GeneralException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrdersRepository ordersRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartService cartService;
	private final OrderDelayService orderDelayService;
	private final ObjectMapper objectMapper;
	private final InternalStoreClient internalStoreClient;

	@Transactional
	public UUID createOrder(Long userId,CreateOrderRequest request) {

		List<RedisCartItem> cartItems = cartService.getCartFromCache(userId);
		if (cartItems.isEmpty()) {
			throw new GeneralException(ErrorStatus.CART_NOT_FOUND);
		}
		UUID storeId = cartItems.get(0).getStoreId();
		boolean allSameStore = cartItems.stream().allMatch(item -> item.getStoreId().equals(storeId));
		if (!allSameStore) {
			throw new GeneralException(OrderErrorStatus.ORDER_DIFFERENT_STORE);
		}

		if (!internalStoreClient.isStoreExists(storeId)) {
			throw new GeneralException(ErrorStatus.STORE_NOT_FOUND);
		}

		List<UUID> menuIds = cartItems.stream()
			.map(RedisCartItem::getMenuId)
			.toList();
		
		List<MenuInfoResponse> menuInfoResponseList = internalStoreClient.getMenuInfoList(menuIds);

		Map<UUID, MenuInfoResponse> menuMap = menuInfoResponseList.stream()
			.collect(java.util.stream.Collectors.toMap(MenuInfoResponse::getMenuId, menu -> menu));

		Long calculatedTotalPrice = cartItems.stream()
			.mapToLong(item -> menuMap.get(item.getMenuId()).getPrice() * item.getQuantity())
			.sum();
		if (!calculatedTotalPrice.equals(request.getTotalPrice())) {
			throw new GeneralException(OrderErrorStatus.ORDER_PRICE_MISMATCH);
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

	private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
		OrderStatus.PENDING, EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.REJECTED, OrderStatus.REFUNDED),
		OrderStatus.ACCEPTED, EnumSet.of(OrderStatus.COOKING),
		OrderStatus.COOKING, EnumSet.of(OrderStatus.IN_DELIVERY),
		OrderStatus.IN_DELIVERY, EnumSet.of(OrderStatus.COMPLETED)
	);

	@Transactional
	public UpdateOrderStatusResponse updateOrderStatus(Long userId,UUID orderId, OrderStatus newStatus) {

		Orders order = ordersRepository.findById(orderId)
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));

		validateOwnerUpdate(userId, order, newStatus);

		String updatedHistory = appendToHistory(order.getOrderHistory(), newStatus);
		order.updateStatusAndHistory(newStatus, updatedHistory);

		return UpdateOrderStatusResponse.from(order);
	}

	private void validateOwnerUpdate(Long userId, Orders order, OrderStatus newStatus) {
		if (!internalStoreClient.isStoreOwner(userId, order.getStoreId())) {
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
}