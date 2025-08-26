package app.domain.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 지정한 Kafka 토픽으로 메시지를 전송한다.
     *
     * 주어진 토픽으로 전달된 메시지를 비동기적으로 발행한다.
     *
     * @param topic 전송 대상 Kafka 토픽 이름
     * @param message 전송할 메시지 객체
     */
    public void sendMessage(String topic, Object message) {
        log.info("Producing message to topic {}: {}", topic, message);
        kafkaTemplate.send(topic, message);
    }
}
