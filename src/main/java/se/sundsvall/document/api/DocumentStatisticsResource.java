package se.sundsvall.document.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.document.api.model.DocumentStatistics;
import se.sundsvall.document.service.statistics.DocumentStatisticsService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static se.sundsvall.document.Constants.DOCUMENT_STATISTICS_BASE_PATH;

@RestController
@Validated
@RequestMapping(DOCUMENT_STATISTICS_BASE_PATH)
@Tag(name = "Document statistics", description = "Document usage statistics")
@ApiResponse(responseCode = "400", description = "Bad request", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(oneOf = {
	Problem.class, ConstraintViolationProblem.class
})))
@ApiResponse(responseCode = "500", description = "Internal Server error", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
public class DocumentStatisticsResource {

	private final DocumentStatisticsService statisticsService;

	public DocumentStatisticsResource(final DocumentStatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Read aggregated usage statistics for a document.",
		description = "Returns total accesses and per-revision/per-file breakdown of file downloads and views. Optional [from, to) date range bounds the aggregation window.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Successful operation", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Not found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	public ResponseEntity<DocumentStatistics> readStatistics(
		@PathVariable @Parameter(name = "municipalityId", description = "Municipality ID", example = "2281") @ValidMunicipalityId final String municipalityId,
		@PathVariable @Parameter(name = "registrationNumber", description = "Document registration number", example = "2023-2281-1337") final String registrationNumber,
		@Parameter(name = "from", description = "Inclusive lower bound (ISO date-time). Omit for unbounded.", example = "2026-01-01T00:00:00+01:00") @RequestParam(name = "from", required = false) @DateTimeFormat(
			iso = ISO.DATE_TIME) final OffsetDateTime from,
		@Parameter(name = "to", description = "Exclusive upper bound (ISO date-time). Omit for unbounded.", example = "2026-04-17T00:00:00+02:00") @RequestParam(name = "to", required = false) @DateTimeFormat(iso = ISO.DATE_TIME) final OffsetDateTime to) {

		return ok(statisticsService.getStatistics(municipalityId, registrationNumber, from, to));
	}
}
