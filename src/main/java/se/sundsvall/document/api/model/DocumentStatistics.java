package se.sundsvall.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Aggregated usage statistics for a document.", accessMode = READ_ONLY)
public class DocumentStatistics {

	@Schema(description = "Municipality ID.", examples = "2281")
	private String municipalityId;

	@Schema(description = "Registration number.", examples = "2023-2281-1337")
	private String registrationNumber;

	@Schema(description = "Inclusive lower bound of the aggregation window. Null if unbounded.", examples = "2026-01-01T00:00:00+01:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime from;

	@Schema(description = "Exclusive upper bound of the aggregation window. Null if unbounded.", examples = "2026-04-17T00:00:00+02:00")
	@DateTimeFormat(iso = ISO.DATE_TIME)
	private OffsetDateTime to;

	@Schema(description = "Total accesses across all revisions and files within the aggregation window.", examples = "142")
	private long totalAccesses;

	@Schema(description = "Per-revision breakdown.")
	private List<RevisionStatistics> perRevision;

	public static DocumentStatistics create() {
		return new DocumentStatistics();
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public DocumentStatistics withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public DocumentStatistics withRegistrationNumber(final String registrationNumber) {
		this.registrationNumber = registrationNumber;
		return this;
	}

	public OffsetDateTime getFrom() {
		return from;
	}

	public void setFrom(final OffsetDateTime from) {
		this.from = from;
	}

	public DocumentStatistics withFrom(final OffsetDateTime from) {
		this.from = from;
		return this;
	}

	public OffsetDateTime getTo() {
		return to;
	}

	public void setTo(final OffsetDateTime to) {
		this.to = to;
	}

	public DocumentStatistics withTo(final OffsetDateTime to) {
		this.to = to;
		return this;
	}

	public long getTotalAccesses() {
		return totalAccesses;
	}

	public void setTotalAccesses(final long totalAccesses) {
		this.totalAccesses = totalAccesses;
	}

	public DocumentStatistics withTotalAccesses(final long totalAccesses) {
		this.totalAccesses = totalAccesses;
		return this;
	}

	public List<RevisionStatistics> getPerRevision() {
		return perRevision;
	}

	public void setPerRevision(final List<RevisionStatistics> perRevision) {
		this.perRevision = perRevision;
	}

	public DocumentStatistics withPerRevision(final List<RevisionStatistics> perRevision) {
		this.perRevision = perRevision;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, municipalityId, perRevision, registrationNumber, to, totalAccesses);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DocumentStatistics other)) {
			return false;
		}
		return Objects.equals(from, other.from)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(perRevision, other.perRevision)
			&& Objects.equals(registrationNumber, other.registrationNumber)
			&& Objects.equals(to, other.to)
			&& totalAccesses == other.totalAccesses;
	}

	@Override
	public String toString() {
		return "DocumentStatistics [municipalityId=" + municipalityId + ", registrationNumber=" + registrationNumber
			+ ", from=" + from + ", to=" + to + ", totalAccesses=" + totalAccesses + ", perRevision=" + perRevision + "]";
	}
}
