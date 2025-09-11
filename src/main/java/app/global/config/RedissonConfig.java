package app.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

	@Value("${REDIS_HOST}")
	private String redisHost;

	@Value("${REDIS_PORT}")
	private int redisPort;

	@Value("${REDIS_PASSWORD}")
	private String redisPassword;
	
	@Value("${REDIS_PROTOCOL:}")
	private String redisProtocol;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();

		SingleServerConfig serverConfig = config.useSingleServer();

		if ("rediss".equalsIgnoreCase(redisProtocol)) {
			serverConfig.setAddress("rediss://" + redisHost + ":" + redisPort)
				.setSslEnableEndpointIdentification(true);
		} else {
			serverConfig.setAddress("redis://" + redisHost + ":" + redisPort);
			if (redisPassword != null && !redisPassword.isBlank()) {
				serverConfig.setPassword(redisPassword);
			}
		}

		return Redisson.create(config);
	}

}