package se.sundsvall.document.integration.eventlog;

import generated.se.sundsvall.eventlog.Event;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import se.sundsvall.document.integration.eventlog.configuration.EventlogConfiguration;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.document.integration.eventlog.configuration.EventlogConfiguration.CLIENT_ID;

@ConditionalOnProperty(name = "integration.eventlog.enabled", havingValue = "true")
@FeignClient(name = CLIENT_ID, url = "${integration.eventlog.url}", configuration = EventlogConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface EventLogClient {

	/**
	 * Create a log event under logKey.
	 *
	 * @param municipalityId Municipality ID of the event
	 * @param logKey         containing UUID to create event for
	 * @param event          the event to create
	 */
	@PostMapping(path = "/{municipalityId}/{logKey}", consumes = APPLICATION_JSON_VALUE, produces = ALL_VALUE)
	ResponseEntity<Void> createEvent(@PathVariable("municipalityId") String municipalityId, @PathVariable("logKey") String logKey, @RequestBody Event event);
}
