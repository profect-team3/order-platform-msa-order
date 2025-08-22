package app.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import app.commonSecurity.TokenPrincipalParser;
import app.domain.cart.model.dto.RedisCartItem;
import app.domain.cart.service.CartService;
import app.domain.order.client.InternalStoreClient;
import app.domain.order.model.dto.request.CreateOrderRequest;
import app.domain.order.model.dto.response.MenuInfoResponse;
import app.domain.order.model.dto.response.OrderDetailResponse;
import app.domain.order.model.entity.OrderItem;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderChannel;
import app.domain.order.model.entity.enums.PaymentMethod;
import app.domain.order.model.entity.enums.ReceiptMethod;
import app.domain.order.model.repository.OrderItemRepository;
import app.domain.order.model.repository.OrdersRepository;
import app.domain.order.service.OrderDelayService;
import app.domain.order.service.OrderService;
import app.domain.order.status.OrderErrorStatus;
import app.global.apiPayload.ApiResponse;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.code.status.SuccessStatus;
import app.global.apiPayload.exception.GeneralException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Test")
@ActiveProfiles("test")
class OrderServiceTest {

	@Mock
	private OrdersRepository ordersRepository;

	@Mock
	private OrderItemRepository orderItemRepository;

	@Mock
	private CartService cartService;

	@Mock
	private OrderDelayService orderDelayService;

	@Mock
	private InternalStoreClient internalStoreClient;

	@Mock
	private TokenPrincipalParser tokenPrincipalParser;

	@Mock
	private Authentication authentication;

	@InjectMocks
	private OrderService orderService;

	private Long userId;
	private UUID storeId;
	private UUID menuId;
	private CreateOrderRequest request;
	private MenuInfoResponse menuInfo;

	@BeforeEach
	void setUp() {
		userId = 1L;
		storeId = UUID.randomUUID();
		menuId = UUID.randomUUID();

		request = new CreateOrderRequest(
			PaymentMethod.CREDIT_CARD,
			OrderChannel.ONLINE,
			ReceiptMethod.DELIVERY,
			"문 앞에 놓아주세요",
			6000L,
			"서울시 강남구"
		);

		menuInfo = new MenuInfoResponse(
			menuId,
			"테스트 메뉴",
			3000L
		);
	}

	@Test
	@DisplayName("주문 생성 성공")
	void createOrder_Success() {
		// Given
		RedisCartItem cartItem = RedisCartItem.builder()
			.menuId(menuId)
			.storeId(storeId)
			.quantity(2)
			.build();
		List<RedisCartItem> cartItems = List.of(cartItem);
		List<MenuInfoResponse> menuInfoResponseList = List.of(menuInfo);
		List<UUID> menuIds=List.of(menuId);
		Orders savedOrder = Orders.builder().ordersId(UUID.randomUUID()).build();

		when(tokenPrincipalParser.getUserId(any(Authentication.class))).thenReturn(String.valueOf(userId));
		when(cartService.getCartFromCache(authentication)).thenReturn(cartItems);
		when(internalStoreClient.isStoreExists(storeId)).thenReturn(ApiResponse.onSuccess(SuccessStatus._OK, true));
		when(internalStoreClient.getMenuInfoList(menuIds)).thenReturn(ApiResponse.onSuccess(SuccessStatus._OK, menuInfoResponseList));
		when(internalStoreClient.decreaseStock(anyList())).thenReturn(ApiResponse.onSuccess(SuccessStatus._OK, true));

		when(ordersRepository.save(any(Orders.class))).thenReturn(savedOrder);

		// When
		UUID result = orderService.createOrder(authentication,request);

		// Then
		assertThat(result).isInstanceOf(UUID.class);
		verify(cartService).getCartFromCache(authentication);
		verify(internalStoreClient).isStoreExists(storeId);
		verify(internalStoreClient).getMenuInfoList(menuIds);
		verify(ordersRepository).save(any(Orders.class));
		verify(orderItemRepository).save(any(OrderItem.class));
		verify(orderDelayService).scheduleRefundDisable(any(UUID.class));
	}

