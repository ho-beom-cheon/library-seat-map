package com.libraryseatmap.api.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

@Service
public class TtlResponseCache {

	private final ConcurrentMap<String, CacheEntry> entries = new ConcurrentHashMap<>();
	private final Clock clock;

	public TtlResponseCache() {
		this(Clock.systemUTC());
	}

	TtlResponseCache(Clock clock) {
		this.clock = clock;
	}

	public <T> T get(String key, Duration ttl, Supplier<T> loader) {
		Instant now = clock.instant();
		CacheEntry current = entries.get(key);
		if (current != null && current.expiresAt().isAfter(now)) {
			return current.typedValue();
		}

		try {
			T value = loader.get();
			entries.put(key, new CacheEntry(value, now.plus(ttl)));
			return value;
		}
		catch (RuntimeException ex) {
			if (current != null) {
				return current.typedValue();
			}
			throw ex;
		}
	}

	public void clear() {
		entries.clear();
	}

	private record CacheEntry(Object value, Instant expiresAt) {
		@SuppressWarnings("unchecked")
		private <T> T typedValue() {
			return (T) value;
		}
	}
}
