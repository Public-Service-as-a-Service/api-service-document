package se.sundsvall.document.service;

import generated.se.sundsvall.eventlog.Event;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.integration.eventlog.EventLogClient;
import se.sundsvall.document.integration.eventlog.configuration.EventlogProperties;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentEventPublisherTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String REGISTRATION_NUMBER = "2023-2281-4";
	private static final String CHANGED_BY = "actor";
	private static final String LOG_KEY = UUID.randomUUID().toString();

	@Test
	void logConfidentialityChange_emitsFormattedEvent() {
		final var client = new TestEventLogClient();
		final var publisher = publisherWithClient(client);
		final var request = ConfidentialityUpdateRequest.create().withChangedBy(CHANGED_BY).withConfidential(true).withLegalCitation("citation");

		publisher.logConfidentialityChange(REGISTRATION_NUMBER, request, MUNICIPALITY_ID);

		assertThat(client.requests).hasSize(1);
		final var req = client.requests.getFirst();
		assertThat(req.municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(req.logKey()).isEqualTo(LOG_KEY);
		assertThat(req.event().getType()).isEqualTo(UPDATE);
		assertThat(req.event().getMessage()).isEqualTo(
			"Confidentiality flag updated to: 'true' with legal citation: 'citation' for document with registrationNumber: '2023-2281-4'. Action performed by: 'actor'");
	}

	@Test
	void logResponsibilitiesChange_sortsNewResponsibilitiesByUsername() {
		final var client = new TestEventLogClient();
		final var publisher = publisherWithClient(client);
		final var oldResp = List.of(responsibility("zeta"));
		final var newResp = List.of(responsibility("beta"), responsibility("alpha"));

		publisher.logResponsibilitiesChange(REGISTRATION_NUMBER, CHANGED_BY, oldResp, newResp, MUNICIPALITY_ID);

		assertThat(client.requests).hasSize(1);
		final var message = client.requests.getFirst().event().getMessage();
		assertThat(message)
			.contains(REGISTRATION_NUMBER)
			.contains(CHANGED_BY)
			.contains("zeta")
			.contains("alpha")
			.contains("beta");
		assertThat(message.indexOf("alpha")).isLessThan(message.indexOf("beta"));
	}

	@Test
	void logStatusChange_includesRevisionInMessage() {
		final var client = new TestEventLogClient();
		final var publisher = publisherWithClient(client);

		publisher.logStatusChange(REGISTRATION_NUMBER, 5, DocumentStatus.DRAFT, DocumentStatus.ACTIVE, CHANGED_BY, MUNICIPALITY_ID);

		assertThat(client.requests).hasSize(1);
		assertThat(client.requests.getFirst().event().getMessage()).isEqualTo(
			"Status changed from 'DRAFT' to 'ACTIVE' for document with registrationNumber: '2023-2281-4' and revision: '5'. Action performed by: 'actor'");
	}

	@Test
	void silentlyNoOps_whenEventlogClientAbsent() {
		final var publisher = new DocumentEventPublisher(Optional.empty(), Optional.of(propertiesMock()));

		publisher.logStatusChange(REGISTRATION_NUMBER, 1, DocumentStatus.DRAFT, DocumentStatus.ACTIVE, CHANGED_BY, MUNICIPALITY_ID);
		publisher.logConfidentialityChange(REGISTRATION_NUMBER, ConfidentialityUpdateRequest.create().withChangedBy(CHANGED_BY).withConfidential(true).withLegalCitation("c"), MUNICIPALITY_ID);
		publisher.logResponsibilitiesChange(REGISTRATION_NUMBER, CHANGED_BY, emptyList(), emptyList(), MUNICIPALITY_ID);
		// No exception, no verification needed — absence of a client means no-op.
	}

	@Test
	void silentlyNoOps_whenEventlogPropertiesAbsent() {
		final var client = new TestEventLogClient();
		final var publisher = new DocumentEventPublisher(Optional.of(client), Optional.empty());

		publisher.logStatusChange(REGISTRATION_NUMBER, 1, DocumentStatus.DRAFT, DocumentStatus.ACTIVE, CHANGED_BY, MUNICIPALITY_ID);

		assertThat(client.requests).isEmpty();
	}

	private DocumentEventPublisher publisherWithClient(final TestEventLogClient client) {
		return new DocumentEventPublisher(Optional.of(client), Optional.of(propertiesMock()));
	}

	private EventlogProperties propertiesMock() {
		final var props = mock(EventlogProperties.class);
		when(props.logKeyUuid()).thenReturn(LOG_KEY);
		return props;
	}

	private static DocumentResponsibilityEntity responsibility(final String username) {
		return DocumentResponsibilityEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withUsername(username);
	}

	private static final class TestEventLogClient implements EventLogClient {
		private final List<Captured> requests = new java.util.ArrayList<>();

		@Override
		public ResponseEntity<Void> createEvent(final String municipalityId, final String logKey, final Event event) {
			requests.add(new Captured(municipalityId, logKey, event));
			return ResponseEntity.noContent().build();
		}
	}

	private record Captured(String municipalityId, String logKey, Event event) {
	}
}
