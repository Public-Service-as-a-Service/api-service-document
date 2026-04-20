package se.sundsvall.document.integration.elasticsearch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Enables Spring Data ES repository scanning for the indexing layer. The connection itself is
 * configured via Spring Boot's default {@code spring.elasticsearch.*} properties
 * (see {@code application.yml}).
 * <p>
 * Gated by {@code document.search.enabled} so {@code @SpringBootTest}s that don't actually need
 * ES can opt out — {@code application-junit.yml} sets it to {@code false}, and
 * {@code OpenApiSpecificationIT} overrides it via {@code @SpringBootTest(properties=…)}. Default
 * is {@code true} (set in {@code application.yml}) so production and the apptest suite both pick
 * it up without extra config.
 */
@Configuration
@ConditionalOnProperty(name = "document.search.enabled", havingValue = "true", matchIfMissing = true)
@EnableElasticsearchRepositories(basePackages = "se.sundsvall.document.integration.elasticsearch")
public class ElasticsearchConfiguration {
}
