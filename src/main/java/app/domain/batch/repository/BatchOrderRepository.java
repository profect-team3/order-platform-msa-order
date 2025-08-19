package app.domain.batch.repository;

import app.domain.batch.dto.OrderBatchDto;
import java.util.List;
import java.util.UUID;

public interface BatchOrderRepository {
    List<OrderBatchDto> findOrdersWithCursor(UUID lastOrderId, int limit);
}
