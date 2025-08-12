package app.domain.order.internal;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.domain.order.model.dto.response.OrderInfoResponse;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.repository.OrdersRepository;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalOrderService {

    private final OrdersRepository ordersRepository;

    public Boolean isOrderExists(UUID orderId) {
        boolean exists = ordersRepository.existsById(orderId);
        if (!exists) {
            throw new GeneralException(ErrorStatus.ORDER_NOT_FOUND);
        }
        return true;
    }

    public OrderInfoResponse getOrderInfo(UUID orderId) {
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
        
        return new OrderInfoResponse(
            order.getTotalPrice(),
            order.getPaymentMethod().name(),
            order.isRefundable()
        );
    }

    @Transactional
    public String  updateOrderStatus(UUID orderId, String orderStatus) {
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
        
        OrderStatus status = OrderStatus.valueOf(orderStatus);
        order.updateOrderStatus(status);
        return "주문 상태를 수정했습니다.";
    }

    @Transactional
    public String addHistory(UUID orderId, String orderState) {
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
        
        order.addHistory(orderState, LocalDateTime.now());
        return "주문 history를 추가했습니다";
    }
}