package com.libraryseatmap.publicapi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class PublicLibraryDateTimeParser {

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

	private static final List<DateTimeFormatter> LOCAL_DATE_TIME_FORMATTERS = List.of(
			DateTimeFormatter.ISO_LOCAL_DATE_TIME,
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
			DateTimeFormatter.ofPattern("yyyyMMddHHmm")
	);

	public Optional<Instant> parse(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}

		String normalized = value.trim();

		try {
			return Optional.of(Instant.parse(normalized));
		}
		catch (DateTimeParseException ignored) {
		}

		try {
			return Optional.of(OffsetDateTime.parse(normalized).toInstant());
		}
		catch (DateTimeParseException ignored) {
		}

		for (DateTimeFormatter formatter : LOCAL_DATE_TIME_FORMATTERS) {
			try {
				LocalDateTime parsed = LocalDateTime.parse(normalized, formatter);
				return Optional.of(parsed.atZone(DEFAULT_ZONE).toInstant());
			}
			catch (DateTimeParseException ignored) {
			}
		}

		try {
			LocalDate parsed = LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE);
			return Optional.of(parsed.atStartOfDay(DEFAULT_ZONE).toInstant());
		}
		catch (DateTimeParseException ignored) {
			return Optional.empty();
		}
	}
}
