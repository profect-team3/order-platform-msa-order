package app.domain.order.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderInfoResponse {
    private UUID orderId;
    private UUID storeId;
    private Long customerId;
    private Long totalPrice;
    private String orderStatus;
    private LocalDateTime orderedAt;
    private String paymentMethod;
    private Boolean isRefundable;
}