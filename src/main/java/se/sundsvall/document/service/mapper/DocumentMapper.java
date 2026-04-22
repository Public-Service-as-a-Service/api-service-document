package se.sundsvall.document.service.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;
import se.sundsvall.document.api.model.Confidentiality;
import se.sundsvall.document.api.model.ConfidentialityUpdateRequest;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.DocumentCreateRequest;
import se.sundsvall.document.api.model.DocumentMetadata;
import se.sundsvall.document.api.model.DocumentResponsibility;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.api.model.DocumentUpdateRequest;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.integration.db.model.ConfidentialityEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.service.storage.BinaryStore;

import static java.util.Collections.emptyList;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;
import static se.sundsvall.document.service.InclusionFilter.PUBLIC;

/**
 * Core document mapping: API ↔ DB for Document/DocumentEntity and the small value objects
 * (metadata, confidentiality, responsibilities). File/attachment handling lives in
 * {@link DocumentDataMapper}; search hit → response mapping lives in {@link DocumentSearchMapper}.
 */
public final class DocumentMapper {

	private DocumentMapper() {}

	/**
	 * API to Database mappings.
	 */

	public static DocumentEntity toDocumentEntity(DocumentCreateRequest documentCreateRequest, String municipalityId) {
		return DocumentEntity.create()
			.withArchive(documentCreateRequest.isArchive())
			.withConfidentiality(toConfidentialityEmbeddable(documentCreateRequest.getConfidentiality()))
			.withCreatedBy(documentCreateRequest.getCreatedBy())
			.withTitle(documentCreateRequest.getTitle())
			.withDescription(documentCreateRequest.getDescription())
			.withMetadata(toDocumentMetadataEmbeddableList(documentCreateRequest.getMetadataList()))
			.withMunicipalityId(municipalityId)
			.withStatus(DocumentStatus.DRAFT)
			.withValidFrom(documentCreateRequest.getValidFrom())
			.withValidTo(documentCreateRequest.getValidTo());
	}

	public static List<Boolean> toInclusionFilter(boolean includeConfidential) {
		if (includeConfidential) {
			return CONFIDENTIAL_AND_PUBLIC.getValue();
		}
		return PUBLIC.getValue();
	}

	public static void applyUpdate(DocumentUpdateRequest request, DocumentEntity entity) {
		Optional.ofNullable(request.getArchive()).ifPresent(entity::setArchive);
		Optional.ofNullable(request.getTitle()).ifPresent(entity::setTitle);
		Optional.ofNullable(request.getDescription()).ifPresent(entity::setDescription);
		Optional.ofNullable(request.getUpdatedBy()).ifPresent(entity::setUpdatedBy);
		Optional.ofNullable(request.getValidFrom()).ifPresent(entity::setValidFrom);
		Optional.ofNullable(request.getValidTo()).ifPresent(entity::setValidTo);
		Optional.ofNullable(request.getMetadataList())
			.map(DocumentMapper::toDocumentMetadataEmbeddableList)
			.ifPresent(newMetadata -> replaceMetadata(entity, newMetadata));
	}

	private static void replaceMetadata(DocumentEntity entity, List<DocumentMetadataEmbeddable> newMetadata) {
		if (entity.getMetadata() != null) {
			entity.getMetadata().clear();
			entity.getMetadata().addAll(newMetadata);
		} else {
			entity.setMetadata(new ArrayList<>(newMetadata));
		}
	}

	public static ConfidentialityEmbeddable toConfidentialityEmbeddable(Confidentiality confidentiality) {
		return Optional.ofNullable(confidentiality)
			.map(c -> ConfidentialityEmbeddable.create()
				.withConfidential(c.isConfidential())
				.withLegalCitation(c.getLegalCitation()))
			.orElse(ConfidentialityEmbeddable.create());
	}

	public static ConfidentialityEmbeddable toConfidentialityEmbeddable(ConfidentialityUpdateRequest confidentialityUpdateRequest) {
		return Optional.ofNullable(confidentialityUpdateRequest)
			.map(c -> ConfidentialityEmbeddable.create()
				.withConfidential(c.getConfidential())
				.withLegalCitation(c.getLegalCitation()))
			.orElse(ConfidentialityEmbeddable.create());
	}

	public static List<DocumentResponsibilityEntity> toDocumentResponsibilityEntities(final List<DocumentResponsibility> responsibilities, final String municipalityId, final String registrationNumber, final String createdBy) {
		return Optional.ofNullable(responsibilities).orElse(emptyList()).stream()
			.map(responsibility -> toDocumentResponsibilityEntity(responsibility, municipalityId, registrationNumber, createdBy))
			.toList();
	}

	public static DocumentResponsibilityEntity toDocumentResponsibilityEntity(final DocumentResponsibility responsibility, final String municipalityId, final String registrationNumber, final String createdBy) {
		return Optional.ofNullable(responsibility)
			.map(value -> DocumentResponsibilityEntity.create()
				.withMunicipalityId(municipalityId)
				.withRegistrationNumber(registrationNumber)
				.withPersonId(value.getPersonId())
				.withCreatedBy(createdBy))
			.orElse(null);
	}

	/**
	 * Database to API mappings.
	 */

	public static List<Document> toDocumentList(List<DocumentEntity> documentEntityList) {
		return Optional.ofNullable(documentEntityList).orElse(emptyList()).stream()
			.map(DocumentMapper::toDocument)
			.toList();
	}

