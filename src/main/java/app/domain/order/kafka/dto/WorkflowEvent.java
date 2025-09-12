package app.domain.order.kafka.dto;

import java.util.Map;
import java.util.UUID;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkflowEvent {
	private UUID aggregateId;
	private String status;   // SUCCESS / FAILED
	private List<Map<String, Object>> payload;
}
