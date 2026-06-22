package com.libraryseatmap.api.cache;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ApiCacheKeys {

	private ApiCacheKeys() {
	}

	public static String district(String district, boolean includeNoSeat, boolean onlyWithSeats, int page, int size) {
		return "district:%s:%s:%s:%d:%d".formatted(
				normalize(district),
				includeNoSeat,
				onlyWithSeats,
				page,
				size
		);
	}

	public static String nearby(Double lat, Double lng, int radiusMeters, String sort, boolean includeNoSeat,
			int limit) {
		return "nearby:%s:%s:%d:%s:%s:%d".formatted(
				roundedCoordinate(lat),
				roundedCoordinate(lng),
				radiusMeters,
				normalize(sort),
				includeNoSeat,
				limit
		);
	}

	public static String libraryDetail(String libraryId) {
		return "library:%s:detail".formatted(normalize(libraryId));
	}

	public static String libraryRooms(String libraryId) {
		return "library:%s:rooms".formatted(normalize(libraryId));
	}

	public static String syncStatus() {
		return "sync:status";
	}

	static String roundedCoordinate(Double value) {
		if (value == null) {
			return "null";
		}
		return BigDecimal.valueOf(value)
				.setScale(3, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return "*";
		}
		return value.trim().toLowerCase();
	}
}
