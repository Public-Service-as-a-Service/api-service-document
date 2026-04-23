package se.sundsvall.document.service.statistics;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class StatisticsConfiguration {

	public static final String STATISTICS_EXECUTOR = "statisticsTaskExecutor";

	@Bean(name = STATISTICS_EXECUTOR)
	public Executor statisticsTaskExecutor() {
		final var executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("stats-");
		executor.initialize();
		return executor;
	}
}
