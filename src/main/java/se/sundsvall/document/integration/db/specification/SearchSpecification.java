package se.sundsvall.document.integration.db.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.document.api.model.DocumentParameters;
import se.sundsvall.document.integration.db.model.DocumentEntity;
import se.sundsvall.document.integration.db.model.DocumentEntity_;
import se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable;
import se.sundsvall.document.integration.db.model.DocumentTypeEntity_;

import static jakarta.persistence.criteria.JoinType.LEFT;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static se.sundsvall.document.integration.db.model.ConfidentialityEmbeddable_.CONFIDENTIAL;
import static se.sundsvall.document.integration.db.model.DocumentDataEntity_.FILE_NAME;
import static se.sundsvall.document.integration.db.model.DocumentDataEntity_.MIME_TYPE;
import static se.sundsvall.document.integration.db.model.DocumentEntity_.CONFIDENTIALITY;
import static se.sundsvall.document.integration.db.model.DocumentEntity_.CREATED_BY;
import static se.sundsvall.document.integration.db.model.DocumentEntity_.DESCRIPTION;
import static se.sundsvall.document.integration.db.model.DocumentEntity_.DOCUMENT_DATA;
import static se.sundsvall.document.integration.db.model.DocumentEntity_.METADATA;
import static se.sundsvall.document.integration.db.model.DocumentEntity_.MUNICIPALITY_ID;
import static se.sundsvall.document.integration.db.model.DocumentEntity_.REGISTRATION_NUMBER;
import static se.sundsvall.document.integration.db.model.DocumentEntity_.REVISION;
import static se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable_.KEY;
import static se.sundsvall.document.integration.db.model.DocumentMetadataEmbeddable_.VALUE;

public interface SearchSpecification {

	static Specification<DocumentEntity> withSearchParameters(final DocumentParameters parameters) {
		return onlyLatestRevisionOfDocuments(parameters.isOnlyLatestRevision())
			.and(matchesMunicipalityId(parameters.getMunicipalityId(), false))
			.and(includeConfidentialDocuments(parameters.isIncludeConfidential()))
			.and(matchesType(parameters.getDocumentTypes()))
			.and(matchesCreatedByExact(parameters.getCreatedBy()))
			.and(matchesMetaData(parameters.getMetaData()));
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
			return cb.lower(root.join(DocumentEntity_.TYPE, JoinType.INNER).get(DocumentTypeEntity_.TYPE)).in(lowerCaseValues);
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
