package se.sundsvall.document.service;

import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;

import static org.springframework.util.CollectionUtils.isEmpty;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocument;
import static se.sundsvall.document.service.mapper.DocumentMapper.toDocumentResponsibilities;
import static se.sundsvall.document.service.mapper.DocumentMapper.toPagedDocumentResponse;

/**
 * Attaches responsibilities to response DTOs. Responsibilities live in a side-table keyed on
 * {@code (municipalityId, registrationNumber)} — not on {@link DocumentEntity} directly — so the
 * read path has to look them up separately. Every service that returns {@link Document} or
 * {@link PagedDocumentResponse} funnels through here to keep that lookup consistent.
 * <p>
 * For paged responses the lookup is batched: one query fetches responsibilities for all distinct
 * registration numbers on the page, rather than N round-trips.
 */
@Component
public class DocumentResponseHydrator {

	private final DocumentResponsibilityRepository documentResponsibilityRepository;

	public DocumentResponseHydrator(final DocumentResponsibilityRepository documentResponsibilityRepository) {
		this.documentResponsibilityRepository = documentResponsibilityRepository;
	}

	public Document hydrate(final DocumentEntity documentEntity) {
		final var responsibilities = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberOrderByPersonIdAsc(
			documentEntity.getMunicipalityId(), documentEntity.getRegistrationNumber());
		return toDocument(documentEntity, responsibilities);
	}

	public PagedDocumentResponse hydrate(final Page<DocumentEntity> documentEntityPage) {
		final var response = toPagedDocumentResponse(documentEntityPage);
		if (response == null || isEmpty(response.getDocuments())) {
			return response;
		}

		final var documents = response.getDocuments();
		final var responsibilitiesByRegistrationNumber = documentResponsibilityRepository.findByMunicipalityIdAndRegistrationNumberIn(
			documents.get(0).getMunicipalityId(),
			documents.stream()
				.map(Document::getRegistrationNumber)
				.distinct()
				.toList()).stream()
			.collect(Collectors.groupingBy(DocumentResponsibilityEntity::getRegistrationNumber));

		documents.forEach(document -> document.setResponsibilities(toDocumentResponsibilities(responsibilitiesByRegistrationNumber.get(document.getRegistrationNumber()))));

		return response;
	}
}
