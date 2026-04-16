package se.sundsvall.document.service.scheduling;

import java.time.Clock;
import java.time.LocalDate;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.document.integration.db.DocumentRepository;

@Component
public class DocumentStatusScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentStatusScheduler.class);

	private final DocumentRepository documentRepository;
	private final Clock clock;

	public DocumentStatusScheduler(final DocumentRepository documentRepository, final Clock clock) {
		this.documentRepository = documentRepository;
		this.clock = clock;
	}

	@Scheduled(cron = "${document.status-job.cron:0 5 0 * * *}", zone = "Europe/Stockholm")
	@SchedulerLock(name = "DocumentStatusScheduler.reconcileStatuses", lockAtLeastFor = "PT30S", lockAtMostFor = "PT10M")
	@Transactional
	public void reconcileStatuses() {
		final var today = LocalDate.now(clock);

		final var promoted = documentRepository.bulkScheduledToActive(today);
		final var expired = documentRepository.bulkExpire(today);

		LOGGER.info("Document status reconciliation for {}: SCHEDULED→ACTIVE={}, →EXPIRED={}", today, promoted, expired);
	}
}
