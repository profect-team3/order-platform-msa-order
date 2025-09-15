package app.domain.order.kafka.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class AlertNotifier {

	private final RestTemplate rest = new RestTemplate();

	@Value("${discord.webhook.url:}")
	private String webhook;

	public void sendEmbed(String title, String description, Map<String, String> fields, int color) {
		if (webhook == null || webhook.isBlank()) {
			log.warn("Discord webhook URL not configured. Skip alert: {}", title);
			return;
		}
		try {
			Map<String, Object> embed = new LinkedHashMap<>();
			embed.put("title", cut(title, 256));
			embed.put("description", fence(cut(description, 3800)));
			embed.put("color", color == 0 ? 0xE74C3C : color);
			embed.put("timestamp", Instant.now().toString());
			embed.put("footer", Map.of("text", "Kafka DLT Monitor"));

			if (fields != null && !fields.isEmpty()) {
				List<Map<String, Object>> f = new ArrayList<>();
				fields.forEach((k, v) -> f.add(Map.of(
					"name", cut(k, 256),
					"value", cut(v, 1024),
					"inline", true
				)));
				embed.put("fields", f);
			}

			Map<String, Object> body = Map.of(
				"content", "",
				"embeds", List.of(embed)
			);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			rest.postForEntity(webhook, new HttpEntity<>(body, headers), String.class);
		} catch (Exception e) {
			log.error("Failed to send Discord alert", e);
		}
	}

	private static String cut(String s, int max) {
		if (s == null) return "";
		return (s.length() <= max) ? s : s.substring(0, Math.max(0, max - 3)) + "...";
	}

	private static String fence(String s) {
		if (s == null || s.isBlank()) return "";
		return "```" + (looksJson(s) ? "json\n" : "\n") + s + "```";
	}

	private static boolean looksJson(String s) {
		s = s.trim();
		return (s.startsWith("{") && s.endsWith("}")) || (s.startsWith("[") && s.endsWith("]"));
	}
}