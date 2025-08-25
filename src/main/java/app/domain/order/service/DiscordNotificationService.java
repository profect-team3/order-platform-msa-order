package app.domain.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordNotificationService {

    @Value("${discord.webhook.url}")
    private String discordWebhookUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public void sendOrderCreatedNotification(String orderId, String userId, String totalPrice) {
        if (discordWebhookUrl == null || discordWebhookUrl.isEmpty()) {
            log.warn("Discord webhook URL is not configured. Skipping notification.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("content", String.format("새로운 주문이 생성되었습니다!\n주문 ID: %s\n사용자 ID: %s\n총 가격: %s", orderId, userId, totalPrice));

            HttpEntity<String> request = new HttpEntity<>(messageNode.toString(), headers);

            restTemplate.postForEntity(discordWebhookUrl, request, String.class);
            log.info("Discord notification sent for order ID: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to send Discord notification for order ID: {}. Error: {}", orderId, e.getMessage());
        }
    }
}
