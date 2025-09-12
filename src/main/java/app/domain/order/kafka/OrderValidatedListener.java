package app.domain.order.kafka;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import app.commonUtil.apiPayload.code.status.ErrorStatus;
import app.commonUtil.apiPayload.exception.GeneralException;
import app.domain.cart.model.dto.RedisCartItem;
import app.domain.cart.service.CartRedisService;
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

	@KafkaListener(topics = "${topics.order.validated}",groupId = "order-valid")
	public void OrderValidated(
		String message,
		@Header("orderId") String orderIdStr,
		@Header("eventType") String eventType
	) {
		List<Map<String, Object>> evt;
		try {
			evt = objectMapper.readValue(message, List.class);
		} catch (JsonProcessingException e) {
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
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
			if (order.getOrderStatus() != OrderStatus.FAILED) {
				order.updateOrderStatus(OrderStatus.FAILED);
				ordersRepo.save(order);
			}
		}
	}


	@KafkaListener(topics = "${topics.payment.result}",groupId = "payment-result")
	public void PaymentResult(
		String message,
		@Header("orderId") String orderIdStr,
		@Header("eventType") String eventType
	) {
		if ("success".equals(eventType)) {
			Map<String, Object> evt;
			try {
				evt = objectMapper.readValue(message, Map.class);

				Long userId = Long.parseLong(evt.get("userId").toString());

				triggerStockDecrease(UUID.fromString(orderIdStr), userId);

			} catch (JsonProcessingException e) {
				throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
			}
		}else{
			Orders order= ordersRepo.findById(UUID.fromString(orderIdStr))
				.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
			order.updateOrderStatus(OrderStatus.FAILED);
		}
	}


	private void triggerStockDecrease( UUID orderId,Long userId) {
		List<RedisCartItem> cartItems= cartRedisService.getCartFromRedis(userId);
		List<Map<String, Object>> payload = new ArrayList<>();

		for (RedisCartItem item : cartItems) {
			Map<String, Object> map = new HashMap<>();
			map.put("menuId", item.getMenuId());
			map.put("quantity", item.getQuantity());
			payload.add(map);
		}

		cartRedisService.clearCartItems(userId);

		String payloadJson;
		try {
			payloadJson = objectMapper.writeValueAsString(payload);
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}
		outboxRepository.save(Outbox.pending(
			orderId.toString(),
			stockDecreaseRequestTopic,
			"OrderStockEvent",
			payloadJson
		));
	}

	@KafkaListener(topics = "${topics.stock.result}",groupId = "stock-result")
	public void StockResult(
		@Header("orderId") String orderIdStr,
		@Header("eventType") String eventType,
		String message
	) {
		Orders order = ordersRepo.findById(UUID.fromString(orderIdStr))
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
		if ("success".equals(eventType)) {
			order.updateOrderStatus(OrderStatus.ACCEPTED_READY);
			ordersRepo.save(order);
		} else {
			if (order.getOrderStatus() != OrderStatus.FAILED) {

				Map<String, Object> payloadJson;
				try {
					payloadJson = objectMapper.readValue(message,Map.class);
				} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
					throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
				}
				List<Outbox> outbox = outboxRepository.findByAggregateId(orderIdStr);
				for(Outbox outboxItem : outbox) {
					outboxItem.updateError(payloadJson.get("errorMessage").toString());
					outboxRepository.save(outboxItem);
				}
				order.updateOrderStatus(OrderStatus.FAILED);
				ordersRepo.save(order);
				emitOrderCanceled(order);
			}

		}
	}

	@KafkaListener(topics = "${topics.order.approve}",groupId = "order-approve-result")
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
			if (order.getOrderStatus() != OrderStatus.REJECTED) {
				order.updateOrderStatus(OrderStatus.REJECTED);
				ordersRepo.save(order);
				emitOrderCanceled(order);
			}
		}
	}

	private void emitOrderCanceled(Orders order) {
		Map<String, Object> payload = Map.of(
			"userId", order.getUserId()
		);
		try {
			String payLoadJson = objectMapper.writeValueAsString(payload);
			outboxRepository.save(
				Outbox.pending(
					order.getOrdersId().toString(),
					orderCanceledTopic,
					"orderCancelEvent",
					payLoadJson
				)
			);
		} catch (JsonProcessingException e) {
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}
	}


}