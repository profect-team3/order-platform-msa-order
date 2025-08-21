package app.global.config;

import java.util.function.Predicate;

import org.springframework.web.client.HttpClientErrorException;

public class RestTemplateCircuitRecordFailurePredicate implements Predicate<Throwable> {

	@Override
	public boolean test(Throwable throwable) {
		if (throwable instanceof HttpClientErrorException) {
			return false;
		}

		return true;
	}
}

