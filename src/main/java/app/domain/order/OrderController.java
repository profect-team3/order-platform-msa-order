package app.domain.order;

import static org.springframework.data.domain.Sort.Direction.*;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.domain.order.model.dto.request.CreateOrderRequest;
import app.domain.order.model.dto.request.UpdateOrderStatusRequest;
import app.domain.order.model.dto.response.OrderDetailResponse;
import app.domain.order.model.dto.response.OrderResponse;
import app.domain.order.model.dto.response.UpdateOrderStatusResponse;
import app.domain.order.service.OrderService;
import app.domain.order.status.OrderSuccessStatus;
import app.global.apiPayload.ApiResponse;
import app.global.apiPayload.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "order", description = "주문 관련 API")
@RestController
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@Operation(summary = "주문 생성 API", description = "사용자의 장바구니를 기반으로 주문을 생성합니다.")
	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	public ApiResponse<UUID> createOrder(
		@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
		UUID orderId = orderService.createOrder(authentication,request);
		return ApiResponse.onSuccess(OrderSuccessStatus.ORDER_CREATED, orderId);
	}

	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "주문 상세 조회 API", description = "주문 ID로 주문 상세 정보를 조회합니다.")
	@GetMapping("/{orderId}")
	public ApiResponse<OrderDetailResponse> getOrderDetail(@PathVariable UUID orderId) {
		OrderDetailResponse result = orderService.getOrderDetail(orderId);
		return ApiResponse.onSuccess(OrderSuccessStatus.ORDER_DETAIL_FETCHED, result);
	}


	@Operation(
		summary = "선택한 사용자 주문내역 조회",
		description = "선택한 사용자의 주문 정보를 확인 합니다."
	)
	@GetMapping("/{userId}/order")
	@PreAuthorize("hasRole('OWNER')")
	public ApiResponse<PagedResponse<OrderDetailResponse>> getCustomerOrderListById(
		@PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable,
		@PathVariable("userId") Long userId
	) {
		return ApiResponse.onSuccess(OrderSuccessStatus.MANAGER_GET_CUSTOMER_ORDER_OK,orderService.getCustomerOrderListById(userId, pageable));
	}


	@PreAuthorize("hasRole('OWNER')")
	@Operation(summary = "주문 상태 변경 API", description = "주문 ID로 주문 상태를 변경합니다.")
	@PatchMapping("/{orderId}/status")
	public ApiResponse<UpdateOrderStatusResponse> updateOrderStatus(
		@PathVariable UUID orderId,
		@Valid @RequestBody UpdateOrderStatusRequest request, Authentication authentication
	) {
		UpdateOrderStatusResponse response = orderService.updateOrderStatus(orderId, request.getNewStatus());
		return ApiResponse.onSuccess(OrderSuccessStatus.ORDER_STATUS_UPDATED, response);
	}

	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "고객 주문 내역 조회 API", description = "자신의 모든 주문 내역을 조회합니다.")
	@GetMapping
	public ApiResponse<List<OrderResponse>> getCustomerOrders( Authentication authentication
	) {
		return ApiResponse.onSuccess(OrderSuccessStatus.ORDER_FETCHED, orderService.getCustomerOrders(authentication));
	}
}