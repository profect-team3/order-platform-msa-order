package app.domain.batch.mongo.entity;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MongoOrderItem {

    private UUID orderItemId;
    private String menuName;
    private Long price;
    private int quantity;

}
