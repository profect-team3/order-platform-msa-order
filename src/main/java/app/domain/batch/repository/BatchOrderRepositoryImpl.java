package app.domain.batch.repository;

import app.domain.batch.dto.OrderBatchDto;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.QOrders;
import app.domain.order.model.repository.OrderItemRepository;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BatchOrderRepositoryImpl implements BatchOrderRepository {

    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;
    private final OrderItemRepository orderItemRepository;

    @Override
    public List<OrderBatchDto> findOrdersWithCursor(UUID lastOrderId, int limit) {
        QOrders orders = QOrders.orders;

        List<Orders> fetchedOrders = queryFactory.selectFrom(orders)
                .where(lastOrderId == null ? Expressions.TRUE : orders.ordersId.gt(lastOrderId))
                .orderBy(orders.ordersId.asc())
                .limit(limit)
                .fetch();

        if (fetchedOrders.isEmpty()) {
            return List.of();
        }

        List<UUID> orderIds = fetchedOrders.stream()
                .map(Orders::getOrdersId)
                .collect(Collectors.toList());

        Map<UUID, List<app.domain.order.model.entity.OrderItem>> orderItemsMap = orderItemRepository.findByOrders_OrdersIdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getOrders().getOrdersId()));

        return fetchedOrders.stream()
                .map(order -> new OrderBatchDto(order, orderItemsMap.getOrDefault(order.getOrdersId(), List.of())))
                .collect(Collectors.toList());
    }
}