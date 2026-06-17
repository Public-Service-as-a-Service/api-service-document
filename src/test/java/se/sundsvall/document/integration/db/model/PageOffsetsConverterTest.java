package se.sundsvall.document.integration.db.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageOffsetsConverterTest {

	private final PageOffsetsConverter converter = new PageOffsetsConverter();

	@Test
	void roundTrip() {
		final var offsets = List.of(0, 2340, 5712, 9001);

		final var serialized = converter.convertToDatabaseColumn(offsets);
		final var deserialized = converter.convertToEntityAttribute(serialized);

		assertThat(serialized).isEqualTo("0,2340,5712,9001");
		assertThat(deserialized).containsExactly(0, 2340, 5712, 9001);
	}

	@Test
	void nullAndEmptyListSerializeToNull() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
		assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
	}

	@Test
	void nullAndBlankStringDeserializeToNull() {
		assertThat(converter.convertToEntityAttribute(null)).isNull();
		assertThat(converter.convertToEntityAttribute("")).isNull();
		assertThat(converter.convertToEntityAttribute("   ")).isNull();
	}

	@Test
	void singleElementRoundTrip() {
		assertThat(converter.convertToDatabaseColumn(List.of(0))).isEqualTo("0");
		assertThat(converter.convertToEntityAttribute("0")).containsExactly(0);
	}
}
