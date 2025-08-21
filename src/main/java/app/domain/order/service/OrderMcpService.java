package app.domain.order.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;


import app.domain.cart.model.dto.RedisCartItem;
import app.domain.cart.service.CartMcpService;
import app.domain.order.client.InternalStoreClient;
import app.domain.order.model.dto.request.CreateOrderRequest;
import app.domain.order.model.dto.request.StockRequest;
import app.domain.order.model.dto.response.MenuInfoResponse;
import app.domain.order.model.entity.OrderItem;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.repository.OrderItemRepository;
import app.domain.order.model.repository.OrdersRepository;
import app.domain.order.status.OrderErrorStatus;
import app.global.apiPayload.ApiResponse;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMcpService {

	private final OrdersRepository ordersRepository;
	private final OrderItemRepository orderItemRepository;
	private final CartMcpService cartMcpService;
	private final OrderDelayService orderDelayService;
	private final InternalStoreClient internalStoreClient;

	@Transactional
	public UUID createOrder(Long userId, CreateOrderRequest request) {

		List<RedisCartItem> cartItems = cartMcpService.getCartFromCache(userId);
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

		List<StockRequest> stockRequests = cartItems.stream()
			.map(StockRequest::from)
			.toList();

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

		ApiResponse<Boolean> stockCheckResponse;
		try {
			stockCheckResponse = internalStoreClient.decreaseStock(stockRequests);
		}catch (HttpServerErrorException | HttpClientErrorException e){
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

	
}
