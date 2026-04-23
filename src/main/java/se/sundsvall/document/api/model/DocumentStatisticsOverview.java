package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Aggregated overview statistics across a document corpus. "
	+ "Counts are computed over the latest revision per registration number.", accessMode = READ_ONLY)
public class DocumentStatisticsOverview {

	public enum Scope {
		USER, MUNICIPALITY
	}

	@Schema(description = "Municipality ID.", examples = "2281")
	private String municipalityId;

	@Schema(description = "Aggregation scope. USER when counts are filtered by createdBy, MUNICIPALITY otherwise.", examples = "USER")
	private Scope scope;

	@Schema(description = "personId of the user the aggregation is scoped to. Null when scope is MUNICIPALITY.", examples = "6c3e4f5a-7b8d-4e9c-a1f2-d3e4b5c6a7f8")
	private String createdBy;

	@Schema(description = "Server time when the response was generated.", examples = "2026-04-23T12:00:00+02:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime generatedAt;

	@Schema(description = "Total number of distinct documents (unique registration numbers) in scope.", examples = "1423")
	private long totalDocuments;

	@Schema(description = "Number of documents per lifecycle status. Every DocumentStatus value is included; zero counts are represented as 0.")
	private Map<DocumentStatus, Long> byStatus;

	@Schema(description = "Number of documents split by confidentiality.")
	private ConfidentialityCounts byConfidentiality;

	@Schema(description = "Number of documents per document type. All types that have at least one document in scope are listed, sorted by count (descending).")
	private List<DocumentTypeCount> byDocumentType;

	@Schema(description = "Number of documents per registration-number year.")
	private List<YearCount> byRegistrationYear;

	@Schema(description = "Revision count distribution across documents in scope.")
	private RevisionDistribution revisionDistribution;

	@Schema(description = "Number of documents (latest revision) that currently have no files attached.", examples = "7")
	private long documentsWithoutFiles;

	@Schema(description = "Documents with ACTIVE status whose validTo falls within the fixed 30-day look-ahead window.")
	private ExpiringSoon expiringSoon;

	public static DocumentStatisticsOverview create() {
		return new DocumentStatisticsOverview();
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public DocumentStatisticsOverview withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public Scope getScope() {
		return scope;
	}

	public void setScope(final Scope scope) {
		this.scope = scope;
	}

	public DocumentStatisticsOverview withScope(final Scope scope) {
		this.scope = scope;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public DocumentStatisticsOverview withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public OffsetDateTime getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(final OffsetDateTime generatedAt) {
		this.generatedAt = generatedAt;
	}

	public DocumentStatisticsOverview withGeneratedAt(final OffsetDateTime generatedAt) {
		this.generatedAt = generatedAt;
		return this;
	}

	public long getTotalDocuments() {
		return totalDocuments;
	}

	public void setTotalDocuments(final long totalDocuments) {
		this.totalDocuments = totalDocuments;
	}

	public DocumentStatisticsOverview withTotalDocuments(final long totalDocuments) {
		this.totalDocuments = totalDocuments;
		return this;
	}

	public Map<DocumentStatus, Long> getByStatus() {
		return byStatus;
	}

	public void setByStatus(final Map<DocumentStatus, Long> byStatus) {
		this.byStatus = byStatus;
	}

	public DocumentStatisticsOverview withByStatus(final Map<DocumentStatus, Long> byStatus) {
		this.byStatus = byStatus;
		return this;
	}

	public ConfidentialityCounts getByConfidentiality() {
		return byConfidentiality;
	}

	public void setByConfidentiality(final ConfidentialityCounts byConfidentiality) {
		this.byConfidentiality = byConfidentiality;
	}

	public DocumentStatisticsOverview withByConfidentiality(final ConfidentialityCounts byConfidentiality) {
		this.byConfidentiality = byConfidentiality;
		return this;
	}

	public List<DocumentTypeCount> getByDocumentType() {
		return byDocumentType;
	}

	public void setByDocumentType(final List<DocumentTypeCount> byDocumentType) {
		this.byDocumentType = byDocumentType;
	}

	public DocumentStatisticsOverview withByDocumentType(final List<DocumentTypeCount> byDocumentType) {
		this.byDocumentType = byDocumentType;
		return this;
	}

	public List<YearCount> getByRegistrationYear() {
		return byRegistrationYear;
	}

	public void setByRegistrationYear(final List<YearCount> byRegistrationYear) {
		this.byRegistrationYear = byRegistrationYear;
	}

	public DocumentStatisticsOverview withByRegistrationYear(final List<YearCount> byRegistrationYear) {
		this.byRegistrationYear = byRegistrationYear;
		return this;
	}

	public RevisionDistribution getRevisionDistribution() {
		return revisionDistribution;
	}

	public void setRevisionDistribution(final RevisionDistribution revisionDistribution) {
		this.revisionDistribution = revisionDistribution;
	}

	public DocumentStatisticsOverview withRevisionDistribution(final RevisionDistribution revisionDistribution) {
		this.revisionDistribution = revisionDistribution;
		return this;
	}

	public long getDocumentsWithoutFiles() {
		return documentsWithoutFiles;
	}

	public void setDocumentsWithoutFiles(final long documentsWithoutFiles) {
		this.documentsWithoutFiles = documentsWithoutFiles;
	}

	public DocumentStatisticsOverview withDocumentsWithoutFiles(final long documentsWithoutFiles) {
		this.documentsWithoutFiles = documentsWithoutFiles;
		return this;
	}

	public ExpiringSoon getExpiringSoon() {
		return expiringSoon;
	}

	public void setExpiringSoon(final ExpiringSoon expiringSoon) {
		this.expiringSoon = expiringSoon;
	}

	public DocumentStatisticsOverview withExpiringSoon(final ExpiringSoon expiringSoon) {
		this.expiringSoon = expiringSoon;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(byConfidentiality, byDocumentType, byRegistrationYear, byStatus, createdBy, documentsWithoutFiles, expiringSoon, generatedAt, municipalityId, revisionDistribution, scope, totalDocuments);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentStatisticsOverview other)) {
			return false;
		}
		return Objects.equals(byConfidentiality, other.byConfidentiality)
			&& Objects.equals(byDocumentType, other.byDocumentType)
			&& Objects.equals(byRegistrationYear, other.byRegistrationYear)
			&& Objects.equals(byStatus, other.byStatus)
			&& Objects.equals(createdBy, other.createdBy)
			&& documentsWithoutFiles == other.documentsWithoutFiles
			&& Objects.equals(expiringSoon, other.expiringSoon)
			&& Objects.equals(generatedAt, other.generatedAt)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(revisionDistribution, other.revisionDistribution)
			&& scope == other.scope
			&& totalDocuments == other.totalDocuments;
	}

	@Override
	public String toString() {
		return "DocumentStatisticsOverview [municipalityId=" + municipalityId + ", scope=" + scope + ", createdBy=" + createdBy + ", generatedAt=" + generatedAt + ", totalDocuments=" + totalDocuments + ", byStatus=" + byStatus
			+ ", byConfidentiality=" + byConfidentiality + ", byDocumentType=" + byDocumentType + ", byRegistrationYear=" + byRegistrationYear + ", revisionDistribution=" + revisionDistribution
			+ ", documentsWithoutFiles=" + documentsWithoutFiles + ", expiringSoon=" + expiringSoon + "]";
	}

	@Schema(description = "Confidentiality split.", accessMode = READ_ONLY)
	public static class ConfidentialityCounts {

		@Schema(description = "Number of confidential documents.", examples = "83")
		private long confidential;

		@Schema(description = "Number of non-confidential documents.", examples = "1340")
		private long nonConfidential;

		public static ConfidentialityCounts create() {
			return new ConfidentialityCounts();
		}

		public long getConfidential() {
			return confidential;
		}

		public void setConfidential(final long confidential) {
			this.confidential = confidential;
		}

		public ConfidentialityCounts withConfidential(final long confidential) {
			this.confidential = confidential;
			return this;
		}

		public long getNonConfidential() {
			return nonConfidential;
		}

		public void setNonConfidential(final long nonConfidential) {
			this.nonConfidential = nonConfidential;
		}

		public ConfidentialityCounts withNonConfidential(final long nonConfidential) {
			this.nonConfidential = nonConfidential;
			return this;
		}

		@Override
		public int hashCode() {
			return Objects.hash(confidential, nonConfidential);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof final ConfidentialityCounts other)) {
				return false;
			}
			return confidential == other.confidential && nonConfidential == other.nonConfidential;
		}

