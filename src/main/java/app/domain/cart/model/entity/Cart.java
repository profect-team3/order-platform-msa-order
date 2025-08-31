package app.domain.cart.model.entity;

import java.util.UUID;

import app.commonUtil.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_cart")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Cart extends BaseEntity {

	@Id
	@GeneratedValue
	@Column(name="cart_id")
	private UUID cartId;

	@Column(name="user_id")
	private Long userId;

	public Cart(UUID cartId, Long userId) {
		this.cartId = cartId;
		this.userId = userId;
	}

}