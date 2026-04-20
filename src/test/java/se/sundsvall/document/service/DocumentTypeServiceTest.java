package se.sundsvall.document.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.document.api.model.DocumentTypeCreateRequest;
import se.sundsvall.document.api.model.DocumentTypeUpdateRequest;
import se.sundsvall.document.integration.db.DocumentTypeRepository;
import se.sundsvall.document.integration.db.model.DocumentTypeEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class DocumentTypeServiceTest {
	private static final String CREATED_BY = "b0000000-0000-0000-0000-0000000000c1";
	private static final String DISPLAY_NAME = "displayName";
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String TYPE = "type";

	@Mock
	private DocumentTypeRepository documentTypeRepositoryMock;

	@InjectMocks
	private DocumentTypeService service;

	@Captor
	private ArgumentCaptor<DocumentTypeEntity> documentTypeEntityCaptor;

	@Test
	void create() {
		// Arrange
		when(documentTypeRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		final var request = DocumentTypeCreateRequest.create()
			.withCreatedBy(CREATED_BY)
			.withDisplayName(DISPLAY_NAME)
			.withType(TYPE);

		// Act
		final var result = service.create(MUNICIPALITY_ID, request);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDisplayName()).isEqualTo(DISPLAY_NAME);
		assertThat(result.getType()).isEqualTo(TYPE.toUpperCase());
		verify(documentTypeRepositoryMock).existsByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE);
		verify(documentTypeRepositoryMock).save(documentTypeEntityCaptor.capture());
		verifyNoMoreInteractions(documentTypeRepositoryMock);
		assertThat(documentTypeEntityCaptor.getValue()).satisfies(entity -> {
			assertThat(entity).isNotNull().hasAllNullFieldsOrPropertiesExcept("createdBy", "displayName", "municipalityId", "type");
			assertThat(entity.getCreatedBy()).isEqualTo(CREATED_BY);
			assertThat(entity.getDisplayName()).isEqualTo(DISPLAY_NAME);
			assertThat(entity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(entity.getType()).isEqualTo(TYPE.toUpperCase());
		});
	}

	@Test
	void createWhenAlreadyExisting() {
		// Arrange
		when(documentTypeRepositoryMock.existsByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE)).thenReturn(true);
		final var request = DocumentTypeCreateRequest.create()
			.withType(TYPE);

		// Act
		final var e = assertThrows(ThrowableProblem.class, () -> service.create(MUNICIPALITY_ID, request));

		// Assert
		assertThat(e.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(e.getMessage()).isEqualTo("Bad Request: Document type with identifier TYPE already exists in municipality with id municipalityId");
		verify(documentTypeRepositoryMock).existsByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE);
		verifyNoMoreInteractions(documentTypeRepositoryMock);
	}

	@Test
	void read() {
		// Arrange
		when(documentTypeRepositoryMock.findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE)).thenReturn(Optional.of(DocumentTypeEntity.create()
			.withDisplayName(DISPLAY_NAME)
			.withType(TYPE)));

		// Act
		final var result = service.read(MUNICIPALITY_ID, TYPE);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDisplayName()).isEqualTo(DISPLAY_NAME);
		assertThat(result.getType()).isEqualTo(TYPE);
		verify(documentTypeRepositoryMock).findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE);
		verifyNoMoreInteractions(documentTypeRepositoryMock);
	}

	@Test
	void readWhenNotFound() {
		// Act
		final var e = assertThrows(ThrowableProblem.class, () -> service.read(MUNICIPALITY_ID, TYPE));

		// Assert
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Document type with identifier type was not found within municipality with id municipalityId");
		verify(documentTypeRepositoryMock).findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE);
		verifyNoMoreInteractions(documentTypeRepositoryMock);
	}

	@Test
	void readAll() {
		// Arrange
		when(documentTypeRepositoryMock.findAllByMunicipalityId(MUNICIPALITY_ID)).thenReturn(List.of(DocumentTypeEntity.create()
			.withDisplayName(DISPLAY_NAME)
			.withType(TYPE)));

		// Act
		final var result = service.read(MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotEmpty().hasSize(1).satisfiesExactly(type -> {
			assertThat(type.getDisplayName()).isEqualTo(DISPLAY_NAME);
			assertThat(type.getType()).isEqualTo(TYPE);
		});
		verify(documentTypeRepositoryMock).findAllByMunicipalityId(MUNICIPALITY_ID);
		verifyNoMoreInteractions(documentTypeRepositoryMock);
	}

	@Test
	void readAllWhenNoneFound() {
		// Act
		final var result = service.read(MUNICIPALITY_ID);

		// Assert
		assertThat(result).isEmpty();

		verify(documentTypeRepositoryMock).findAllByMunicipalityId(MUNICIPALITY_ID);
		verifyNoMoreInteractions(documentTypeRepositoryMock);
	}

	@Test
	void update() {
		// Arrange
		final var updatedBy = "b0000000-0000-0000-0000-0000000000d1";
		final var updatedDisplayName = DISPLAY_NAME + "_UPDATED";
		final var request = DocumentTypeUpdateRequest.create()
			.withDisplayName(updatedDisplayName)
			.withUpdatedBy(updatedBy);
		final var entity = DocumentTypeEntity.create()
			.withDisplayName(DISPLAY_NAME)
			.withCreatedBy(CREATED_BY)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withType(TYPE.toUpperCase());
		when(documentTypeRepositoryMock.findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE)).thenReturn(Optional.of(entity));
		when(documentTypeRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		service.update(MUNICIPALITY_ID, TYPE, request);

		// Assert
		verify(documentTypeRepositoryMock).findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE);
		verify(documentTypeRepositoryMock).save(documentTypeEntityCaptor.capture());
		verifyNoMoreInteractions(documentTypeRepositoryMock);
		assertThat(documentTypeEntityCaptor.getValue()).satisfies(capture -> {
			assertThat(capture).isNotNull().hasAllNullFieldsOrPropertiesExcept("createdBy", "displayName", "lastUpdatedBy", "municipalityId", "type");
			assertThat(capture.getCreatedBy()).isEqualTo(CREATED_BY);
			assertThat(capture.getDisplayName()).isEqualTo(updatedDisplayName);
			assertThat(capture.getLastUpdatedBy()).isEqualTo(updatedBy);
			assertThat(capture.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(capture.getType()).isEqualTo(TYPE.toUpperCase());
		});
	}

	@Test
	void updateWhenNotFound() {
		// Act
		final var request = DocumentTypeUpdateRequest.create();
		final var e = assertThrows(ThrowableProblem.class, () -> service.update(MUNICIPALITY_ID, TYPE, request));

		// Assert
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Document type with identifier type was not found within municipality with id municipalityId");
		verify(documentTypeRepositoryMock).findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE);
		verifyNoMoreInteractions(documentTypeRepositoryMock);
	}

	@Test
	void delete() {
		// Arrange
		final var entity = DocumentTypeEntity.create()
			.withDisplayName(DISPLAY_NAME)
			.withType(TYPE);
		when(documentTypeRepositoryMock.findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE)).thenReturn(Optional.of(entity));

		// Act
		service.delete(MUNICIPALITY_ID, TYPE);

		// Assert
		verify(documentTypeRepositoryMock).findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE);
		verify(documentTypeRepositoryMock).delete(entity);
		verifyNoMoreInteractions(documentTypeRepositoryMock);
	}

	@Test
	void deleteWhenNotFound() {
		// Act
		final var e = assertThrows(ThrowableProblem.class, () -> service.delete(MUNICIPALITY_ID, TYPE));

		// Assert
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getMessage()).isEqualTo("Not Found: Document type with identifier type was not found within municipality with id municipalityId");
		verify(documentTypeRepositoryMock).findByMunicipalityIdAndType(MUNICIPALITY_ID, TYPE);
		verifyNoMoreInteractions(documentTypeRepositoryMock);
	}
}
