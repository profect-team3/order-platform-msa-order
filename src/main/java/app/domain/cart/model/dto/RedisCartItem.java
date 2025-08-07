package app.domain.cart.model.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class RedisCartItem {

	@NotNull
	private UUID menuId;

	@NotNull
	private UUID storeId;

	@NotNull
	private int quantity;

	public RedisCartItem(UUID menuId, UUID storeId, int quantity) {
		this.menuId = menuId;
		this.storeId = storeId;
		this.quantity = quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
