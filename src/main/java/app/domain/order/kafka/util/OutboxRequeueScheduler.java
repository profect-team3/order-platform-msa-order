package app.domain.order.kafka.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;


@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRequeueScheduler {
    private final OutboxRequeueWorker worker;

    @Value("${outbox.requeue.older-than:PT5M}")
    private Duration olderThan;

    @Scheduled(fixedDelayString = "${outbox.requeue.interval:30s}")
    public void requeueScheduler() {
        int n = worker.requeueOnce(olderThan);
        if (n > 0) log.info("[Outbox] requeued FAILED -> PENDING: {} rows", n);
    }
}