	@Test
	@DisplayName("매장을 찾을 수 없음")
	void createOrder_StoreNotFound() {
		// Given
		RedisCartItem cartItem = RedisCartItem.builder()
			.menuId(menuId)
			.storeId(storeId)
			.quantity(2)
			.build();
		List<RedisCartItem> cartItems = List.of(cartItem);
		List<UUID> menuIds=List.of(menuId);

		when(tokenPrincipalParser.getUserId(any(Authentication.class))).thenReturn(String.valueOf(userId));
		when(cartService.getCartFromCache(authentication)).thenReturn(cartItems);
		when(internalStoreClient.isStoreExists(storeId)).thenReturn(ApiResponse.onSuccess(SuccessStatus._OK, false));
		// When & Then
		assertThatThrownBy(() -> orderService.createOrder(authentication,request))
			.isInstanceOf(GeneralException.class)
			.satisfies(ex -> {
				GeneralException generalEx = (GeneralException)ex;
				assertThat(generalEx.getErrorReason().getMessage()).isEqualTo(ErrorStatus.STORE_NOT_FOUND.getMessage());
			});

		verify(cartService).getCartFromCache(authentication);
		verify(internalStoreClient).isStoreExists(storeId);
		verify(ordersRepository, never()).save(any());
		verify(internalStoreClient, never()).getMenuInfoList(menuIds);

	}

	@Test
	@DisplayName("장바구니에 2개 매장의 메뉴가 들어있음")
	void createOrder_DifferentStores() {
		// Given
		UUID anotherStoreId = UUID.randomUUID();
		RedisCartItem cartItem1 = RedisCartItem.builder()
			.menuId(menuId)
			.storeId(storeId)
			.quantity(2)
			.build();
		RedisCartItem cartItem2 = RedisCartItem.builder()
			.menuId(UUID.randomUUID())
			.storeId(anotherStoreId)
			.quantity(1)
			.build();
		List<RedisCartItem> cartItems = List.of(cartItem1, cartItem2);

		when(tokenPrincipalParser.getUserId(any(Authentication.class))).thenReturn(String.valueOf(userId));
		when(cartService.getCartFromCache(authentication)).thenReturn(cartItems);

		// When & Then
		assertThatThrownBy(() -> orderService.createOrder(authentication,request))
			.isInstanceOf(GeneralException.class)
			.satisfies(ex -> {
				GeneralException generalEx = (GeneralException)ex;
				assertThat(generalEx.getErrorReason().getMessage()).isEqualTo(
					OrderErrorStatus.ORDER_DIFFERENT_STORE.getMessage());
			});

		verify(cartService).getCartFromCache(authentication);
		verify(internalStoreClient,never()).isStoreExists(storeId);
	}

	@Test
	@DisplayName("장바구니가 비어있음")
	void createOrder_EmptyCart() {
		// Given
		List<RedisCartItem> cartItems = List.of();

		when(tokenPrincipalParser.getUserId(any(Authentication.class))).thenReturn(String.valueOf(userId));
		when(cartService.getCartFromCache(authentication)).thenReturn(cartItems);

		// When & Then
		assertThatThrownBy(() -> orderService.createOrder(authentication,request))
			.isInstanceOf(GeneralException.class)
			.satisfies(ex -> {
				GeneralException generalEx = (GeneralException)ex;
				assertThat(generalEx.getErrorReason().getMessage()).isEqualTo(ErrorStatus.CART_NOT_FOUND.getMessage());
			});

		verify(cartService).getCartFromCache(authentication);
		verify(ordersRepository, never()).save(any());
	}

	@Test
	@DisplayName("요청 총액과 장바구니 아이템 총액 불일치")
	void createOrder_PriceMismatch() {
		// Given
		RedisCartItem cartItem = RedisCartItem.builder()
			.menuId(menuId)
			.storeId(storeId)
			.quantity(2)
			.build();
		List<RedisCartItem> cartItems = List.of(cartItem);

		CreateOrderRequest mismatchRequest = new CreateOrderRequest(
			PaymentMethod.CREDIT_CARD,
			OrderChannel.ONLINE,
			ReceiptMethod.DELIVERY,
			"문 앞에 놓아주세요",
			15000L,
			"서울시 강남구"
		);

		List<UUID> menuIds=List.of(menuId);
		List<MenuInfoResponse> menuInfoResponseList = List.of(menuInfo);

		when(tokenPrincipalParser.getUserId(any(Authentication.class))).thenReturn(String.valueOf(userId));
		when(cartService.getCartFromCache(authentication)).thenReturn(cartItems);
		when(internalStoreClient.isStoreExists(storeId)).thenReturn(ApiResponse.onSuccess(SuccessStatus._OK, true));
		when(internalStoreClient.getMenuInfoList(menuIds)).thenReturn(ApiResponse.onSuccess(SuccessStatus._OK, menuInfoResponseList));


		// When & Then
		assertThatThrownBy(() -> orderService.createOrder(authentication,mismatchRequest))
			.isInstanceOf(GeneralException.class)
			.satisfies(ex -> {
				GeneralException generalEx = (GeneralException)ex;
				assertThat(generalEx.getErrorReason().getMessage()).isEqualTo(
					OrderErrorStatus.ORDER_PRICE_MISMATCH.getMessage());
			});

		verify(cartService).getCartFromCache(authentication);
		verify(internalStoreClient).isStoreExists(storeId);
		verify(internalStoreClient).getMenuInfoList(menuIds);
		verify(ordersRepository, never()).save(any());
	}

