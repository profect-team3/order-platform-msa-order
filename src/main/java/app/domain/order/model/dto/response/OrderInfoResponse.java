package app.domain.order.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderInfoResponse {
    private Long totalPrice;
    private String paymentMethod;
    private Boolean isRefundable;
}