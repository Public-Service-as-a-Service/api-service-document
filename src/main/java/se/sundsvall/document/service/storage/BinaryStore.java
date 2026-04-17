package se.sundsvall.document.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Service-layer abstraction over the location of a document's file content.
 * <p>
 * Current implementation: {@link S3BinaryStore} backed by an S3-compatible object store
 * (AWS S3, MinIO, Garage). The interface is kept to leave room for alternate backends later
 * (e.g. encrypted-S3, WOPI, archival) without churning {@code DocumentService}/{@code DocumentMapper}.
 */
public interface BinaryStore {

	/**
	 * Store the bytes from `in` and return a {@link StorageRef} that can later read them back.
	 * <p>
	 * Implementations MUST stream — no full-file buffering in the heap.
	 *
	 * @param in           the byte stream; will be read to completion
	 * @param sizeInBytes  total length (must match `in`); required by S3 `PutObject`
	 * @param contentType  MIME type; may be persisted as object metadata
	 * @param userMetadata optional key/value pairs attached as object user-metadata. Intended for
	 *                     minimal context when inspecting the bucket manually — keep the values
	 *                     small and immutable. May be empty.
	 */
	StorageRef put(InputStream in, long sizeInBytes, String contentType, Map<String, String> userMetadata);

	/**
	 * Stream the bytes identified by `ref` into `out`. Used for the file-download endpoint.
	 * <p>
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
	 * <p>
	 * {@link S3BinaryStore} issues a server-side {@code CopyObject} to a new key.
	 */
	StorageRef copy(StorageRef ref);
}
