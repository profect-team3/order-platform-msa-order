package app.domain.batch.dto;

import app.domain.order.model.entity.OrderItem;
import app.domain.order.model.entity.Orders;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderBatchDto {
    private UUID ordersId;
    private UUID storeId;
    private Long userId;
    private Long totalPrice;
    private String deliveryAddress;
    private String paymentMethod;
    private String orderChannel;
    private String receiptMethod;
    private String orderStatus;
    private boolean isRefundable;
    private String orderHistory;
    private String requestMessage;
    private List<OrderItem> orderItems;

    public OrderBatchDto(Orders order, List<OrderItem> orderItems) {
        this.ordersId = order.getOrdersId();
        this.storeId = order.getStoreId();
        this.userId = order.getUserId();
        this.totalPrice = order.getTotalPrice();
        this.deliveryAddress = order.getDeliveryAddress();
        this.paymentMethod = order.getPaymentMethod().name();
        this.orderChannel = order.getOrderChannel().name();
        this.receiptMethod = order.getReceiptMethod().name();
        this.orderStatus = order.getOrderStatus().name();
        this.isRefundable = order.isRefundable();
        this.orderHistory = order.getOrderHistory();
        this.requestMessage = order.getRequestMessage();
        this.orderItems = orderItems;
    }
}
