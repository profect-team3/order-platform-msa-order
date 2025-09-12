package app.domain.order.kafka;

import app.commonUtil.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class Outbox extends BaseEntity {


	@Id @GeneratedValue
	private UUID id;

	@Column(nullable = false, length = 64)
	private String aggregateId;    // e.g. orderRequestId

	@Column(nullable = false, length = 128)
	private String topic;          // e.g. order.create.requested

	@Column(nullable = false, length = 128)
	private String eventType;      // e.g. OrderCreateRequestedEvent

	@Column(nullable = false)
	private String payloadJson;    // 직렬화된 JSON

	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
	private Status status;         // PENDING, SENT, FAILED

	@Column(length = 512)
	private String lastError;

	public enum Status { PENDING, SENT, FAILED }

	public void updateError(String lastError){
		this.lastError = lastError;
	}
	public static Outbox pending( String aggregateId, String topic,String eventType, String payloadJson) {
		 return Outbox.builder()
			 .aggregateId(aggregateId)
			 .topic(topic)
			 .eventType(eventType)
			 .payloadJson(payloadJson)
			 .status(Status.PENDING)
			 .build();
	}
}