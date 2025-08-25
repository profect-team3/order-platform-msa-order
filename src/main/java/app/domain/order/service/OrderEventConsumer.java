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