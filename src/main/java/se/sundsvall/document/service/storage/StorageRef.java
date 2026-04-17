package se.sundsvall.document.service.storage;

import java.util.Objects;

/**
 * Opaque reference to a stored binary.
 * <p>
 * {@code backend} identifies the {@link BinaryStore} that produced the ref; {@code locator} is
 * backend-specific (the S3 object key for the current implementation). Persisted on
 * {@link se.sundsvall.document.integration.db.model.DocumentDataEntity} as {@code storage_locator}.
 */
public record StorageRef(String backend, String locator) {

	public static final String BACKEND_S3 = "s3";

	public StorageRef {
		Objects.requireNonNull(backend, "backend");
		Objects.requireNonNull(locator, "locator");
	}

	public static StorageRef s3(String objectKey) {
		return new StorageRef(BACKEND_S3, objectKey);
	}
}
