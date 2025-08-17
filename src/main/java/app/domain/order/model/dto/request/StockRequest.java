package app.domain.order.model.dto.request;

import java.util.UUID;

import app.domain.cart.model.dto.RedisCartItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockRequest {
    private UUID menuId;
    private int quantity;

    public static StockRequest from(RedisCartItem cartItem) {
        return StockRequest.builder()
            .menuId(cartItem.getMenuId())
            .quantity(cartItem.getQuantity())
            .build();
    }
}