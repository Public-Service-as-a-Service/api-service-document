package se.sundsvall.document.service;

import generated.se.sundsvall.eventlog.Event;
import generated.se.sundsvall.eventlog.Metadata;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.model.ConfidentialityEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentTypeEntity;
import se.sundsvall.document.integration.eventlog.EventLogClient;
import se.sundsvall.document.integration.eventlog.configuration.EventlogProperties;

import static generated.se.sundsvall.eventlog.EventType.UPDATE;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;

@ExtendWith(MockitoExtension.class)
class DocumentStatusServiceTest {

	private static final String FILE_NAME = "image.png";
	private static final String MIME_TYPE = "image/png";
	private static final long FILE_SIZE_IN_BYTES = 227546L;
	private static final OffsetDateTime CREATED = now(systemDefault());
	private static final boolean CONFIDENTIAL = true;
	private static final String CREATED_BY = "b0000000-0000-0000-0000-000000000099";
	private static final String DESCRIPTION = "Description";
	private static final String ID = randomUUID().toString();
	private static final String LEGAL_CITATION = "legalCitation";
	private static final String METADATA_KEY = "key";
	private static final String METADATA_VALUE = "value";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String REGISTRATION_NUMBER = "2023-2281-4";
	private static final String DOCUMENT_DATA_ID = randomUUID().toString();
	private static final String STORAGE_LOCATOR = randomUUID().toString();
	private static final String DOCUMENT_TYPE = "documentType";
	private static final String DOCUMENT_TYPE_DISPLAYNAME = "documentTypeDisplayname";
	private static final int REVISION = 1;

	@Mock
	private EventlogProperties eventlogPropertiesMock;

	@Mock
	private DocumentRepository documentRepositoryMock;

	@Mock
	private DocumentResponsibilityRepository documentResponsibilityRepositoryMock;

	@Mock
	private DocumentStatusPolicy statusPolicyMock;

	@Mock
	private ApplicationEventPublisher applicationEventPublisherMock;

	@Captor
	private ArgumentCaptor<DocumentEntity> documentEntityCaptor;

	@Captor
	private ArgumentCaptor<List<DocumentEntity>> documentEntitiesCaptor;

	private TestEventLogClient eventLogClient;
	private DocumentStatusService documentStatusService;

	@BeforeEach
	void setUp() {
		eventLogClient = new TestEventLogClient();
		documentStatusService = new DocumentStatusService(
			documentRepositoryMock,
			new DocumentResponseHydrator(documentResponsibilityRepositoryMock),
			statusPolicyMock,
			new DocumentEventPublisher(Optional.of(eventLogClient), Optional.of(eventlogPropertiesMock)),
			applicationEventPublisherMock);
	}

	@Test
	void updateConfidentiality() {

		// Arrange
		final var eventLogKey = UUID.randomUUID().toString();
		final var newConfidentialValue = true;
		final var existingEntities = List.of(createDocumentEntity(), createDocumentEntity().withRevision(REVISION + 1));
		final var confidentialityUpdateRequest = ConfidentialityUpdateRequest.create()
			.withUpdatedBy(CREATED_BY)
			.withConfidential(newConfidentialValue)
			.withLegalCitation(LEGAL_CITATION);

		when(eventlogPropertiesMock.logKeyUuid()).thenReturn(eventLogKey);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(existingEntities);
		when(documentRepositoryMock.saveAll(any())).thenReturn(existingEntities);

		// Act
		documentStatusService.updateConfidentiality(REGISTRATION_NUMBER, confidentialityUpdateRequest, MUNICIPALITY_ID);

		// Assert
		verify(documentRepositoryMock).findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue());
		verify(documentRepositoryMock).saveAll(documentEntitiesCaptor.capture());
		assertThat(eventLogClient.requests).hasSize(1);

