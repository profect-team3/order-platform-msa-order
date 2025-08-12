package app.domain.order.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import app.domain.order.model.dto.response.OrderInfoResponse;
import app.domain.order.status.OrderSuccessStatus;
import app.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InternalOrderController {

    private final InternalOrderService internalOrderService;

    @GetMapping("internal/order/{orderId}/exists")
    public ApiResponse<Boolean> isOrderExists(@PathVariable UUID orderId) {
        Boolean exists = internalOrderService.isOrderExists(orderId);
        return ApiResponse.onSuccess(OrderSuccessStatus.ORDER_EXISTS,exists);
    }

    @GetMapping("internal/order/{orderId}")
    public ApiResponse<OrderInfoResponse> getOrderInfo(@PathVariable UUID orderId) {
        OrderInfoResponse orderInfoResponse = internalOrderService.getOrderInfo(orderId);
        return ApiResponse.onSuccess(OrderSuccessStatus.ORDER_FETCHED,orderInfoResponse);
    }

    @PostMapping("internal/order/{orderId}/status")
    public ApiResponse<String> updateOrderStatus(@PathVariable UUID orderId, @RequestBody String orderStatus) {
        String result=internalOrderService.updateOrderStatus(orderId, orderStatus);
        return ApiResponse.onSuccess(OrderSuccessStatus.ORDER_STATUS_UPDATED,result);
    }

    @PostMapping("internal/order/{orderId}/history")
    public ApiResponse<String> addOrderHistory(@PathVariable UUID orderId, @RequestBody String orderState) {
        String result= internalOrderService.addHistory(orderId, orderState);
        return ApiResponse.onSuccess(OrderSuccessStatus.ORDER_HISTORY_ADDED,result);
    }
}