		@Override
		public String toString() {
			return "ConfidentialityCounts [confidential=" + confidential + ", nonConfidential=" + nonConfidential + "]";
		}
	}

	@Schema(description = "Count for a single document type.", accessMode = READ_ONLY)
	public static class DocumentTypeCount {

		@Schema(description = "Document type.", examples = "INVOICE")
		private String type;

		@Schema(description = "Number of documents of this type.", examples = "540")
		private long count;

		public static DocumentTypeCount create() {
			return new DocumentTypeCount();
		}

		public String getType() {
			return type;
		}

		public void setType(final String type) {
			this.type = type;
		}

		public DocumentTypeCount withType(final String type) {
			this.type = type;
			return this;
		}

		public long getCount() {
			return count;
		}

		public void setCount(final long count) {
			this.count = count;
		}

		public DocumentTypeCount withCount(final long count) {
			this.count = count;
			return this;
		}

		@Override
		public int hashCode() {
			return Objects.hash(count, type);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof final DocumentTypeCount other)) {
				return false;
			}
			return count == other.count && Objects.equals(type, other.type);
		}

		@Override
		public String toString() {
			return "DocumentTypeCount [type=" + type + ", count=" + count + "]";
		}
	}

	@Schema(description = "Count for a single registration-number year.", accessMode = READ_ONLY)
	public static class YearCount {

		@Schema(description = "Registration-number year.", examples = "2023")
		private int year;

		@Schema(description = "Number of documents registered that year.", examples = "410")
		private long count;

		public static YearCount create() {
			return new YearCount();
		}

		public int getYear() {
			return year;
		}

		public void setYear(final int year) {
			this.year = year;
		}

		public YearCount withYear(final int year) {
			this.year = year;
			return this;
		}

		public long getCount() {
			return count;
		}

		public void setCount(final long count) {
			this.count = count;
		}

		public YearCount withCount(final long count) {
			this.count = count;
			return this;
		}

		@Override
		public int hashCode() {
			return Objects.hash(count, year);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof final YearCount other)) {
				return false;
			}
			return count == other.count && year == other.year;
		}

		@Override
		public String toString() {
			return "YearCount [year=" + year + ", count=" + count + "]";
		}
	}

	@Schema(description = "Revision count distribution across documents in scope.", accessMode = READ_ONLY)
	public static class RevisionDistribution {

		@Schema(description = "Documents that only have one revision.", examples = "980")
		private long single;

		@Schema(description = "Documents with exactly two revisions.", examples = "300")
		private long two;

		@Schema(description = "Documents with three or more revisions.", examples = "143")
		private long threeOrMore;

		@Schema(description = "Highest revision number observed across documents in scope.", examples = "14")
		private int maxRevision;

		public static RevisionDistribution create() {
			return new RevisionDistribution();
		}

		public long getSingle() {
			return single;
		}

		public void setSingle(final long single) {
			this.single = single;
		}

		public RevisionDistribution withSingle(final long single) {
			this.single = single;
			return this;
		}

		public long getTwo() {
			return two;
		}

		public void setTwo(final long two) {
			this.two = two;
		}

		public RevisionDistribution withTwo(final long two) {
			this.two = two;
			return this;
		}

		public long getThreeOrMore() {
			return threeOrMore;
		}

		public void setThreeOrMore(final long threeOrMore) {
			this.threeOrMore = threeOrMore;
		}

		public RevisionDistribution withThreeOrMore(final long threeOrMore) {
			this.threeOrMore = threeOrMore;
			return this;
		}

		public int getMaxRevision() {
			return maxRevision;
		}

		public void setMaxRevision(final int maxRevision) {
			this.maxRevision = maxRevision;
		}

		public RevisionDistribution withMaxRevision(final int maxRevision) {
			this.maxRevision = maxRevision;
			return this;
		}

		@Override
		public int hashCode() {
			return Objects.hash(maxRevision, single, threeOrMore, two);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof final RevisionDistribution other)) {
				return false;
			}
			return maxRevision == other.maxRevision && single == other.single && threeOrMore == other.threeOrMore && two == other.two;
		}

		@Override
		public String toString() {
			return "RevisionDistribution [single=" + single + ", two=" + two + ", threeOrMore=" + threeOrMore + ", maxRevision=" + maxRevision + "]";
		}
	}

	@Schema(description = "Documents expiring within the fixed 30-day look-ahead window.", accessMode = READ_ONLY)
	public static class ExpiringSoon {

		@Schema(description = "Look-ahead window size in days (fixed at 30).", examples = "30")
		private int withinDays;

		@Schema(description = "Number of ACTIVE documents whose validTo falls within the window.", examples = "42")
		private long count;

		public static ExpiringSoon create() {
			return new ExpiringSoon();
		}

		public int getWithinDays() {
			return withinDays;
		}

		public void setWithinDays(final int withinDays) {
			this.withinDays = withinDays;
		}

		public ExpiringSoon withWithinDays(final int withinDays) {
			this.withinDays = withinDays;
			return this;
		}

		public long getCount() {
			return count;
		}

		public void setCount(final long count) {
			this.count = count;
		}

		public ExpiringSoon withCount(final long count) {
			this.count = count;
			return this;
		}

		@Override
		public int hashCode() {
			return Objects.hash(count, withinDays);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof final ExpiringSoon other)) {
				return false;
			}
			return count == other.count && withinDays == other.withinDays;
		}

		@Override
		public String toString() {
			return "ExpiringSoon [withinDays=" + withinDays + ", count=" + count + "]";
		}
	}
}
