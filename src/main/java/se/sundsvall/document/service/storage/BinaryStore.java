package se.sundsvall.document.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Service-layer abstraction over the location of a document's file content.
 *
 * Implementations:
 * - `JdbcBinaryStore` stores the bytes in the MariaDB `document_data_binary.binary_file` LONGBLOB column.
 * - `S3BinaryStore` stores the bytes in an S3-compatible object store (AWS S3, MinIO, Garage).
 *
 * Picked at runtime via the `document.storage.backend` property (`jdbc` or `s3`). See Stage 1 of the
 * storage-migration plan for context.
 */
public interface BinaryStore {

	/**
	 * Store the bytes from `in` and return a {@link StorageRef} that can later read them back.
	 *
	 * Implementations MUST stream — no full-file buffering in the heap.
	 *
	 * @param in           the byte stream; will be read to completion
	 * @param sizeInBytes  total length (must match `in`); required by S3 `PutObject`
	 * @param contentType  MIME type; may be persisted as object metadata
	 * @param userMetadata optional key/value pairs attached as object user-metadata (S3 only; the
	 *                     JDBC backend ignores them). Intended for minimal context when inspecting
	 *                     the bucket manually — keep the values small and immutable. May be empty.
	 */
	StorageRef put(InputStream in, long sizeInBytes, String contentType, Map<String, String> userMetadata);

	/**
	 * Stream the bytes identified by `ref` into `out`. Used for the file-download endpoint.
	 *
	 * MUST stream — no full-file buffering in the heap.
	 */
	void streamTo(StorageRef ref, OutputStream out) throws IOException;

	/**
	 * Hard-delete the bytes identified by `ref`. Used by the single-file-delete endpoint.
	 * Revision copies retain their own ref, so deleting one does not affect older revisions.
	 */
	void delete(StorageRef ref);

	/**
	 * Return a new {@link StorageRef} referring to the same bytes. Used when a document is updated
	 * without replacing its files — the new revision's `document_data` rows get copied refs.
	 *
	 * - `JdbcBinaryStore` creates a new `document_data_binary` row that shares the underlying Blob.
	 * - `S3BinaryStore` issues a server-side `CopyObject` to a new key.
	 */
	StorageRef copy(StorageRef ref);
}
