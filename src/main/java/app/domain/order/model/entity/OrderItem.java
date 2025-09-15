package app.domain.order.model.entity;

import java.util.UUID;

import app.commonUtil.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_b_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class OrderItem extends BaseEntity {

	@Id
	@GeneratedValue
	private UUID orderItemId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orders_id", nullable = false)
	private Orders orders;

	@Column(length = 100)
	private String menuName;

	@Column
	private Long price;

	@Column(nullable = false)
	private int quantity;

	public OrderItem(UUID orderItemId, Orders orders, String menuName, Long price, int quantity) {
		this.orderItemId = orderItemId;
		this.orders = orders;
		this.menuName = menuName;
		this.price = price;
		this.quantity = quantity;
	}
}