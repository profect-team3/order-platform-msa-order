package app.domain.order.model.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import app.commonUtil.entity.BaseEntity;
import app.domain.order.model.entity.enums.OrderChannel;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.entity.enums.PaymentMethod;
import app.domain.order.model.entity.enums.PaymentStatus;
import app.domain.order.model.entity.enums.ReceiptMethod;
import app.domain.order.model.entity.enums.ValidationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Orders extends BaseEntity {
	@Id
	@GeneratedValue
	private UUID ordersId;


	private UUID storeId;


	private Long userId; // nullable (오프라인 주문 고려)

	@Column(nullable = false)
	private Long totalPrice;

	@Column(nullable = false)
	private String deliveryAddress; // 오프라인,포장은 "없음"

	@Column(nullable = false, length = 50)
	@Enumerated(EnumType.STRING)
	private PaymentMethod paymentMethod;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private OrderChannel orderChannel;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ReceiptMethod receiptMethod;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private OrderStatus orderStatus;

	@Enumerated(EnumType.STRING)
	private ValidationStatus validationStatus = ValidationStatus.PENDING;

	@Enumerated(EnumType.STRING)
	private PaymentStatus paymentStatus = PaymentStatus.PENDING;

	@Column(nullable = false)
	private boolean isRefundable;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String orderHistory;

	private String requestMessage;

	public Orders(UUID ordersId, UUID storeId, Long userId, Long totalPrice, String deliveryAddress, PaymentMethod paymentMethod, OrderChannel orderChannel, ReceiptMethod receiptMethod, OrderStatus orderStatus, boolean isRefundable, String orderHistory, String requestMessage) {
		this.ordersId = ordersId;
		this.storeId = storeId;
		this.userId = userId;
		this.totalPrice = totalPrice;
		this.deliveryAddress = deliveryAddress;
		this.paymentMethod = paymentMethod;
		this.orderChannel = orderChannel;
		this.receiptMethod = receiptMethod;
		this.orderStatus = orderStatus;
		this.isRefundable = isRefundable;
		this.orderHistory = orderHistory;
		this.requestMessage = requestMessage;
	}
	public void updateOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	public void updatePaymentStatus(PaymentStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
	public void updateValidationStatus(ValidationStatus validationStatus) {
		this.validationStatus = validationStatus;
	}

	public void addHistory(String state, LocalDateTime dateTime) {
		String newEntry = state + ":" + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

		if (this.orderHistory == null || this.orderHistory.toString().isEmpty()) {
			this.orderHistory = newEntry;
		} else {
			this.orderHistory = this.orderHistory.toString() + "\n" + newEntry;
		}
	}

	public void disableRefund() {
		this.isRefundable = false;
	}

	public void updateStatusAndHistory(OrderStatus newStatus, String updatedHistory) {
		this.orderStatus = newStatus;
		this.orderHistory = updatedHistory;
	}

}