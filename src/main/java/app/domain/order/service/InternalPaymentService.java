package app.domain.order.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.domain.order.model.dto.response.OrderInfo;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.repository.OrdersRepository;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalPaymentService {

    private final OrdersRepository ordersRepository;

    public Boolean isOrderExists(UUID orderId) {
        return ordersRepository.existsById(orderId);
    }

    public OrderInfo getOrderInfo(UUID orderId) {
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
        
        return new OrderInfo(
            order.getTotalPrice(),
            order.getPaymentMethod().name(),
            order.isRefundable()
        );
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, String orderStatus) {
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
        
        OrderStatus status = OrderStatus.valueOf(orderStatus);
        order.updateOrderStatus(status);
    }

    @Transactional
    public void addHistory(UUID orderId, String orderState) {
        Orders order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
        
        order.addHistory(orderState, LocalDateTime.now());
    }
}