	@Test
	@DisplayName("메뉴를 찾을 수 없음")
	void createOrder_MenuNotFound() {
		// Given
		RedisCartItem cartItem = RedisCartItem.builder()
			.menuId(menuId)
			.storeId(storeId)
			.quantity(2)
			.build();
		List<RedisCartItem> cartItems = List.of(cartItem);
		List<UUID> menuIds=List.of(menuId);

		when(tokenPrincipalParser.getUserId(any(Authentication.class))).thenReturn(String.valueOf(userId));
		when(cartService.getCartFromCache(authentication)).thenReturn(cartItems);
		when(internalStoreClient.isStoreExists(storeId)).thenReturn(ApiResponse.onSuccess(SuccessStatus._OK, true));
		when(internalStoreClient.getMenuInfoList(menuIds)).thenThrow(new GeneralException(ErrorStatus.MENU_NOT_FOUND));

		// When & Then
		assertThatThrownBy(() -> orderService.createOrder(authentication, request))
			.isInstanceOf(GeneralException.class)
			.satisfies(ex -> {
				GeneralException generalEx = (GeneralException)ex;
				assertThat(generalEx.getErrorReason().getMessage()).isEqualTo(ErrorStatus.MENU_NOT_FOUND.getMessage());
			});

		verify(cartService).getCartFromCache(authentication);
		verify(internalStoreClient).isStoreExists(storeId);
		verify(internalStoreClient).getMenuInfoList(menuIds);
		verify(ordersRepository, never()).save(any());
	}

	@Test
	@DisplayName("주문 상세 조회 성공")
	void getOrderDetail_Success() {
		// Given
		UUID orderId = UUID.randomUUID();

		Orders order = Orders.builder()
			.ordersId(orderId)
			.userId(userId)
			.storeId(storeId)
			.totalPrice(10000L)
			.deliveryAddress("서울시 강남구")
			.paymentMethod(PaymentMethod.CREDIT_CARD)
			.orderChannel(OrderChannel.ONLINE)
			.receiptMethod(ReceiptMethod.DELIVERY)
			.orderStatus(app.domain.order.model.entity.enums.OrderStatus.PENDING)
			.requestMessage("문 앞에 놓아주세요")
			.build();

		OrderItem orderItem = OrderItem.builder()
			.orders(order)
			.menuName("테스트메뉴")
			.price(5000L)
			.quantity(2)
			.build();
		List<OrderItem> orderItems = List.of(orderItem);

		when(ordersRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(orderItemRepository.findByOrders(order)).thenReturn(orderItems);

		// When
		OrderDetailResponse result = orderService.getOrderDetail(orderId);

		// Then
		assertThat(result.getMenuList()).hasSize(1);
		assertThat(result.getMenuList().get(0).getMenuName()).isEqualTo("테스트메뉴");
		assertThat(result.getMenuList().get(0).getQuantity()).isEqualTo(2);
		assertThat(result.getMenuList().get(0).getPrice()).isEqualTo(5000);
		assertThat(result.getTotalPrice()).isEqualTo(10000L);
		assertThat(result.getDeliveryAddress()).isEqualTo("서울시 강남구");
		assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
		assertThat(result.getOrderChannel()).isEqualTo(OrderChannel.ONLINE);
		assertThat(result.getReceiptMethod()).isEqualTo(ReceiptMethod.DELIVERY);
		assertThat(result.getOrderStatus()).isEqualTo(app.domain.order.model.entity.enums.OrderStatus.PENDING);
		assertThat(result.getRequestMessage()).isEqualTo("문 앞에 놓아주세요");

		verify(ordersRepository).findById(orderId);
		verify(orderItemRepository).findByOrders(order);
	}

	@Test
	@DisplayName("주문을 찾을 수 없음")
	void getOrderDetail_OrderNotFound() {
		// Given
		UUID orderId = UUID.randomUUID();

		when(ordersRepository.findById(orderId)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> orderService.getOrderDetail(orderId))
			.isInstanceOf(GeneralException.class)
			.satisfies(ex -> {
				GeneralException generalEx = (GeneralException)ex;
				assertThat(generalEx.getErrorReason().getMessage()).isEqualTo(ErrorStatus.ORDER_NOT_FOUND.getMessage());
			});

		verify(ordersRepository).findById(orderId);
		verify(orderItemRepository, never()).findByOrders(any());
	}
}