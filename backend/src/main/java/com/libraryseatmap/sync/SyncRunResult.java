package com.libraryseatmap.sync;

public record SyncRunResult(
		String endpoint,
		SyncTargetDistrict target,
		String status,
		int processedRows,
		int skippedRows
) {
}
