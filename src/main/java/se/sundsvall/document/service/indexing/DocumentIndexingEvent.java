package se.sundsvall.document.service.indexing;

import java.util.List;

/**
 * Signals that a document revision needs to be re-indexed in Elasticsearch. Dispatched via
 * Spring's {@code ApplicationEventPublisher} after the DB transaction commits and consumed by
 * {@link DocumentIndexingService}.
 * <p>
 * Only the primary-key id is passed — the listener re-loads the entity in its own read-only
 * transaction. Doing so avoids {@code LazyInitializationException} on {@code documentData} in
 * the committed-but-detached state, and gives the listener a consistent snapshot after the
 * writer's commit.
 * <p>
 * This is <em>not</em> the same as {@code DocumentEventPublisher} — that publishes external
 * audit-trail events to the eventlog service. This event is purely an in-JVM signal to the
 * local indexer.
 *
 * @param documentId      the {@code DocumentEntity.id} to (re-)index. May reference a brand-new
 *                        revision (on create / file add / file delete) or an existing revision
 *                        whose searchable fields changed (update / confidentiality / status).
 * @param dataIdsToRemove IDs of {@code DocumentDataEntity} rows that were indexed previously but
 *                        should be removed from ES (typically the file IDs dropped by a copy-on-write
 *                        delete). Empty when nothing to remove.
 */
public record DocumentIndexingEvent(String documentId, List<String> dataIdsToRemove) {

	public static DocumentIndexingEvent reindex(final String documentId) {
		return new DocumentIndexingEvent(documentId, List.of());
	}

	public static DocumentIndexingEvent reindexAfterDelete(final String documentId, final List<String> removedDataIds) {
		return new DocumentIndexingEvent(documentId, removedDataIds);
	}
}
