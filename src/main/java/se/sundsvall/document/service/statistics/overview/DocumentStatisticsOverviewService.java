package se.sundsvall.document.service.statistics.overview;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.document.api.model.DocumentStatisticsOverview;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.ConfidentialityCounts;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.DocumentTypeCount;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.ExpiringSoon;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.RevisionDistribution;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.Scope;
import se.sundsvall.document.api.model.DocumentStatisticsOverview.YearCount;
import se.sundsvall.document.api.model.DocumentStatus;
import se.sundsvall.document.integration.db.DocumentStatisticsOverviewRepository;
import se.sundsvall.document.service.DocumentStatusPolicy;

@Service
@Transactional(readOnly = true)
public class DocumentStatisticsOverviewService {

	static final int EXPIRING_SOON_WINDOW_DAYS = 30;

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentStatisticsOverviewService.class);

	private final DocumentStatisticsOverviewRepository repository;
	private final DocumentStatusPolicy statusPolicy;
	private final Clock clock;

	public DocumentStatisticsOverviewService(
		final DocumentStatisticsOverviewRepository repository,
		final DocumentStatusPolicy statusPolicy,
		final Clock clock) {

		this.repository = repository;
		this.statusPolicy = statusPolicy;
		this.clock = clock;
	}

	public DocumentStatisticsOverview getOverview(final String municipalityId, final String createdBy) {
		final var scopedBy = normalize(createdBy);
		final var scope = scopedBy == null ? Scope.MUNICIPALITY : Scope.USER;

		LOGGER.debug("Computing document statistics overview (municipalityId='{}', scope={}, createdBy='{}')", municipalityId, scope, scopedBy);

		final var today = statusPolicy.today();
		final var windowEnd = today.plusDays(EXPIRING_SOON_WINDOW_DAYS);

		return DocumentStatisticsOverview.create()
			.withMunicipalityId(municipalityId)
			.withScope(scope)
			.withCreatedBy(scopedBy)
			.withGeneratedAt(OffsetDateTime.now(clock))
			.withTotalDocuments(repository.countTotalDocuments(municipalityId, scopedBy))
			.withByStatus(buildStatusMap(repository.countByStatus(municipalityId, scopedBy)))
			.withByConfidentiality(buildConfidentiality(repository.countByConfidentiality(municipalityId, scopedBy)))
			.withByDocumentType(buildDocumentTypeCounts(repository.countByDocumentType(municipalityId, scopedBy)))
			.withByRegistrationYear(buildYearCounts(repository.countByRegistrationYear(municipalityId, scopedBy)))
			.withRevisionDistribution(buildRevisionDistribution(repository.latestRevisionPerDocument(municipalityId, scopedBy)))
			.withDocumentsWithoutFiles(repository.countDocumentsWithoutFiles(municipalityId, scopedBy))
			.withExpiringSoon(ExpiringSoon.create()
				.withWithinDays(EXPIRING_SOON_WINDOW_DAYS)
				.withCount(repository.countExpiringSoon(municipalityId, scopedBy, today, windowEnd)));
	}

	private static String normalize(final String createdBy) {
		return (createdBy == null || createdBy.isBlank()) ? null : createdBy;
	}

	private static Map<DocumentStatus, Long> buildStatusMap(final List<StatusCountProjection> rows) {
		final var map = new EnumMap<DocumentStatus, Long>(DocumentStatus.class);
		Arrays.stream(DocumentStatus.values()).forEach(status -> map.put(status, 0L));
		rows.forEach(row -> map.put(row.status(), row.count()));
		return map;
	}

	private static ConfidentialityCounts buildConfidentiality(final List<ConfidentialityCountProjection> rows) {
		var confidential = 0L;
		var nonConfidential = 0L;
		for (final var row : rows) {
			if (row.confidential()) {
				confidential += row.count();
			} else {
				nonConfidential += row.count();
			}
		}
		return ConfidentialityCounts.create()
			.withConfidential(confidential)
			.withNonConfidential(nonConfidential);
	}

	private static List<DocumentTypeCount> buildDocumentTypeCounts(final List<TypeCountProjection> rows) {
		return rows.stream()
			.map(row -> DocumentTypeCount.create().withType(row.type()).withCount(row.count()))
			.sorted(Comparator.comparingLong(DocumentTypeCount::getCount).reversed()
				.thenComparing(DocumentTypeCount::getType, Comparator.nullsLast(Comparator.naturalOrder())))
			.toList();
	}

	private static List<YearCount> buildYearCounts(final List<YearCountProjection> rows) {
		return rows.stream()
			.filter(row -> row.year() != null)
			.map(row -> YearCount.create().withYear(Integer.parseInt(row.year())).withCount(row.count()))
			.sorted(Comparator.comparingInt(YearCount::getYear))
			.toList();
	}

	private static RevisionDistribution buildRevisionDistribution(final List<Integer> maxRevisions) {
		var single = 0L;
		var two = 0L;
		var threeOrMore = 0L;
		var max = 0;
		for (final var rev : maxRevisions) {
			if (rev == null) {
				continue;
			}
			if (rev == 1) {
				single++;
			} else if (rev == 2) {
				two++;
			} else {
				threeOrMore++;
			}
			if (rev > max) {
				max = rev;
			}
		}
		return RevisionDistribution.create()
			.withSingle(single)
			.withTwo(two)
			.withThreeOrMore(threeOrMore)
			.withMaxRevision(max);
	}
}
