package app.domain.order.model.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class MenuInfoResponse {
    private UUID menuId;
    private String name;
    private Long price;

    public MenuInfoResponse(UUID menuId, String name, Long price) {
        this.menuId = menuId;
        this.name = name;
        this.price = price;
    }
}