package com.libraryseatmap.sync;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.libraryseatmap.api.dto.SyncStatusDto.RecentSyncLogResponse;
import com.libraryseatmap.api.dto.SyncStatusDto.SyncStatusResponse;
import com.libraryseatmap.library.domain.RoomSeatLatest;
import com.libraryseatmap.library.domain.SyncLog;
import com.libraryseatmap.library.repository.RoomSeatLatestRepository;
import com.libraryseatmap.library.repository.SyncLogRepository;

@Service
@Transactional(readOnly = true)
public class SyncStatusService {

	private static final List<String> SUCCESS_STATUSES = List.of("SUCCESS", "PARTIAL_SUCCESS");

	private final SyncLogRepository syncLogRepository;
	private final RoomSeatLatestRepository roomSeatLatestRepository;
	private final Clock clock;

	@Autowired
	public SyncStatusService(SyncLogRepository syncLogRepository, RoomSeatLatestRepository roomSeatLatestRepository) {
		this(syncLogRepository, roomSeatLatestRepository, Clock.systemUTC());
	}

	SyncStatusService(SyncLogRepository syncLogRepository, RoomSeatLatestRepository roomSeatLatestRepository,
			Clock clock) {
		this.syncLogRepository = syncLogRepository;
		this.roomSeatLatestRepository = roomSeatLatestRepository;
		this.clock = clock;
	}

	public SyncStatusResponse status() {
		Instant checkedAt = clock.instant();
		List<SyncLog> recentLogs = syncLogRepository.findTop10ByOrderByStartedAtDesc();
		Optional<SyncLog> lastSuccessfulSync = syncLogRepository.findFirstByStatusInOrderByFinishedAtDesc(
				SUCCESS_STATUSES);
		SyncLog lastLog = recentLogs.isEmpty() ? null : recentLogs.get(0);
		List<RoomSeatLatest> latestRows = roomSeatLatestRepository.findAll();
		FreshnessSummary freshness = freshnessSummary(latestRows, checkedAt);
		boolean lastSyncFailed = lastLog != null && lastLog.getStatus().startsWith("FAILED");

		return new SyncStatusResponse(
				overallStatus(freshness, lastSyncFailed),
				checkedAt,
				lastLog == null ? null : lastLog.getFinishedAt(),
				lastSuccessfulSync.map(SyncLog::getFinishedAt).orElse(null),
				lastLog == null ? "NO_SYNC_LOG" : lastLog.getStatus(),
				!latestRows.isEmpty(),
				freshness.dataFreshness(),
				latestRows.size(),
				freshness.staleRows(),
				freshness.expiredRows(),
				message(freshness, lastSyncFailed),
				recentLogs.stream().map(this::toRecentLog).toList()
		);
	}

	private FreshnessSummary freshnessSummary(List<RoomSeatLatest> latestRows, Instant checkedAt) {
		if (latestRows.isEmpty()) {
			return new FreshnessSummary("NO_DATA", 0, 0);
		}

		long staleRows = 0;
		long expiredRows = 0;
		String overall = "FRESH";
		for (RoomSeatLatest latest : latestRows) {
			String freshness = freshness(latest.getObservedAt(), checkedAt);
			if ("STALE".equals(freshness)) {
				staleRows++;
			}
			if ("EXPIRED".equals(freshness)) {
				expiredRows++;
			}
			overall = worseFreshness(overall, freshness);
		}
		return new FreshnessSummary(overall, staleRows, expiredRows);
	}

	private String freshness(Instant observedAt, Instant checkedAt) {
		if (observedAt == null) {
			return "NO_DATA";
		}
		Duration age = Duration.between(observedAt, checkedAt);
		if (age.isNegative() || age.compareTo(Duration.ofMinutes(3)) <= 0) {
			return "FRESH";
		}
		if (age.compareTo(Duration.ofMinutes(10)) <= 0) {
			return "USABLE";
		}
		if (age.compareTo(Duration.ofMinutes(30)) <= 0) {
			return "STALE";
		}
		return "EXPIRED";
	}

	private String worseFreshness(String left, String right) {
		return severity(right) > severity(left) ? right : left;
	}

	private int severity(String freshness) {
		return switch (freshness) {
			case "FRESH" -> 0;
			case "USABLE" -> 1;
			case "STALE" -> 2;
			case "EXPIRED" -> 3;
			default -> 4;
		};
	}

	private String overallStatus(FreshnessSummary freshness, boolean lastSyncFailed) {
		if ("NO_DATA".equals(freshness.dataFreshness())) {
			return "NO_DATA";
		}
		if (lastSyncFailed || "STALE".equals(freshness.dataFreshness())
				|| "EXPIRED".equals(freshness.dataFreshness())) {
			return "DEGRADED";
		}
		return "UP";
	}

	private String message(FreshnessSummary freshness, boolean lastSyncFailed) {
		if ("NO_DATA".equals(freshness.dataFreshness())) {
			return "No seat data has been collected yet.";
		}
		if (lastSyncFailed) {
			return "Seat data refresh is not healthy. Serving the latest stored data.";
		}
		if ("EXPIRED".equals(freshness.dataFreshness())) {
			return "Seat data is delayed for more than 30 minutes.";
		}
		if ("STALE".equals(freshness.dataFreshness())) {
			return "Some seat data refresh is delayed.";
		}
		return "Seat data refresh is healthy.";
	}

	private RecentSyncLogResponse toRecentLog(SyncLog log) {
		return new RecentSyncLogResponse(
				log.getEndpoint(),
				log.getDistrict(),
				log.getStatus(),
				log.getRowCount(),
				log.getStartedAt(),
				log.getFinishedAt(),
				log.getErrorMessage() != null && !log.getErrorMessage().isBlank()
		);
	}

	private record FreshnessSummary(String dataFreshness, long staleRows, long expiredRows) {
	}
}
