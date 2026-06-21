package com.libraryseatmap.publicapi.dto;

import java.time.Instant;
import java.util.Optional;

public record RealtimeReadingRoomItem(
		String sourceStdgCd,
		String libraryId,
		String libraryName,
		String roomExternalId,
		String roomNo,
		String roomName,
		String roomType,
		String floorInfo,
		Integer currentVisitorCount,
		Integer totalSeats,
		Integer usedSeats,
		Integer reservedSeats,
		Integer availableSeats,
		String rawObservedAt,
		Optional<Instant> observedAt
) {
}
