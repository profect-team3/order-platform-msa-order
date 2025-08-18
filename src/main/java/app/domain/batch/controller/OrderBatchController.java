package app.domain.batch.controller;

import app.domain.batch.service.OrderBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/batch")
public class OrderBatchController {

    private final OrderBatchService orderBatchService;

    @PostMapping("/orders")
    public String triggerOrderMigration() {
        orderBatchService.migrateOrdersToMongo();
        return "Order migration triggered successfully!";
    }
}
