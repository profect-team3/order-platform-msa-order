package app.domain.order.model.dto.response;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class OrderDecisionResponse {

	private UUID orderId;
	private String status;

	public OrderDecisionResponse(UUID orderId, String status) {
		this.orderId = orderId;
		this.status = status;
	}
}
