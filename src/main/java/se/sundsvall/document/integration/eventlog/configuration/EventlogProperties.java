package se.sundsvall.document.integration.eventlog.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConditionalOnProperty(name = "integration.eventlog.enabled", havingValue = "true")
@ConfigurationProperties("integration.eventlog")
public record EventlogProperties(int connectTimeout, int readTimeout, String logKeyUuid) {
}