	public static PagedDocumentResponse toPagedDocumentResponse(Page<DocumentEntity> documentEntityPage) {
		return Optional.ofNullable(documentEntityPage)
			.map(page -> PagedDocumentResponse.create()
				.withDocuments(toDocumentList(page.getContent()))
				.withMetaData(PagingMetaData.create()
					.withPage(page.getPageable().getPageNumber())
					.withLimit(page.getPageable().getPageSize())
					.withCount(page.getNumberOfElements())
					.withTotalRecords(page.getTotalElements())
					.withTotalPages(page.getTotalPages())))
			.orElse(null);
	}

	public static Document toDocument(DocumentEntity documentEntity) {
		return toDocument(documentEntity, emptyList());
	}

	public static Document toDocument(DocumentEntity documentEntity, List<DocumentResponsibilityEntity> responsibilities) {
		return Optional.ofNullable(documentEntity)
			.map(docEntity -> Document.create()
				.withConfidentiality(toConfidentiality(docEntity.getConfidentiality()))
				.withArchive(docEntity.isArchive())
				.withCreated(docEntity.getCreated())
				.withCreatedBy(docEntity.getCreatedBy())
				.withUpdatedBy(docEntity.getUpdatedBy())
				.withTitle(docEntity.getTitle())
				.withDescription(docEntity.getDescription())
				.withDocumentData(DocumentDataMapper.toDocumentDataList(docEntity.getDocumentData()))
				.withId(docEntity.getId())
				.withMetadataList(toDocumentMetadataList(docEntity.getMetadata()))
				.withMunicipalityId(docEntity.getMunicipalityId())
				.withRegistrationNumber(docEntity.getRegistrationNumber())
				.withResponsibilities(toDocumentResponsibilities(responsibilities))
				.withRevision(docEntity.getRevision())
				.withStatus(docEntity.getStatus())
				.withType(docEntity.getType().getType())
				.withValidFrom(docEntity.getValidFrom())
				.withValidTo(docEntity.getValidTo()))
			.orElse(null);
	}

	public static Confidentiality toConfidentiality(ConfidentialityEmbeddable confidentialityEmbedded) {
		return Optional.ofNullable(confidentialityEmbedded)
			.map(c -> Confidentiality.create()
				.withConfidential(c.isConfidential())
				.withLegalCitation(c.getLegalCitation()))
			.orElse(null);
	}

	public static List<DocumentResponsibility> toDocumentResponsibilities(final List<DocumentResponsibilityEntity> responsibilities) {
		return Optional.ofNullable(responsibilities).orElse(emptyList()).stream()
			.map(DocumentMapper::toDocumentResponsibility)
			.toList();
	}

	public static DocumentResponsibility toDocumentResponsibility(final DocumentResponsibilityEntity responsibility) {
		return Optional.ofNullable(responsibility)
			.map(value -> DocumentResponsibility.create()
				.withPersonId(value.getPersonId()))
			.orElse(null);
	}

	/**
	 * Database to Database mappings.
	 */

	public static DocumentEntity copyDocumentEntity(DocumentEntity documentEntity, BinaryStore binaryStore) {
		return Optional.ofNullable(documentEntity)
			.map(docEntity -> DocumentEntity.create()
				.withConfidentiality(docEntity.getConfidentiality())
				.withArchive(documentEntity.isArchive())
				.withCreatedBy(docEntity.getCreatedBy())
				.withTitle(docEntity.getTitle())
				.withDescription(docEntity.getDescription())
				.withDocumentData(DocumentDataMapper.copyDocumentDataEntities(docEntity.getDocumentData(), binaryStore))
				.withMetadata(copyDocumentMetadataEmbeddableList(docEntity.getMetadata()))
				.withMunicipalityId(docEntity.getMunicipalityId())
				.withRegistrationNumber(docEntity.getRegistrationNumber())
				.withRevision(docEntity.getRevision())
				.withStatus(DocumentStatus.DRAFT)
				.withType(docEntity.getType())
				.withValidFrom(docEntity.getValidFrom())
				.withValidTo(docEntity.getValidTo()))
			.orElse(null);
	}

	/**
	 * Private methods
	 */

	private static List<DocumentMetadataEmbeddable> copyDocumentMetadataEmbeddableList(List<DocumentMetadataEmbeddable> documentMetadataEmbeddableList) {
		return Optional.ofNullable(documentMetadataEmbeddableList).orElse(emptyList()).stream()
			.map(docMetadataEmbeddable -> DocumentMetadataEmbeddable.create()
				.withKey(docMetadataEmbeddable.getKey())
				.withValue(docMetadataEmbeddable.getValue()))
			.toList();
	}

	private static List<DocumentMetadataEmbeddable> toDocumentMetadataEmbeddableList(List<DocumentMetadata> documentMetadataList) {
		return Optional.ofNullable(documentMetadataList).orElse(emptyList()).stream()
			.map(documentMetadata -> DocumentMetadataEmbeddable.create()
				.withKey(documentMetadata.getKey())
				.withValue(documentMetadata.getValue()))
			.toList();
	}

	private static List<DocumentMetadata> toDocumentMetadataList(List<DocumentMetadataEmbeddable> documentMetadataEmbeddableList) {
		return Optional.ofNullable(documentMetadataEmbeddableList).orElse(emptyList()).stream()
			.map(docMetadataEmbeddable -> DocumentMetadata.create()
				.withKey(docMetadataEmbeddable.getKey())
				.withValue(docMetadataEmbeddable.getValue()))
			.toList();
	}
}
