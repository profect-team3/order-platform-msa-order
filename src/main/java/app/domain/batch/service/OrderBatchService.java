package app.domain.batch.service;

import app.domain.batch.mongo.MongoOrderRepository;
import app.domain.batch.mongo.entity.MongoOrder;
import app.domain.batch.mongo.entity.MongoOrderItem;
import app.domain.order.model.entity.OrderItem;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.repository.OrderItemRepository;
import app.domain.order.model.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderBatchService {

    private final OrdersRepository ordersRepository;
    private final MongoOrderRepository mongoOrderRepository;
    private final OrderItemRepository orderItemRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional(readOnly = true)
    public void migrateOrdersToMongo() {
        log.info("Order migration to MongoDB started.");

        List<Orders> orders = ordersRepository.findAll();
        List<MongoOrder> mongoOrders = orders.stream()
                .map(order -> {
                    List<OrderItem> orderItems = orderItemRepository.findByOrders(order);
                    List<MongoOrderItem> mongoOrderItems = orderItems.stream()
                            .map(orderItem -> MongoOrderItem.builder()
                                    .orderItemId(orderItem.getOrderItemId())
                                    .menuName(orderItem.getMenuName())
                                    .price(orderItem.getPrice())
                                    .quantity(orderItem.getQuantity())
                                    .build())
                            .collect(Collectors.toList());

                    return MongoOrder.builder()
                            .ordersId(order.getOrdersId().toString())
                            .storeId(order.getStoreId().toString())
                            .userId(order.getUserId())
                            .totalPrice(order.getTotalPrice())
                            .deliveryAddress(order.getDeliveryAddress())
                            .paymentMethod(order.getPaymentMethod())
                            .orderChannel(order.getOrderChannel())
                            .receiptMethod(order.getReceiptMethod())
                            .orderStatus(order.getOrderStatus())
                            .isRefundable(order.isRefundable())
                            .orderHistory(order.getOrderHistory())
                            .requestMessage(order.getRequestMessage())
                            .orderItems(mongoOrderItems)
                            .build();
                })
                .collect(Collectors.toList());

        mongoOrderRepository.saveAll(mongoOrders);

        log.info("Successfully migrated {} orders to MongoDB.", mongoOrders.size());
    }
}
