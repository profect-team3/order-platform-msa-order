package app.domain.order.service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RDelayedQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDelayService {

	private final RedissonClient redissonClient;

	@Value("${redis.refund.queue}")
	private String REFUND_DISABLE_QUEUE;

	public void scheduleRefundDisable(UUID orderId) {
		RQueue<UUID> queue = redissonClient.getQueue(REFUND_DISABLE_QUEUE);
		RDelayedQueue<UUID> delayedQueue = redissonClient.getDelayedQueue(queue);

		delayedQueue.offer(orderId, 5, TimeUnit.MINUTES);
	}
}