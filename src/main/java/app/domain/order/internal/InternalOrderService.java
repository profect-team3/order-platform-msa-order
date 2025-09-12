package app.domain.order.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.commonUtil.apiPayload.code.status.ErrorStatus;
import app.commonUtil.apiPayload.exception.GeneralException;
import app.domain.order.model.dto.response.OrderInfoResponse;
import app.domain.order.model.dto.response.StoreOrderInfo;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.repository.OrdersRepository;
import app.domain.order.status.OrderErrorStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalOrderService {

    private final OrdersRepository ordersRepository;


    public List<StoreOrderInfo> getOrdersByStoreId(UUID storeId){
        List<Orders> ordersList= ordersRepository.findByStoreId(storeId);
        if(ordersList.size()==0)
            throw new GeneralException(OrderErrorStatus.ORDER_STORE_NOT_FOUND);
        return ordersList.stream()
            .map(order -> new StoreOrderInfo(
                order.getOrdersId(),
                order.getStoreId(),
                order.getUserId(),
                order.getTotalPrice(),
                String.valueOf(order.getOrderStatus()),
                order.getCreatedAt()
            ))
            .toList();

    }
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

        if(!order.getOrderStatus().equals(OrderStatus.ACCEPTED_READY)){
            throw new GeneralException(OrderErrorStatus.INVALID_ORDER_REQUEST);
        }

        return new OrderInfoResponse(
            order.getOrdersId(),
            order.getStoreId(),
            order.getUserId(),
            order.getTotalPrice(),
            String.valueOf(order.getOrderStatus()),
            order.getCreatedAt(),
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