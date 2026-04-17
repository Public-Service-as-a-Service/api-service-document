package se.sundsvall.document.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import se.sundsvall.dept44.problem.Problem;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * {@link BinaryStore} implementation that persists bytes in an S3-compatible object store.
 * Works with AWS S3, MinIO and Garage.
 * <p>
 * Object key = UUID (one per write). No global dedup — revision copies produce new keys via
 * server-side {@code CopyObject}; this matches the current MariaDB behaviour (bytes duplicated
 * per revision) and preserves the "delete old revision doesn't affect current" guarantee.
 */
@Component
public class S3BinaryStore implements BinaryStore {

	private final S3Client s3Client;
	private final S3StorageProperties properties;

	public S3BinaryStore(S3Client s3Client, S3StorageProperties properties) {
		this.s3Client = s3Client;
		this.properties = properties;
	}

	@Override
	public StorageRef put(InputStream in, long sizeInBytes, String contentType, Map<String, String> userMetadata) {
		final var key = UUID.randomUUID().toString();
		try {
			final var builder = PutObjectRequest.builder()
				.bucket(properties.bucket())
				.key(key)
				.contentType(contentType)
				.contentLength(sizeInBytes);
			if (userMetadata != null && !userMetadata.isEmpty()) {
				builder.metadata(encodeMetadataValues(userMetadata));
			}
			s3Client.putObject(builder.build(), RequestBody.fromInputStream(in, sizeInBytes));
			return StorageRef.s3(key);
		} catch (final Exception e) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, "Failed to store binary in S3: " + e.getMessage());
		}
	}

	// SigV4 signs header values byte-for-byte; HTTP/1.1 header transport mangles anything outside
	// US-ASCII, so we URL-encode metadata values to keep the signed and transmitted bytes identical.
	// Values stay readable in `mc stat` (e.g. `fet_s%C3%A4l.jpg` for `fet_säl.jpg`).
	private static Map<String, String> encodeMetadataValues(Map<String, String> raw) {
		final var encoded = new LinkedHashMap<String, String>(raw.size());
		raw.forEach((k, v) -> encoded.put(k, URLEncoder.encode(v, StandardCharsets.UTF_8).replace("+", "%20")));
		return encoded;
	}

	@Override
	public void streamTo(StorageRef ref, OutputStream out) throws IOException {
		final var request = GetObjectRequest.builder()
			.bucket(properties.bucket())
			.key(ref.locator())
			.build();
		try (var response = s3Client.getObject(request)) {
			StreamUtils.copy(response, out);
		} catch (final NoSuchKeyException e) {
			throw Problem.valueOf(NOT_FOUND, "No binary content found for locator: " + ref.locator());
		}
	}

	@Override
	public void delete(StorageRef ref) {
		final var request = DeleteObjectRequest.builder()
			.bucket(properties.bucket())
			.key(ref.locator())
			.build();
		s3Client.deleteObject(request);
	}

	@Override
	public StorageRef copy(StorageRef ref) {
		final var newKey = UUID.randomUUID().toString();
		final var request = CopyObjectRequest.builder()
			.sourceBucket(properties.bucket())
			.sourceKey(ref.locator())
			.destinationBucket(properties.bucket())
			.destinationKey(newKey)
			.build();
		s3Client.copyObject(request);
		return StorageRef.s3(newKey);
	}
}
