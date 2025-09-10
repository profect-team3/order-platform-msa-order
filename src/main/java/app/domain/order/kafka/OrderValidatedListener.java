package app.domain.order.kafka;


import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import app.commonUtil.apiPayload.code.status.ErrorStatus;
import app.commonUtil.apiPayload.exception.GeneralException;
import app.domain.order.kafka.dto.WorkflowCommand;
import app.domain.order.kafka.dto.WorkflowEvent;
import app.domain.order.kafka.repository.OutboxRepository;
import app.domain.order.model.entity.OrderItem;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.entity.enums.OrderStatus;
import app.domain.order.model.entity.enums.PaymentStatus;
import app.domain.order.model.entity.enums.ValidationStatus;
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

	@Value("${topics.order.canceled:example}")
	private String orderCanceledTopic;

	@Value("${topics.stock.decrease.request:example}")
	private String stockDecreaseRequestTopic;

	@KafkaListener(
		topics = "${topics.order.validated}"
	)
	@Transactional
	public void OrderValidated(
		String message,
		@Header("orderId") String orderIdStr,
		@Header("eventType") String eventType
	) {
		System.out.println("1");
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
			if (order.getValidationStatus() != ValidationStatus.SUCCESS) {
				order.updateValidationStatus(ValidationStatus.SUCCESS);

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
				}
				ordersRepo.save(order);
			}
			maybeTriggerStockDecrease(order);
		} else {
			// 유효성 실패 → 취소
			if (order.getOrderStatus() != OrderStatus.CANCELED) {
				order.updateValidationStatus(ValidationStatus.FAILED);
				order.updateOrderStatus(OrderStatus.CANCELED);
				ordersRepo.save(order);
				emitOrderCanceled(order, "VALIDATION_FAILED");
			}
		}
	}


	@KafkaListener(
		topics = "${topics.payment.result:example}"
	)
	@Transactional
	public void PaymentResult(WorkflowEvent evt) {
		Orders order = ordersRepo.findById(evt.getAggregateId())
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));

		if ("SUCCESS".equals(evt.getStatus())) {
			if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
				order.updatePaymentStatus(PaymentStatus.SUCCESS);
				ordersRepo.save(order);
			}
			maybeTriggerStockDecrease(order);
		} else {
			if (order.getOrderStatus() != OrderStatus.CANCELED) {
				order.updatePaymentStatus(PaymentStatus.FAILED);
				order.updateOrderStatus(OrderStatus.CANCELED);
				ordersRepo.save(order);
				emitOrderCanceled(order, "PAYMENT_FAILED");
			}

		}
	}

	private void maybeTriggerStockDecrease(Orders order) {
		if (order.getOrderStatus() == OrderStatus.READY_FOR_STOCK ||
			order.getOrderStatus() == OrderStatus.STOCK_REQUESTED) {
			return;
		}

		if (order.getValidationStatus() == ValidationStatus.SUCCESS &&
			order.getPaymentStatus()    == PaymentStatus.SUCCESS &&
			order.getOrderStatus()      == OrderStatus.PENDING) {

			order.updateOrderStatus(OrderStatus.READY_FOR_STOCK);
			ordersRepo.save(order);


			List<OrderItem> orderItems =itemRepo.findByOrders(order);
			Map<String, Object> payload = new HashMap<>();
			payload.put("orderItems", orderItems);

			WorkflowCommand event = WorkflowCommand.builder()
				.aggregateId(order.getOrdersId())
				.payload(List.of(payload))
				.build();

			String payloadJson;
			try {
				payloadJson = objectMapper.writeValueAsString(event);
			} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
				throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
			}


			outboxRepository.save(Outbox.pending(
				order.getOrdersId().toString(),
				stockDecreaseRequestTopic,
				"OrderStockEvent",
				payloadJson
			));

			order.updateOrderStatus(OrderStatus.STOCK_REQUESTED);
			ordersRepo.save(order);
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