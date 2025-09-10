package app.global.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
@RequiredArgsConstructor
public class KafkaErrorConfig {

	private final KafkaTemplate<Object, Object> template;

	@Bean
	public DefaultErrorHandler defaultErrorHandler() {
		// 지수 백오프: 200ms → 5s, 최대 3회 재시도
		ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(3);
		backoff.setInitialInterval(200);
		backoff.setMultiplier(2.0);
		backoff.setMaxInterval(5000);

		// DLT 라우팅: <원토픽>.DLT
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
			template,
			(record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())
		);

		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);

		// 재시도해도 의미 없는 예외는 즉시 DLT
		handler.addNotRetryableExceptions(
			IllegalArgumentException.class
		);

		return handler;
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
		ConsumerFactory<Object, Object> cf) {
		var f = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
		f.setConsumerFactory(cf);
		f.setCommonErrorHandler(defaultErrorHandler());
		return f;
	}
}