package se.sundsvall.document.service.scheduling;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.document.integration.db.DocumentRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentStatusSchedulerTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 4, 16);

	@Mock
	private DocumentRepository documentRepositoryMock;

	@Test
	void reconcileStatuses_runsBothBulkUpdates() {
		final var clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Europe/Stockholm")).toInstant(), ZoneId.of("Europe/Stockholm"));
		final var scheduler = new DocumentStatusScheduler(documentRepositoryMock, clock);

		when(documentRepositoryMock.bulkScheduledToActive(TODAY)).thenReturn(2);
		when(documentRepositoryMock.bulkExpire(TODAY)).thenReturn(3);

		scheduler.reconcileStatuses();

		verify(documentRepositoryMock).bulkScheduledToActive(TODAY);
		verify(documentRepositoryMock).bulkExpire(TODAY);
		verifyNoMoreInteractions(documentRepositoryMock);
	}
}
