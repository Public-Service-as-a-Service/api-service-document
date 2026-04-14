package se.sundsvall.document.service.storage;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@ConditionalOnProperty(name = "document.storage.backend", havingValue = "s3")
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3StorageConfiguration {

	@Bean
	S3Client s3Client(S3StorageProperties properties) {
		// AWS SDK v2.30+ enables flexible checksums by default (STREAMING-*-TRAILER payload modes);
		// Garage v2 doesn't support them and rejects with "Invalid payload signature". Pin the
		// client to pre-v2.30 behaviour: skip CRC32 trailers and use single-chunk PutObject with
		// full SHA-256 precomputed at signing time.
		final var builder = S3Client.builder()
			.region(Region.of(properties.region() != null ? properties.region() : "us-east-1"))
			.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
			.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
			.serviceConfiguration(S3Configuration.builder()
				.pathStyleAccessEnabled(properties.pathStyleAccess())
				.chunkedEncodingEnabled(false)
				.build());

		if (properties.endpoint() != null) {
			builder.endpointOverride(URI.create(properties.endpoint()));
		}

		if (properties.accessKey() != null && properties.secretKey() != null) {
			builder.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
		} else {
			builder.credentialsProvider(DefaultCredentialsProvider.create());
		}

		return builder.build();
	}
}
