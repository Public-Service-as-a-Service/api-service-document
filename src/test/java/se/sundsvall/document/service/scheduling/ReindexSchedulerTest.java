package se.sundsvall.document.service.scheduling;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import se.sundsvall.document.integration.db.DocumentDataRepository;
import se.sundsvall.document.integration.db.model.DocumentDataEntity;
import se.sundsvall.document.service.extraction.ExtractionStatus;
import se.sundsvall.document.service.extraction.TextExtractor;
import se.sundsvall.document.service.extraction.TextExtractor.ExtractedText;
import se.sundsvall.document.service.indexing.DocumentIndexingEvent;
import se.sundsvall.document.service.storage.BinaryStore;
import se.sundsvall.document.service.storage.StorageRef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReindexSchedulerTest {

	@Mock
	private DocumentDataRepository documentDataRepository;

	@Mock
	private BinaryStore binaryStore;

	@Mock
	private TextExtractor textExtractor;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	private ReindexScheduler scheduler;

	@BeforeEach
	void setUp() {
		scheduler = new ReindexScheduler(50, documentDataRepository, binaryStore, textExtractor,
			applicationEventPublisher, new SimpleMeterRegistry());
	}

	@Test
	void reindexBacklog_whenNoCandidates_noopsQuietly() {
		when(documentDataRepository.findReindexCandidates(any(), any(Pageable.class))).thenReturn(List.of());

		scheduler.reindexBacklog();

		verifyNoInteractions(binaryStore, textExtractor, applicationEventPublisher);
	}

	@Test
	void reindexBacklog_successPath_reprocessesAndPublishesDedupedEvents() {
		// Two candidate rows under the same parent document — expect one reindex event, not two.
		final var row1 = candidate("data-1", "doc-A", "application/pdf", "s3://a/1");
		final var row2 = candidate("data-2", "doc-A", "application/pdf", "s3://a/2");
		when(documentDataRepository.findReindexCandidates(any(), any(Pageable.class))).thenReturn(List.of(row1, row2));
		stubDownloadReturnsBytes(new byte[] {
			1, 2, 3
		});
		when(textExtractor.extract(any(), eq("application/pdf"), anyLong()))
			.thenReturn(ExtractedText.successWithPages("text", "application/pdf", 1, List.of(0)));

		scheduler.reindexBacklog();

		verify(documentDataRepository).save(argThat(e -> "data-1".equals(e.getId())
			&& e.getExtractionStatus() == ExtractionStatus.SUCCESS
			&& e.getPageCount() == 1));
		verify(documentDataRepository).save(argThat(e -> "data-2".equals(e.getId())
			&& e.getExtractionStatus() == ExtractionStatus.SUCCESS));

		final var eventCaptor = ArgumentCaptor.forClass(DocumentIndexingEvent.class);
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getAllValues()).hasSize(1);
		assertThat(eventCaptor.getValue().documentId()).isEqualTo("doc-A");
	}

	@Test
	void reindexBacklog_whenExtractorThrows_marksRowFailedAndSkipsEvent() {
		final var row = candidate("data-bad", "doc-B", "application/pdf", "s3://bad/1");
		when(documentDataRepository.findReindexCandidates(any(), any(Pageable.class))).thenReturn(List.of(row));
		when(documentDataRepository.findById("data-bad")).thenReturn(Optional.of(row));
		stubDownloadReturnsBytes(new byte[] {
			1
		});
		when(textExtractor.extract(any(), eq("application/pdf"), anyLong()))
			.thenThrow(new RuntimeException("boom"));

		scheduler.reindexBacklog();

		// markFailed path must flip status to FAILED and wipe text/page data.
		verify(documentDataRepository).save(argThat(e -> "data-bad".equals(e.getId())
			&& e.getExtractionStatus() == ExtractionStatus.FAILED
			&& e.getExtractedText() == null
			&& e.getPageCount() == null
			&& e.getPageOffsets() == null));
		// A failed row MUST NOT produce a reindex event — we don't want ES to be updated from a
		// half-extracted state. Human intervention via re-upload is the recovery path.
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	@Test
	void reindexBacklog_whenDownloadFails_stillMarksRowFailed() throws Exception {
		final var row = candidate("data-nobytes", "doc-C", "application/pdf", "s3://missing/1");
		when(documentDataRepository.findReindexCandidates(any(), any(Pageable.class))).thenReturn(List.of(row));
		when(documentDataRepository.findById("data-nobytes")).thenReturn(Optional.of(row));
		doThrow(new RuntimeException("object not found"))
			.when(binaryStore).streamTo(any(StorageRef.class), any(OutputStream.class));

		scheduler.reindexBacklog();

		verify(documentDataRepository).save(argThat(e -> e.getExtractionStatus() == ExtractionStatus.FAILED));
		verifyNoInteractions(applicationEventPublisher);
	}

	private static DocumentDataEntity candidate(final String id, final String documentId, final String mimeType, final String storageLocator) {
		final var entity = DocumentDataEntity.create()
			.withId(id)
			.withMimeType(mimeType)
			.withStorageLocator(storageLocator)
			.withExtractionStatus(ExtractionStatus.PENDING_REINDEX);
		// documentId is read-only for Hibernate (@Column insertable=false, updatable=false), but
		// exposed via a setter so test fixtures don't need reflection.
		entity.setDocumentId(documentId);
		return entity;
	}

	private void stubDownloadReturnsBytes(final byte[] bytes) {
		try {
			doAnswer(inv -> {
				final OutputStream out = inv.getArgument(1);
				out.write(bytes);
				return null;
			}).when(binaryStore).streamTo(any(StorageRef.class), any(OutputStream.class));
		} catch (final IOException e) {
			throw new AssertionError(e);
		}
	}
}
