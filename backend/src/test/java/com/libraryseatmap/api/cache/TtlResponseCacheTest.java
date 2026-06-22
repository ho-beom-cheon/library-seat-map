package com.libraryseatmap.api.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class TtlResponseCacheTest {

	@Test
	void returnsFreshCachedValueWithoutCallingLoaderAgain() {
		MutableClock clock = new MutableClock(Instant.parse("2026-06-22T00:00:00Z"));
		TtlResponseCache cache = new TtlResponseCache(clock);
		AtomicInteger calls = new AtomicInteger();

		String first = cache.get("key", Duration.ofMinutes(1), () -> "value-" + calls.incrementAndGet());
		String second = cache.get("key", Duration.ofMinutes(1), () -> "value-" + calls.incrementAndGet());

		assertThat(first).isEqualTo("value-1");
		assertThat(second).isEqualTo("value-1");
		assertThat(calls).hasValue(1);
	}

	@Test
	void returnsExpiredCachedValueWhenLoaderFails() {
		MutableClock clock = new MutableClock(Instant.parse("2026-06-22T00:00:00Z"));
		TtlResponseCache cache = new TtlResponseCache(clock);
		cache.get("key", Duration.ofSeconds(1), () -> "last-good");
		clock.advance(Duration.ofSeconds(2));

		String value = cache.get("key", Duration.ofSeconds(1), () -> {
			throw new IllegalStateException("database unavailable");
		});

		assertThat(value).isEqualTo("last-good");
	}

	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		private void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("UTC");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
