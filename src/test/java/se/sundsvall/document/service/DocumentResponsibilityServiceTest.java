package se.sundsvall.document.service;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.Metadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.document.api.model.DocumentResponsibilitiesUpdateRequest;
import se.sundsvall.document.api.model.DocumentResponsibility;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.integration.eventlog.EventLogClient;
import se.sundsvall.document.integration.eventlog.configuration.EventlogProperties;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentResponsibilityServiceTest {

	private static final String CREATED_BY = "b0000000-0000-0000-0000-000000000099";
	private static final String PERSON_ID = "6b8d4a1c-34e2-4f73-a5f1-b7c2e9a0d8c4";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String REGISTRATION_NUMBER = "2023-2281-4";

	@Mock
	private EventlogProperties eventlogPropertiesMock;

	@Mock
	private DocumentRepository documentRepositoryMock;

	@Mock
	private DocumentResponsibilityRepository documentResponsibilityRepositoryMock;

	@Captor
	private ArgumentCaptor<List<DocumentResponsibilityEntity>> responsibilityEntitiesCaptor;

	private TestEventLogClient eventLogClient;
	private DocumentResponsibilityService documentResponsibilityService;

	@BeforeEach
	void setUp() {
		eventLogClient = new TestEventLogClient();
		documentResponsibilityService = new DocumentResponsibilityService(
			documentRepositoryMock,
			documentResponsibilityRepositoryMock,
			new DocumentEventPublisher(Optional.of(eventLogClient), Optional.of(eventlogPropertiesMock)));
	}

	@Test
	void updateResponsibilities() {

		// Arrange
		final var eventLogKey = UUID.randomUUID().toString();
		final var otherPersonId = "11111111-2222-3333-4444-555555555555";
		final var oldPersonId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
		final var request = DocumentResponsibilitiesUpdateRequest.create()
			.withUpdatedBy(CREATED_BY)
			.withResponsibilities(List.of(
				DocumentResponsibility.create().withPersonId(PERSON_ID),
				DocumentResponsibility.create().withPersonId(otherPersonId)));
		final var oldResponsibilities = List.of(DocumentResponsibilityEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRegistrationNumber(REGISTRATION_NUMBER)
			.withPersonId(oldPersonId));

		when(eventlogPropertiesMock.logKeyUuid()).thenReturn(eventLogKey);
		when(documentRepositoryMock.existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(true);
		when(documentResponsibilityRepositoryMock.findByMunicipalityIdAndRegistrationNumberOrderByPersonIdAsc(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(oldResponsibilities);

		// Act
		documentResponsibilityService.updateResponsibilities(REGISTRATION_NUMBER, request, MUNICIPALITY_ID);

		// Assert
		verify(documentRepositoryMock).existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER);
		verify(documentResponsibilityRepositoryMock).findByMunicipalityIdAndRegistrationNumberOrderByPersonIdAsc(MUNICIPALITY_ID, REGISTRATION_NUMBER);
		verify(documentResponsibilityRepositoryMock).deleteByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER);
		verify(documentResponsibilityRepositoryMock).flush();
		verify(documentResponsibilityRepositoryMock).saveAll(responsibilityEntitiesCaptor.capture());
		assertThat(eventLogClient.requests).hasSize(1);

		assertThat(responsibilityEntitiesCaptor.getValue())
			.extracting(DocumentResponsibilityEntity::getMunicipalityId, DocumentResponsibilityEntity::getRegistrationNumber, DocumentResponsibilityEntity::getPersonId,
				DocumentResponsibilityEntity::getCreatedBy)
			.containsExactly(
				tuple(MUNICIPALITY_ID, REGISTRATION_NUMBER, PERSON_ID, CREATED_BY),
				tuple(MUNICIPALITY_ID, REGISTRATION_NUMBER, otherPersonId, CREATED_BY));

		final var capturedEvent = eventLogClient.requests.getFirst().event();
		assertThat(eventLogClient.requests.getFirst().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(eventLogClient.requests.getFirst().logKey()).isEqualTo(eventLogKey);
		assertThat(capturedEvent.getType()).isEqualTo(UPDATE);
		assertThat(capturedEvent.getMessage()).contains("Responsibilities updated from:", oldPersonId, CREATED_BY, otherPersonId, REGISTRATION_NUMBER);
		assertThat(capturedEvent.getMetadata())
			.extracting(Metadata::getKey, Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple("RegistrationNumber", REGISTRATION_NUMBER),
				tuple("ExecutedBy", CREATED_BY));
	}

	@Test
	void updateResponsibilitiesWhenDocumentIsNotFound() {

		// Arrange
		final var request = DocumentResponsibilitiesUpdateRequest.create()
			.withUpdatedBy(CREATED_BY)
			.withResponsibilities(List.of(DocumentResponsibility.create().withPersonId(PERSON_ID)));

		when(documentRepositoryMock.existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER)).thenReturn(false);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> documentResponsibilityService.updateResponsibilities(REGISTRATION_NUMBER, request, MUNICIPALITY_ID));

		// Assert
		assertThat(exception).isNotNull();
		assertThat(exception.getMessage()).isEqualTo("Not Found: No document with registrationNumber: '2023-2281-4' could be found!");

		verify(documentRepositoryMock).existsByMunicipalityIdAndRegistrationNumber(MUNICIPALITY_ID, REGISTRATION_NUMBER);
		assertThat(eventLogClient.requests).isEmpty();
		verifyNoInteractions(documentResponsibilityRepositoryMock);
	}

	private static final class TestEventLogClient implements EventLogClient {

		private final List<EventLogRequest> requests = new ArrayList<>();

		@Override
		public ResponseEntity<Void> createEvent(final String municipalityId, final String logKey, final Event event) {
			requests.add(new EventLogRequest(municipalityId, logKey, event));
			return ResponseEntity.noContent().build();
		}
	}

	private record EventLogRequest(String municipalityId, String logKey, Event event) {
	}
}
