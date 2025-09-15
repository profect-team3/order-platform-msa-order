package app.domain.order.kafka.util;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.domain.order.kafka.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxRequeueWorker {
    private final OutboxRepository outboxRepository;

    @Transactional
    public int requeueOnce(Duration olderThan) {
        var now = LocalDateTime.now();
        var retryBefore = now.minus(olderThan);
        return outboxRepository.requeueFailed(now, retryBefore);
    }
}