package com.libraryseatmap.library.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.libraryseatmap.library.recommendation.FreshnessPolicy.FreshnessState;

class FreshnessPolicyTest {

	private final Instant now = Instant.parse("2026-06-21T12:00:00Z");
	private final FreshnessPolicy freshnessPolicy = new FreshnessPolicy(Clock.fixed(now, ZoneOffset.UTC));

	@Test
	void fiveMinuteOldDataIsUsable() {
		FreshnessPolicy.FreshnessResult result = freshnessPolicy.evaluate(now.minusSeconds(5 * 60));

		assertThat(result.state()).isEqualTo(FreshnessState.USABLE);
		assertThat(result.multiplier()).isEqualTo(0.85);
	}

	@Test
	void thirtyOneMinuteOldDataIsExpired() {
		FreshnessPolicy.FreshnessResult result = freshnessPolicy.evaluate(now.minusSeconds(31 * 60));

		assertThat(result.state()).isEqualTo(FreshnessState.EXPIRED);
		assertThat(result.multiplier()).isZero();
	}
}
