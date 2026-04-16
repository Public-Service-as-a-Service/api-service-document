package se.sundsvall.document.service;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

	/**
	 * All "today"-logic in this service uses dates in the Europe/Stockholm zone (matches
	 * how users think about validity windows and how the database stores LocalDate values).
	 * Tests can override this bean with a {@link Clock#fixed} to pin time.
	 */
	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Europe/Stockholm"));
	}
}
