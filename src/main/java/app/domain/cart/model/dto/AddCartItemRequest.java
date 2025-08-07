package app.domain.cart.model.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class AddCartItemRequest {

	@NotNull(message = "메뉴 ID는 필수입니다.")
	private UUID menuId;

	@NotNull(message = "매장 ID는 필수입니다.")
	private UUID storeId;

	@NotNull(message = "수량은 필수입니다.")
	@Min(value = 1, message = "수량은 1 이상이어야 합니다.")
	private Integer quantity;

	public AddCartItemRequest(UUID menuId, UUID storeId, Integer quantity) {
		this.menuId = menuId;
		this.storeId = storeId;
		this.quantity = quantity;
	}
}