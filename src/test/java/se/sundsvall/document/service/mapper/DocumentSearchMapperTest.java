package se.sundsvall.document.service.mapper;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import se.sundsvall.document.api.model.Match;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentSearchMapperTest {

	@Test
	void tokenizeQueries_splitsOnWordBoundariesAndLowercases() {
		final var terms = DocumentSearchMapper.tokenizeQueries(List.of("Hello, World!", "Router Bandwidth"));

		assertThat(terms).containsExactlyInAnyOrder("hello", "world", "router", "bandwidth");
	}

	@Test
	void tokenizeQueries_handlesUnicodeLetters() {
		final var terms = DocumentSearchMapper.tokenizeQueries(List.of("Sundsvall Kommun", "Östersund"));

		assertThat(terms).contains("sundsvall", "kommun", "östersund");
	}

	@Test
	void tokenizeQueries_skipsBlankAndNullQueries() {
		final var terms = DocumentSearchMapper.tokenizeQueries(List.of("  ", "valid query"));

		assertThat(terms).containsExactlyInAnyOrder("valid", "query");
	}

	@Test
	void tokenizeQueries_emptyInputReturnsEmptySet() {
		assertThat(DocumentSearchMapper.tokenizeQueries(null)).isEmpty();
		assertThat(DocumentSearchMapper.tokenizeQueries(List.of())).isEmpty();
	}

	@Test
	void computeMatches_findsAllOccurrencesOfQueryTerms() {
		// Two occurrences of "bandwidth", each on its own page.
		final var text = "Router bandwidth spec.\nTotal bandwidth per node.";
		// 1-page starts at 0, page 2 starts right after the newline.
		final var pageOffsets = List.of(0, text.indexOf("\nTotal") + 1);

		final var matches = DocumentSearchMapper.computeMatches(text, pageOffsets, Set.of("bandwidth"));

		assertThat(matches).hasSize(2);
		assertThat(matches).extracting(Match::getField).containsOnly("extractedText");
		assertThat(matches.get(0).getStart()).isEqualTo(text.indexOf("bandwidth"));
		assertThat(matches.get(0).getEnd()).isEqualTo(text.indexOf("bandwidth") + "bandwidth".length());
		assertThat(matches.get(0).getPage()).isEqualTo(1);
		assertThat(matches.get(1).getStart()).isEqualTo(text.indexOf("bandwidth", 10));
		assertThat(matches.get(1).getPage()).isEqualTo(2);
	}

	@Test
	void computeMatches_caseInsensitive() {
		final var text = "Bandwidth BANDWIDTH bandwidth";

		final var matches = DocumentSearchMapper.computeMatches(text, List.of(0), Set.of("bandwidth"));

		assertThat(matches).hasSize(3);
	}

	@Test
	void computeMatches_wordBoundaryOnly_doesNotMatchSubstrings() {
		// "cat" must NOT match inside "caterpillar" — BreakIterator yields "caterpillar" as one
		// token, which is not in {"cat"}.
		final var text = "The cat saw a caterpillar.";

		final var matches = DocumentSearchMapper.computeMatches(text, List.of(0), Set.of("cat"));

		assertThat(matches).hasSize(1);
		assertThat(matches.get(0).getStart()).isEqualTo(4);
		assertThat(matches.get(0).getEnd()).isEqualTo(7);
	}

	@Test
	void computeMatches_emptyOrNullText_returnsEmptyList() {
		assertThat(DocumentSearchMapper.computeMatches(null, List.of(0), Set.of("any"))).isEmpty();
		assertThat(DocumentSearchMapper.computeMatches("", List.of(0), Set.of("any"))).isEmpty();
	}

	@Test
	void computeMatches_emptyTerms_returnsEmptyList() {
		assertThat(DocumentSearchMapper.computeMatches("some text", List.of(0), Set.of())).isEmpty();
	}

	@Test
	void computeMatches_nullPageOffsets_producesMatchesWithNullPage() {
		final var text = "Router bandwidth spec.";

		final var matches = DocumentSearchMapper.computeMatches(text, null, Set.of("bandwidth"));

		assertThat(matches).hasSize(1);
		assertThat(matches.get(0).getPage()).isNull();
	}

	@Test
	void resolvePage_emptyOffsetsReturnsNull() {
		assertThat(DocumentSearchMapper.resolvePage(100, null)).isNull();
		assertThat(DocumentSearchMapper.resolvePage(100, List.of())).isNull();
	}

	@Test
	void resolvePage_exactBoundaryHits() {
		// Page starts: [0, 100, 200]. Offset 100 = start of page 2.
		final var offsets = List.of(0, 100, 200);

		assertThat(DocumentSearchMapper.resolvePage(0, offsets)).isEqualTo(1);
		assertThat(DocumentSearchMapper.resolvePage(50, offsets)).isEqualTo(1);
		assertThat(DocumentSearchMapper.resolvePage(99, offsets)).isEqualTo(1);
		assertThat(DocumentSearchMapper.resolvePage(100, offsets)).isEqualTo(2);
		assertThat(DocumentSearchMapper.resolvePage(150, offsets)).isEqualTo(2);
		assertThat(DocumentSearchMapper.resolvePage(200, offsets)).isEqualTo(3);
		assertThat(DocumentSearchMapper.resolvePage(999, offsets)).isEqualTo(3);
	}
}
