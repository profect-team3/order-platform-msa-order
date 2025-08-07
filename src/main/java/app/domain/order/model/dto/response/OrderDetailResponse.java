package app.domain.order.model.dto.response;

import java.util.List;

import app.domain.order.model.entity.OrderItem;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderChannel;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.entity.enums.PaymentMethod;
import app.domain.order.model.entity.enums.ReceiptMethod;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class OrderDetailResponse {

	public OrderDetailResponse(List<Menu> menuList, Long totalPrice, String deliveryAddress, PaymentMethod paymentMethod, OrderChannel orderChannel, ReceiptMethod receiptMethod, OrderStatus orderStatus, String requestMessage) {
		this.menuList = menuList;
		this.totalPrice = totalPrice;
		this.deliveryAddress = deliveryAddress;
		this.paymentMethod = paymentMethod;
		this.orderChannel = orderChannel;
		this.receiptMethod = receiptMethod;
		this.orderStatus = orderStatus;
		this.requestMessage = requestMessage;
	}

	private List<Menu> menuList;
	private Long totalPrice;
	private String deliveryAddress;
	private PaymentMethod paymentMethod;
	private OrderChannel orderChannel;
	private ReceiptMethod receiptMethod;
	private OrderStatus orderStatus;
	private String requestMessage;

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Builder
	public static class Menu {
		private String menuName;
		private int quantity;
		private Long price;

		public Menu(String menuName, int quantity, Long price) {
			this.menuName = menuName;
			this.quantity = quantity;
			this.price = price;
		}

		public static Menu from(OrderItem orderItem) {
			return Menu.builder()
				.menuName(orderItem.getMenuName())
				.quantity(orderItem.getQuantity())
				.price(orderItem.getPrice())
				.build();
		}
	}

	public static OrderDetailResponse from(Orders orders, List<OrderItem> orderItems) {
		return OrderDetailResponse.builder()
			.menuList(orderItems.stream().map(Menu::from).toList())
			.totalPrice(orders.getTotalPrice())
			.deliveryAddress(orders.getDeliveryAddress())
			.paymentMethod(orders.getPaymentMethod())
			.orderChannel(orders.getOrderChannel())
			.receiptMethod(orders.getReceiptMethod())
			.orderStatus(orders.getOrderStatus())
			.requestMessage(orders.getRequestMessage())
			.build();
	}
}