		final var capturedDocumentEntities = documentEntitiesCaptor.getValue();
		assertThat(capturedDocumentEntities)
			.isNotNull()
			.hasSize(2)
			.extracting(DocumentEntity::getConfidentiality, DocumentEntity::getRegistrationNumber, DocumentEntity::getRevision)
			.containsExactlyInAnyOrder(
				tuple(ConfidentialityEmbeddable.create().withConfidential(newConfidentialValue).withLegalCitation(LEGAL_CITATION), REGISTRATION_NUMBER, REVISION),
				tuple(ConfidentialityEmbeddable.create().withConfidential(newConfidentialValue).withLegalCitation(LEGAL_CITATION), REGISTRATION_NUMBER, REVISION + 1));

		final var capturedEvent = eventLogClient.requests.getFirst().event();
		assertThat(eventLogClient.requests.getFirst().municipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(eventLogClient.requests.getFirst().logKey()).isEqualTo(eventLogKey);
		assertThat(capturedEvent).isNotNull();
		assertThat(capturedEvent.getExpires()).isCloseTo(now(systemDefault()).plusYears(10), within(2, ChronoUnit.SECONDS));
		assertThat(capturedEvent.getType()).isEqualTo(UPDATE);
		assertThat(capturedEvent.getMessage()).isEqualTo("Confidentiality flag updated to: 'true' with legal citation: 'legalCitation' for document with registrationNumber: '2023-2281-4'. Action performed by: 'b0000000-0000-0000-0000-000000000099'");
		assertThat(capturedEvent.getOwner()).isEqualTo("Document");
		assertThat(capturedEvent.getMetadata())
			.extracting(Metadata::getKey, Metadata::getValue)
			.containsExactlyInAnyOrder(
				tuple("RegistrationNumber", REGISTRATION_NUMBER),
				tuple("ExecutedBy", CREATED_BY));
	}

