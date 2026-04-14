package se.sundsvall.document.service.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.SecureRandom;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import se.sundsvall.dept44.problem.ThrowableProblem;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Contract test for {@link S3BinaryStore}. Boots a real MinIO via TestContainers and exercises the
 * put/streamTo/copy/delete round-trip plus error paths. No Spring context — keeps the integration-test
 * profile on the default {@code jdbc} backend for the rest of the {@code apptest/*IT} suite.
 */
class S3BinaryStoreIT {

	private static final String BUCKET = "document-it";

	private static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2024-10-13T13-34-11Z");

	private static S3Client s3Client;
	private static S3BinaryStore binaryStore;

	@BeforeAll
	static void setUp() {
		MINIO.start();

		s3Client = S3Client.builder()
			.endpointOverride(URI.create(MINIO.getS3URL()))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(MINIO.getUserName(), MINIO.getPassword())))
			.region(Region.US_EAST_1)
			.serviceConfiguration(S3Configuration.builder()
				.pathStyleAccessEnabled(true)
				.build())
			.build();

		s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());

		final var properties = new S3StorageProperties(
			MINIO.getS3URL(),
			"us-east-1",
			BUCKET,
			MINIO.getUserName(),
			MINIO.getPassword(),
			true);
		binaryStore = new S3BinaryStore(s3Client, properties);
	}

	@AfterAll
	static void tearDown() {
		if (s3Client != null) {
			s3Client.close();
		}
		MINIO.stop();
	}

	@Test
	void put_thenStreamTo_roundTripsBytes() throws Exception {
		final var payload = randomBytes(5 * 1024 * 1024); // 5 MB

		final var ref = binaryStore.put(new ByteArrayInputStream(payload), payload.length, "application/octet-stream");

		assertThat(ref).isNotNull();
		assertThat(ref.backend()).isEqualTo("s3");
		assertThat(ref.locator()).isNotBlank();

		final var sink = new ByteArrayOutputStream();
		binaryStore.streamTo(ref, sink);

		assertThat(sink.toByteArray()).isEqualTo(payload);
	}

	@Test
	void copy_createsIndependentObjectWithSameContent() throws Exception {
		final var payload = "hello storage".getBytes();

		final var original = binaryStore.put(new ByteArrayInputStream(payload), payload.length, "text/plain");
		final var copy = binaryStore.copy(original);

		assertThat(copy.backend()).isEqualTo("s3");
		assertThat(copy.locator()).isNotEqualTo(original.locator());

		// Delete the original; the copy must still be readable.
		binaryStore.delete(original);

		final var sink = new ByteArrayOutputStream();
		binaryStore.streamTo(copy, sink);
		assertThat(sink.toByteArray()).isEqualTo(payload);
	}

	@Test
	void delete_removesObject_andStreamToThrowsNotFound() {
		final var payload = "temp".getBytes();
		final var ref = binaryStore.put(new ByteArrayInputStream(payload), payload.length, "text/plain");

		binaryStore.delete(ref);

		assertThatThrownBy(() -> binaryStore.streamTo(ref, new ByteArrayOutputStream()))
			.isInstanceOf(ThrowableProblem.class)
			.satisfies(ex -> assertThat(((ThrowableProblem) ex).getStatus()).isEqualTo(NOT_FOUND));
	}

	@Test
	void streamTo_onMissingKey_throwsNotFound() {
		final var ref = StorageRef.s3(UUID.randomUUID().toString());

		assertThatThrownBy(() -> binaryStore.streamTo(ref, new ByteArrayOutputStream()))
			.isInstanceOf(ThrowableProblem.class)
			.satisfies(ex -> assertThat(((ThrowableProblem) ex).getStatus()).isEqualTo(NOT_FOUND));
	}

	@Test
	void delete_onMissingKey_isIdempotent() {
		final var ref = StorageRef.s3(UUID.randomUUID().toString());

		assertThatCode(() -> binaryStore.delete(ref)).doesNotThrowAnyException();
	}

	private static byte[] randomBytes(int size) {
		final var bytes = new byte[size];
		new SecureRandom().nextBytes(bytes);
		return bytes;
	}
}
