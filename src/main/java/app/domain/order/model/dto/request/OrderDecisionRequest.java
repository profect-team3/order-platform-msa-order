package app.domain.order.model.dto.request;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class OrderDecisionRequest {

	private String status;

	public OrderDecisionRequest(String status) {
		this.status = status;
	}
}
