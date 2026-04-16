package se.sundsvall.document.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.sundsvall.document.api.model.DocumentStatus.ACTIVE;
import static se.sundsvall.document.api.model.DocumentStatus.DRAFT;
import static se.sundsvall.document.api.model.DocumentStatus.EXPIRED;
import static se.sundsvall.document.api.model.DocumentStatus.REVOKED;
import static se.sundsvall.document.api.model.DocumentStatus.SCHEDULED;

class DocumentStatusPolicyTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 4, 16);
	private static final String REGISTRATION_NUMBER = "2026-2281-1337";

	private DocumentStatusPolicy policy;

	@BeforeEach
	void setUp() {
		final var fixedClock = Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Europe/Stockholm")).toInstant(), ZoneId.of("Europe/Stockholm"));
		policy = new DocumentStatusPolicy(fixedClock);
	}

	@Test
	void todayUsesInjectedClock() {
		assertThat(policy.today()).isEqualTo(TODAY);
	}

	@Test
	void resolvePublishedStatus_nullValidity_returnsActive() {
		assertThat(policy.resolvePublishedStatus(null, null, REGISTRATION_NUMBER)).isEqualTo(ACTIVE);
	}

	@Test
	void resolvePublishedStatus_validFromInPast_returnsActive() {
		assertThat(policy.resolvePublishedStatus(TODAY.minusDays(1), null, REGISTRATION_NUMBER)).isEqualTo(ACTIVE);
	}

	@Test
	void resolvePublishedStatus_validFromToday_returnsActive() {
		assertThat(policy.resolvePublishedStatus(TODAY, null, REGISTRATION_NUMBER)).isEqualTo(ACTIVE);
	}

	@Test
	void resolvePublishedStatus_validFromInFuture_returnsScheduled() {
		assertThat(policy.resolvePublishedStatus(TODAY.plusDays(1), null, REGISTRATION_NUMBER)).isEqualTo(SCHEDULED);
	}

	@Test
	void resolvePublishedStatus_validToInPast_throwsConflict() {
		assertThatThrownBy(() -> policy.resolvePublishedStatus(null, TODAY.minusDays(1), REGISTRATION_NUMBER))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("already expired");
	}

	@Test
	void resolvePublishedStatus_validToToday_returnsActive() {
		assertThat(policy.resolvePublishedStatus(null, TODAY, REGISTRATION_NUMBER)).isEqualTo(ACTIVE);
	}

	@Test
	void reconcile_activeWithExpiredValidTo_returnsExpired() {
		assertThat(policy.reconcile(ACTIVE, null, TODAY.minusDays(1))).contains(EXPIRED);
	}

	@Test
	void reconcile_scheduledWithExpiredValidTo_returnsExpired() {
		assertThat(policy.reconcile(SCHEDULED, TODAY.plusDays(5), TODAY.minusDays(1))).contains(EXPIRED);
	}

	@Test
	void reconcile_scheduledWithReachedValidFrom_returnsActive() {
		assertThat(policy.reconcile(SCHEDULED, TODAY.minusDays(1), null)).contains(ACTIVE);
	}

	@Test
	void reconcile_scheduledWithFutureValidFrom_returnsEmpty() {
		assertThat(policy.reconcile(SCHEDULED, TODAY.plusDays(1), null)).isEmpty();
	}

	@Test
	void reconcile_activeWithStillValid_returnsEmpty() {
		assertThat(policy.reconcile(ACTIVE, TODAY.minusDays(1), TODAY.plusDays(1))).isEmpty();
	}

	@Test
	void reconcile_expiredStaysExpired() {
		assertThat(policy.reconcile(EXPIRED, null, TODAY.minusDays(1))).isEmpty();
	}

	@Test
	void reconcile_draftIsNotAutoTransitioned() {
		assertThat(policy.reconcile(DRAFT, null, TODAY.minusDays(1))).isEmpty();
	}

	@Test
	void reconcile_revokedIsNotAutoTransitioned() {
		assertThat(policy.reconcile(REVOKED, null, TODAY.minusDays(1))).isEmpty();
	}

	@Test
	void effectivePublishedStatuses_nullDefaultsToPublished() {
		assertThat(policy.effectivePublishedStatuses(null)).containsExactly(SCHEDULED, ACTIVE, EXPIRED);
	}

	@Test
	void effectivePublishedStatuses_emptyDefaultsToPublished() {
		assertThat(policy.effectivePublishedStatuses(List.of())).containsExactly(SCHEDULED, ACTIVE, EXPIRED);
	}

	@Test
	void effectivePublishedStatuses_explicitListPassedThrough() {
		final var explicit = List.of(DRAFT);
		assertThat(policy.effectivePublishedStatuses(explicit)).isSameAs(explicit);
	}

	@Test
	void nonPublicStatuses_returnsDraftAndRevoked() {
		assertThat(DocumentStatusPolicy.nonPublicStatuses()).containsExactly(DRAFT, REVOKED);
	}
}
