package app.domain.order.model.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
public class MenuInfo {
    private UUID menuId;
    private String name;
    private Long price;
}