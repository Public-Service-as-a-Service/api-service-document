package apptest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import se.sundsvall.dept44.test.AbstractAppTest;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Shared test base for the {@code apptest/*IT} suite. Boots a single {@link MinIOContainer} for the
 * whole JVM and wires the S3 endpoint / bucket / credentials into Spring via
 * {@link DynamicPropertySource}.
 * <p>
 * Pre-seeds the bucket with dummy JPEG bytes under every {@code storage_locator} UUID that
 * {@code testdata-it.sql} references, so read-file ITs can stream bytes back without further setup.
 */
abstract class AbstractDocumentAppTest extends AbstractAppTest {

	protected static final String S3_BUCKET = "document-it";

	protected static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2024-10-13T13-34-11Z");

	private static final List<String> SEEDED_OBJECT_KEYS = List.of(
		"d35254ce-d26c-47e3-806f-4cf68cf2fa56",
		"3b570ff2-b631-4584-a9fb-77dce2f6d85b",
		"53978846-e715-455b-a4e7-440084f0b49b",
		"297282c6-d06e-4c33-8bc8-0828866ff7e5",
		"bfb3ad87-cb18-4b70-9594-128d284a7e6e",
		"0ac27b16-88c3-4180-9617-d8502e24932b",
		"93227b16-88c3-4180-9617-d8502e24932b");

	static {
		MINIO.start();
		try (var s3 = buildSeedClient()) {
			s3.createBucket(CreateBucketRequest.builder().bucket(S3_BUCKET).build());
			final var bytes = readSeedImage();
			for (final var key : SEEDED_OBJECT_KEYS) {
				s3.putObject(PutObjectRequest.builder()
					.bucket(S3_BUCKET)
					.key(key)
					.contentType("image/jpeg")
					.contentLength((long) bytes.length)
					.build(), RequestBody.fromBytes(bytes));
			}
		} catch (final IOException e) {
			throw new IllegalStateException("Failed to seed MinIO test bucket", e);
		}
	}

	@DynamicPropertySource
	static void s3Properties(DynamicPropertyRegistry registry) {
		registry.add("document.storage.s3.endpoint", MINIO::getS3URL);
		registry.add("document.storage.s3.bucket", () -> S3_BUCKET);
		registry.add("document.storage.s3.region", () -> "us-east-1");
		registry.add("document.storage.s3.access-key", MINIO::getUserName);
		registry.add("document.storage.s3.secret-key", MINIO::getPassword);
		registry.add("document.storage.s3.path-style-access", () -> "true");
	}

	private static S3Client buildSeedClient() {
		return S3Client.builder()
			.endpointOverride(URI.create(MINIO.getS3URL()))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(MINIO.getUserName(), MINIO.getPassword())))
			.region(Region.US_EAST_1)
			.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
			.build();
	}

	private static byte[] readSeedImage() throws IOException {
		try (var in = new ClassPathResource("minio-seed-image.jpg").getInputStream()) {
			return in.readAllBytes();
		}
	}
}
