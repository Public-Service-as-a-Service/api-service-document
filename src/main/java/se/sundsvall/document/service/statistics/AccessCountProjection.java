package se.sundsvall.document.service.statistics;

import se.sundsvall.document.api.model.DocumentAccessType;

public record AccessCountProjection(
	int revision,
	String documentDataId,
	DocumentAccessType accessType,
	long count) {
}
