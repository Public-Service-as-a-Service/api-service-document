package openapi;

import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.yaml.YAMLMapper;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;
import se.sundsvall.dept44.util.ResourceUtils;
import se.sundsvall.document.Application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.Files.writeString;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

@ActiveProfiles("it")
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = Application.class, properties = {
	"spring.main.banner-mode=off",
	"logging.level.se.sundsvall.dept44.payload=OFF",
	"wiremock.server.port=10101",
	// This test only generates the OpenAPI spec — it doesn't exercise search, and booting the
	// full app without an ES container would try to connect to the default localhost:9200.
	"document.search.enabled=false",
	"spring.autoconfigure.exclude="
		+ "org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration,"
		+ "org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration,"
		+ "org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration,"
		+ "org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchRepositoriesAutoConfiguration"
})
class OpenApiSpecificationIT {

	private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

	@Value("${openapi.name}")
	private String openApiName;
	@Value("${openapi.version}")
	private String openApiVersion;

	@Value("classpath:/api/openapi.yaml")
	private Resource openApiResource;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void compareOpenApiSpecifications() throws IOException {
		final var existingOpenApiSpecification = ResourceUtils.asString(openApiResource);
		final var currentOpenApiSpecification = getCurrentOpenApiSpecification();

		writeString(Path.of("target/openapi.yml"), currentOpenApiSpecification);

		assertThatJson(toJson(currentOpenApiSpecification))
			.withOptions(List.of(Option.IGNORING_ARRAY_ORDER))
			.whenIgnoringPaths("servers")
			.isEqualTo(toJson(existingOpenApiSpecification));
	}

	/**
	 * Fetches and returns the current OpenAPI specification in YAML format.
	 *
	 * @return the current OpenAPI specification
	 */
	private String getCurrentOpenApiSpecification() {
		final var uri = UriComponentsBuilder.fromPath("/api-docs.yaml")
			.buildAndExpand(openApiName, openApiVersion)
			.toUri();

		return restTemplate.getForObject(uri, String.class);
	}

	/**
	 * Attempts to convert the given YAML (no YAML-check...) to JSON.
	 *
	 * @param yaml the YAML to convert
	 * @return a JSON string
	 */
	private String toJson(final String yaml) {
		try {
			return YAML_MAPPER.readTree(yaml).toString();
		} catch (final JacksonException e) {
			throw new IllegalStateException("Unable to convert YAML to JSON", e);
		}
	}
}
