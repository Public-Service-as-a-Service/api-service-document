package se.sundsvall.document.service.mapper;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import se.sundsvall.dept44.models.api.paging.PagingMetaData;
import se.sundsvall.document.api.model.DocumentMatch;
import se.sundsvall.document.api.model.FileMatch;
import se.sundsvall.document.api.model.Match;
import se.sundsvall.document.api.model.PagedDocumentMatchResponse;
import se.sundsvall.document.integration.elasticsearch.DocumentIndexEntity;

/**
 * Maps Elasticsearch hits directly to the stripped search-match response, without DB hydration.
 * Separated from {@link DocumentMapper} because the source (ES hits) and destination (match
 * response) are both distinct from the DB-centric mappings.
 */
public final class DocumentSearchMapper {

	private DocumentSearchMapper() {}

	public static PagedDocumentMatchResponse toPagedDocumentMatchResponse(SearchHits<DocumentIndexEntity> hits, List<String> queries, Pageable pageable, boolean onlyLatestRevision) {
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

		// Tokenize queries once per response — the same term set drives match computation for every
		// hit. Uses Unicode word segmentation (BreakIterator) to mirror ES's StandardAnalyzer
		// behaviour: word boundaries per UAX #29 and ROOT-locale lowercasing. Close enough that
		// exact-term matches line up between what ES scored and what we highlight; fuzzy/synonym
		// matches are out of scope here (see class comment in DocumentSearchService).
		final var queryTerms = tokenizeQueries(queries);

		// Carry the first hit per documentId into a small metadata record so we can set
		// registrationNumber/revision on the DocumentMatch without a second pass. Every hit under
		// the same documentId reports the same regNum+revision anyway (documentId is the
		// revision-scoped DocumentEntity ID — copy-on-write mints a new one per revision).
		final var filesByDocumentId = new LinkedHashMap<String, List<FileMatch>>();
		final var metaByDocumentId = new LinkedHashMap<String, DocumentMeta>();
		for (final var hit : hits.getSearchHits()) {
			final var entity = hit.getContent();
			if (onlyLatestRevision && entity.getRevision() < maxRevisionByRegistrationNumber.get(entity.getRegistrationNumber())) {
				continue;
			}
			// getHighlightFields() returns an empty map (never null) when no highlights matched —
			// still normalize to null on empty so the JSON field is omitted rather than rendered as {}.
			final var rawHighlights = hit.getHighlightFields();
			final var highlights = (rawHighlights == null || rawHighlights.isEmpty()) ? null : rawHighlights;
			final var matches = computeMatches(entity.getExtractedText(), entity.getPageOffsets(), queryTerms);
			filesByDocumentId.computeIfAbsent(entity.getDocumentId(), k -> new ArrayList<>())
				.add(FileMatch.create()
					.withId(entity.getId())
					.withFileName(entity.getFileName())
					.withHighlights(highlights)
					.withPageCount(entity.getPageCount())
					.withMatches(matches)
					.withExtractionStatus(entity.getExtractionStatus())
					.withScore(hit.getScore())
					.withConfidential(entity.isConfidential()));
			metaByDocumentId.putIfAbsent(entity.getDocumentId(), new DocumentMeta(entity.getRegistrationNumber(), entity.getRevision()));
		}

		final var documents = filesByDocumentId.entrySet().stream()
			.map(entry -> {
				final var meta = metaByDocumentId.get(entry.getKey());
				return DocumentMatch.create()
					.withId(entry.getKey())
					.withRegistrationNumber(meta.registrationNumber())
					.withRevision(meta.revision())
					.withFiles(entry.getValue());
			})
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

	/**
	 * Walk {@code extractedText} with {@link BreakIterator} and record every word-span whose
	 * lowercased form is one of the query terms. Each match carries its char offsets and the
	 * 1-based page number the offset falls on (null for formats without {@code pageOffsets}).
	 * <p>
	 * Empty list is returned when there's no text to scan or when the query produced no terms —
	 * the response field is always an empty list rather than null so the JSON shape stays stable
	 * for consumers.
	 */
	static List<Match> computeMatches(final String extractedText, final List<Integer> pageOffsets, final Set<String> queryTerms) {
		if (extractedText == null || extractedText.isEmpty() || queryTerms.isEmpty()) {
			return List.of();
		}
		final var results = new ArrayList<Match>();
		final var it = BreakIterator.getWordInstance(Locale.ROOT);
		it.setText(extractedText);
		var start = it.first();
		for (var end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
			final var token = extractedText.substring(start, end).toLowerCase(Locale.ROOT);
			if (queryTerms.contains(token)) {
				results.add(Match.create()
					.withField("extractedText")
					.withStart(start)
					.withEnd(end)
					.withPage(resolvePage(start, pageOffsets)));
			}
		}
		return results;
	}

	/**
	 * Binary search: returns the 1-based index of the largest page-offset ≤ {@code offset}, or
	 * null when {@code pageOffsets} is missing (non-paged formats, or rows not yet backfilled).
	 */
	static Integer resolvePage(final int offset, final List<Integer> pageOffsets) {
		if (pageOffsets == null || pageOffsets.isEmpty()) {
			return null;
		}
		// Collections.binarySearch returns (-(insertion point) - 1) on miss. We want the largest
		// index where pageOffsets[i] <= offset — that's insertion_point - 1 on miss, or the exact
		// index on hit.
		final var probe = Collections.binarySearch(pageOffsets, offset);
		final var idx = probe >= 0 ? probe : -probe - 2;
		if (idx < 0) {
			// offset lies before the first page offset — defensive, shouldn't happen since
			// pageOffsets[0] is always 0 for a well-formed extraction.
			return 1;
		}
		return idx + 1;
	}

	static Set<String> tokenizeQueries(final List<String> queries) {
		if (queries == null || queries.isEmpty()) {
			return Set.of();
		}
		final var terms = new HashSet<String>();
		final var it = BreakIterator.getWordInstance(Locale.ROOT);
		for (final var query : queries) {
			if (query == null || query.isBlank()) {
				continue;
			}
			it.setText(query);
			var start = it.first();
			for (var end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
				final var token = query.substring(start, end).toLowerCase(Locale.ROOT);
				// BreakIterator yields whitespace/punctuation segments too — keep only tokens that
				// contain at least one letter or digit so we're matching words, not spaces.
				if (hasLetterOrDigit(token)) {
					terms.add(token);
				}
			}
		}
		return terms;
	}

	private static boolean hasLetterOrDigit(final String s) {
		for (var i = 0; i < s.length(); i++) {
			if (Character.isLetterOrDigit(s.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	private record DocumentMeta(String registrationNumber, int revision) {
	}
}
