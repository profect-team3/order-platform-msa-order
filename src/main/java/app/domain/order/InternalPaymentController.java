package app.domain.order;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import app.domain.order.model.dto.response.OrderInfo;
import app.domain.order.service.InternalPaymentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InternalPaymentController {

    private final InternalPaymentService internalPaymentService;

    @GetMapping("internal/order/{orderId}/exists")
    public Boolean isOrderExists(@PathVariable UUID orderId) {
        Boolean exists = internalPaymentService.isOrderExists(orderId);
        return exists;
    }

    @GetMapping("internal/order/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable UUID orderId) {
        OrderInfo orderInfo = internalPaymentService.getOrderInfo(orderId);
        return orderInfo;
    }

    @PatchMapping("internal/order/{orderId}/status")
    public void updateOrderStatus(@PathVariable UUID orderId, @RequestBody String orderStatus) {
        internalPaymentService.updateOrderStatus(orderId, orderStatus);
    }

    @PostMapping("internal/order/{orderId}/history")
    public void addOrderHistory(@PathVariable UUID orderId, @RequestBody String orderState) {
        internalPaymentService.addHistory(orderId, orderState);
    }
}