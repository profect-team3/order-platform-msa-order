package app.domain.batch.job;

import app.domain.batch.dto.OrderBatchDto;
import app.domain.batch.repository.BatchOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderBatchReader implements ItemReader<OrderBatchDto> {

    private final BatchOrderRepository batchOrderRepository;
    private Iterator<OrderBatchDto> orderIterator;
    private UUID lastOrderId = null;
    private boolean initialized = false;
    private final int batchSize = 100;

    @Override
    public OrderBatchDto read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!initialized) {
            initialize();
        }

        if (orderIterator != null && orderIterator.hasNext()) {
            OrderBatchDto current = orderIterator.next();
            lastOrderId = current.getOrdersId();
            return current;
        }

        List<OrderBatchDto> nextBatch = loadNextBatch();
        if (!nextBatch.isEmpty()) {
            orderIterator = nextBatch.iterator();
            OrderBatchDto current = orderIterator.next();
            lastOrderId = current.getOrdersId();
            return current;
        }

        return null;
    }

    private void initialize() {
        lastOrderId = null; // Start from the beginning
        List<OrderBatchDto> firstBatch = loadNextBatch();
        if (!firstBatch.isEmpty()) {
            orderIterator = firstBatch.iterator();
        }
        initialized = true;
    }

    private List<OrderBatchDto> loadNextBatch() {
        return batchOrderRepository.findOrdersWithCursor(lastOrderId, batchSize);
    }

    public void reset() {
        initialized = false;
        lastOrderId = null;
        orderIterator = null;
    }
}
