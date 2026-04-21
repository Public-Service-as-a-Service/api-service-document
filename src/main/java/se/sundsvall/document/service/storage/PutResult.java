package se.sundsvall.document.service.storage;

/**
 * Outcome of a {@link BinaryStore#put} call.
 * <p>
 * The SHA-256 is computed while the bytes are being streamed to the backend — one read, two sinks —
 * so callers get content-level identity for dedupe without re-reading from S3.
 */
public record PutResult(StorageRef ref, String sha256) {
}
