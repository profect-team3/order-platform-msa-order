package app.domain.order.kafka;


import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import app.commonUtil.apiPayload.code.status.ErrorStatus;
import app.commonUtil.apiPayload.exception.GeneralException;
import app.domain.cart.model.dto.RedisCartItem;
import app.domain.cart.service.CartRedisService;
import app.domain.order.kafka.dto.WorkflowEvent;
import app.domain.order.kafka.repository.OutboxRepository;
import app.domain.order.model.entity.OrderItem;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.repository.OrderItemRepository;
import app.domain.order.model.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderValidatedListener {

	private final OrdersRepository ordersRepo;
	private final OrderItemRepository itemRepo;
	private final ObjectMapper objectMapper;
	private final OutboxRepository outboxRepository;
	private final CartRedisService cartRedisService;

	@Value("${topics.order.canceled}")
	private String orderCanceledTopic;

	@Value("${topics.stock.request}")
	private String stockDecreaseRequestTopic;

	@Value("${topics.order.completed}")
	private String orderCompletedTopic;




	@KafkaListener(topics = "${topics.order.validated}")
	@Transactional
	public void OrderValidated(
		String message,
		@Header("orderId") String orderIdStr,
		@Header("eventType") String eventType
	) {
		List<Map<String, Object>> evt;

		try {
			evt = objectMapper.readValue(message, List.class);
			System.out.println(evt);

		} catch (JsonProcessingException e) {
			Map<String, Object> headers = new HashMap<>();
			headers.put("eventType", "fail");
			headers.put("orderId", null);
			Map<String, Object> errorPayload = new HashMap<>();
			errorPayload.put("errorMessage", "Parse error");

			return;
		}


		Orders order = ordersRepo.findById(UUID.fromString(orderIdStr))
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
		if ("success".equals(eventType)) {
				if (evt != null) {
					for (Map<String, Object> it : evt) {
						OrderItem entity = OrderItem.builder()
							.orders(order)
							.menuName(String.valueOf(it.get("menuName")))
							.price(Long.parseLong(String.valueOf(it.get("price"))))
							.quantity(Integer.parseInt(String.valueOf(it.get("quantity"))))
							.build();
						itemRepo.save(entity);
					}
				order.updateOrderStatus(OrderStatus.CREATED);
				ordersRepo.save(order);
			}
		} else {
			if (order.getOrderStatus() != OrderStatus.CANCELED) {
				order.updateOrderStatus(OrderStatus.CANCELED);
				ordersRepo.save(order);
				emitOrderCanceled(order, "VALIDATION_FAILED");
			}
		}
	}


	@KafkaListener(
		topics = "${topics.payment.result}"
	)
	@Transactional
	public void PaymentResult(
		String message,
		@Header("orderId") String orderIdStr,
		@Header("eventType") String eventType
	) {
		Map<String, Object> evt;
		try {
			evt = objectMapper.readValue(message, Map.class);
			System.out.println(evt);

		} catch (JsonProcessingException e) {
			Map<String, Object> headers = new HashMap<>();
			headers.put("eventType", "fail");
			headers.put("orderId", null);
			Map<String, Object> errorPayload = new HashMap<>();
			errorPayload.put("errorMessage", "Parse error");

			return;
		}

		Orders order = ordersRepo.findById(UUID.fromString(orderIdStr))
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));

		if ("success".equals(eventType)) {
			triggerStockDecrease(order);
		} else {
			if (order.getOrderStatus() != OrderStatus.CANCELED) {
				order.updateOrderStatus(OrderStatus.CANCELED);
				ordersRepo.save(order);
				emitOrderCanceled(order, "PAYMENT_FAILED");
			}

		}
	}

	private void triggerStockDecrease(Orders order) {
		List<RedisCartItem> cartItems= cartRedisService.getCartFromRedis(order.getUserId());

		List<Map<String, Object>> payload = new ArrayList<>();

		for (RedisCartItem item : cartItems) {
			Map<String, Object> map = new HashMap<>();
			map.put("menuId", item.getMenuId());
			map.put("quantity", item.getQuantity());
			payload.add(map);
		}

		cartRedisService.clearCartItems(order.getUserId());

		String payloadJson;
		try {
			payloadJson = objectMapper.writeValueAsString(payload);
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}
		outboxRepository.save(Outbox.pending(
			order.getOrdersId().toString(),
			stockDecreaseRequestTopic,
			"OrderStockEvent",
			payloadJson
		));

		ordersRepo.save(order);

	}



	@KafkaListener(topics = "${topics.stock.result}")
	@Transactional
	public void StockResult(
		@Header("orderId") String orderIdStr,
		@Header("eventType") String eventType
	) {
		Orders order = ordersRepo.findById(UUID.fromString(orderIdStr))
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
		if ("success".equals(eventType)) {
			order.updateOrderStatus(OrderStatus.ACCEPTED_READY);
		} else {
			if (order.getOrderStatus() != OrderStatus.CANCELED) {
				order.updateOrderStatus(OrderStatus.CANCELED);
				ordersRepo.save(order);
				emitOrderCanceled(order, "STOCK_FAILED");
			}

		}
	}

	@KafkaListener(topics = "${topics.order.accept}")
	@Transactional
	public void  orderAcceptResult(
		@Header("orderId") String orderIdStr,
		@Header("eventType") String eventType
	) {
		Orders order = ordersRepo.findById(UUID.fromString(orderIdStr))
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
		if ("success".equals(eventType)) {
			order.updateOrderStatus(OrderStatus.ACCEPTED);

			Map<String, Object> payload = new HashMap<>();
			payload.put("storeId", order.getStoreId());
			payload.put("orderTime", order.getUpdatedAt());
			payload.put("totalPrice", order.getTotalPrice());

			String payloadJson;
			try {
				payloadJson = objectMapper.writeValueAsString(payload);
			} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
				throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
			}

			outboxRepository.save(
				Outbox.pending(
					order.getOrdersId().toString(),
					orderCompletedTopic,
					"OrderCompletedEvent",
					payloadJson
				)
			);
		} else {
			if (order.getOrderStatus() != OrderStatus.CANCELED) {
				order.updateOrderStatus(OrderStatus.CANCELED);
				ordersRepo.save(order);
				emitOrderCanceled(order, "ACCEPT_FAILED");
			}

		}
	}

	private void emitOrderCanceled(Orders order, String reason) {
		Map<String, Object> payload = Map.of(
			"orderId", order.getOrdersId(),
			"reason", reason,
			"occurredAt", Instant.now()
		);
		WorkflowEvent ev = WorkflowEvent.builder()
			.aggregateId(order.getOrdersId())
			.status("FAILED")
			.payload(List.of(payload))
			.build();
		try {
			String payLoadJson = objectMapper.writeValueAsString(ev);
			outboxRepository.save(
				Outbox.pending(
					order.getOrdersId().toString(),
					orderCanceledTopic,
					"OrderCanceledEvent",
					payLoadJson
				)
			);
		} catch (JsonProcessingException e) {
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}
	}


}