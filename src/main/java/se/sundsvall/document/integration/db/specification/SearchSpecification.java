package se.sundsvall.document.integration.db.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.document.api.model.DocumentParameters;
import se.sundsvall.document.api.model.DocumentResponsibility;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentResponsibilityEntity;

import static jakarta.persistence.criteria.JoinType.LEFT;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public interface SearchSpecification {

	String CONFIDENTIAL = "confidential";
	String CONFIDENTIALITY = "confidentiality";
	String CREATED_BY = "createdBy";
	String DESCRIPTION = "description";
	String DOCUMENT_DATA = "documentData";
	String FILE_NAME = "fileName";
	String KEY = "key";
	String METADATA = "metadata";
	String MIME_TYPE = "mimeType";
	String MUNICIPALITY_ID = "municipalityId";
	String REGISTRATION_NUMBER = "registrationNumber";
	String REVISION = "revision";
	String TYPE = "type";
	String VALID_FROM = "validFrom";
	String VALID_TO = "validTo";
	String VALUE = "value";

	static Specification<DocumentEntity> withSearchParameters(final DocumentParameters parameters) {
		return onlyLatestRevisionOfDocuments(parameters.isOnlyLatestRevision())
			.and(matchesMunicipalityId(parameters.getMunicipalityId(), false))
			.and(includeConfidentialDocuments(parameters.isIncludeConfidential()))
			.and(matchesType(parameters.getDocumentTypes()))
			.and(matchesCreatedByExact(parameters.getCreatedBy()))
			.and(matchesMetaData(parameters.getMetaData()))
			.and(matchesResponsibilities(parameters.getResponsibilities()))
			.and(matchesValidOn(parameters.getValidOn()));
	}

	static Specification<DocumentEntity> matchesResponsibilities(final List<DocumentResponsibility> responsibilities) {
		return (root, query, cb) -> {
			if (responsibilities == null || responsibilities.isEmpty()) {
				return cb.and();
			}

			final var subQuery = query.subquery(Long.class);
			final var responsibilityRoot = subQuery.from(DocumentResponsibilityEntity.class);

			final var usernamePredicates = responsibilities.stream()
				.filter(Objects::nonNull)
				.filter(responsibility -> responsibility.getUsername() != null && !responsibility.getUsername().isBlank())
				.map(responsibility -> cb.equal(responsibilityRoot.get("username"), normalizeUsername(responsibility.getUsername())))
				.toList();

			if (usernamePredicates.isEmpty()) {
				return cb.disjunction();
			}

			subQuery.select(cb.literal(1L))
				.where(
					cb.equal(responsibilityRoot.get("municipalityId"), root.get(MUNICIPALITY_ID)),
					cb.equal(responsibilityRoot.get("registrationNumber"), root.get(REGISTRATION_NUMBER)),
					cb.or(usernamePredicates.toArray(new Predicate[0])));

			return cb.exists(subQuery);
		};
	}

	private static String normalizeUsername(final String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}

	private static Specification<DocumentEntity> matchesValidOn(LocalDate validOn) {
		return (root, query, cb) -> {
			if (validOn == null) {
				return cb.and();
			}
			return cb.and(
				cb.or(cb.isNull(root.get(VALID_FROM)), cb.lessThanOrEqualTo(root.get(VALID_FROM), validOn)),
				cb.or(cb.isNull(root.get(VALID_TO)), cb.greaterThanOrEqualTo(root.get(VALID_TO), validOn)));
		};
	}

	static Specification<DocumentEntity> matchesMetaData(final List<DocumentParameters.MetaData> metaData) {
		if (metaData == null || metaData.isEmpty()) {
			return (root, query, cb) -> cb.and();
		}

		Specification<DocumentEntity> metaDataSpec = (root, query, cb) -> cb.and();

		for (var data : metaData) {
			var singleMetaDataSpec = Specification.where(hasKeyAndMatchesAll(data))
				.and(hasKeyAndMatchesAny(data))
				.and(hasOnlyKey(data))
				.and(hasOnlyMatchesAny(data))
				.and(hasOnlyMatchesAll(data));

			metaDataSpec = metaDataSpec.and(singleMetaDataSpec);
		}

		return metaDataSpec;
	}

	static Specification<DocumentEntity> hasKeyAndMatchesAll(DocumentParameters.MetaData metaData) {
		return (root, query, cb) -> {
			if (metaData.getKey() == null || metaData.getMatchesAll() == null || metaData.getMatchesAll().isEmpty()) {
				return cb.and();
			}

			Subquery<Long> subquery = query.subquery(Long.class);
			Root<DocumentEntity> subRoot = subquery.from(DocumentEntity.class);
			Join<DocumentEntity, DocumentMetadataEmbeddable> subMetadataJoin = subRoot.join(METADATA, JoinType.INNER);

			subquery.select(cb.count(subMetadataJoin.get(VALUE)));
			subquery.where(
				cb.equal(subRoot, root),
				cb.equal(cb.lower(subMetadataJoin.get(KEY)), metaData.getKey().toLowerCase()),
				subMetadataJoin.get(VALUE).in(
					metaData.getMatchesAll().stream()
						.map(String::toLowerCase)
						.toList()));

			return cb.equal(subquery, (long) metaData.getMatchesAll().size());
		};
	}

	static Specification<DocumentEntity> hasKeyAndMatchesAny(DocumentParameters.MetaData metaData) {
		return (root, query, cb) -> {
			if (metaData.getKey() == null || metaData.getMatchesAny() == null || metaData.getMatchesAny().isEmpty()) {
				return cb.and();
			}

			Join<DocumentEntity, DocumentMetadataEmbeddable> metadataJoin = root.join(METADATA, JoinType.INNER);

			var anyValuePredicates = metaData.getMatchesAny().stream()
				.map(value -> cb.equal(cb.lower(metadataJoin.get(VALUE)), value.toLowerCase()))
				.toList();

			return cb.and(
				cb.equal(cb.lower(metadataJoin.get(KEY)), metaData.getKey().toLowerCase()),
				cb.or(anyValuePredicates.toArray(new Predicate[0])));
		};
	}

	static Specification<DocumentEntity> hasOnlyKey(DocumentParameters.MetaData metaData) {
		return (root, query, cb) -> {
			if (metaData.getKey() == null || (metaData.getMatchesAny() != null && !metaData.getMatchesAny().isEmpty()) ||
				(metaData.getMatchesAll() != null && !metaData.getMatchesAll().isEmpty())) {
				return cb.and();
			}
			return cb.equal(cb.lower(root.join(METADATA, JoinType.INNER).get(KEY)), metaData.getKey().toLowerCase());
		};
	}

	static Specification<DocumentEntity> hasOnlyMatchesAny(DocumentParameters.MetaData metaData) {
		return (root, query, cb) -> {
			if (metaData.getMatchesAny() == null || metaData.getMatchesAny().isEmpty() || metaData.getKey() != null) {
				return cb.and();
			}

			var anyValuePredicates = metaData.getMatchesAny().stream()
				.map(value -> cb.equal(cb.lower(root.join(METADATA, JoinType.INNER).get(VALUE)), value.toLowerCase()))
				.toList();

			return cb.or(anyValuePredicates.toArray(new Predicate[0]));
		};
	}

	static Specification<DocumentEntity> hasOnlyMatchesAll(DocumentParameters.MetaData metaData) {
		return (root, query, cb) -> {
			if (metaData.getMatchesAll() == null || metaData.getMatchesAll().isEmpty() || metaData.getKey() != null) {
				return cb.and();
			}

			var allValuePredicates = metaData.getMatchesAll().stream()
				.map(value -> cb.equal(cb.lower(root.join(METADATA, JoinType.INNER).get(VALUE)), value.toLowerCase()))
				.toList();

			return cb.and(allValuePredicates.toArray(new Predicate[0]));
		};
	}

	private static Specification<DocumentEntity> matchesType(final List<String> type) {
		return (root, query, cb) -> {
			if (type == null || type.isEmpty()) {
				return cb.and();
			}
			var lowerCaseValues = type.stream()
				.filter(Objects::nonNull)
				.map(String::toLowerCase)
				.toList();
			return cb.lower(root.join(TYPE, JoinType.INNER).get(TYPE)).in(lowerCaseValues);
		};
	}

	static Specification<DocumentEntity> withSearchQuery(String query, boolean includeConfidential, boolean onlyLatestRevision, String municipalityId) {
		final var queryString = toQueryString(query);

		return onlyLatestRevisionOfDocuments(onlyLatestRevision)
			.and(matchesMunicipalityId(municipalityId, false))
			.and(matchesCreatedBy(queryString)
				.or(matchesDescription(queryString))
				.or(matchesMunicipalityId(queryString, true))
				.or(matchesRegistrationNumber(queryString))
				.or(matchesFileName(queryString))
				.or(matchesMimeType(queryString))
				.or(matchesMetadataKey(queryString))
				.or(matchesMetadataValue(queryString)))
			.and(includeConfidentialDocuments(includeConfidential))
			.and(distinct());
	}

	private static Specification<DocumentEntity> onlyLatestRevisionOfDocuments(boolean onlyLatestRevision) {
		if (!onlyLatestRevision) {
			return (root, query, cb) -> cb.and(); // Do not add any filter to return all documents regardless of revision
		}

		return (root, query, cb) -> {
			var subQuery = query.subquery(Integer.class);
			var subRoot = subQuery.from(DocumentEntity.class);
			subQuery.select(cb.max(subRoot.get(REVISION)))
				.where(cb.equal(root.get(REGISTRATION_NUMBER), subRoot.get(REGISTRATION_NUMBER)));
			return cb.equal(root.get(REVISION), subQuery);

		}; // Only return latest revision of documents
	}

	private static Specification<DocumentEntity> includeConfidentialDocuments(boolean includeConfidential) {
		if (includeConfidential) {
			return (entity, cq, cb) -> cb.and(); // Do not add any filter to return all documents regardless of whether they are confidential or not
		}
		return (entity, cq, cb) -> cb.equal(entity.get(CONFIDENTIALITY).get(CONFIDENTIAL), false); // Return non-confidential documents only
	}

	private static Specification<DocumentEntity> matchesCreatedByExact(String createdBy) {
		return (entity, cq, cb) -> {
			if (createdBy == null || createdBy.isBlank()) {
				return cb.and();
			}
			return cb.equal(cb.lower(entity.get(CREATED_BY)), createdBy.toLowerCase());
		};
	}

	private static Specification<DocumentEntity> matchesCreatedBy(String query) {
		return (entity, cq, cb) -> cb.like(cb.lower(entity.get(CREATED_BY)), query);
	}

	private static Specification<DocumentEntity> matchesDescription(String query) {
		return (entity, cq, cb) -> cb.like(cb.lower(entity.get(DESCRIPTION)), query);
	}

	private static Specification<DocumentEntity> matchesMunicipalityId(String query, boolean like) {
		if (like) {
			return (entity, cq, cb) -> cb.like(cb.lower(entity.get(MUNICIPALITY_ID)), query);
		} else {
			return (entity, cq, cb) -> cb.equal(cb.lower(entity.get(MUNICIPALITY_ID)), query);
		}
	}

	private static Specification<DocumentEntity> matchesRegistrationNumber(String query) {
		return (entity, cq, cb) -> cb.like(cb.lower(entity.get(REGISTRATION_NUMBER)), query);
	}

	private static Specification<DocumentEntity> matchesFileName(String query) {
		return (entity, cq, cb) -> cb.like(cb.lower(entity.join(DOCUMENT_DATA, LEFT).get(FILE_NAME)), query);
	}

	private static Specification<DocumentEntity> matchesMimeType(String query) {
		return (entity, cq, cb) -> cb.like(cb.lower(entity.join(DOCUMENT_DATA, LEFT).get(MIME_TYPE)), query);
	}

	private static Specification<DocumentEntity> matchesMetadataKey(String query) {
		return (entity, cq, cb) -> cb.like(cb.lower(entity.join(METADATA, LEFT).get(KEY)), query);
	}

	private static Specification<DocumentEntity> matchesMetadataValue(String query) {
		return (entity, cq, cb) -> cb.like(cb.lower(entity.join(METADATA, LEFT).get(VALUE)), query);
	}

	private static Specification<DocumentEntity> distinct() {
		return (entity, cq, cb) -> {
			cq.distinct(true);
			return cb.and();
		};
	}

	private static String toQueryString(String query) {
		return Optional.ofNullable(query)
			.map(String::trim)
			.map(String::toLowerCase)
			.map(str -> str.replace('*', '%'))
			.orElse(EMPTY);
	}
}
