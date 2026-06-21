package com.libraryseatmap.publicapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PublicLibraryDateTimeParserTest {

	private final PublicLibraryDateTimeParser parser = new PublicLibraryDateTimeParser();

	@Test
	void parsesOffsetDateTime() {
		assertThat(parser.parse("2026-06-21T14:32:00+09:00"))
				.contains(Instant.parse("2026-06-21T05:32:00Z"));
	}

	@Test
	void parsesLocalDateTimeAsKst() {
		assertThat(parser.parse("2026-06-21 14:32:00"))
				.contains(Instant.parse("2026-06-21T05:32:00Z"));
	}

	@Test
	void parsesCompactDateTimeAsKst() {
		assertThat(parser.parse("20260621143200"))
				.contains(Instant.parse("2026-06-21T05:32:00Z"));
	}

	@Test
	void returnsEmptyForBlankOrUnknownValue() {
		assertThat(parser.parse("")).isEmpty();
		assertThat(parser.parse("not-a-date")).isEmpty();
	}
}
