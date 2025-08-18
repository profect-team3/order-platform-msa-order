package app.domain.batch.mongo.entity;

import app.domain.order.model.entity.enums.OrderChannel;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.entity.enums.PaymentMethod;
import app.domain.order.model.entity.enums.ReceiptMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document(collection = "mongo_orders")
@Getter
@NoArgsConstructor
@Builder
public class MongoOrder {

    @Id
    private String id;

    private String ordersId;
    private String storeId;
    private Long userId;
    private Long totalPrice;
    private String deliveryAddress;
    private PaymentMethod paymentMethod;
    private OrderChannel orderChannel;
    private ReceiptMethod receiptMethod;
    private OrderStatus orderStatus;
    private boolean isRefundable;
    private String orderHistory;
    private String requestMessage;
    private List<MongoOrderItem> orderItems;
    private Long version;

    public MongoOrder(String id, String ordersId, String storeId, Long userId, Long totalPrice, String deliveryAddress, PaymentMethod paymentMethod, OrderChannel orderChannel, ReceiptMethod receiptMethod, OrderStatus orderStatus, boolean isRefundable, String orderHistory, String requestMessage, List<MongoOrderItem> orderItems, Long version) {
        this.id = id;
        this.ordersId = ordersId;
        this.storeId = storeId;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
        this.orderChannel = orderChannel;
        this.receiptMethod = receiptMethod;
        this.orderStatus = orderStatus;
        this.isRefundable = isRefundable;
        this.orderHistory = orderHistory;
        this.requestMessage = requestMessage;
        this.orderItems = orderItems;
        this.version = version;
    }
}
