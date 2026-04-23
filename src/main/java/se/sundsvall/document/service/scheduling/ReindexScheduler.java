package se.sundsvall.document.service.scheduling;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashSet;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.document.integration.db.DocumentDataRepository;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.service.extraction.ExtractionStatus;
import se.sundsvall.document.service.extraction.PagedMimeTypes;
import se.sundsvall.document.service.extraction.TextExtractor;
import se.sundsvall.document.service.indexing.DocumentIndexingEvent;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.StorageRef;

/**
 * Backfill job that rehydrates extracted text and page metadata for {@code document_data} rows
 * that pre-date the current extractor. Covers two historical gaps in one pass:
 * <ul>
 * <li><b>V1_11 legacy</b>: rows marked {@code PENDING_REINDEX} that never had Tika run against
 * them (the V1_11 migration added {@code extracted_text} with that status as the default but
 * shipped without a running backfill).</li>
 * <li><b>V1_15 page data</b>: rows with {@code extraction_status = SUCCESS} but no
 * {@code page_count}, where the MIME type is a paged format — they have text but lack the
 * page-offset breakdown the search mapper uses to resolve a match to a page.</li>
 * </ul>
 * Runs as a scheduled, ShedLock-guarded batch so multi-pod deployments don't collide. Each row
 * is persisted through a single {@link DocumentDataRepository#save} call so its transaction
 * boundary is isolated from the rest of the batch — a poisoned file flips to {@code FAILED}
 * without rolling back previously-reprocessed peers.
 * <p>
 * On extraction failure the row is marked {@code FAILED} with no text — same contract as a
 * fresh upload — so the job never loops forever on a single bad file.
 */
