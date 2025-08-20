package app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.domain.order.OrderController;
import app.domain.order.model.dto.request.CreateOrderRequest;
import app.domain.order.model.dto.request.UpdateOrderStatusRequest;
import app.domain.order.model.dto.response.OrderDetailResponse;
import app.domain.order.model.dto.response.OrderResponse;
import app.domain.order.model.dto.response.UpdateOrderStatusResponse;
import app.domain.order.model.entity.enums.OrderChannel;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.entity.enums.PaymentMethod;
import app.domain.order.model.entity.enums.ReceiptMethod;
import app.domain.order.service.OrderService;
import app.domain.order.status.OrderSuccessStatus;
import app.global.apiPayload.PagedResponse;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.exception.GeneralException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController 테스트")
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private WebApplicationContext context;

	@MockitoBean
	private OrderService orderService;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
			.webAppContextSetup(context)
			.build();
	}

	@Test
	@DisplayName("주문 생성 - 성공")
	void createOrder_Success() throws Exception {
		UUID orderId = UUID.randomUUID();
		CreateOrderRequest request = new CreateOrderRequest(
			PaymentMethod.CREDIT_CARD,
			OrderChannel.ONLINE,
			ReceiptMethod.DELIVERY,
			"문 앞에 놓아주세요",
			10000L,
			"서울시 강남구"
		);

		given(orderService.createOrder(any(), any(CreateOrderRequest.class)))
			.willReturn(orderId);

		mockMvc.perform(post("/")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(OrderSuccessStatus.ORDER_CREATED.getCode()))
			.andExpect(jsonPath("$.message").value(OrderSuccessStatus.ORDER_CREATED.getMessage()))
			.andExpect(jsonPath("$.result").value(orderId.toString()));

		verify(orderService).createOrder(any(), any(CreateOrderRequest.class));
	}

	@Test
	@DisplayName("주문 생성 - 총 금액 0 이하 실패")
	void createOrder_InvalidTotalPrice() throws Exception {
		CreateOrderRequest request = new CreateOrderRequest(
			PaymentMethod.CREDIT_CARD,
			OrderChannel.ONLINE,
			ReceiptMethod.DELIVERY,
			"문 앞에 놓아주세요",
			0L,
			"서울시 강남구"
		);

		mockMvc.perform(post("/")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorStatus._BAD_REQUEST.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorStatus._BAD_REQUEST.getMessage()))
			.andExpect(jsonPath("$.result.totalPrice").value("총 금액은 양의 정수여야 합니다."));

		verify(orderService, never()).createOrder(any(), any());
	}

	@Test
	@DisplayName("주문 생성 - 서비스 에러")
	void createOrder_ServiceError() throws Exception {
		CreateOrderRequest request = new CreateOrderRequest(
			PaymentMethod.CREDIT_CARD,
			OrderChannel.ONLINE,
			ReceiptMethod.DELIVERY,
			"문 앞에 놓아주세요",
			10000L,
			"서울시 강남구"
		);

		given(orderService.createOrder(any(), any(CreateOrderRequest.class)))
			.willThrow(new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR));

		mockMvc.perform(post("/")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value(ErrorStatus._INTERNAL_SERVER_ERROR.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorStatus._INTERNAL_SERVER_ERROR.getMessage()));

		verify(orderService).createOrder(any(), any(CreateOrderRequest.class));
	}

	@Test
	@DisplayName("주문 상세 조회 - 성공")
	void getOrderDetail_Success() throws Exception {
		UUID orderId = UUID.randomUUID();
		OrderDetailResponse response = new OrderDetailResponse(
			List.of(new OrderDetailResponse.Menu("후라이드 치킨", 2, 18000L)),
			36000L,
			"서울시 강남구",
			PaymentMethod.CREDIT_CARD,
			OrderChannel.ONLINE,
			ReceiptMethod.DELIVERY,
			OrderStatus.PENDING,
			"문 앞에 놓아주세요"
		);

		given(orderService.getOrderDetail(orderId)).willReturn(response);

		mockMvc.perform(get("/{orderId}", orderId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(OrderSuccessStatus.ORDER_DETAIL_FETCHED.getCode()))
			.andExpect(jsonPath("$.message").value(OrderSuccessStatus.ORDER_DETAIL_FETCHED.getMessage()))
			.andExpect(jsonPath("$.result.menuList").isArray())
			.andExpect(jsonPath("$.result.menuList.length()").value(1))
			.andExpect(jsonPath("$.result.menuList[0].menuName").value("후라이드 치킨"))
			.andExpect(jsonPath("$.result.menuList[0].quantity").value(2))
			.andExpect(jsonPath("$.result.menuList[0].price").value(18000))
			.andExpect(jsonPath("$.result.totalPrice").value(36000))
			.andExpect(jsonPath("$.result.deliveryAddress").value("서울시 강남구"))
			.andExpect(jsonPath("$.result.paymentMethod").value("CREDIT_CARD"))
			.andExpect(jsonPath("$.result.orderChannel").value("ONLINE"))
			.andExpect(jsonPath("$.result.receiptMethod").value("DELIVERY"))
			.andExpect(jsonPath("$.result.orderStatus").value("PENDING"))
			.andExpect(jsonPath("$.result.requestMessage").value("문 앞에 놓아주세요"));

		verify(orderService).getOrderDetail(orderId);
	}

	@Test
	@DisplayName("주문 상세 조회 - 주문을 찾을 수 없음")
	void getOrderDetail_OrderNotFound() throws Exception {
		UUID orderId = UUID.randomUUID();

		given(orderService.getOrderDetail(orderId))
			.willThrow(new GeneralException(ErrorStatus.ORDER_NOT_FOUND));

		mockMvc.perform(get("/{orderId}", orderId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorStatus.ORDER_NOT_FOUND.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorStatus.ORDER_NOT_FOUND.getMessage()));

		verify(orderService).getOrderDetail(orderId);
	}

	@Test
	@DisplayName("주문 상세 조회 - 서버 에러")
	void getOrderDetail_ServerError() throws Exception {
		UUID orderId = UUID.randomUUID();

		given(orderService.getOrderDetail(orderId))
			.willThrow(new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR));

		mockMvc.perform(get("/{orderId}", orderId))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value(ErrorStatus._INTERNAL_SERVER_ERROR.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorStatus._INTERNAL_SERVER_ERROR.getMessage()));

		verify(orderService).getOrderDetail(orderId);
	}

	@Test
	@DisplayName("필수 파라미터 누락 - paymentMethod 없음")
	void createOrder_MissingPaymentMethod() throws Exception {
		String jsonWithoutPaymentMethod = "{\"orderChannel\": \"ONLINE\", \"receiptMethod\": \"DELIVERY\", \"totalPrice\": 10000, \"deliveryAddress\": \"서울시 강남구\"}";

		mockMvc.perform(post("/")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonWithoutPaymentMethod))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorStatus._BAD_REQUEST.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorStatus._BAD_REQUEST.getMessage()))
			.andExpect(jsonPath("$.result.paymentMethod").value("결제 방법은 필수입니다."));

		verify(orderService, never()).createOrder(any(), any());
	}

	@Test
	@DisplayName("필수 파라미터 누락 - orderChannel 없음")
	void createOrder_MissingOrderChannel() throws Exception {
		String jsonWithoutOrderChannel = "{\"paymentMethod\": \"CREDIT_CARD\", \"receiptMethod\": \"DELIVERY\", \"totalPrice\": 10000, \"deliveryAddress\": \"서울시 강남구\"}";

		mockMvc.perform(post("/")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonWithoutOrderChannel))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorStatus._BAD_REQUEST.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorStatus._BAD_REQUEST.getMessage()))
			.andExpect(jsonPath("$.result.orderChannel").value("주문 채널은 필수입니다."));

		verify(orderService, never()).createOrder(any(), any());
	}

	@Test
	@DisplayName("필수 파라미터 누락 - receiptMethod 없음")
	void createOrder_MissingReceiptMethod() throws Exception {
		String jsonWithoutReceiptMethod = "{\"paymentMethod\": \"CREDIT_CARD\", \"orderChannel\": \"ONLINE\", \"totalPrice\": 10000, \"deliveryAddress\": \"서울시 강남구\"}";

		mockMvc.perform(post("/")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonWithoutReceiptMethod))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorStatus._BAD_REQUEST.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorStatus._BAD_REQUEST.getMessage()))
			.andExpect(jsonPath("$.result.receiptMethod").value("수령 방법은 필수입니다."));

		verify(orderService, never()).createOrder(any(), any());
	}

	@Test
	@DisplayName("필수 파라미터 누락 - totalPrice 없음")
	void createOrder_MissingTotalPrice() throws Exception {
		String jsonWithoutTotalPrice = "{\"paymentMethod\": \"CREDIT_CARD\", \"orderChannel\": \"ONLINE\", \"receiptMethod\": \"DELIVERY\", \"deliveryAddress\": \"서울시 강남구\"}";

		mockMvc.perform(post("/")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonWithoutTotalPrice))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorStatus._BAD_REQUEST.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorStatus._BAD_REQUEST.getMessage()))
			.andExpect(jsonPath("$.result.totalPrice").value("총 금액은 필수입니다."));

		verify(orderService, never()).createOrder(any(), any());
	}

	@Test
	@DisplayName("필수 파라미터 누락 - deliveryAddress 없음")
	void createOrder_MissingDeliveryAddress() throws Exception {
		String jsonWithoutDeliveryAddress = "{\"paymentMethod\": \"CREDIT_CARD\", \"orderChannel\": \"ONLINE\", \"receiptMethod\": \"DELIVERY\", \"totalPrice\": 10000}";

		mockMvc.perform(post("/")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonWithoutDeliveryAddress))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorStatus._BAD_REQUEST.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorStatus._BAD_REQUEST.getMessage()))
			.andExpect(jsonPath("$.result.deliveryAddress").value("배송 주소는 필수입니다."));

		verify(orderService, never()).createOrder(any(), any());
	}

	@Test
	@DisplayName("주문 상세 조회 - 잘못된 UUID 형식")
	void getOrderDetail_InvalidUUID() throws Exception {
		String invalidUUID = "invalid-uuid";

		mockMvc.perform(get("/{orderId}", invalidUUID))
			.andExpect(status().isBadRequest());

		verify(orderService, never()).getOrderDetail(any());
	}

	@Nested
	@DisplayName("주문 상태 변경 테스트")
	class UpdateOrderStatusTest {

		@Test
		@DisplayName("성공: 유효한 요청으로 주문 상태를 변경하면 200 OK와 변경된 상태 정보를 반환한다.")
		void updateOrderStatus_Success() throws Exception {
			// given
			UUID orderId = UUID.randomUUID();
			UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
			request.setNewStatus(OrderStatus.ACCEPTED);

			UpdateOrderStatusResponse mockResponse = UpdateOrderStatusResponse.builder()
				.orderId(orderId)
				.updatedStatus(OrderStatus.ACCEPTED)
				.build();

			given(orderService.updateOrderStatus(eq(orderId), eq(OrderStatus.ACCEPTED))).willReturn(
				mockResponse);

			// when
			ResultActions resultActions = mockMvc.perform(
				patch("/{orderId}/status", orderId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)));

			// then
			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(
					jsonPath("$.code").value(OrderSuccessStatus.ORDER_STATUS_UPDATED.getCode()))
				.andExpect(jsonPath("$.result.orderId").value(orderId.toString()))
				.andExpect(jsonPath("$.result.updatedStatus").value("ACCEPTED"))
				.andDo(print());
		}

		@Test
		@DisplayName("실패(유효성 검증): 주문 상태(newStatus)가 누락된 요청은 400 Bad Request를 반환한다.")
		void updateOrderStatus_Fail_Validation() throws Exception {
			// given
			UUID orderId = UUID.randomUUID();
			UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
			request.setNewStatus(null);

			// when
			ResultActions resultActions = mockMvc.perform(
				patch("/{orderId}/status", orderId)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)));

			// then
			resultActions
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value(ErrorStatus._BAD_REQUEST.getCode()))
				.andExpect(jsonPath("$.result.newStatus").exists())
				.andDo(print());
		}

	}

	@Nested
	@DisplayName("고객 주문 내역 조회 API 테스트")
	class GetCustomerOrdersTest {

		@Test
		@DisplayName("성공: 고객의 주문 내역을 성공적으로 조회한다.")
		void getCustomerOrders_Success() throws Exception {
			// given
			List<OrderResponse> mockResponse = List.of(mock(OrderResponse.class),
				mock(OrderResponse.class));
			given(orderService.getCustomerOrders(any())).willReturn(mockResponse);

			// when
			ResultActions resultActions = mockMvc.perform(get("")
				.with(csrf()));

			// then
			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value(OrderSuccessStatus.ORDER_FETCHED.getCode()))
				.andExpect(jsonPath("$.result").isArray())
				.andExpect(jsonPath("$.result.length()").value(2));

			verify(orderService).getCustomerOrders(any());
		}

		@Test
		@DisplayName("실패: 주문 내역이 없는 경우 예외를 반환한다.")
		void getCustomerOrders_NotFound() throws Exception {
			// given
			given(orderService.getCustomerOrders(any())).willThrow(
				new GeneralException(ErrorStatus.ORDER_NOT_FOUND));

			// when
			ResultActions resultActions = mockMvc.perform(get("")
				.with(csrf()));

			// then
			resultActions
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value(ErrorStatus.ORDER_NOT_FOUND.getCode()));

			verify(orderService).getCustomerOrders(any());
		}
	}

	@Nested
	@DisplayName("선택한 사용자 주문내역 조회 테스트")
	class GetCustomerOrderListByIdTest {

		@Test
		@DisplayName("성공: 특정 사용자의 주문 내역을 페이지로 성공적으로 조회한다.")
		void getCustomerOrderListById_Success() throws Exception {
			// given
			Long userId = 1L;
			OrderDetailResponse orderDetailResponse = new OrderDetailResponse(
				List.of(new OrderDetailResponse.Menu("후라이드 치킨", 2, 18000L)),
				36000L,
				"서울시 강남구",
				PaymentMethod.CREDIT_CARD,
				OrderChannel.ONLINE,
				ReceiptMethod.DELIVERY,
				OrderStatus.PENDING,
				"문 앞에 놓아주세요"
			);
			Page<OrderDetailResponse> page = new PageImpl<>(List.of(orderDetailResponse));
			PagedResponse<OrderDetailResponse> mockResponse = PagedResponse.from(page);

			given(orderService.getCustomerOrderListById(eq(userId), any(Pageable.class))).willReturn(
				mockResponse);

			// when
			ResultActions resultActions = mockMvc.perform(get("/{userId}/order", userId)
				.with(csrf()));

			// then
			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value(
					OrderSuccessStatus.MANAGER_GET_CUSTOMER_ORDER_OK.getCode()))
				.andExpect(jsonPath("$.result.content").isArray())
				.andExpect(jsonPath("$.result.content.length()").value(1));

			verify(orderService).getCustomerOrderListById(eq(userId), any(Pageable.class));
		}
	}
}