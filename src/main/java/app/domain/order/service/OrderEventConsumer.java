package app.domain.order.service;

import app.domain.order.model.entity.Orders;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final DiscordNotificationService discordNotificationService;
    private final ObjectMapper objectMapper;

    /**
     * Kafka로부터 주문 생성 이벤트 메시지(JSON)를 수신해 Discord 알림을 전송합니다.
     *
     * <p>메시지를 Orders 객체로 역직렬화한 뒤 주문 ID를 로그에 남기고,
     * DiscordNotificationService.sendOrderCreatedNotification(...)을 호출하여 알림을 보냅니다.
     * 내부에서 발생한 모든 예외는 잡아 로그로 기록하며 호출자에게 예외를 전달하지 않습니다.
     *
     * @param message Kafka로 수신한 주문 생성 이벤트의 JSON 문자열 (Orders 형식)
     */
    @KafkaListener(topics = "order-created-events", groupId = "discord-notifier-group")
    public void consumeOrderCreatedEvent(String message) {
        try {
            Orders order = objectMapper.readValue(message, Orders.class);
            log.info("Consumed order created event: {}", order.getOrdersId());
            discordNotificationService.sendOrderCreatedNotification(
                order.getOrdersId().toString(),
                order.getUserId().toString(),
                order.getTotalPrice().toString()
            );
        } catch (Exception e) {
            log.error("Error consuming order created event: {}", e.getMessage());
        }
    }
}