@Component
@ConditionalOnProperty(name = "document.reindex-job.enabled", havingValue = "true", matchIfMissing = true)
public class ReindexScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReindexScheduler.class);

	private final int batchSize;
	private final DocumentDataRepository documentDataRepository;
	private final BinaryStore binaryStore;
	private final TextExtractor textExtractor;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final Counter reindexSuccess;
	private final Counter reindexFailures;
	private final Timer reindexDuration;

	public ReindexScheduler(
		@Value("${document.reindex-job.batch-size:50}") final int batchSize,
		final DocumentDataRepository documentDataRepository,
		final BinaryStore binaryStore,
		final TextExtractor textExtractor,
		final ApplicationEventPublisher applicationEventPublisher,
		final MeterRegistry meterRegistry) {

		this.batchSize = batchSize;
		this.documentDataRepository = documentDataRepository;
		this.binaryStore = binaryStore;
		this.textExtractor = textExtractor;
		this.applicationEventPublisher = applicationEventPublisher;
		this.reindexSuccess = Counter.builder("document.reindex.success")
			.description("Count of document_data rows successfully reprocessed by the reindex scheduler")
			.register(meterRegistry);
		this.reindexFailures = Counter.builder("document.reindex.failures")
			.description("Count of document_data rows that failed reprocessing in the reindex scheduler")
			.register(meterRegistry);
		this.reindexDuration = Timer.builder("document.reindex.duration")
			.description("Wall-clock duration of a single document_data row reprocessing attempt")
			.register(meterRegistry);
		// Backlog gauge: lets operators watch the candidate pool drain without scraping the DB.
		// Polled on each meter scrape — the query is cheap (count over a predicate backed by
		// extraction_status + a small mime-type IN clause).
		Gauge.builder("document.reindex.backlog",
			documentDataRepository, repo -> repo.countReindexCandidates(PagedMimeTypes.ALL))
			.description("Count of document_data rows currently awaiting reindex reprocessing")
			.register(meterRegistry);
	}

	/**
	 * {@code Propagation.NOT_SUPPORTED} so no outer transaction is held open across the batch —
	 * each {@link #reprocess} call gets its own short-lived repository transaction (via
	 * {@link DocumentDataRepository#save}), which is exactly what we want: a poisoned row's
	 * rollback doesn't cascade into previously-successful peers on the same batch.
	 */
	@Scheduled(cron = "${document.reindex-job.cron:0 */5 * * * *}", zone = "Europe/Stockholm")
	@SchedulerLock(name = "ReindexScheduler.reindexBacklog", lockAtLeastFor = "PT30S", lockAtMostFor = "PT10M")
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void reindexBacklog() {
		final var candidates = documentDataRepository.findReindexCandidates(PagedMimeTypes.ALL, PageRequest.of(0, batchSize));

		if (candidates.isEmpty()) {
			LOGGER.debug("Reindex backlog empty — nothing to do");
			return;
		}

		// Insertion-ordered so we emit reindex events in the same order documents were picked,
		// which keeps logs grep-friendly. Dedup is the important bit — one document may own
		// multiple rows in this batch.
		final var documentIdsTouched = new LinkedHashSet<String>();
		var succeeded = 0;
		var failed = 0;
		for (final var candidate : candidates) {
			final var timerSample = Timer.start();
			try {
				reprocess(candidate);
				documentIdsTouched.add(candidate.getDocumentId());
				succeeded++;
			} catch (final Exception e) {
				failed++;
				LOGGER.warn("Reindex failed (documentDataId='{}', documentId='{}', errorType={}): {}",
					candidate.getId(), candidate.getDocumentId(), e.getClass().getSimpleName(), e.getMessage());
				markFailed(candidate.getId());
			} finally {
				timerSample.stop(reindexDuration);
			}
		}

		reindexSuccess.increment(succeeded);
		reindexFailures.increment(failed);

		// Emit reindex events after all per-row writes commit so the AFTER_COMMIT listener in
		// DocumentIndexingService sees the updated rows. Coalesced per document — one event per
		// parent, not one per file.
		documentIdsTouched.forEach(docId -> applicationEventPublisher.publishEvent(DocumentIndexingEvent.reindex(docId)));

		LOGGER.info("Reindex batch done (candidates={}, succeeded={}, failed={}, documentsReindexed={})",
			candidates.size(), succeeded, failed, documentIdsTouched.size());
	}

	private void reprocess(final DocumentDataEntity candidate) {
		final var bytes = downloadBytes(candidate.getStorageLocator());
		final var extraction = textExtractor.extract(new ByteArrayInputStream(bytes), candidate.getMimeType(), bytes.length);

		candidate.setExtractedText(extraction.text());
		candidate.setExtractionStatus(extraction.status());
		candidate.setPageCount(extraction.pageCount());
		candidate.setPageOffsets(extraction.pageOffsets());
		documentDataRepository.save(candidate);
	}

	private void markFailed(final String documentDataId) {
		// Best-effort: if even the status flip throws, we swallow so the next candidate is still
		// attempted. The row will be picked up again on the next run and will likely fail the same
		// way; that's acceptable — a truly poisoned row is rare and operator-visible via the
		// document.reindex.failures counter.
		try {
			documentDataRepository.findById(documentDataId).ifPresent(entity -> {
				entity.setExtractionStatus(ExtractionStatus.FAILED);
				entity.setExtractedText(null);
				entity.setPageCount(null);
				entity.setPageOffsets(null);
				documentDataRepository.save(entity);
			});
		} catch (final Exception e) {
			LOGGER.warn("Could not flip status to FAILED (documentDataId='{}'): {}", documentDataId, e.getMessage());
		}
	}

	private byte[] downloadBytes(final String storageLocator) {
		try (final var out = new ByteArrayOutputStream()) {
			binaryStore.streamTo(StorageRef.s3(storageLocator), out);
			return out.toByteArray();
		} catch (final Exception e) {
			throw new IllegalStateException("Failed to download bytes from storage (locator='" + storageLocator + "'): " + e.getMessage(), e);
		}
	}
}
