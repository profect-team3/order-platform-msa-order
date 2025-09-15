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
	private String aggregateId;

	@Column(nullable = false, length = 128)
	private String topic;

	@Column(nullable = false, length = 128)
	private String eventType;

	@Column(nullable = false)
	private String payloadJson;

	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
	private Status status;

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