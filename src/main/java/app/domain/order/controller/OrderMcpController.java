package app.domain.order.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.commonUtil.apiPayload.ApiResponse;
import app.domain.order.model.dto.request.CreateOrderRequest;
import app.domain.order.service.OrderMcpService;
import app.domain.order.status.OrderSuccessStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mcp/order")
public class OrderMcpController {

	private final OrderMcpService orderMcpService;

	@PostMapping
	public ApiResponse<UUID> createOrder(
		@Valid @RequestBody CreateOrderRequest request, @RequestParam Long userId) {
		UUID orderId = orderMcpService.createOrder(userId,request);
		return ApiResponse.onSuccess(OrderSuccessStatus.ORDER_CREATED, orderId);
	}

}
