package app.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	@Value("${kafka.topic.partitions:3}")
	private int PARTITIONS;

	@Value("${kafka.topic.rf:1}")
	private short RF;

	private static final String RET_3D  = String.valueOf(3L  * 24 * 60 * 60 * 1000);
	private static final String RET_14D = String.valueOf(14L * 24 * 60 * 60 * 1000);
	@Value("${topics.order.create_requested}") private String tOrderCreateRequested;
	@Value("${topics.order.validated}")        private String tOrderValidated;
	@Value("${topics.order.canceled}")         private String tOrderCanceled;
	@Value("${topics.order.approve}")          private String tOrderApprove;
	@Value("${topics.order.completed}")        private String tOrderCompleted;
	@Value("${topics.payment.result}")         private String tPaymentResult;
	@Value("${topics.stock.request}")          private String tStockRequest;
	@Value("${topics.stock.result}")           private String tStockResult;


	@Bean NewTopic orderCreateRequested() { return base(tOrderCreateRequested); }
	@Bean NewTopic orderCreateRequestedDLT() { return dlt(tOrderCreateRequested); }

	@Bean NewTopic orderValidated() { return base(tOrderValidated); }
	@Bean NewTopic orderValidatedDLT() { return dlt(tOrderValidated); }

	@Bean NewTopic orderCanceled() { return base(tOrderCanceled); }
	@Bean NewTopic orderCanceledDLT() { return dlt(tOrderCanceled); }

	@Bean NewTopic orderApprove() { return base(tOrderApprove); }
	@Bean NewTopic orderApproveDLT() { return dlt(tOrderApprove); }

	@Bean NewTopic orderCompleted() { return base(tOrderCompleted); }
	@Bean NewTopic orderCompletedDLT() { return dlt(tOrderCompleted); }

	@Bean NewTopic paymentResult() { return base(tPaymentResult); }
	@Bean NewTopic paymentResultDLT() { return dlt(tPaymentResult); }

	@Bean NewTopic stockRequest() { return base(tStockRequest); }
	@Bean NewTopic stockRequestDLT() { return dlt(tStockRequest); }

	@Bean NewTopic stockResult() { return base(tStockResult); }
	@Bean NewTopic stockResultDLT() { return dlt(tStockResult); }

	private NewTopic base(String name) {
		return TopicBuilder.name(name)
			.partitions(PARTITIONS)
			.replicas(RF)
			.config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
			.config(TopicConfig.RETENTION_MS_CONFIG, RET_3D)
			.build();
	}

	private NewTopic dlt(String name) {
		return TopicBuilder.name(name + ".DLT")
			.partitions(PARTITIONS)
			.replicas(RF)
			.config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
			.config(TopicConfig.RETENTION_MS_CONFIG, RET_14D)
			.build();
	}
}