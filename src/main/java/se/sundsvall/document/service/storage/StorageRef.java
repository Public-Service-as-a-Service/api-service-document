package se.sundsvall.document.service.storage;

import java.util.Objects;

/**
 * Opaque reference to a stored binary.
 * <p>
 * `backend` picks the {@link BinaryStore} that produced it; `locator` is backend-specific
 * (the `document_data_binary.id` row for jdbc, the S3 object key for s3).
 * <p>
 * Persisted on {@link se.sundsvall.document.integration.db.model.DocumentDataEntity} as two
 * columns so that a single row can point at either backend — this is what enables the
 * per-row migration path (Stage 2) without a flag-day cut-over.
 */
public record StorageRef(String backend, String locator) {

	public static final String BACKEND_JDBC = "jdbc";
	public static final String BACKEND_S3 = "s3";

	public StorageRef {
		Objects.requireNonNull(backend, "backend");
		Objects.requireNonNull(locator, "locator");
	}

	public static StorageRef jdbc(String documentDataBinaryId) {
		return new StorageRef(BACKEND_JDBC, documentDataBinaryId);
	}

	public static StorageRef s3(String objectKey) {
		return new StorageRef(BACKEND_S3, objectKey);
	}
}
