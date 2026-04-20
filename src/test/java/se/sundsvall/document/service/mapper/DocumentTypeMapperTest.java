package se.sundsvall.document.service.mapper;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.document.api.model.DocumentTypeCreateRequest;
import se.sundsvall.document.api.model.DocumentTypeUpdateRequest;
import se.sundsvall.document.integration.db.model.DocumentTypeEntity;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTypeMapperTest {

	private static final String MUNICIPALITY_ID = "muncipalityId";
	private static final String CREATED_BY = "b0000000-0000-0000-0000-0000000000c1";
	private static final String TYPE = "type";
	private static final String DISPLAY_NAME = "displayName";
	private static final String UPDATED_BY = "b0000000-0000-0000-0000-0000000000d1";

	@Test
	void toDocumentTypeEntity() {
		// Arrange
		final var request = DocumentTypeCreateRequest.create()
			.withCreatedBy(CREATED_BY)
			.withDisplayName(DISPLAY_NAME)
			.withType(TYPE);

		// Act
		final var entity = DocumentTypeMapper.toDocumentTypeEntity(MUNICIPALITY_ID, request);

		// Assert
		assertThat(entity).isNotNull().hasAllNullFieldsOrPropertiesExcept("createdBy", "displayName", "municipalityId", "type");
		assertThat(entity.getCreatedBy()).isEqualTo(CREATED_BY);
		assertThat(entity.getDisplayName()).isEqualTo(DISPLAY_NAME);
		assertThat(entity.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(entity.getType()).isEqualTo(TYPE.toUpperCase());
	}

	@Test
	void toDocumentTypeEntityFromNull() {
		assertThat(DocumentTypeMapper.toDocumentTypeEntity(MUNICIPALITY_ID, null)).isNull();
	}

	@Test
	void updateDocumentTypeEntity() {
		// Arrange
		final var entity = DocumentTypeEntity.create();
		final var request = DocumentTypeUpdateRequest.create()
			.withDisplayName(DISPLAY_NAME)
			.withType(TYPE)
			.withUpdatedBy(UPDATED_BY);

		// Act
		final var updatedEntity = DocumentTypeMapper.updateDocumentTypeEntity(entity, request);

		// Assert
		assertThat(updatedEntity).isEqualTo(entity).hasAllNullFieldsOrPropertiesExcept("displayName", "lastUpdatedBy", "type");
		assertThat(updatedEntity.getDisplayName()).isEqualTo(DISPLAY_NAME);
		assertThat(updatedEntity.getLastUpdatedBy()).isEqualTo(UPDATED_BY);
		assertThat(updatedEntity.getType()).isEqualTo(TYPE.toUpperCase());
	}

	@Test
	void updateDocumentTypeEntityWhenEntityNull() {
		assertThat(DocumentTypeMapper.updateDocumentTypeEntity(null, DocumentTypeUpdateRequest.create())).isNull();
	}

	@Test
	void updateDocumentTypeEntityWhenRequestNull() {
		assertThat(DocumentTypeMapper.updateDocumentTypeEntity(DocumentTypeEntity.create(), null)).isEqualTo(DocumentTypeEntity.create());
	}

	@Test
	void toDocumentTypes() {
		// Arrange
		final var entities = List.of(
			DocumentTypeEntity.create().withDisplayName("Abc").withType(TYPE + "_1"),
			DocumentTypeEntity.create().withDisplayName("Abb").withType(TYPE + "_2"));

		// Act
		final var beans = DocumentTypeMapper.toDocumentTypes(entities);

		// Assert
		assertThat(beans).hasSize(2).satisfiesExactly(type -> {
			assertThat(type.getType()).isEqualTo(TYPE + "_2");
		}, type -> {
			assertThat(type.getType()).isEqualTo(TYPE + "_1");
		});
	}

	@Test
	void toDocumentTypesFromNull() {
		assertThat(DocumentTypeMapper.toDocumentTypes(null)).isEmpty();
	}

	@Test
	void toDocumentTypesWithListContainingNulls() {
		// Arrange
		final var entities = new ArrayList<DocumentTypeEntity>();
		entities.add(null);
		entities.add(DocumentTypeEntity.create().withType(TYPE));

		// Act
		final var result = DocumentTypeMapper.toDocumentTypes(entities);

		// Assert
		assertThat(result).hasSize(1).doesNotContainNull().satisfiesExactlyInAnyOrder(type -> {
			assertThat(type.getType()).isEqualTo(TYPE);
		});
	}

	@Test
	void toDocumentType() {
		// Arrange
		final var entity = DocumentTypeEntity.create()
			.withDisplayName(DISPLAY_NAME)
			.withType(TYPE);

		// Act
		final var bean = DocumentTypeMapper.toDocumentType(entity);

		// Assert
		assertThat(bean).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(bean.getDisplayName()).isEqualTo(DISPLAY_NAME);
		assertThat(bean.getType()).isEqualTo(TYPE);
	}

	@Test
	void toDocumentTypeFromNull() {
		assertThat(DocumentTypeMapper.toDocumentType(null)).isNull();
	}
}
