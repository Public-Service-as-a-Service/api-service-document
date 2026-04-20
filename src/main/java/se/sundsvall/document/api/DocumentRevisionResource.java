package se.sundsvall.document.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.document.api.model.Document;
import se.sundsvall.document.api.model.PagedDocumentResponse;
import se.sundsvall.document.service.DocumentFileService;
import se.sundsvall.document.service.DocumentService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.document.api.Constants.DOCUMENT_REVISIONS_BASE_PATH;

@RestController
@Validated
@RequestMapping(DOCUMENT_REVISIONS_BASE_PATH)
@Tag(name = "Document revisions", description = "Document revision operations")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
public class DocumentRevisionResource {

	private final DocumentService documentService;
	private final DocumentFileService documentFileService;

	public DocumentRevisionResource(final DocumentService documentService, final DocumentFileService documentFileService) {
		this.documentService = documentService;
		this.documentFileService = documentFileService;
	}

	@GetMapping(produces = {
		APPLICATION_JSON_VALUE
	})
	@Operation(summary = "Read document revisions.", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),

	})
	public ResponseEntity<PagedDocumentResponse> readRevisions(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @PathVariable("municipalityId") @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "registrationNumber", description = "Document registration number", example = "2023-2281-1337") @PathVariable("registrationNumber") final String registrationNumber,
		@Parameter(name = "includeConfidential", description = "Include confidential records", example = "true") @RequestParam(name = "includeConfidential", defaultValue = "false") final boolean includeConfidential,
		@ParameterObject final Pageable pageable) {

		return ok(documentService.readAll(registrationNumber, includeConfidential, pageable, municipalityId));
	}

	@GetMapping(path = "/{revision}", produces = {
		APPLICATION_JSON_VALUE
	})
	@Operation(summary = "Read document revision.", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),

	})
	public ResponseEntity<Document> readRevision(
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @PathVariable("municipalityId") @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "registrationNumber", description = "Document registration number", example = "2023-2281-1337") @PathVariable("registrationNumber") final String registrationNumber,
		@Parameter(name = "revision", description = "Document revision", example = "2") @Min(0) @PathVariable("revision") final int revision,
		@Parameter(name = "includeConfidential", description = "Include confidential records", example = "true") @RequestParam(name = "includeConfidential", defaultValue = "false") final boolean includeConfidential) {

		return ok(documentService.read(registrationNumber, revision, includeConfidential, municipalityId));
	}

	@GetMapping(path = "/{revision}/files/{documentDataId}", produces = {
		APPLICATION_JSON_VALUE
	})
	@Operation(summary = "Read document file revision.", responses = {
		@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class))),

	})
	public ResponseEntity<Void> readFileRevision(
		final HttpServletResponse response,
		@Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @PathVariable("municipalityId") @ValidMunicipalityId final String municipalityId,
		@Parameter(name = "registrationNumber", description = "Document registration number", example = "2023-2281-1337") @PathVariable("registrationNumber") final String registrationNumber,
		@Parameter(name = "revision", description = "Document revision", example = "2") @Min(0) @PathVariable("revision") final int revision,
		@Parameter(name = "documentDataId", description = "Document data ID", example = "082ba08f-03c7-409f-b8a6-940a1397ba38") @PathVariable("documentDataId") @ValidUuid final String documentDataId,
		@Parameter(name = "includeConfidential", description = "Include confidential records", example = "true") @RequestParam(name = "includeConfidential", defaultValue = "false") final boolean includeConfidential) {

		documentFileService.readFile(registrationNumber, revision, documentDataId, includeConfidential, response, municipalityId);
		return ok().build();
	}
}
