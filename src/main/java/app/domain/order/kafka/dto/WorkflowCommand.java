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
public class WorkflowCommand {
	private  UUID aggregateId;
	private  List<Map<String, Object>> payload;

}