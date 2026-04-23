package se.sundsvall.document.service.statistics;

import se.sundsvall.document.api.model.DocumentAccessType;

/**
 * Context for a document file access — controls how the access is recorded as statistics.
 */
public record AccessContext(boolean countStats, DocumentAccessType accessType, String sentBy) {

	public static AccessContext defaultContext() {
		return new AccessContext(true, DocumentAccessType.DOWNLOAD, null);
	}
}
