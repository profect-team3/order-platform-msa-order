package app.domain.batch.job;

import app.domain.batch.mongo.entity.MongoOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
@RequiredArgsConstructor
public class OrderBatchWriter implements ItemWriter<MongoOrder> {

    private final MongoTemplate mongoTemplate;

    @Override
    public void write(Chunk<? extends MongoOrder> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        String collectionName = "mongo_orders";
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, collectionName);

        for (MongoOrder item : chunk) {
            Query query = new Query(where("ordersId").is(item.getOrdersId()));

            Update update = new Update();
            update.set("storeId", item.getStoreId());
            update.set("userId", item.getUserId());
            update.set("totalPrice", item.getTotalPrice());
            update.set("deliveryAddress", item.getDeliveryAddress());
            update.set("paymentMethod", item.getPaymentMethod());
            update.set("orderChannel", item.getOrderChannel());
            update.set("receiptMethod", item.getReceiptMethod());
            update.set("orderStatus", item.getOrderStatus());
            update.set("isRefundable", item.isRefundable());
            update.set("orderHistory", item.getOrderHistory());
            update.set("requestMessage", item.getRequestMessage());
            update.set("orderItems", item.getOrderItems());

            // Optimistic locking: increment version
            update.inc("version", 1);

            bulkOps.upsert(query, update);
        }

        bulkOps.execute();
    }
}
