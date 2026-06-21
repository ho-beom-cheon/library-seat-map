package com.libraryseatmap.sync;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LibrarySyncScheduler {

	private final LibrarySyncProperties properties;
	private final LibrarySyncService syncService;

	public LibrarySyncScheduler(LibrarySyncProperties properties, LibrarySyncService syncService) {
		this.properties = properties;
		this.syncService = syncService;
	}

	@Scheduled(cron = "${library.sync.info-cron:0 10 3 * * *}", zone = "Asia/Seoul")
	public void syncLibraryInfo() {
		if (!properties.isEnabled()) {
			return;
		}
		syncService.syncLibraryInfoForAllDistricts();
	}

	@Scheduled(fixedDelayString = "${library.sync.realtime-delay-ms:120000}")
	public void syncRealtimeRooms() {
		if (!properties.isEnabled()) {
			return;
		}
		syncService.syncRealtimeRoomsForAllDistricts();
	}
}
