package app.global.config;

import java.net.SocketTimeoutException;

import app.commonUtil.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaErrorConfig {

	private final KafkaTemplate<String, String> template;

	@Bean
	public DefaultErrorHandler defaultErrorHandler() {
		var backoff = new ExponentialBackOffWithMaxRetries(3);
		backoff.setInitialInterval(200);
		backoff.setMultiplier(2.0);
		backoff.setMaxInterval(5000);


		var recoverer = new DeadLetterPublishingRecoverer(
			template,
			(record, ex) -> {
			 return new TopicPartition(record.topic() + ".DLT", record.partition());
			}
		);
		var handler = new DefaultErrorHandler(recoverer, backoff);

		handler.addNotRetryableExceptions(
			IllegalArgumentException.class,
			GeneralException.class
		);

		handler.addRetryableExceptions(SocketTimeoutException.class);

		handler.setRetryListeners((rec, ex, deliveryAttempt) ->
			log.warn("Retry {}/3 topic={} key={} err={}",
				deliveryAttempt, rec.topic(), rec.key(), ex.toString())
		);

		return handler;
	}

}