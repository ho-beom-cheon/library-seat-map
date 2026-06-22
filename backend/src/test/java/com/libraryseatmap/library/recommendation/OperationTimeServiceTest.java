package com.libraryseatmap.library.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.libraryseatmap.library.domain.Library;
import com.libraryseatmap.library.recommendation.OperationTimeService.OperationStatus;
import com.libraryseatmap.library.recommendation.OperationTimeService.OperationTimeConfidence;
import com.libraryseatmap.publicapi.dto.LibraryInfoItem;

class OperationTimeServiceTest {

	private static final Instant THURSDAY_NOON_IN_SEOUL = Instant.parse("2026-06-25T03:00:00Z");

	private final OperationTimeService operationTimeService = new OperationTimeService(
			Clock.fixed(THURSDAY_NOON_IN_SEOUL, ZoneOffset.UTC));

	@Test
	void openLibraryReturnsRemainingTimeScore() {
		OperationTimeService.OperationTimeResult result = operationTimeService.evaluate(
				library("09:00", "18:00", null), 10, 60);

		assertThat(result.status()).isEqualTo(OperationStatus.OPEN);
		assertThat(result.confidence()).isEqualTo(OperationTimeConfidence.HIGH);
		assertThat(result.excluded()).isFalse();
		assertThat(result.timeScore()).isEqualTo(15);
	}

	@Test
	void parseFailureKeepsCandidateWithLowConfidence() {
		OperationTimeService.OperationTimeResult result = operationTimeService.evaluate(
				library("unknown", "18:00", null), 10, 60);

		assertThat(result.status()).isEqualTo(OperationStatus.UNKNOWN);
		assertThat(result.confidence()).isEqualTo(OperationTimeConfidence.LOW);
		assertThat(result.excluded()).isFalse();
	}

	@Test
	void closedInfoForTodayExcludesCandidate() {
		OperationTimeService.OperationTimeResult result = operationTimeService.evaluate(
				library("09:00", "18:00", "Thursday"), 10, 60);

		assertThat(result.status()).isEqualTo(OperationStatus.CLOSED);
		assertThat(result.excluded()).isTrue();
	}

	private Library library(String openTime, String closeTime, String closedInfo) {
		Library library = new Library("LIB-OP", "Operation Library");
		library.applyLibraryInfo(new LibraryInfoItem(
				"LIB-OP",
				"Operation Library",
				"1168000000",
				"Seoul",
				"Gangnam-gu",
				"Public",
				"123 Test-ro",
				"Gangnam Office",
				"02-1234-5678",
				"https://example.test/library/LIB-OP",
				new BigDecimal("37.5000000"),
				new BigDecimal("127.0000000"),
				closedInfo,
				openTime,
				closeTime,
				openTime,
				closeTime,
				null,
				null,
				100,
				"20260621"
		), THURSDAY_NOON_IN_SEOUL);
		return library;
	}
}
