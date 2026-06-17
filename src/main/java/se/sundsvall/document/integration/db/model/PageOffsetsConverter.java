package se.sundsvall.document.integration.db.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV codec for {@code page_offsets}. We chose CSV over a JSON column / element collection because
 * a PDF's offsets are always read as a whole list together with the extracted text; no partial
 * reads, no filtering by offset value. CSV keeps the column a plain LONGTEXT (no JSON-engine
 * dependency), parses in microseconds even for thousand-page docs, and round-trips losslessly for
 * any non-negative 32-bit int.
 */
@Converter
public class PageOffsetsConverter implements AttributeConverter<List<Integer>, String> {

	@Override
	public String convertToDatabaseColumn(final List<Integer> offsets) {
		if (offsets == null || offsets.isEmpty()) {
			return null;
		}
		return offsets.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	@Override
	public List<Integer> convertToEntityAttribute(final String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		return Arrays.stream(s.split(","))
			.map(String::trim)
			.filter(t -> !t.isEmpty())
			.map(Integer::parseInt)
			.toList();
	}
}
