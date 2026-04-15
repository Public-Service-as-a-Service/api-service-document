package se.sundsvall.document.api.validation;

import java.util.List;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;
import se.sundsvall.document.api.model.DocumentType;
import se.sundsvall.document.service.DocumentTypeService;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class DocumentTypeValidator {

	private final DocumentTypeService documentTypeService;

	DocumentTypeValidator(DocumentTypeService documentTypeService) {
		this.documentTypeService = documentTypeService;
	}

	public void validate(final String municipalityId, final String documentType) {
		if (isNull(documentType)) {
			return;
		}

		final var validTypes = documentTypeService.read(municipalityId).stream().map(DocumentType::getType).toList();

		validTypes.stream()
			.filter(type -> Strings.CI.equals(type, documentType))
			.findAny()
			.orElseThrow(() -> new ConstraintViolationProblem(BAD_REQUEST, List.of(new Violation("type",
				"document type '%s' must match one of %s".formatted(documentType, validTypes)))));
	}
}
