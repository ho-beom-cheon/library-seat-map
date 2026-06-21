package com.libraryseatmap.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class LibraryQueryDto {

	private LibraryQueryDto() {
	}

	public record LibraryListResponse(
			List<LibrarySummaryResponse> items,
			int page,
			int size,
			long total
	) {
	}

	public record NearbyLibrariesResponse(List<NearbyLibraryResponse> items) {
	}

	public record ReadingRoomListResponse(List<ReadingRoomSeatResponse> items) {
	}

	public record LibrarySummaryResponse(
			String libraryId,
			String name,
			String district,
			String address,
			BigDecimal lat,
			BigDecimal lng,
			String markerState,
			Integer availableSeats,
			Integer totalSeats,
			BigDecimal usageRate,
			Instant lastSyncedAt,
			String dataFreshness
	) {
	}

	public record NearbyLibraryResponse(
			String libraryId,
			String name,
			String district,
			String address,
			BigDecimal lat,
			BigDecimal lng,
			long distanceMeters,
			int estimatedWalkMinutes,
			String markerState,
			Integer availableSeats,
			Integer totalSeats,
			BigDecimal usageRate,
			double recommendScore,
			Instant lastSyncedAt,
			String dataFreshness,
			String recommendReason
	) {
	}

	public record LibraryDetailResponse(
			String libraryId,
			String name,
			String district,
			String address,
			String phone,
			String homepageUrl,
			BigDecimal lat,
			BigDecimal lng,
			String weekdayOpenTime,
			String weekdayCloseTime,
			String weekendOpenTime,
			String weekendCloseTime,
			String closedInfo,
			String operationStatus,
			String markerState,
			Integer availableSeats,
			Integer totalSeats,
			BigDecimal usageRate,
			String dataFreshness,
			Instant lastSyncedAt,
			List<ReadingRoomSeatResponse> rooms
	) {
	}

	public record ReadingRoomSeatResponse(
			String roomId,
			String roomName,
			String roomType,
			String floorInfo,
			Integer totalSeats,
			Integer usedSeats,
			Integer reservedSeats,
			Integer availableSeats,
			BigDecimal usageRate,
			String markerState,
			String dataFreshness,
			Instant observedAt,
			Instant lastSyncedAt
	) {
	}
}
