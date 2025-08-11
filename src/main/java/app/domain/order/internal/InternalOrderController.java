package app.domain.order.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import app.domain.order.model.dto.response.OrderInfoResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InternalOrderController {

    private final InternalOrderService internalOrderService;

    @GetMapping("internal/order/{orderId}/exists")
    public Boolean isOrderExists(@PathVariable UUID orderId) {
        Boolean exists = internalOrderService.isOrderExists(orderId);
        return exists;
    }

    @GetMapping("internal/order/{orderId}")
    public OrderInfoResponse getOrderInfo(@PathVariable UUID orderId) {
        OrderInfoResponse orderInfoResponse = internalOrderService.getOrderInfo(orderId);
        return orderInfoResponse;
    }

    @PostMapping("internal/order/{orderId}/status")
    public void updateOrderStatus(@PathVariable UUID orderId, @RequestBody String orderStatus) {
        internalOrderService.updateOrderStatus(orderId, orderStatus);
    }

    @PostMapping("internal/order/{orderId}/history")
    public void addOrderHistory(@PathVariable UUID orderId, @RequestBody String orderState) {
        internalOrderService.addHistory(orderId, orderState);
    }
}