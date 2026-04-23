package se.sundsvall.document.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.DocumentParameters;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.api.model.PagedDocumentMatchResponse;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.elasticsearch.DocumentIndexEntity;

import static se.sundsvall.document.service.mapper.DocumentSearchMapper.toPagedDocumentMatchResponse;

/**
 * Read-only search surface for documents. Handles three flavours:
 * <ul>
 * <li>{@link #search} — Elasticsearch full-text with hydrated {@code PagedDocumentResponse}.</li>
 * <li>{@link #searchFileMatches} — Elasticsearch full-text returning a stripped response of only
 * matching files per document. Supports 1–N OR'd text queries (phrase + fuzzy per query).</li>
 * <li>{@link #searchByParameters} — SQL-based parameterized filter via {@link DocumentRepository}.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DocumentSearchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSearchService.class);

	// All text-ish fields a phrase match runs against. Phrase match requires adjacent tokens in the
	// listed order.
	private static final List<String> PHRASE_FIELDS = List.of(
		"title", "extractedText", "description", "fileName", "mimeType",
		"registrationNumber", "createdBy", "metadataKeys", "metadataValues");

	// Fields the fuzzy/AND branch searches across. registrationNumber and mimeType are intentionally
	// excluded here: fuzziness=AUTO permits 1–2 character edits, which on identifier-shaped values
	// produces noise (e.g. "DOC-2025-0001" would match "DOC-2025-0002", "application/pdf" would
	// match "application/pd…"). Exact matches on those fields are still caught by the phrase branch.
	// extractedText stays in with a reduced boost so OCR'd body text can still match but doesn't
	// drown out hits in the higher-signal fields (title, description, createdBy).
	private static final List<String> FUZZY_FIELDS = List.of(
		"title", "description", "fileName", "createdBy",
		"metadataKeys", "metadataValues", "extractedText^0.5");

	private static final float PHRASE_BOOST = 3.0f;

	private final DocumentRepository documentRepository;
	private final DocumentResponseHydrator responseHydrator;
	private final DocumentStatusPolicy statusPolicy;
	// Optional so the junit profile (which excludes the ES auto-configurations) can still boot the
	// Spring context for WebTestClient tests that don't exercise search. In production ES is always
	// present; the full-text methods will throw if called without ES wired.
	private final Optional<ElasticsearchOperations> elasticsearchOperations;

	public DocumentSearchService(
		final DocumentRepository documentRepository,
		final DocumentResponseHydrator responseHydrator,
		final DocumentStatusPolicy statusPolicy,
		final Optional<ElasticsearchOperations> elasticsearchOperations) {

		this.documentRepository = documentRepository;
		this.responseHydrator = responseHydrator;
		this.statusPolicy = statusPolicy;
		this.elasticsearchOperations = elasticsearchOperations;
	}

	public PagedDocumentResponse search(String query, boolean includeConfidential, boolean onlyLatestRevision, Pageable pageable, String municipalityId) {
		// `List.of` rejects null; route through a null-safe single-element list so callers that pass
		// a null/blank query still hit the empty-query path in the helper (instead of NPE here).
		// This endpoint defaults to published statuses only — resolved here so runFulltextSearch
		// can stay status-defaulting-free (see searchFileMatches for the contrasting policy).
		final var hits = runFulltextSearch(query == null ? List.of() : List.of(query), includeConfidential,
			statusPolicy.effectivePublishedStatuses(null), null, municipalityId, pageable);
		return hydrateHitsToPagedDocumentResponse(hits, pageable, onlyLatestRevision);
	}

	public PagedDocumentMatchResponse searchFileMatches(List<String> queries, boolean includeConfidential, boolean onlyLatestRevision, List<DocumentStatus> statuses, List<String> documentTypes, Pageable pageable, String municipalityId) {
		// Pass the caller's statuses through verbatim. This endpoint's default (no list supplied)
		// is "no status filter at all" — i.e. search every status including DRAFT and REVOKED.
		// Supplying a list narrows the search to exactly those statuses.
		final var hits = runFulltextSearch(queries, includeConfidential, statuses, documentTypes, municipalityId, pageable);
		return toPagedDocumentMatchResponse(hits, pageable, onlyLatestRevision);
	}

	public PagedDocumentResponse searchByParameters(final DocumentParameters parameters) {
		final var pageable = PageRequest.of(parameters.getPage() - 1, parameters.getLimit(), parameters.sort());
		final var effectiveStatuses = statusPolicy.effectivePublishedStatuses(parameters.getStatuses());
		return responseHydrator.hydrate(documentRepository.searchByParameters(parameters, pageable, effectiveStatuses));
	}

	private PagedDocumentResponse hydrateHitsToPagedDocumentResponse(SearchHits<DocumentIndexEntity> hits, Pageable pageable, boolean onlyLatestRevision) {
		// Collapse the per-file hits into unique documents, preserving ES relevance order.
		final var orderedDocumentIds = hits.getSearchHits().stream()
			.map(h -> h.getContent().getDocumentId())
			.distinct()
			.toList();

		if (orderedDocumentIds.isEmpty()) {
			return responseHydrator.hydrate(new PageImpl<>(List.of(), pageable, 0));
		}

		final var byId = documentRepository.findAllById(orderedDocumentIds).stream()
			.collect(Collectors.toMap(DocumentEntity::getId, e -> e, (a, b) -> a));

		var hydrated = orderedDocumentIds.stream()
			.map(byId::get)
			.filter(Objects::nonNull)
			.toList();

		if (onlyLatestRevision) {
			// Page-local: we only compare revisions among hits on this page, so a document whose
			// latest revision lives on another page will still survive the filter here. This keeps
			// _meta.totalRecords (file-level ES hit total) coherent with what the page actually
			// shows — a global filter would require a second ES roundtrip per registrationNumber.
			hydrated = hydrated.stream()
				.collect(Collectors.groupingBy(DocumentEntity::getRegistrationNumber)).values().stream()
				.map(group -> group.stream().max(Comparator.comparingInt(DocumentEntity::getRevision)).orElseThrow())
				.toList();
		}

		return responseHydrator.hydrate(new PageImpl<>(hydrated, pageable, hits.getTotalHits()));
	}

	private SearchHits<DocumentIndexEntity> runFulltextSearch(List<String> queries, boolean includeConfidential, List<DocumentStatus> statuses, List<String> documentTypes, String municipalityId, Pageable pageable) {
		// Status filter is now pass-through: callers who want a default must resolve it upfront.
		// Null/empty → no status filter (every status searched). Non-empty → narrow to that list.
		final var effectiveStatuses = statuses == null ? List.<DocumentStatus>of() : statuses;
		final var effectiveDocumentTypes = documentTypes == null ? List.<String>of()
			: documentTypes.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).toList();
		// Drop nulls/blanks defensively — the Resource already enforces @NotBlank, but service
		// callers other than HTTP (e.g. future batch jobs, tests) deserve the same safety.
		final var effectiveQueries = queries == null ? List.<String>of()
			: queries.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).toList();
		final var textSearchEnabled = !effectiveQueries.isEmpty();

		LOGGER.debug("ES search (municipalityId='{}', queries={}, textSearchEnabled={}, includeConfidential={}, statuses={}, documentTypes={}, page={}, size={})",
			municipalityId, effectiveQueries, textSearchEnabled, includeConfidential, effectiveStatuses, effectiveDocumentTypes,
			pageable.getPageNumber(), pageable.getPageSize());

		final var esQuery = NativeQuery.builder()
			.withQuery(q -> q.bool(b -> {
				if (textSearchEnabled) {
					// Multiple queries are OR'd (minimum_should_match=1); each is expanded to a
					// phrase-OR-fuzzy bool — see buildPerQueryMatcher.
					b.must(m -> m.bool(orBool -> {
						effectiveQueries.forEach(single -> orBool.should(buildPerQueryMatcher(single)));
						orBool.minimumShouldMatch("1");
						return orBool;
					}));
				}
				b.filter(f -> f.term(t -> t.field("municipalityId").value(municipalityId)));
				if (!includeConfidential) {
					b.filter(f -> f.term(t -> t.field("confidential").value(false)));
				}
				if (effectiveStatuses != null && !effectiveStatuses.isEmpty()) {
					b.filter(f -> f.terms(t -> t.field("status").terms(tt -> tt.value(
						effectiveStatuses.stream().map(s -> FieldValue.of(s.name())).toList()))));
				}
				if (!effectiveDocumentTypes.isEmpty()) {
					b.filter(f -> f.terms(t -> t.field("documentType").terms(tt -> tt.value(
						effectiveDocumentTypes.stream().map(FieldValue::of).toList()))));
				}
				return b;
			}))
			.withHighlightQuery(buildHighlightQuery())
			.withPageable(pageable)
			.build();

		final var hits = elasticsearchOperations
			.orElseThrow(() -> Problem.valueOf(HttpStatus.SERVICE_UNAVAILABLE, "Search is not available — Elasticsearch is not configured"))
			.search(esQuery, DocumentIndexEntity.class);

		LOGGER.debug("ES search returned {} file hit(s) (totalHits={}, municipalityId='{}')",
			hits.getSearchHits().size(), hits.getTotalHits(), municipalityId);
		return hits;
	}

	// A bool that OR's two clauses for a single user-supplied query string:
	// 1) phrase multi_match (adjacent tokens, boosted) — exact phrases rank highest.
	// 2) best_fields multi_match with operator=AND and fuzziness=AUTO — all terms must be present
	// in the best-scoring field but order is free, and 1–2 character typos per term are tolerated.
	private static Query buildPerQueryMatcher(String query) {
		return Query.of(q -> q.bool(b -> b
			.should(s -> s.multiMatch(mm -> mm
				.query(query)
				.type(TextQueryType.Phrase)
				.boost(PHRASE_BOOST)
				.fields(PHRASE_FIELDS)))
			.should(s -> s.multiMatch(mm -> mm
				.query(query)
				.type(TextQueryType.BestFields)
				.operator(Operator.And)
				.fuzziness("AUTO")
				.fields(FUZZY_FIELDS)))
			.minimumShouldMatch("1")));
	}

	private static HighlightQuery buildHighlightQuery() {
		// Matched fragments for each text-ish field. Fragment size is approximate — ES expands to
		// token/sentence boundaries. Tags default to <em>…</em>; callers can restyle on display.
		final var parameters = HighlightParameters.builder()
			.withFragmentSize(150)
			.withNumberOfFragments(3)
			.withPreTags("<em>")
			.withPostTags("</em>")
			.build();
		final var fields = List.of(
			new HighlightField("title"),
			new HighlightField("description"),
			new HighlightField("fileName"),
			new HighlightField("extractedText"));
		return new HighlightQuery(new Highlight(parameters, fields), DocumentIndexEntity.class);
	}
}
