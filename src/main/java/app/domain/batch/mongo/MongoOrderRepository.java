package app.domain.batch.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import app.domain.batch.mongo.entity.MongoOrder;

import java.util.List;

public interface MongoOrderRepository extends MongoRepository<MongoOrder, String> {
    List<MongoOrder> findByStoreId(String storeId);
}
