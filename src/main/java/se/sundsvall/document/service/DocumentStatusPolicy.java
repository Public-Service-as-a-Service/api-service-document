package se.sundsvall.document.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.document.api.model.DocumentStatus;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.document.api.model.DocumentStatus.ACTIVE;
import static se.sundsvall.document.api.model.DocumentStatus.DRAFT;
import static se.sundsvall.document.api.model.DocumentStatus.EXPIRED;
import static se.sundsvall.document.api.model.DocumentStatus.REVOKED;
import static se.sundsvall.document.api.model.DocumentStatus.SCHEDULED;
import static se.sundsvall.document.service.Constants.ERROR_PUBLISH_EXPIRED;

/**
 * Central policy for status transitions and defaults. Takes a {@link Clock} so service,
 * scheduler and tests can share a single time source and avoid implicit timezone issues.
 */
@Component
public class DocumentStatusPolicy {

	private static final List<DocumentStatus> PUBLISHED_STATUSES = List.of(SCHEDULED, ACTIVE, EXPIRED);

	private final Clock clock;

	public DocumentStatusPolicy(final Clock clock) {
		this.clock = clock;
	}

	public LocalDate today() {
		return LocalDate.now(clock);
	}

	/**
	 * Resolves the status a document should have when transitioned into "published" state
	 * (via publish or unrevoke).
	 */
	public DocumentStatus resolvePublishedStatus(LocalDate validFrom, LocalDate validTo, String registrationNumber) {
		final var today = today();
		if (nonNull(validTo) && validTo.isBefore(today)) {
			throw Problem.valueOf(CONFLICT, ERROR_PUBLISH_EXPIRED.formatted(registrationNumber, validTo));
		}
		if (nonNull(validFrom) && validFrom.isAfter(today)) {
			return SCHEDULED;
		}
		return ACTIVE;
	}

	/**
	 * Forward-only reconciliation used by the pre-read safety net. Returns an updated
	 * status when the current status is stale relative to today's date, otherwise empty.
	 *
	 * <ul>
	 * <li>SCHEDULED → ACTIVE when validFrom has been reached (and validTo permits)</li>
	 * <li>ACTIVE/SCHEDULED → EXPIRED when validTo has passed</li>
	 * </ul>
	 *
	 * EXPIRED is terminal; DRAFT and REVOKED are never auto-transitioned.
	 */
	public Optional<DocumentStatus> reconcile(DocumentStatus current, LocalDate validFrom, LocalDate validTo) {
		final var today = today();
		final var expired = nonNull(validTo) && validTo.isBefore(today);

		if (expired && (current == ACTIVE || current == SCHEDULED)) {
			return Optional.of(EXPIRED);
		}
		if (current == SCHEDULED && !expired && (validFrom == null || !validFrom.isAfter(today))) {
			return Optional.of(ACTIVE);
		}
		return Optional.empty();
	}

	/**
	 * Returns the status set to use when no explicit filter is supplied. Defaults to the
	 * "published" statuses — DRAFT and REVOKED are excluded.
	 */
	public List<DocumentStatus> effectivePublishedStatuses(List<DocumentStatus> requested) {
		if (requested == null || requested.isEmpty()) {
			return PUBLISHED_STATUSES;
		}
		return requested;
	}

	public static List<DocumentStatus> nonPublicStatuses() {
		return List.of(DRAFT, REVOKED);
	}
}
