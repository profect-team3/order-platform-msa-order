package app.domain.order;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import app.domain.order.model.dto.response.OrderInfo;
import app.domain.order.service.InternalOrderService;
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
    public OrderInfo getOrderInfo(@PathVariable UUID orderId) {
        OrderInfo orderInfo = internalOrderService.getOrderInfo(orderId);
        return orderInfo;
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