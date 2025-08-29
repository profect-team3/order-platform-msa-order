package app;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.RegistryEventConsumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class CircuitBreakerTest {

    @Autowired
    private RegistryEventConsumer<CircuitBreaker> registryEventConsumer;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void circuitBreaker_open_direct_call() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("test");
        cb.transitionToClosedState();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        for (int i = 0; i < 5; i++) {
            try {
                cb.executeSupplier(() -> { throw new RuntimeException("fail"); });
            } catch (Exception ignored) {}
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

}
