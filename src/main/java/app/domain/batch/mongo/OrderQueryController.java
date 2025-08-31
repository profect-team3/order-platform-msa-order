package app.domain.batch.mongo;

import app.domain.batch.mongo.entity.MongoOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderQueryController {

    private final MongoOrderRepository mongoOrderRepository;

    @GetMapping("/store/{storeId}")
    public List<MongoOrder> getOrdersByStoreId(@PathVariable String storeId) {
        return mongoOrderRepository.findByStoreId(storeId);
    }
}
