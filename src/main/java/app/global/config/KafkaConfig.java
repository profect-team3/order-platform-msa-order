package app.global.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id}")
    private String groupId;

    /**
     * Kafka 메시지 전송용 ProducerFactory를 생성하여 빈으로 등록합니다.
     *
     * 반환된 ProducerFactory는 다음으로 구성됩니다:
     * - bootstrap 서버: 클래스 필드 `bootstrapServers`에서 주입된 값
     * - 키 직렬화: StringSerializer
     * - 값 직렬화: JsonSerializer (JSON 직렬화 시 타입 정보 헤더 사용 비활성화)
     *
     * @return String 키와 Object 값을 사용하는 구성된 ProducerFactory 인스턴스
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Producer용 KafkaTemplate<String, Object> 빈을 생성하여 반환합니다.
     *
     * Kafka 프로듀서 설정(producerFactory)을 사용해 메시지 전송을 수행하는 KafkaTemplate 인스턴스를 생성합니다.
     *
     * @return 구성된 ProducerFactory를 사용하는 KafkaTemplate<String, Object> 빈
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Kafka 소비자용 ConsumerFactory를 생성하여 반환합니다.
     *
     * <p>application properties로 주입된 bootstrapServers와 groupId를 사용해 기본 소비자 설정을 구성하며,
     * 키와 값을 모두 StringDeserializer로 처리하도록 설정합니다.</p>
     *
     * @return bootstrapServers와 groupId가 설정되고 StringDeserializer를 키·값 디시리얼라이저로 사용하는 DefaultKafkaConsumerFactory 인스턴스
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka 리스너 컨테이너 팩토리를 생성하여 반환합니다.
     *
     * 이 팩토리는 내부적으로 문자열 키/값을 사용하는 ConsumerFactory를 설정하여
     * @KafkaListener가 사용하는 ConcurrentKafkaListenerContainer를 생성할 수 있도록 구성합니다.
     *
     * @return 문자열 키/값 타입으로 구성된 ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}