package app.domain.order.model.entity.enums;

public enum OrderStatus {
	PENDING,
	ACCEPTED,
	CANCELED,
	READY_FOR_STOCK,
	STOCK_REQUESTED,
	COOKING,
	IN_DELIVERY,
	COMPLETED,
	REJECTED,
	REFUNDED,
	FAILED;
}