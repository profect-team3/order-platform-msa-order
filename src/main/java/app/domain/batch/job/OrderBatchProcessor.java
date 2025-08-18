package app.domain.batch.job;

import app.domain.batch.dto.OrderBatchDto;
import app.domain.batch.mongo.entity.MongoOrder;
import app.domain.batch.mongo.entity.MongoOrderItem;
import app.domain.order.model.entity.enums.OrderChannel;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.entity.enums.PaymentMethod;
import app.domain.order.model.entity.enums.ReceiptMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderBatchProcessor implements ItemProcessor<OrderBatchDto, MongoOrder> {

    @Override
    public MongoOrder process(OrderBatchDto dto) throws Exception {
        List<MongoOrderItem> mongoOrderItems = dto.getOrderItems().stream()
                .map(orderItem -> MongoOrderItem.builder()
                        .orderItemId(orderItem.getOrderItemId())
                        .menuName(orderItem.getMenuName())
                        .price(orderItem.getPrice())
                        .quantity(orderItem.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return MongoOrder.builder()
                .ordersId(dto.getOrdersId().toString())
                .storeId(dto.getStoreId().toString())
                .userId(dto.getUserId())
                .totalPrice(dto.getTotalPrice())
                .deliveryAddress(dto.getDeliveryAddress())
                .paymentMethod(PaymentMethod.valueOf(dto.getPaymentMethod()))
                .orderChannel(OrderChannel.valueOf(dto.getOrderChannel()))
                .receiptMethod(ReceiptMethod.valueOf(dto.getReceiptMethod()))
                .orderStatus(OrderStatus.valueOf(dto.getOrderStatus()))
                .isRefundable(dto.isRefundable())
                .orderHistory(dto.getOrderHistory())
                .requestMessage(dto.getRequestMessage())
                .orderItems(mongoOrderItems)
                .version(0L) // Initialize version for new documents
                .build();
    }
}
