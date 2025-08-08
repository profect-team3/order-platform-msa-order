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
    public ResponseEntity<Boolean> isOrderExists(@PathVariable UUID orderId) {
        Boolean exists = internalPaymentService.isOrderExists(orderId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("internal/order/{orderId}")
    public ResponseEntity<OrderInfo> getOrderInfo(@PathVariable UUID orderId) {
        OrderInfo orderInfo = internalPaymentService.getOrderInfo(orderId);
        return ResponseEntity.ok(orderInfo);
    }

    @PatchMapping("internal/order/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable UUID orderId, @RequestBody String orderStatus) {
        internalPaymentService.updateOrderStatus(orderId, orderStatus);
        return ResponseEntity.ok().build();
    }

    @PostMapping("internal/order/{orderId}/history")
    public ResponseEntity<Void> addOrderHistory(@PathVariable UUID orderId, @RequestBody String orderState) {
        internalPaymentService.addHistory(orderId, orderState);
        return ResponseEntity.ok().build();
    }
}