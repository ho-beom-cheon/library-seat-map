package com.libraryseatmap.api;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.libraryseatmap.api.cache.ApiCacheKeys;
import com.libraryseatmap.api.cache.TtlResponseCache;
import com.libraryseatmap.api.dto.SyncStatusDto.ManualSyncRunItemResponse;
import com.libraryseatmap.api.dto.SyncStatusDto.ManualSyncRunResponse;
import com.libraryseatmap.api.dto.SyncStatusDto.SyncStatusResponse;
import com.libraryseatmap.sync.LibrarySyncProperties;
import com.libraryseatmap.sync.LibrarySyncService;
import com.libraryseatmap.sync.SyncRunResult;
import com.libraryseatmap.sync.SyncStatusService;

@RestController
@RequestMapping("/api/sync")
public class SyncStatusController {

	private static final Duration SYNC_STATUS_TTL = Duration.ofSeconds(30);

	private final LibrarySyncProperties syncProperties;
	private final LibrarySyncService librarySyncService;
	private final SyncStatusService syncStatusService;
	private final TtlResponseCache responseCache;

	public SyncStatusController(LibrarySyncProperties syncProperties, LibrarySyncService librarySyncService,
			SyncStatusService syncStatusService, TtlResponseCache responseCache) {
		this.syncProperties = syncProperties;
		this.librarySyncService = librarySyncService;
		this.syncStatusService = syncStatusService;
		this.responseCache = responseCache;
	}

	@GetMapping("/status")
	public SyncStatusResponse status() {
		return responseCache.get(ApiCacheKeys.syncStatus(), SYNC_STATUS_TTL, syncStatusService::status);
	}

	@PostMapping("/run")
	public ManualSyncRunResponse run() {
		if (!syncProperties.isManualTriggerEnabled()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manual sync trigger is disabled.");
		}

		Instant startedAt = Instant.now();
		List<SyncRunResult> runs = runSync();
		Instant finishedAt = Instant.now();
		responseCache.clear();

		int processedRows = runs.stream().mapToInt(SyncRunResult::processedRows).sum();
		int skippedRows = runs.stream().mapToInt(SyncRunResult::skippedRows).sum();
		List<ManualSyncRunItemResponse> items = runs.stream()
				.map(this::toResponse)
				.toList();
		return new ManualSyncRunResponse(overallStatus(runs), startedAt, finishedAt, processedRows, skippedRows, items);
	}

	private List<SyncRunResult> runSync() {
		List<SyncRunResult> infoRuns = librarySyncService.syncLibraryInfoForAllDistricts();
		List<SyncRunResult> realtimeRuns = librarySyncService.syncRealtimeRoomsForAllDistricts();
		return java.util.stream.Stream.concat(infoRuns.stream(), realtimeRuns.stream()).toList();
	}

	private ManualSyncRunItemResponse toResponse(SyncRunResult result) {
		return new ManualSyncRunItemResponse(
				result.endpoint(),
				result.target().name(),
				result.target().stdgCd(),
				result.status(),
				result.processedRows(),
				result.skippedRows());
	}

	private String overallStatus(List<SyncRunResult> runs) {
		if (runs.isEmpty()) {
			return "NO_TARGETS";
		}
		if (runs.stream().anyMatch((run) -> run.status().startsWith("FAILED"))) {
			return "FAILED";
		}
		if (runs.stream().anyMatch((run) -> run.status().contains("PARTIAL"))) {
			return "PARTIAL_SUCCESS";
		}
		return "SUCCESS";
	}
}
