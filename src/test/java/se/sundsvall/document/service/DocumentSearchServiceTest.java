package se.sundsvall.document.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import se.sundsvall.dept44.problem.ThrowableProblem;
import se.sundsvall.document.api.model.DocumentParameters;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.DocumentRepository;
import se.sundsvall.document.integration.db.DocumentResponsibilityRepository;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;
import se.sundsvall.document.integration.db.model.DocumentTypeEntity;
import se.sundsvall.document.integration.elasticsearch.DocumentIndexEntity;
import se.sundsvall.document.service.extraction.ExtractionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSearchServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final List<DocumentStatus> PUBLISHED_STATUSES = List.of(DocumentStatus.SCHEDULED, DocumentStatus.ACTIVE, DocumentStatus.EXPIRED);

	@Mock
	private DocumentRepository documentRepositoryMock;

	@Mock
	private DocumentResponsibilityRepository documentResponsibilityRepositoryMock;

	@Mock
	private DocumentStatusPolicy statusPolicyMock;

	@Mock
	private ElasticsearchOperations elasticsearchOperationsMock;

	@Mock
	private SearchHits<DocumentIndexEntity> searchHitsMock;

	private DocumentSearchService documentSearchService;

	@BeforeEach
	void setUp() {
		documentSearchService = new DocumentSearchService(
			documentRepositoryMock,
			new DocumentResponseHydrator(documentResponsibilityRepositoryMock),
			statusPolicyMock,
			Optional.of(elasticsearchOperationsMock));
	}

	@Test
	void search_whenEsReturnsNoHits_returnsEmptyPage() {
		// Arrange
		final var pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "revision"));
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(any(Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of());

		// Act
		final var result = documentSearchService.search("no-hits", false, false, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDocuments()).isEmpty();
	}

	@Test
	void searchFileMatches_whenEsReturnsNoHits_returnsEmptyPage() {
		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(any(Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of());
		when(searchHitsMock.getTotalHits()).thenReturn(0L);

		// Act
		final var result = documentSearchService.searchFileMatches(List.of("no-hits"), false, false, null, null, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getDocuments()).isEmpty();
		assertThat(result.getMetadata().getTotalRecords()).isZero();
		verifyNoInteractions(documentRepositoryMock);
	}

	@Test
	void searchFileMatches_groupsFilesByDocumentIdPreservingOrder() {
		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		final var docA = "doc-a";
		final var docB = "doc-b";
		final var hits = List.of(
			fileHit(docA, "reg-a", 1, "file-a1", "alpha.pdf"),
			fileHit(docB, "reg-b", 1, "file-b1", "beta.pdf"),
			fileHit(docA, "reg-a", 1, "file-a2", "alpha-2.pdf"));
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(any(Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(3L);

		// Act
		final var result = documentSearchService.searchFileMatches(List.of("any"), false, false, null, null, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getDocuments()).hasSize(2);
		assertThat(result.getDocuments().get(0).getId()).isEqualTo(docA);
		assertThat(result.getDocuments().get(0).getRegistrationNumber()).isEqualTo("reg-a");
		assertThat(result.getDocuments().get(0).getRevision()).isEqualTo(1);
		assertThat(result.getDocuments().get(0).getFiles())
			.extracting("id", "fileName")
			.containsExactly(tuple("file-a1", "alpha.pdf"), tuple("file-a2", "alpha-2.pdf"));
		assertThat(result.getDocuments().get(1).getId()).isEqualTo(docB);
		assertThat(result.getDocuments().get(1).getRegistrationNumber()).isEqualTo("reg-b");
		assertThat(result.getDocuments().get(1).getRevision()).isEqualTo(1);
		assertThat(result.getDocuments().get(1).getFiles())
			.extracting("id", "fileName")
			.containsExactly(tuple("file-b1", "beta.pdf"));
		verifyNoInteractions(documentRepositoryMock);
	}

	@Test
	void searchFileMatches_onlyLatestRevision_dropsOlderRevisionsOfSameRegistrationNumber() {
		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		final var hits = List.of(
			fileHit("doc-rev1", "reg-shared", 1, "file-1", "v1.pdf"),
			fileHit("doc-rev2", "reg-shared", 2, "file-2", "v2.pdf"),
			fileHit("doc-other", "reg-other", 1, "file-3", "other.pdf"));
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(any(Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(3L);

		// Act
		final var result = documentSearchService.searchFileMatches(List.of("any"), false, true, null, null, pageRequest, MUNICIPALITY_ID);

		// Assert — doc-rev1 (revision 1 of reg-shared) should be dropped because reg-shared has a revision 2 on the page.
		assertThat(result.getDocuments()).extracting("id").containsExactly("doc-rev2", "doc-other");
	}

	@Test
	void searchFileMatches_propagatesHighlightsPerFile() {
		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		final var highlightsForA = Map.of(
			"extractedText", List.of("The max <em>bandwidth</em> of this router is 10Gbit/s"),
			"title", List.of("Router <em>bandwidth</em> spec"));
		final var highlightsForB = Map.of(
			"extractedText", List.of("total <em>bandwidth</em> per node"));
		final var hits = List.of(
			fileHitWithHighlights("doc-a", "reg-a", 1, "file-a1", "alpha.pdf", highlightsForA),
			fileHitWithHighlights("doc-b", "reg-b", 1, "file-b1", "beta.pdf", highlightsForB));
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(any(Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(2L);

		// Act
		final var result = documentSearchService.searchFileMatches(List.of("bandwidth"), false, false, null, null, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getDocuments()).hasSize(2);
		assertThat(result.getDocuments().get(0).getFiles().get(0).getHighlights()).isEqualTo(highlightsForA);
		assertThat(result.getDocuments().get(1).getFiles().get(0).getHighlights()).isEqualTo(highlightsForB);
	}

	@Test
	void searchFileMatches_populatesFileMatchEnrichmentFieldsEndToEnd() {
		// Covers the fields the mapper has to lift off each hit: score (from SearchHit),
		// extractionStatus / confidential / pageCount (from DocumentIndexEntity), and the
		// server-computed matches list with per-match page resolution.
		final var pageRequest = PageRequest.of(0, 10);
		final var extractedText = "Router bandwidth spec.\nTotal bandwidth per node.";
		final var page2Offset = extractedText.indexOf("\nTotal") + 1;
		final var hit = enrichedHit("doc-1", "reg-1", 1, "file-1", "alpha.pdf",
			extractedText, List.of(0, page2Offset), 2, ExtractionStatus.SUCCESS, true, 7.42f);
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(any(Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of(hit));
		when(searchHitsMock.getTotalHits()).thenReturn(1L);

		final var result = documentSearchService.searchFileMatches(List.of("bandwidth"), false, false, null, null, pageRequest, MUNICIPALITY_ID);

		final var fileMatch = result.getDocuments().get(0).getFiles().get(0);
		assertThat(fileMatch.getPageCount()).isEqualTo(2);
		assertThat(fileMatch.getExtractionStatus()).isEqualTo(ExtractionStatus.SUCCESS);
		assertThat(fileMatch.getConfidential()).isTrue();
		assertThat(fileMatch.getScore()).isEqualTo(7.42f);
		// Two "bandwidth" occurrences — one on each page.
		assertThat(fileMatch.getMatches()).hasSize(2);
		assertThat(fileMatch.getMatches())
			.extracting("field", "page")
			.containsExactly(tuple("extractedText", 1), tuple("extractedText", 2));
		// Offsets must reference the original extractedText — verify the slice matches the query.
		assertThat(extractedText.substring(fileMatch.getMatches().get(0).getStart(), fileMatch.getMatches().get(0).getEnd()))
			.isEqualTo("bandwidth");
	}

	@Test
	void searchFileMatches_omitsHighlightsWhenNoneMatched() {
		// Arrange — existing fileHit() helper doesn't stub getHighlightFields(), so it returns null.
		final var pageRequest = PageRequest.of(0, 10);
		final var hits = List.of(fileHit("doc-a", "reg-a", 1, "file-a1", "alpha.pdf"));
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(any(Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(1L);

		// Act
		final var result = documentSearchService.searchFileMatches(List.of("any"), false, false, null, null, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getDocuments().get(0).getFiles().get(0).getHighlights()).isNull();
	}

	@Test
	void searchFileMatches_multipleQueriesProduceOrOfPhrasePlusFuzzyClausesInEsQuery() {
		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		final var queryCaptor = ArgumentCaptor.forClass(Query.class);
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(List.of(DocumentStatus.ACTIVE));
		when(elasticsearchOperationsMock.search(queryCaptor.capture(), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of());
		when(searchHitsMock.getTotalHits()).thenReturn(0L);

		// Act
		documentSearchService.searchFileMatches(List.of("alpha", "beta", "gamma"), false, false, null, null, pageRequest, MUNICIPALITY_ID);

		// Assert — walk the captured NativeQuery tree: outer bool must → OR-bool (one should per
		// query) → per-query bool (phrase OR fuzzy, min_should_match=1).
		final var captured = (NativeQuery) queryCaptor.getValue();
		final var outerBool = captured.getQuery().bool();
		assertThat(outerBool.must()).hasSize(1);
		final var orBool = outerBool.must().get(0).bool();
		assertThat(orBool.minimumShouldMatch()).isEqualTo("1");
		assertThat(orBool.should()).hasSize(3);

		for (var i = 0; i < 3; i++) {
			final var expectedQuery = List.of("alpha", "beta", "gamma").get(i);
			final var perQueryBool = orBool.should().get(i).bool();
			assertThat(perQueryBool.minimumShouldMatch()).isEqualTo("1");
			assertThat(perQueryBool.should()).hasSize(2)
				.extracting(c -> c.multiMatch().query())
				.containsOnly(expectedQuery);
			// Exactly one of the two should-clauses is a phrase, the other is a fuzzy best_fields AND.
			final var types = perQueryBool.should().stream()
				.map(c -> c.multiMatch().type())
				.toList();
			assertThat(types).containsExactlyInAnyOrder(TextQueryType.Phrase, TextQueryType.BestFields);
		}
	}

	@Test
	void searchFileMatches_singleQueryHasPhraseAndFuzzyClausesWithCorrectFields() {
		// Arrange — verifies the per-query bool: phrase branch covers all fields and is boosted,
		// fuzzy branch uses best_fields + AND + AUTO fuzziness and excludes identifier-shaped
		// fields (registrationNumber, mimeType).
		final var pageRequest = PageRequest.of(0, 10);
		final var queryCaptor = ArgumentCaptor.forClass(Query.class);
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(List.of(DocumentStatus.ACTIVE));
		when(elasticsearchOperationsMock.search(queryCaptor.capture(), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of());
		when(searchHitsMock.getTotalHits()).thenReturn(0L);

		// Act
		documentSearchService.searchFileMatches(List.of("Jeppe Jepsson"), false, false, null, null, pageRequest, MUNICIPALITY_ID);

		// Assert
		final var captured = (NativeQuery) queryCaptor.getValue();
		final var perQueryBool = captured.getQuery().bool().must().get(0).bool().should().get(0).bool();

		final var phraseClause = perQueryBool.should().stream()
			.map(c -> c.multiMatch())
			.filter(mm -> mm.type() == TextQueryType.Phrase)
			.findFirst().orElseThrow();
		final var fuzzyClause = perQueryBool.should().stream()
			.map(c -> c.multiMatch())
			.filter(mm -> mm.type() == TextQueryType.BestFields)
			.findFirst().orElseThrow();

		// Phrase branch: all fields, boosted above 1, exact phrase.
		assertThat(phraseClause.query()).isEqualTo("Jeppe Jepsson");
		assertThat(phraseClause.boost()).isGreaterThan(1.0f);
		assertThat(phraseClause.fields()).contains(
			"title", "extractedText", "description", "fileName", "mimeType",
			"registrationNumber", "createdBy", "metadataKeys", "metadataValues");

		// Fuzzy branch: AND + AUTO, and crucially neither registrationNumber nor mimeType is
		// present (edit-distance matches on identifier-shaped values produce noise).
		assertThat(fuzzyClause.query()).isEqualTo("Jeppe Jepsson");
		assertThat(fuzzyClause.operator()).isEqualTo(Operator.And);
		assertThat(fuzzyClause.fuzziness()).isEqualTo("AUTO");
		assertThat(fuzzyClause.fields()).noneMatch(f -> f.startsWith("registrationNumber") || f.startsWith("mimeType"));
		assertThat(fuzzyClause.fields()).anyMatch(f -> f.startsWith("createdBy"));
	}

	@Test
	void searchFileMatches_statusesParamNarrowsSearch() {
		// Arrange — caller explicitly asks for DRAFT + REVOKED. /file-matches passes the list
		// through verbatim (no default-publishing policy), so the ES bool filters exactly those.
		final var pageRequest = PageRequest.of(0, 10);
		final var requested = List.of(DocumentStatus.DRAFT, DocumentStatus.REVOKED);
		final var queryCaptor = ArgumentCaptor.forClass(Query.class);
		when(elasticsearchOperationsMock.search(queryCaptor.capture(), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of());
		when(searchHitsMock.getTotalHits()).thenReturn(0L);

		// Act
		documentSearchService.searchFileMatches(List.of("any"), false, false, requested, null, pageRequest, MUNICIPALITY_ID);

		// Assert — the caller's statuses make it into the status terms filter on the outer bool.
		verifyNoInteractions(statusPolicyMock);
		final var captured = (NativeQuery) queryCaptor.getValue();
		final var outerBool = captured.getQuery().bool();
		final var statusFilterValues = outerBool.filter().stream()
			.filter(co.elastic.clients.elasticsearch._types.query_dsl.Query::isTerms)
			.filter(q -> "status".equals(q.terms().field()))
			.flatMap(q -> q.terms().terms().value().stream())
			.map(co.elastic.clients.elasticsearch._types.FieldValue::stringValue)
			.toList();
		assertThat(statusFilterValues).containsExactlyInAnyOrder("DRAFT", "REVOKED");
	}

	@Test
	void searchFileMatches_documentTypesParamAddsTermsFilter() {
		// Arrange — caller supplies two documentTypes; expect a terms filter on the documentType field.
		final var pageRequest = PageRequest.of(0, 10);
		final var queryCaptor = ArgumentCaptor.forClass(Query.class);
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(queryCaptor.capture(), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of());
		when(searchHitsMock.getTotalHits()).thenReturn(0L);

		// Act
		documentSearchService.searchFileMatches(List.of("any"), false, false, null, List.of("EMPLOYEE_CERTIFICATE", "HOLIDAY_EXCHANGE"), pageRequest, MUNICIPALITY_ID);

		// Assert
		final var captured = (NativeQuery) queryCaptor.getValue();
		final var outerBool = captured.getQuery().bool();
		final var docTypeFilterValues = outerBool.filter().stream()
			.filter(co.elastic.clients.elasticsearch._types.query_dsl.Query::isTerms)
			.filter(q -> "documentType".equals(q.terms().field()))
			.flatMap(q -> q.terms().terms().value().stream())
			.map(co.elastic.clients.elasticsearch._types.FieldValue::stringValue)
			.toList();
		assertThat(docTypeFilterValues).containsExactlyInAnyOrder("EMPLOYEE_CERTIFICATE", "HOLIDAY_EXCHANGE");
	}

	@Test
	void searchFileMatches_nullStatusesAndDocumentTypesMeansNoFiltersAtAll() {
		// Arrange — neither param supplied. /file-matches default is "search every status and
		// every document type", so the ES bool should carry NO status- or documentType-terms
		// filter. Confirms the policy flip from "published-only default" to "search all".
		final var pageRequest = PageRequest.of(0, 10);
		final var queryCaptor = ArgumentCaptor.forClass(Query.class);
		when(elasticsearchOperationsMock.search(queryCaptor.capture(), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(List.of());
		when(searchHitsMock.getTotalHits()).thenReturn(0L);

		// Act
		documentSearchService.searchFileMatches(List.of("any"), false, false, null, null, pageRequest, MUNICIPALITY_ID);

		// Assert — no terms filter on either field
		final var captured = (NativeQuery) queryCaptor.getValue();
		final var outerBool = captured.getQuery().bool();
		final var hasStatusFilter = outerBool.filter().stream()
			.filter(co.elastic.clients.elasticsearch._types.query_dsl.Query::isTerms)
			.anyMatch(q -> "status".equals(q.terms().field()));
		final var hasDocumentTypeFilter = outerBool.filter().stream()
			.filter(co.elastic.clients.elasticsearch._types.query_dsl.Query::isTerms)
			.anyMatch(q -> "documentType".equals(q.terms().field()));
		assertThat(hasStatusFilter).as("no status terms filter when param absent").isFalse();
		assertThat(hasDocumentTypeFilter).as("no documentType terms filter when param absent").isFalse();
		// effectivePublishedStatuses must not be consulted for /file-matches default.
		verifyNoInteractions(statusPolicyMock);
	}

	@Test
	void searchFileMatches_propagatesHitTotalAsMetaTotalRecords() {
		// Arrange
		final var pageRequest = PageRequest.of(2, 5);
		final var hits = List.of(
			fileHit("doc-a", "reg-a", 1, "file-a1", "alpha.pdf"),
			fileHit("doc-a", "reg-a", 1, "file-a2", "alpha-2.pdf"));
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(any(Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(42L);

		// Act
		final var result = documentSearchService.searchFileMatches(List.of("any"), false, false, null, null, pageRequest, MUNICIPALITY_ID);

		// Assert — file-level total (same as existing search endpoint), page + size reflect pageable.
		assertThat(result.getMetadata().getTotalRecords()).isEqualTo(42L);
		assertThat(result.getMetadata().getPage()).isEqualTo(2);
		assertThat(result.getMetadata().getLimit()).isEqualTo(5);
		assertThat(result.getMetadata().getCount()).isEqualTo(1);
		assertThat(result.getMetadata().getTotalPages()).isEqualTo(9);
	}

	@Test
	void search_hydratesHitsIntoDocumentsWithResponsibilities() {
		// Arrange
		final var pageRequest = PageRequest.of(0, 10);
		final var docId = "doc-1";
		final var regNum = "reg-1";
		final var hits = List.of(fileHit(docId, regNum, 1, "file-1", "a.pdf"));
		final var entity = DocumentEntity.create()
			.withId(docId)
			.withRegistrationNumber(regNum)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRevision(1)
			.withType(DocumentTypeEntity.create().withType("t"));
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(elasticsearchOperationsMock.search(any(Query.class), eq(DocumentIndexEntity.class))).thenReturn(searchHitsMock);
		when(searchHitsMock.getSearchHits()).thenReturn(hits);
		when(searchHitsMock.getTotalHits()).thenReturn(1L);
		when(documentRepositoryMock.findAllById(List.of(docId))).thenReturn(List.of(entity));
		when(documentResponsibilityRepositoryMock.findByMunicipalityIdAndRegistrationNumberIn(eq(MUNICIPALITY_ID), any()))
			.thenReturn(List.of(DocumentResponsibilityEntity.create().withRegistrationNumber(regNum).withPersonId("p1")));

		// Act
		final var result = documentSearchService.search("hello", false, false, pageRequest, MUNICIPALITY_ID);

		// Assert
		assertThat(result.getDocuments()).hasSize(1);
		assertThat(result.getDocuments().get(0).getRegistrationNumber()).isEqualTo(regNum);
		verify(documentRepositoryMock).findAllById(List.of(docId));
	}

	@Test
	void search_whenElasticsearchUnavailable_throwsServiceUnavailable() {
		// Arrange — search service built without ES operations.
		final var offline = new DocumentSearchService(
			documentRepositoryMock, new DocumentResponseHydrator(documentResponsibilityRepositoryMock), statusPolicyMock, Optional.empty());
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);

		// Act + Assert
		final var ex = assertThrows(ThrowableProblem.class,
			() -> offline.search("x", false, false, PageRequest.of(0, 10), MUNICIPALITY_ID));
		assertThat(ex.getMessage()).contains("Elasticsearch is not configured");
	}

	@Test
	void searchByParameters_delegatesToRepositoryAndHydratesResponsibilities() {
		// Arrange
		final var regNum = "reg-param";
		final var entity = DocumentEntity.create()
			.withId("doc-param")
			.withRegistrationNumber(regNum)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withRevision(1)
			.withType(DocumentTypeEntity.create().withType("t"));
		final var pageable = PageRequest.of(0, 100);
		final Page<DocumentEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
		final var parameters = new DocumentParameters().withMunicipalityId(MUNICIPALITY_ID);
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(documentRepositoryMock.searchByParameters(eq(parameters), any(), eq(PUBLISHED_STATUSES))).thenReturn(page);
		when(documentResponsibilityRepositoryMock.findByMunicipalityIdAndRegistrationNumberIn(eq(MUNICIPALITY_ID), any()))
			.thenReturn(List.of(DocumentResponsibilityEntity.create().withRegistrationNumber(regNum).withPersonId("p1")));

		// Act
		final var result = documentSearchService.searchByParameters(parameters);

		// Assert
		assertThat(result.getDocuments()).hasSize(1);
		assertThat(result.getDocuments().get(0).getRegistrationNumber()).isEqualTo(regNum);
		verify(documentRepositoryMock).searchByParameters(eq(parameters), any(), eq(PUBLISHED_STATUSES));
	}

	@Test
	void searchByParameters_emptyPage_skipsResponsibilityHydration() {
		// Arrange
		final var parameters = new DocumentParameters().withMunicipalityId(MUNICIPALITY_ID);
		final var pageable = PageRequest.of(0, 100);
		final Page<DocumentEntity> empty = new PageImpl<>(List.of(), pageable, 0);
		lenient().when(statusPolicyMock.effectivePublishedStatuses(any())).thenReturn(PUBLISHED_STATUSES);
		when(documentRepositoryMock.searchByParameters(eq(parameters), any(), eq(PUBLISHED_STATUSES))).thenReturn(empty);

		// Act
		final var result = documentSearchService.searchByParameters(parameters);

		// Assert
		assertThat(result.getDocuments()).isEmpty();
		verifyNoInteractions(documentResponsibilityRepositoryMock);
	}

	@SuppressWarnings("unchecked")
	private static SearchHit<DocumentIndexEntity> fileHit(String documentId, String registrationNumber, int revision, String fileId, String fileName) {
		final var entity = new DocumentIndexEntity();
		entity.setId(fileId);
		entity.setDocumentId(documentId);
		entity.setRegistrationNumber(registrationNumber);
		entity.setRevision(revision);
		entity.setFileName(fileName);
		final var hit = (SearchHit<DocumentIndexEntity>) mock(SearchHit.class);
		when(hit.getContent()).thenReturn(entity);
		return hit;
	}

	private static SearchHit<DocumentIndexEntity> fileHitWithHighlights(String documentId, String registrationNumber, int revision, String fileId, String fileName, Map<String, List<String>> highlights) {
		final var hit = fileHit(documentId, registrationNumber, revision, fileId, fileName);
		when(hit.getHighlightFields()).thenReturn(highlights);
		return hit;
	}

	@SuppressWarnings("unchecked")
	private static SearchHit<DocumentIndexEntity> enrichedHit(String documentId, String registrationNumber, int revision, String fileId, String fileName,
		String extractedText, List<Integer> pageOffsets, int pageCount, ExtractionStatus extractionStatus, boolean confidential, float score) {
		final var entity = new DocumentIndexEntity();
		entity.setId(fileId);
		entity.setDocumentId(documentId);
		entity.setRegistrationNumber(registrationNumber);
		entity.setRevision(revision);
		entity.setFileName(fileName);
		entity.setExtractedText(extractedText);
		entity.setPageOffsets(pageOffsets);
		entity.setPageCount(pageCount);
		entity.setExtractionStatus(extractionStatus);
		entity.setConfidential(confidential);
		final var hit = (SearchHit<DocumentIndexEntity>) mock(SearchHit.class);
		when(hit.getContent()).thenReturn(entity);
		when(hit.getScore()).thenReturn(score);
		return hit;
	}
}
