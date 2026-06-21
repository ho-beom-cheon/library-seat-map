package com.libraryseatmap.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.libraryseatmap.library.domain.Library;
import com.libraryseatmap.library.domain.ReadingRoom;
import com.libraryseatmap.library.domain.RoomSeatLatest;
import com.libraryseatmap.library.domain.SeatSnapshot;
import com.libraryseatmap.library.domain.SyncLog;
import com.libraryseatmap.library.repository.LibraryRepository;
import com.libraryseatmap.library.repository.ReadingRoomRepository;
import com.libraryseatmap.library.repository.RoomSeatLatestRepository;
import com.libraryseatmap.library.repository.SeatSnapshotRepository;
import com.libraryseatmap.library.repository.SyncLogRepository;
import com.libraryseatmap.publicapi.PublicLibraryApiClient;
import com.libraryseatmap.publicapi.dto.LibraryInfoItem;
import com.libraryseatmap.publicapi.dto.PublicLibraryApiHeader;
import com.libraryseatmap.publicapi.dto.PublicLibraryApiPage;
import com.libraryseatmap.publicapi.dto.RealtimeReadingRoomItem;

@SpringBootTest(properties = "library.sync.districts=Gangnam:1168000000")
@Transactional
class LibrarySyncServiceTest {

	private static final SyncTargetDistrict GANGNAM = new SyncTargetDistrict("Gangnam", "1168000000");
	private static final PublicLibraryApiHeader SUCCESS_HEADER = new PublicLibraryApiHeader("00", "OK");

	@Autowired
	private LibrarySyncService syncService;

	@Autowired
	private LibraryRepository libraryRepository;

	@Autowired
	private ReadingRoomRepository readingRoomRepository;

	@Autowired
	private SeatSnapshotRepository seatSnapshotRepository;

	@Autowired
	private RoomSeatLatestRepository roomSeatLatestRepository;

	@Autowired
	private SyncLogRepository syncLogRepository;

	@MockitoBean
	private PublicLibraryApiClient apiClient;

	@Test
	void syncLibraryInfoUpsertsLibraryAndWritesLog() {
		when(apiClient.fetchLibraries("1168000000"))
				.thenReturn(new PublicLibraryApiPage<>(SUCCESS_HEADER, List.of(libraryInfo()), 1));

		SyncRunResult result = syncService.syncLibraryInfo(GANGNAM);

		assertThat(result.status()).isEqualTo("SUCCESS");
		Library library = libraryRepository.findById("LIB-1").orElseThrow();
		assertThat(library.getName()).isEqualTo("Test Library");
		assertThat(library.getDistrict()).isEqualTo("Gangnam-gu");
		assertThat(library.getSourceStdgCd()).isEqualTo("1168000000");
		assertThat(library.getLastInfoSyncedAt()).isNotNull();

		SyncLog log = syncLogRepository.findTop10ByOrderByStartedAtDesc().get(0);
		assertThat(log.getEndpoint()).isEqualTo("/info_v2");
		assertThat(log.getStdgCd()).isEqualTo("1168000000");
		assertThat(log.getDistrict()).isEqualTo("Gangnam");
		assertThat(log.getStatus()).isEqualTo("SUCCESS");
		assertThat(log.getRowCount()).isEqualTo(1);
	}

	@Test
	void syncRealtimeRoomsUpsertsRoomSnapshotLatestAndAvoidsDuplicateSnapshots() {
		libraryRepository.save(new Library("LIB-1", "Test Library"));
		Instant observedAt = Instant.now().minusSeconds(30).truncatedTo(ChronoUnit.SECONDS);
		when(apiClient.fetchRealtimeRooms("1168000000"))
				.thenReturn(new PublicLibraryApiPage<>(SUCCESS_HEADER, List.of(realtimeRoom(observedAt)), 1));

		syncService.syncRealtimeRooms(GANGNAM);
		syncService.syncRealtimeRooms(GANGNAM);

		ReadingRoom room = readingRoomRepository.findByLibraryIdAndRoomExternalId("LIB-1", "ROOM-1").orElseThrow();
		assertThat(room.getRoomName()).isEqualTo("Main Room");
		assertThat(room.getTotalSeats()).isEqualTo(100);

		List<SeatSnapshot> snapshots = seatSnapshotRepository.findByRoomIdOrderByObservedAtDesc(room.getId());
		assertThat(snapshots).hasSize(1);
		assertThat(snapshots.get(0).getUsedSeats()).isEqualTo(40);
		assertThat(snapshots.get(0).getReservedSeats()).isEqualTo(5);

		RoomSeatLatest latest = roomSeatLatestRepository.findById(room.getId()).orElseThrow();
		assertThat(latest.getAvailableSeats()).isEqualTo(55);
		assertThat(latest.getUsageRate()).isEqualByComparingTo("0.4000");
		assertThat(latest.getFreshnessStatus()).isEqualTo("FRESH");

		List<SyncLog> logs = syncLogRepository.findTop10ByOrderByStartedAtDesc();
		assertThat(logs).filteredOn(log -> "/rlt_rdrm_info_v2".equals(log.getEndpoint())).hasSize(2);
	}

	private LibraryInfoItem libraryInfo() {
		return new LibraryInfoItem(
				"LIB-1",
				"Test Library",
				"1168000000",
				"Seoul",
				"Gangnam-gu",
				"Public",
				"123 Test-ro",
				"Gangnam Office",
				"02-1234-5678",
				"https://example.test/library",
				null,
				null,
				"Monday",
				"09:00",
				"18:00",
				"09:00",
				"17:00",
				null,
				null,
				100,
				"20260621"
		);
	}

	private RealtimeReadingRoomItem realtimeRoom(Instant observedAt) {
		return new RealtimeReadingRoomItem(
				"1168000000",
				"LIB-1",
				"Test Library",
				"ROOM-1",
				"1",
				"Main Room",
				"General",
				"2F",
				42,
				100,
				40,
				5,
				55,
				"20260621230000",
				Optional.of(observedAt)
		);
	}
}
