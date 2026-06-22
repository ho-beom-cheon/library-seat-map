package com.libraryseatmap.api.dto;

import java.time.Instant;
import java.util.List;

public final class SyncStatusDto {

	private SyncStatusDto() {
	}

	public record SyncStatusResponse(
			String status,
			Instant checkedAt,
			Instant lastFinishedAt,
			Instant lastSuccessfulSyncAt,
			String lastSyncStatus,
			boolean servingLatestData,
			String dataFreshness,
			long latestRowCount,
			long staleRowCount,
			long expiredRowCount,
			String message,
			List<RecentSyncLogResponse> recentLogs
	) {
	}

	public record RecentSyncLogResponse(
			String endpoint,
			String district,
			String status,
			Integer rowCount,
			Instant startedAt,
			Instant finishedAt,
			boolean hasError
	) {
	}

	public record ManualSyncRunResponse(
			String status,
			Instant startedAt,
			Instant finishedAt,
			int processedRows,
			int skippedRows,
			List<ManualSyncRunItemResponse> runs
	) {
	}

	public record ManualSyncRunItemResponse(
			String endpoint,
			String district,
			String stdgCd,
			String status,
			int processedRows,
			int skippedRows
	) {
	}
}
