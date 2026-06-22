package com.libraryseatmap.api;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.libraryseatmap.library.domain.Library;
import com.libraryseatmap.library.domain.ReadingRoom;
import com.libraryseatmap.library.domain.RoomSeatLatest;
import com.libraryseatmap.library.repository.LibraryRepository;
import com.libraryseatmap.library.repository.ReadingRoomRepository;
import com.libraryseatmap.library.repository.RoomSeatLatestRepository;
import com.libraryseatmap.publicapi.dto.LibraryInfoItem;
import com.libraryseatmap.publicapi.dto.RealtimeReadingRoomItem;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecommendationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LibraryRepository libraryRepository;

	@Autowired
	private ReadingRoomRepository readingRoomRepository;

	@Autowired
	private RoomSeatLatestRepository roomSeatLatestRepository;

	private Instant now;

	@BeforeEach
	void setUp() {
		now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		saveLibraryWithLatest("LIB-RISK", "Near Risk Library", "Gangnam-gu", "37.5001000", "127.0001000",
				100, 99, 1, now.minusSeconds(60));
		saveLibraryWithLatest("LIB-AMPLE", "Ample Seat Library", "Gangnam-gu", "37.5100000", "127.0100000",
				120, 62, 58, now.minusSeconds(60));
		saveLibraryWithLatest("LIB-FULL", "Full Library", "Gangnam-gu", "37.5020000", "127.0020000",
				100, 100, 0, now.minusSeconds(60));
		saveLibraryWithLatest("LIB-STALE", "Stale Library", "Gangnam-gu", "37.5030000", "127.0030000",
				100, 40, 60, now.minus(31, ChronoUnit.MINUTES));
		libraryRepository.save(library("LIB-NO-SEAT", "No Seat Library", "Gangnam-gu",
				"37.5040000", "127.0040000"));
	}

	@Test
	void recommendationsPreferAvailableSeatsOverNearestFullRisk() throws Exception {
		mockMvc.perform(get("/api/recommendations/libraries")
						.param("lat", "37.5000000")
						.param("lng", "127.0000000")
						.param("radiusMeters", "5000")
						.param("minimumStudyMinutes", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].libraryId").value("LIB-AMPLE"))
				.andExpect(jsonPath("$.items[0].recommendScore").value(greaterThan(0.0)))
				.andExpect(jsonPath("$.items[0].distanceMeters").value(greaterThan(0)))
				.andExpect(jsonPath("$.items[0].operationStatus").value("OPEN"))
				.andExpect(jsonPath("$.items[1].libraryId").value("LIB-RISK"));
	}

	@Test
	void districtRecommendationWorksWithoutLocation() throws Exception {
		mockMvc.perform(get("/api/recommendations/libraries")
						.param("district", "Gangnam-gu")
						.param("minimumStudyMinutes", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].libraryId").value("LIB-AMPLE"))
				.andExpect(jsonPath("$.items[0].recommendReason").value("좌석 여유와 이용률을 기준으로 추천합니다."));
	}

	@Test
	void locationOrDistrictIsRequired() throws Exception {
		mockMvc.perform(get("/api/recommendations/libraries"))
				.andExpect(status().isBadRequest());
	}

	private void saveLibraryWithLatest(String libraryId, String name, String district, String lat, String lng,
			int totalSeats, int usedSeats, int availableSeats, Instant observedAt) {
		libraryRepository.save(library(libraryId, name, district, lat, lng));
		ReadingRoom room = new ReadingRoom(libraryId, "ROOM-" + libraryId, "Main Room");
		room.applyRealtimeRoom(realtimeRoom(libraryId, name, observedAt), totalSeats, now);
		ReadingRoom savedRoom = readingRoomRepository.save(room);
		BigDecimal usageRate = BigDecimal.valueOf(usedSeats)
				.divide(BigDecimal.valueOf(totalSeats), 4, RoundingMode.HALF_UP);
		RoomSeatLatest latest = new RoomSeatLatest(savedRoom.getId(), libraryId, observedAt, now, totalSeats,
				usedSeats, availableSeats, usageRate, "FRESH");
		latest.updateSeatState(observedAt, now, usedSeats, totalSeats, usedSeats, 0, availableSeats,
				usageRate, "FRESH");
		roomSeatLatestRepository.save(latest);
	}

	private Library library(String libraryId, String name, String district, String lat, String lng) {
		Library library = new Library(libraryId, name);
		library.applyLibraryInfo(new LibraryInfoItem(
				libraryId,
				name,
				"1168000000",
				"Seoul",
				district,
				"Public",
				"123 Test-ro",
				"Gangnam Office",
				"02-1234-5678",
				"https://example.test/library/" + libraryId,
				new BigDecimal(lat),
				new BigDecimal(lng),
				null,
				"00:00",
				"00:00",
				"00:00",
				"00:00",
				null,
				null,
				100,
				"20260621"
		), now);
		return library;
	}

	private RealtimeReadingRoomItem realtimeRoom(String libraryId, String libraryName, Instant observedAt) {
		return new RealtimeReadingRoomItem(
				"1168000000",
				libraryId,
				libraryName,
				"ROOM-" + libraryId,
				"1",
				"Main Room",
				"General",
				"2F",
				42,
				100,
				40,
				0,
				60,
				"20260621230000",
				Optional.of(observedAt)
		);
	}
}
