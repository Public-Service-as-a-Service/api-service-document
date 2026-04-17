package se.sundsvall.document.service.statistics;

import java.time.OffsetDateTime;
import se.sundsvall.document.api.model.DocumentAccessType;

public record DocumentAccessedEvent(
	String municipalityId,
	String documentId,
	String registrationNumber,
	int revision,
	String documentDataId,
	DocumentAccessType accessType,
	String accessedBy,
	OffsetDateTime accessedAt) {
}
