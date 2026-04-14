package se.sundsvall.document.service.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the S3-compatible object store used by {@link S3BinaryStore}.
 *
 * Works with AWS S3, MinIO and Garage. Path-style access (`path-style-access=true`) is usually
 * required for MinIO and Garage; AWS S3 supports it too but prefers virtual-host style.
 */
@ConfigurationProperties(prefix = "document.storage.s3")
public record S3StorageProperties(
	String endpoint,
	String region,
	String bucket,
	String accessKey,
	String secretKey,
	boolean pathStyleAccess) {
}
