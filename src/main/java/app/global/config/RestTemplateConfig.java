package app.global.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import app.commonUtil.security.TokenPrincipalParser;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final TokenPrincipalParser tokenPrincipalParser;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .additionalInterceptors((req, body, ex) -> {
                tokenPrincipalParser.tryGetAccessToken()
                    .ifPresent(token -> req.getHeaders().setBearerAuth(token));
                return ex.execute(req, body);
            })
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }
}