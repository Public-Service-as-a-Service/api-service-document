package se.sundsvall.document.service.statistics;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.DocumentAccessType;
import se.sundsvall.document.api.model.DocumentStatistics;
import se.sundsvall.document.api.model.FileStatistics;
import se.sundsvall.document.api.model.RevisionStatistics;
import se.sundsvall.document.integration.db.DocumentAccessLogRepository;
import se.sundsvall.document.integration.db.DocumentRepository;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.document.service.Constants.ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND;
import static se.sundsvall.document.service.InclusionFilter.CONFIDENTIAL_AND_PUBLIC;

@Service
@Transactional(readOnly = true)
public class DocumentStatisticsService {

	private final DocumentRepository documentRepository;
	private final DocumentAccessLogRepository accessLogRepository;

	public DocumentStatisticsService(final DocumentRepository documentRepository, final DocumentAccessLogRepository accessLogRepository) {
		this.documentRepository = documentRepository;
		this.accessLogRepository = accessLogRepository;
	}

	public DocumentStatistics getStatistics(final String municipalityId, final String registrationNumber, final OffsetDateTime from, final OffsetDateTime to) {

		if (!documentRepository.existsByMunicipalityIdAndRegistrationNumber(municipalityId, registrationNumber)) {
			throw Problem.valueOf(NOT_FOUND, ERROR_DOCUMENT_BY_REGISTRATION_NUMBER_NOT_FOUND.formatted(registrationNumber));
		}

		final var counts = accessLogRepository.aggregate(municipalityId, registrationNumber, from, to);
		final var fileNames = collectFileNames(municipalityId, registrationNumber);

		return DocumentStatistics.create()
			.withMunicipalityId(municipalityId)
			.withRegistrationNumber(registrationNumber)
			.withFrom(from)
			.withTo(to)
			.withTotalAccesses(counts.stream().mapToLong(AccessCountProjection::count).sum())
			.withPerRevision(buildRevisionBreakdown(counts, fileNames));
	}

	private List<RevisionStatistics> buildRevisionBreakdown(final List<AccessCountProjection> counts, final Map<String, String> fileNames) {

		// Group by revision -> documentDataId -> accessType -> count
		final Map<Integer, Map<String, Map<DocumentAccessType, Long>>> grouped = new HashMap<>();
		for (final var row : counts) {
			grouped
				.computeIfAbsent(row.revision(), r -> new HashMap<>())
				.computeIfAbsent(row.documentDataId(), id -> new HashMap<>())
				.merge(row.accessType(), row.count(), Long::sum);
		}

		final List<RevisionStatistics> result = new ArrayList<>();
		grouped.forEach((revision, fileMap) -> {
			final List<FileStatistics> perFile = new ArrayList<>();
			long downloads = 0;
			long views = 0;
			for (final var fileEntry : fileMap.entrySet()) {
				final var typeMap = fileEntry.getValue();
				final long fileDownloads = typeMap.getOrDefault(DocumentAccessType.DOWNLOAD, 0L);
				final long fileViews = typeMap.getOrDefault(DocumentAccessType.VIEW, 0L);
				downloads += fileDownloads;
				views += fileViews;
				perFile.add(FileStatistics.create()
					.withDocumentDataId(fileEntry.getKey())
					.withFileName(fileNames.get(fileEntry.getKey()))
					.withDownloads(fileDownloads)
					.withViews(fileViews));
			}
			perFile.sort(Comparator.comparing(
				f -> ofNullable(f.getFileName()).orElse(f.getDocumentDataId()),
				Comparator.nullsLast(Comparator.naturalOrder())));
			result.add(RevisionStatistics.create()
				.withRevision(revision)
				.withDownloads(downloads)
				.withViews(views)
				.withPerFile(perFile));
		});
		result.sort(Comparator.comparingInt(RevisionStatistics::getRevision));
		return result;
	}

	private Map<String, String> collectFileNames(final String municipalityId, final String registrationNumber) {
		final var fileNames = new HashMap<String, String>();
		documentRepository.findByMunicipalityIdAndRegistrationNumberAndConfidentialityConfidentialIn(
			municipalityId, registrationNumber, CONFIDENTIAL_AND_PUBLIC.getValue())
			.forEach(doc -> ofNullable(doc.getDocumentData()).ifPresent(data -> data.forEach(d -> fileNames.putIfAbsent(d.getId(), d.getFileName()))));
		return fileNames;
	}
}
