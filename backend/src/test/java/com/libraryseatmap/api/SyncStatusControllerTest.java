package com.libraryseatmap.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.libraryseatmap.api.cache.TtlResponseCache;
import com.libraryseatmap.library.domain.Library;
import com.libraryseatmap.library.domain.ReadingRoom;
import com.libraryseatmap.library.domain.RoomSeatLatest;
import com.libraryseatmap.library.domain.SyncLog;
import com.libraryseatmap.library.repository.LibraryRepository;
import com.libraryseatmap.library.repository.ReadingRoomRepository;
import com.libraryseatmap.library.repository.RoomSeatLatestRepository;
import com.libraryseatmap.library.repository.SyncLogRepository;
import com.libraryseatmap.publicapi.dto.LibraryInfoItem;
import com.libraryseatmap.publicapi.dto.RealtimeReadingRoomItem;
import com.libraryseatmap.sync.LibrarySyncProperties;
import com.libraryseatmap.sync.LibrarySyncService;
import com.libraryseatmap.sync.SyncRunResult;
import com.libraryseatmap.sync.SyncTargetDistrict;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SyncStatusControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TtlResponseCache responseCache;

	@Autowired
	private LibrarySyncProperties syncProperties;

	@MockitoBean
	private LibrarySyncService librarySyncService;

	@Autowired
	private LibraryRepository libraryRepository;

	@Autowired
	private ReadingRoomRepository readingRoomRepository;

	@Autowired
	private RoomSeatLatestRepository roomSeatLatestRepository;

	@Autowired
	private SyncLogRepository syncLogRepository;

	@BeforeEach
	void clearCache() {
		responseCache.clear();
		syncProperties.setManualTriggerEnabled(false);
		reset(librarySyncService);
	}

	@Test
	void statusReturnsNoDataWhenLatestRowsAreEmpty() throws Exception {
		mockMvc.perform(get("/api/sync/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("NO_DATA"))
				.andExpect(jsonPath("$.servingLatestData").value(false))
				.andExpect(jsonPath("$.dataFreshness").value("NO_DATA"))
				.andExpect(jsonPath("$.latestRowCount").value(0));
	}

	@Test
	void statusReportsServingLatestDataWhenLastSyncFailed() throws Exception {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		saveLibraryWithExpiredLatest(now);
		syncLogRepository.save(SyncLog.finished("/rlt_rdrm_info_v2", "1168000000", "Gangnam-gu", "SUCCESS", null,
				"00", "ok", 1, now.minus(40, ChronoUnit.MINUTES), now.minus(39, ChronoUnit.MINUTES), null));
		syncLogRepository.save(SyncLog.finished("/rlt_rdrm_info_v2", "1168000000", "Gangnam-gu", "FAILED_PROVIDER",
				null, "99", "provider error", 0, now.minus(1, ChronoUnit.MINUTES), now, "provider unavailable"));

		mockMvc.perform(get("/api/sync/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DEGRADED"))
				.andExpect(jsonPath("$.lastSyncStatus").value("FAILED_PROVIDER"))
				.andExpect(jsonPath("$.servingLatestData").value(true))
				.andExpect(jsonPath("$.dataFreshness").value("EXPIRED"))
				.andExpect(jsonPath("$.expiredRowCount").value(1))
				.andExpect(jsonPath("$.recentLogs[0].hasError").value(true));
	}

	@Test
	void manualRunReturnsForbiddenWhenTriggerIsDisabled() throws Exception {
		mockMvc.perform(post("/api/sync/run"))
				.andExpect(status().isForbidden());

		verifyNoInteractions(librarySyncService);
	}

	@Test
	void manualRunExecutesInfoThenRealtimeSyncWhenTriggerIsEnabled() throws Exception {
		syncProperties.setManualTriggerEnabled(true);
		SyncTargetDistrict target = new SyncTargetDistrict("Gangnam-gu", "1168000000");
		when(librarySyncService.syncLibraryInfoForAllDistricts())
				.thenReturn(List.of(new SyncRunResult("/info_v2", target, "SUCCESS", 28, 0)));
		when(librarySyncService.syncRealtimeRoomsForAllDistricts())
				.thenReturn(List.of(new SyncRunResult("/rlt_rdrm_info_v2", target, "PARTIAL_SUCCESS", 33, 2)));

		mockMvc.perform(post("/api/sync/run"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"))
				.andExpect(jsonPath("$.processedRows").value(61))
				.andExpect(jsonPath("$.skippedRows").value(2))
				.andExpect(jsonPath("$.runs[0].endpoint").value("/info_v2"))
				.andExpect(jsonPath("$.runs[0].district").value("Gangnam-gu"))
				.andExpect(jsonPath("$.runs[0].stdgCd").value("1168000000"))
				.andExpect(jsonPath("$.runs[0].processedRows").value(28))
				.andExpect(jsonPath("$.runs[1].endpoint").value("/rlt_rdrm_info_v2"))
				.andExpect(jsonPath("$.runs[1].status").value("PARTIAL_SUCCESS"));
	}

	private void saveLibraryWithExpiredLatest(Instant now) {
		Library library = new Library("LIB-SYNC", "Sync Library");
		library.applyLibraryInfo(new LibraryInfoItem(
				"LIB-SYNC",
				"Sync Library",
				"1168000000",
				"Seoul",
				"Gangnam-gu",
				"Public",
				"123 Test-ro",
				"Gangnam Office",
				"02-1234-5678",
				"https://example.test/library/LIB-SYNC",
				new BigDecimal("37.5010000"),
				new BigDecimal("127.0010000"),
				"Monday",
				"09:00",
				"18:00",
				"09:00",
				"17:00",
				null,
				null,
				100,
				"20260621"
		), now);
		libraryRepository.save(library);

		Instant observedAt = now.minus(31, ChronoUnit.MINUTES);
		ReadingRoom room = new ReadingRoom("LIB-SYNC", "ROOM-SYNC", "Main Room");
		room.applyRealtimeRoom(realtimeRoom(observedAt), 100, now);
		ReadingRoom savedRoom = readingRoomRepository.save(room);
		RoomSeatLatest latest = new RoomSeatLatest(savedRoom.getId(), "LIB-SYNC", observedAt, now, 100,
				40, 60, new BigDecimal("0.4000"), "EXPIRED");
		latest.updateSeatState(observedAt, now, 40, 100, 40, 0, 60, new BigDecimal("0.4000"), "EXPIRED");
		roomSeatLatestRepository.save(latest);
	}

	private RealtimeReadingRoomItem realtimeRoom(Instant observedAt) {
		return new RealtimeReadingRoomItem(
				"1168000000",
				"LIB-SYNC",
				"Sync Library",
				"ROOM-SYNC",
				"1",
				"Main Room",
				"General",
				"2F",
				40,
				100,
				40,
				0,
				60,
				"20260621230000",
				Optional.of(observedAt)
		);
	}
}
