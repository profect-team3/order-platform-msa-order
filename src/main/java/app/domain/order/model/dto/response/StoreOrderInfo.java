package app.domain.order.model.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreOrderInfo {
    private UUID orderId;
    private UUID storeId;
    private Long customerId;
    private Long totalPrice;
    private String orderStatus;
    private LocalDateTime orderedAt;
}
