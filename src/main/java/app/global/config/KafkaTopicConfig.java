package app.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.Map;

import app.domain.order.kafka.dto.WorkflowEvent;

@Configuration
public class KafkaTopicConfig {

	// 기본 값: 파티션 12, 복제 3, 리텐션 3일
	private static final int PARTITIONS = 12;
	private static final short RF = 3;
	private static final String RET_3D = String.valueOf(3L * 24 * 60 * 60 * 1000);

	@Value("${topics.order.create_requested}") private String tRequested;
	@Value("${topics.order.validated}") private String tValidated;

	@Bean
	public NewTopic orderCreateRequestedTopic() {
		return new NewTopic(tRequested, PARTITIONS, RF)
			.configs(Map.of(
				TopicConfig.RETENTION_MS_CONFIG, RET_3D,
				TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE
			));
	}

	@Bean
	public NewTopic orderValidatedTopic() {
		return new NewTopic(tValidated, PARTITIONS, RF)
			.configs(Map.of(
				TopicConfig.RETENTION_MS_CONFIG, RET_3D,
				TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE
			));
	}

	// ---- DLT (DLQ) 토픽들: 리텐션 더 길게 (예: 14일) ----
	@Bean
	public NewTopic orderCreateRequestedDLT() {
		return new NewTopic(tRequested + ".DLT", PARTITIONS, RF)
			.configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(14L * 24 * 60 * 60 * 1000)));
	}

	@Bean
	public NewTopic orderValidatedDLT() {
		return new NewTopic(tValidated + ".DLT", PARTITIONS, RF)
			.configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(14L * 24 * 60 * 60 * 1000)));
	}

	// @Bean
	// public ConcurrentKafkaListenerContainerFactory<String, WorkflowEvent> workflowEventListenerFactory(
	// 	ConsumerFactory<String, WorkflowEvent> cf) {
	// 	var factory = new ConcurrentKafkaListenerContainerFactory<String, WorkflowEvent>();
	// 	factory.setConsumerFactory(cf);
	// 	return factory;
	// }



}