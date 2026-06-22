package com.libraryseatmap.api;

import java.time.Duration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.libraryseatmap.api.cache.ApiCacheKeys;
import com.libraryseatmap.api.cache.TtlResponseCache;
import com.libraryseatmap.api.dto.SyncStatusDto.SyncStatusResponse;
import com.libraryseatmap.sync.SyncStatusService;

@RestController
@RequestMapping("/api/sync")
public class SyncStatusController {

	private static final Duration SYNC_STATUS_TTL = Duration.ofSeconds(30);

	private final SyncStatusService syncStatusService;
	private final TtlResponseCache responseCache;

	public SyncStatusController(SyncStatusService syncStatusService, TtlResponseCache responseCache) {
		this.syncStatusService = syncStatusService;
		this.responseCache = responseCache;
	}

	@GetMapping("/status")
	public SyncStatusResponse status() {
		return responseCache.get(ApiCacheKeys.syncStatus(), SYNC_STATUS_TTL, syncStatusService::status);
	}
}
