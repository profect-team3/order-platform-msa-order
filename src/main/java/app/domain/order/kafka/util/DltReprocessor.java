package app.domain.order.kafka.util;

import static org.springframework.kafka.support.KafkaHeaders.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DltReprocessor {

	private final KafkaTemplate<String, String> kafka;
	private final AlertNotifier notifier;

	private static final String RETRY_HEADER = "x-retry-attempt";
	private static final int MAX_RETRY_FROM_DLT = 3;

	@KafkaListener(topics = {
		"${topics.order.validated}.DLT",
		"${topics.payment.result}.DLT",
		"${topics.stock.result}.DLT",
		"${topics.order.approve}.DLT"
	}, groupId = "replayer-from-dlt")
	public void reprocess(ConsumerRecord<String, String> rec) {

		var origTopicHeader = rec.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC);
		if (origTopicHeader == null) {
			log.error("Missing DLT_ORIGINAL_TOPIC header. key={}, offset={}", rec.key(), rec.offset());
			return;
		}
		String origTopic = new String(origTopicHeader.value(), StandardCharsets.UTF_8);

		int attempt = Optional.ofNullable(rec.headers().lastHeader(RETRY_HEADER))
			.map(h -> Integer.parseInt(new String(h.value(), StandardCharsets.UTF_8)))
			.orElse(0);

		if (attempt >= MAX_RETRY_FROM_DLT) {
			String exClass = getHeader(rec, KafkaHeaders.DLT_EXCEPTION_FQCN);
			String exMsg   = getHeader(rec, KafkaHeaders.DLT_EXCEPTION_MESSAGE);
			String orderId = getHeader(rec, "orderId");
			System.out.println(rec.partition()+"fdfasdfadfadfa"+rec.offset());
			Integer oPart = getIntHeader(rec, DLT_ORIGINAL_PARTITION, ORIGINAL_PARTITION);
			Long    oOff  = getLongHeader(rec, DLT_ORIGINAL_OFFSET,    ORIGINAL_OFFSET);

			String originalPOff = (oPart == null ? "-" : oPart.toString())
				+ " / "
				+ (oOff  == null ? "-" : oOff.toString());


			String desc = pretty(rec.value());
			Map<String, String> fields = new LinkedHashMap<>();
			fields.put("Topic", origTopic);
			fields.put("Key", String.valueOf(rec.key()));
			fields.put("OrderId", emptyDash(orderId));
			fields.put("Original P/Off", originalPOff);
			fields.put("DLT Partition/Offset", rec.partition() + " / " + rec.offset());
			fields.put("Attempts (DLT)", attempt + " / " + MAX_RETRY_FROM_DLT);
			fields.put("Exception", emptyDash(exClass));
			fields.put("Message", emptyDash(exMsg));

			notifier.sendEmbed("Kafka DLT 경고", desc, fields, 0xE74C3C);
			return;
		}

		ProducerRecord<String, String> retry = new ProducerRecord<>(origTopic, rec.value());
		copyAllHeaders(rec, retry, Set.of(RETRY_HEADER));
		retry.headers().remove(RETRY_HEADER);
		retry.headers().add(RETRY_HEADER, Integer.toString(attempt + 1).getBytes(StandardCharsets.UTF_8));

		kafka.send(retry);
		log.info("DLT 재시도 {}/{} -> {} key={}", attempt + 1, MAX_RETRY_FROM_DLT, origTopic, rec.key());
	}

	private static String getHeader(ConsumerRecord<String, String> rec, String name) {
		var h = rec.headers().lastHeader(name);
		return (h == null) ? null : new String(h.value(), StandardCharsets.UTF_8);
	}

	private static String emptyDash(String s) {
		return (s == null || s.isBlank()) ? "-" : s;
	}

	private static String pretty(String s) {
		if (s == null) return "";
		return s.length() > 3500 ? s.substring(0, 3497) + "..." : s;
	}

	private static void copyAllHeaders(ConsumerRecord<String, String> src,
		ProducerRecord<String, String> dst,
		Set<String> exclude) {
		src.headers().forEach(h -> {
			if (exclude != null && exclude.contains(h.key())) return;
			dst.headers().add(h.key(), h.value());
		});
	}



	private static Integer getIntHeader(ConsumerRecord<?, ?> rec, String... names) {
		for (String n : names) {
			var h = rec.headers().lastHeader(n);
			if (h != null) {
				var bb = ByteBuffer.wrap(h.value());
				return bb.remaining() >= Integer.BYTES ? bb.getInt() : null;
			}
		}
		return null;
	}

	private static Long getLongHeader(ConsumerRecord<?, ?> rec, String... names) {
		for (String n : names) {
			var h = rec.headers().lastHeader(n);
			if (h != null) {
				var bb = ByteBuffer.wrap(h.value());
				return bb.remaining() >= Long.BYTES ? bb.getLong() : null;
			}
		}
		return null;
	}
}