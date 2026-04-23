package se.sundsvall.document.service.statistics.overview;

import se.sundsvall.document.api.model.DocumentStatus;

public record StatusCountProjection(DocumentStatus status, long count) {
}
