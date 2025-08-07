package app.domain.cart.model.entity;

import java.util.UUID;

import app.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
	private UUID cartId;


	private Long userId;

	public Cart(UUID cartId, Long userId) {
		this.cartId = cartId;
		this.userId = userId;
	}

}