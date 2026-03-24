package se.sundsvall.document.integration.eventlog.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static java.util.Objects.nonNull;

@ConditionalOnProperty(name = "integration.eventlog.enabled", havingValue = "true")
@Import(FeignConfiguration.class)
public class EventlogConfiguration {

	public static final String CLIENT_ID = "eventlog";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(EventlogProperties eventlogProperties, @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository) {
		final var feignMultiCustomizer = FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRequestTimeoutsInSeconds(eventlogProperties.connectTimeout(), eventlogProperties.readTimeout());

		if (nonNull(clientRegistrationRepository)) {
			feignMultiCustomizer.withRetryableOAuth2InterceptorForClientRegistration(clientRegistrationRepository.findByRegistrationId(CLIENT_ID));
		}

		return feignMultiCustomizer.composeCustomizersToOne();
	}
}
