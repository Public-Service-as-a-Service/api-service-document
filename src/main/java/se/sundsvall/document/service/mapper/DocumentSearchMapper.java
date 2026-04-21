package se.sundsvall.document.service.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;
import se.sundsvall.document.api.model.DocumentMatch;
import se.sundsvall.document.api.model.FileMatch;
import se.sundsvall.document.api.model.PagedDocumentMatchResponse;
import se.sundsvall.document.integration.elasticsearch.DocumentIndexEntity;

/**
 * Maps Elasticsearch hits directly to the stripped search-match response, without DB hydration.
 * Separated from {@link DocumentMapper} because the source (ES hits) and destination (match
 * response) are both distinct from the DB-centric mappings.
 */
public final class DocumentSearchMapper {

	private DocumentSearchMapper() {}

	public static PagedDocumentMatchResponse toPagedDocumentMatchResponse(SearchHits<DocumentIndexEntity> hits, Pageable pageable, boolean onlyLatestRevision) {
		// ES hits are file-level (one doc per file). Group by parent documentId, preserving
		// ES relevance order, so the response lists each matched document once with only
		// the files that actually matched. All data needed is on DocumentIndexEntity — no
		// DB hydration.

		// onlyLatestRevision is page-local: we only compare revisions among hits on this page, so a
		// document whose latest revision lives on another page will still survive the filter here.
		// This keeps _meta.totalRecords (file-level ES hit total) coherent with what the page shows
		// — a global filter would require a second ES roundtrip per registrationNumber.
		final var maxRevisionByRegistrationNumber = new HashMap<String, Integer>();
		if (onlyLatestRevision) {
			for (final var hit : hits.getSearchHits()) {
				final var entity = hit.getContent();
				maxRevisionByRegistrationNumber.merge(entity.getRegistrationNumber(), entity.getRevision(), Math::max);
			}
		}

		final var filesByDocumentId = new LinkedHashMap<String, List<FileMatch>>();
		for (final var hit : hits.getSearchHits()) {
			final var entity = hit.getContent();
			if (onlyLatestRevision && entity.getRevision() < maxRevisionByRegistrationNumber.get(entity.getRegistrationNumber())) {
				continue;
			}
			// getHighlightFields() returns an empty map (never null) when no highlights matched —
			// still normalize to null on empty so the JSON field is omitted rather than rendered as {}.
			final var rawHighlights = hit.getHighlightFields();
			final var highlights = (rawHighlights == null || rawHighlights.isEmpty()) ? null : rawHighlights;
			filesByDocumentId.computeIfAbsent(entity.getDocumentId(), k -> new ArrayList<>())
				.add(FileMatch.create()
					.withId(entity.getId())
					.withFileName(entity.getFileName())
					.withHighlights(highlights));
		}

		final var documents = filesByDocumentId.entrySet().stream()
			.map(entry -> DocumentMatch.create()
				.withId(entry.getKey())
				.withFiles(entry.getValue()))
			.toList();

		final var totalRecords = hits.getTotalHits();
		final var pageSize = pageable.getPageSize();
		final var totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) totalRecords / pageSize);

		return PagedDocumentMatchResponse.create()
			.withDocuments(documents)
			.withMetaData(PagingMetaData.create()
				.withPage(pageable.getPageNumber())
				.withLimit(pageSize)
				.withCount(documents.size())
				.withTotalRecords(totalRecords)
				.withTotalPages(totalPages));
	}
}
