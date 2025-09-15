package app.domain.order.kafka;

import app.domain.order.kafka.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import net.bytebuddy.asm.Advice;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, String> kafka;

	@Value("${outbox.relay.batch-size:200}")
	private int batchSize;
	//짧은 주기로 반복
	@Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:500}")
	@Transactional
	public void relay() {
		List<Outbox> batch = outboxRepository.fetchPendingForUpdate(batchSize);
		if (batch.isEmpty()) return;

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		for (Outbox e : batch) {
			try {
				ProducerRecord<String, String> record =
					new ProducerRecord<>(e.getTopic(), e.getPayloadJson());

				record.headers().add(new RecordHeader("orderId", e.getAggregateId().getBytes()));

				kafka.send(record).get();

				outboxRepository.markSent(e.getId(), now);

			} catch (Exception ex) {
				String err = abbreviate(ex.toString(), 480);
				log.error("Outbox relay failed id={} topic={} err={}", e.getId(), e.getTopic(), err);
				outboxRepository.markFailed(e.getId(), now, err);
			}
		}
	}

	private static String abbreviate(String s, int max) {
		return (s == null || s.length() <= max) ? s : s.substring(0, max);
	}
}