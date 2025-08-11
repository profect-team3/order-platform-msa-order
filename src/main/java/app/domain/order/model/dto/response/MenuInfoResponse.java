package app.domain.order.model.dto.response;

import java.util.UUID;
import lombok.Getter;

@Getter
public class MenuInfoResponse {
    private UUID menuId;
    private String name;
    private Long price;
}