package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableKafka
public class OrderApplication {

	/**
	 * 애플리케이션의 진입점으로 Spring Boot 애플리케이션을 시작합니다.
	 *
	 * 전달된 커맨드라인 인수를 사용하여 OrderApplication 클래스로 Spring 컨텍스트를 초기화하고 실행합니다.
	 *
	 * @param args 커맨드라인 인수 배열
	 */
	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}
}