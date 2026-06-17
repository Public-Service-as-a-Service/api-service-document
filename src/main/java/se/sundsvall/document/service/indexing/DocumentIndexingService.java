package se.sundsvall.document.service.indexing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.elasticsearch.DocumentIndexEntity;
import se.sundsvall.document.integration.elasticsearch.DocumentIndexRepository;

import static java.util.Optional.ofNullable;

/**
 * Indexes a document's current state into Elasticsearch after the DB transaction commits.
 * <p>
 * Runs synchronously on the caller's HTTP thread — upload latency includes the ES round-trip.
 * An ES outage does not break the upload: the DB state is already committed, this listener
 * just logs and bumps a failure counter. A later re-index job (out of scope in v1) can recover
 * by re-reading {@code DocumentDataEntity.extracted_text}.
 */
@Service
@ConditionalOnProperty(name = "document.search.enabled", havingValue = "true", matchIfMissing = true)
public class DocumentIndexingService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentIndexingService.class);

	private final DocumentRepository documentRepository;
	private final DocumentIndexRepository indexRepository;
	private final Counter indexFailures;

	public DocumentIndexingService(
		final DocumentRepository documentRepository,
		final DocumentIndexRepository indexRepository,
		final MeterRegistry meterRegistry) {

		this.documentRepository = documentRepository;
		this.indexRepository = indexRepository;
		this.indexFailures = Counter.builder("document.es.index.failures")
			.description("Count of document indexing attempts that failed to write to Elasticsearch")
			.register(meterRegistry);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void onIndexing(final DocumentIndexingEvent event) {
		DocumentEntity revision = null;
		try {
			revision = documentRepository.findById(event.documentId()).orElse(null);
			if (revision != null) {
				final var docs = toIndexEntities(revision);
				if (!docs.isEmpty()) {
					indexRepository.saveAll(docs);
					LOGGER.info("Indexed {} file(s) for registrationNumber='{}' revision={}",
						docs.size(), revision.getRegistrationNumber(), revision.getRevision());
				}
			} else {
				LOGGER.debug("No document found for id='{}' — skipping indexing (likely deleted between commit and event)", event.documentId());
			}
			if (!event.dataIdsToRemove().isEmpty()) {
				indexRepository.deleteAllById(event.dataIdsToRemove());
				LOGGER.info("Removed {} stale file index entr(y/ies) ids={}",
					event.dataIdsToRemove().size(), event.dataIdsToRemove());
			}
		} catch (final Exception e) {
			indexFailures.increment();
			LOGGER.warn("Failed to index document in Elasticsearch (documentId='{}', registrationNumber='{}', revision={})",
				event.documentId(),
				revision != null ? revision.getRegistrationNumber() : "<unknown>",
				revision != null ? revision.getRevision() : -1,
				e);
		}
	}

	private static List<DocumentIndexEntity> toIndexEntities(final DocumentEntity revision) {
		final var dataList = ofNullable(revision.getDocumentData()).orElse(List.of());
		final var result = new ArrayList<DocumentIndexEntity>(dataList.size());
		for (final var data : dataList) {
			result.add(toIndexEntity(revision, data));
		}
		return result;
	}

	private static DocumentIndexEntity toIndexEntity(final DocumentEntity revision, final DocumentDataEntity data) {
		final var metadataKeys = new ArrayList<String>();
		final var metadataValues = new ArrayList<String>();
		for (final var entry : ofNullable(revision.getMetadata()).orElse(List.<DocumentMetadataEmbeddable>of())) {
			if (entry.getKey() != null) {
				metadataKeys.add(entry.getKey());
			}
			if (entry.getValue() != null) {
				metadataValues.add(entry.getValue());
			}
		}

		final var confidential = revision.getConfidentiality() != null && revision.getConfidentiality().isConfidential();
		final var documentType = revision.getType() != null ? revision.getType().getType() : null;

		return new DocumentIndexEntity()
			.setId(data.getId())
			.setDocumentId(revision.getId())
			.setRegistrationNumber(revision.getRegistrationNumber())
			.setRevision(revision.getRevision())
			.setMunicipalityId(revision.getMunicipalityId())
			.setDocumentType(documentType)
			.setStatus(revision.getStatus())
			.setConfidential(confidential)
			.setValidFrom(revision.getValidFrom())
			.setValidTo(revision.getValidTo())
			.setFileName(data.getFileName())
			.setMimeType(data.getMimeType())
			.setTitle(revision.getTitle())
			.setDescription(revision.getDescription())
			.setCreatedBy(revision.getCreatedBy())
			.setMetadataKeys(metadataKeys)
			.setMetadataValues(metadataValues)
			.setExtractedText(data.getExtractedText())
			.setExtractionStatus(data.getExtractionStatus())
			.setPageCount(data.getPageCount())
			.setPageOffsets(data.getPageOffsets());
	}
}
