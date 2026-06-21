package com.libraryseatmap.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class RecommendationDto {

	private RecommendationDto() {
	}

	public record RecommendationLibrariesResponse(List<RecommendedLibraryResponse> items) {
	}

	public record RecommendedLibraryResponse(
			String libraryId,
			String name,
			String district,
			String address,
			BigDecimal lat,
			BigDecimal lng,
			Long distanceMeters,
			Integer estimatedWalkMinutes,
			String markerState,
			Integer availableSeats,
			Integer totalSeats,
			BigDecimal usageRate,
			double recommendScore,
			Instant lastSyncedAt,
			String dataFreshness,
			String operationStatus,
			String operationTimeConfidence,
			String recommendReason
	) {
	}
}