	@Test
	void publishFromDraft_setsStatusAndSaves() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.DRAFT);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.resolvePublishedStatus(any(), any(), eq(REGISTRATION_NUMBER))).thenReturn(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentStatusService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());
		assertThat(documentEntityCaptor.getValue().getStatus()).isEqualTo(DocumentStatus.ACTIVE);
	}

	@Test
	void publishFromActive_throwsConflict() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));

		final var ex = assertThrows(ThrowableProblem.class, () -> documentStatusService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null));

		assertThat(ex.getMessage()).contains("not allowed");
		verify(documentRepositoryMock, never()).save(any(DocumentEntity.class));
	}

	@Test
	void revokeFromActive_setsRevoked() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentStatusService.revoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.REVOKED);
	}

	@Test
	void revokeFromDraft_throwsConflict() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.DRAFT);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));

		assertThrows(ThrowableProblem.class, () -> documentStatusService.revoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null));

		verify(documentRepositoryMock, never()).save(any(DocumentEntity.class));
	}

	@Test
	void unrevokeFromRevoked_restoresPublishedStatus() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.REVOKED);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.resolvePublishedStatus(any(), any(), eq(REGISTRATION_NUMBER))).thenReturn(DocumentStatus.SCHEDULED);
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentStatusService.unrevoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.SCHEDULED);
	}

	@Test
	void unrevokeFromActive_throwsConflict() {
		final var entity = createDocumentEntity().withStatus(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));

		assertThrows(ThrowableProblem.class, () -> documentStatusService.unrevoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null));
	}

	@Test
	void publishWhenDocumentMissing_throwsNotFound() {
		when(documentRepositoryMock.findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(MUNICIPALITY_ID, REGISTRATION_NUMBER, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(empty());

		final var ex = assertThrows(ThrowableProblem.class, () -> documentStatusService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, null));
		assertThat(ex.getMessage()).contains("could be found");
	}

	@Test
	void publishWithSpecificRevision_usesRevisionFinderAndSaves() {
		final var targetRevision = 5;
		final var entity = createDocumentEntity().withRevision(targetRevision).withStatus(DocumentStatus.DRAFT);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, targetRevision, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.resolvePublishedStatus(any(), any(), eq(REGISTRATION_NUMBER))).thenReturn(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentStatusService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, targetRevision);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
		verify(documentRepositoryMock, never()).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(any(), any(), any());
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());
		assertThat(documentEntityCaptor.getValue().getRevision()).isEqualTo(targetRevision);
		assertThat(documentEntityCaptor.getValue().getStatus()).isEqualTo(DocumentStatus.ACTIVE);
	}

	@Test
	void revokeWithSpecificRevision_usesRevisionFinderAndSaves() {
		final var targetRevision = 3;
		final var entity = createDocumentEntity().withRevision(targetRevision).withStatus(DocumentStatus.ACTIVE);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, targetRevision, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentStatusService.revoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, targetRevision);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.REVOKED);
		verify(documentRepositoryMock, never()).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(any(), any(), any());
		verify(documentRepositoryMock).save(documentEntityCaptor.capture());
		assertThat(documentEntityCaptor.getValue().getRevision()).isEqualTo(targetRevision);
	}

	@Test
	void unrevokeWithSpecificRevision_usesRevisionFinderAndSaves() {
		final var targetRevision = 2;
		final var entity = createDocumentEntity().withRevision(targetRevision).withStatus(DocumentStatus.REVOKED);
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, targetRevision, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(Optional.of(entity));
		when(statusPolicyMock.resolvePublishedStatus(any(), any(), eq(REGISTRATION_NUMBER))).thenReturn(DocumentStatus.SCHEDULED);
		when(documentRepositoryMock.save(any(DocumentEntity.class))).thenAnswer(i -> i.getArgument(0));

		final var result = documentStatusService.unrevoke(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, targetRevision);

		assertThat(result.getStatus()).isEqualTo(DocumentStatus.SCHEDULED);
		verify(documentRepositoryMock, never()).findTopByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialInOrderByRevisionDesc(any(), any(), any());
	}

	@Test
	void publishWithUnknownRevision_throwsNotFound() {
		final var targetRevision = 99;
		when(documentRepositoryMock.findByMunicipalityIdAndRegistrationNumberAndRevisionAndConfidentialityConfidentialIn(MUNICIPALITY_ID, REGISTRATION_NUMBER, targetRevision, CONFIDENTIAL_AND_PUBLIC.getValue())).thenReturn(empty());

		final var ex = assertThrows(ThrowableProblem.class, () -> documentStatusService.publish(REGISTRATION_NUMBER, "actor", MUNICIPALITY_ID, targetRevision));

		assertThat(ex.getMessage()).contains("revision: '%d'".formatted(targetRevision));
		verify(documentRepositoryMock, never()).save(any(DocumentEntity.class));
	}

	private DocumentEntity createDocumentEntity() {
		try {
			return DocumentEntity.create()
				.withCreated(CREATED)
				.withCreatedBy(CREATED_BY)
				.withConfidentiality(ConfidentialityEmbeddable.create()
					.withConfidential(CONFIDENTIAL)
					.withLegalCitation(LEGAL_CITATION))
				.withDescription(DESCRIPTION)
				.withDocumentData(List.of(createDocumentDataEntity()))
				.withId(ID)
				.withMetadata(new ArrayList<>(List.of(DocumentMetadataEmbeddable.create().withKey(METADATA_KEY).withValue(METADATA_VALUE))))
				.withMunicipalityId(MUNICIPALITY_ID)
				.withRegistrationNumber(REGISTRATION_NUMBER)
				.withRevision(REVISION)
				.withType(createDocumentTypeEntity());
		} catch (final Exception e) {
			fail("Entity could not be created!");
		}
		return null;
	}

	private DocumentTypeEntity createDocumentTypeEntity() {
		return DocumentTypeEntity.create()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withType(DOCUMENT_TYPE)
			.withDisplayName(DOCUMENT_TYPE_DISPLAYNAME);
	}

	private DocumentDataEntity createDocumentDataEntity() {
		return DocumentDataEntity.create()
			.withId(DOCUMENT_DATA_ID)
			.withStorageLocator(STORAGE_LOCATOR)
			.withFileName(FILE_NAME)
			.withMimeType(MIME_TYPE)
			.withFileSizeInBytes(FILE_SIZE_IN_BYTES);
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
