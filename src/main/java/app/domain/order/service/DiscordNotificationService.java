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

    /**
     * 주문 생성 시 Discord 웹훅으로 알림을 전송합니다.
     *
     * <p>디스코드 웹훅 URL(discord.webhook.url)이 설정되어 있지 않으면 알림 전송을 건너뜁니다. 전송 실패 시 예외를 호출자에게 던지지 않고 내부에서 로깅합니다.</p>
     *
     * @param orderId   알림에 포함할 주문 ID
     * @param userId    알림에 포함할 사용자 ID
     * @param totalPrice 알림에 포함할 주문의 총 가격(문자열 형식)
     */
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
