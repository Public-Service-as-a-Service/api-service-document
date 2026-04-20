package se.sundsvall.document.integration.elasticsearch;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Enables Spring Data ES repository scanning for the indexing layer. The connection itself is
 * configured via Spring Boot's default {@code spring.elasticsearch.*} properties
 * (see {@code application.yml}).
 * <p>
 * Skipped in the {@code junit} profile — WebTestClient tests mock {@link DocumentIndexRepository}
 * and {@link org.springframework.data.elasticsearch.core.ElasticsearchOperations} via
 * {@code @MockitoBean}, so real ES wiring is not needed and would fail to connect.
 */
@Configuration
@Profile("!junit")
@EnableElasticsearchRepositories(basePackages = "se.sundsvall.document.integration.elasticsearch")
public class ElasticsearchConfiguration {
}
