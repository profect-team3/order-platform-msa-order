package app.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

	@Primary
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();

		// LocalDateTime 처리
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// 빈 객체 직렬화 실패 방지
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

		// Enum을 case-insensitive하게 매핑
		mapper.configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);

		// 문자열 기반 Enum 역직렬화 허용
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);

		return mapper;
	}
}
