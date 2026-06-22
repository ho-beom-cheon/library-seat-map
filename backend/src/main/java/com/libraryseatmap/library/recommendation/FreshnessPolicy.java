package com.libraryseatmap.library.recommendation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
public class FreshnessPolicy {

	private final Clock clock;

	public FreshnessPolicy() {
		this(Clock.systemUTC());
	}

	FreshnessPolicy(Clock clock) {
		this.clock = clock;
	}

	public FreshnessResult evaluate(Instant observedAt) {
		if (observedAt == null) {
			return new FreshnessResult(FreshnessState.NO_DATA, null, 0.0);
		}
		Duration age = Duration.between(observedAt, clock.instant());
		long ageMinutes = Math.max(0, age.toMinutes());
		if (age.isNegative() || age.compareTo(Duration.ofMinutes(3)) <= 0) {
			return new FreshnessResult(FreshnessState.FRESH, ageMinutes, 1.0);
		}
		if (age.compareTo(Duration.ofMinutes(10)) <= 0) {
			return new FreshnessResult(FreshnessState.USABLE, ageMinutes, 0.85);
		}
		if (age.compareTo(Duration.ofMinutes(30)) <= 0) {
			return new FreshnessResult(FreshnessState.STALE, ageMinutes, 0.5);
		}
		return new FreshnessResult(FreshnessState.EXPIRED, ageMinutes, 0.0);
	}

	public enum FreshnessState {
		FRESH(0),
		USABLE(1),
		STALE(2),
		EXPIRED(3),
		NO_DATA(4);

		private final int severity;

		FreshnessState(int severity) {
			this.severity = severity;
		}

		int severity() {
			return severity;
		}
	}

	public record FreshnessResult(FreshnessState state, Long ageMinutes, double multiplier) {
	}
}
