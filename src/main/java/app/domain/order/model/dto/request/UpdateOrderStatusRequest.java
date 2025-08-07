package app.domain.order.model.dto.request;

import app.domain.order.model.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateOrderStatusRequest {
	@NotNull
	private OrderStatus newStatus;

	public UpdateOrderStatusRequest() {
	}

	public UpdateOrderStatusRequest(OrderStatus newStatus) {
		this.newStatus = newStatus;
	}
}