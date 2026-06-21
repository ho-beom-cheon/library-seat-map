package com.libraryseatmap.api;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
class LibraryQueryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LibraryRepository libraryRepository;

	@Autowired
	private ReadingRoomRepository readingRoomRepository;

	@Autowired
	private RoomSeatLatestRepository roomSeatLatestRepository;

	@BeforeEach
	void setUp() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Library library = library("LIB-1", "Seat Library", "Gangnam-gu", "37.5010000", "127.0010000", now);
		libraryRepository.save(library);

		ReadingRoom room = new ReadingRoom("LIB-1", "ROOM-1", "Main Room");
		room.applyRealtimeRoom(realtimeRoom(now), 100, now);
		ReadingRoom savedRoom = readingRoomRepository.save(room);
		RoomSeatLatest latest = new RoomSeatLatest(savedRoom.getId(), "LIB-1", now.minusSeconds(60), now, 100,
				40, 60, new BigDecimal("0.4000"), "FRESH");
		latest.updateSeatState(now.minusSeconds(60), now, 42, 100, 40, 0, 60,
				new BigDecimal("0.4000"), "FRESH");
		roomSeatLatestRepository.save(latest);

		libraryRepository.save(library("LIB-2", "No Seat Library", "Gangnam-gu", "37.8000000", "127.3000000", now));
	}

	@Test
	void librariesFiltersOnlyWithSeatData() throws Exception {
		mockMvc.perform(get("/api/libraries")
						.param("district", "Gangnam-gu")
						.param("onlyWithSeats", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.items[0].libraryId").value("LIB-1"))
				.andExpect(jsonPath("$.items[0].markerState").value("AVAILABLE"))
				.andExpect(jsonPath("$.items[0].availableSeats").value(60))
				.andExpect(jsonPath("$.items[0].dataFreshness").value("FRESH"));
	}

	@Test
	void nearbyReturnsDistanceAndWalkEstimate() throws Exception {
		mockMvc.perform(get("/api/libraries/nearby")
						.param("lat", "37.5000000")
						.param("lng", "127.0000000")
						.param("radiusMeters", "1000")
						.param("sort", "distance"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].libraryId").value("LIB-1"))
				.andExpect(jsonPath("$.items[0].distanceMeters").value(greaterThan(0)))
				.andExpect(jsonPath("$.items[0].estimatedWalkMinutes").value(greaterThan(0)))
				.andExpect(jsonPath("$.items[0].recommendScore").value(greaterThan(0.0)));
	}

	@Test
	void libraryDetailIncludesRoomsAndAggregatedSeatState() throws Exception {
		mockMvc.perform(get("/api/libraries/LIB-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.libraryId").value("LIB-1"))
				.andExpect(jsonPath("$.markerState").value("AVAILABLE"))
				.andExpect(jsonPath("$.availableSeats").value(60))
				.andExpect(jsonPath("$.rooms[0].roomName").value("Main Room"))
				.andExpect(jsonPath("$.rooms[0].usedSeats").value(40));
	}

	@Test
	void roomsReturnsNoDataForLibraryWithoutRoomLatest() throws Exception {
		mockMvc.perform(get("/api/libraries/LIB-2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.libraryId").value("LIB-2"))
				.andExpect(jsonPath("$.markerState").value("NO_DATA"))
				.andExpect(jsonPath("$.dataFreshness").value("NO_DATA"));
	}

	private Library library(String libraryId, String name, String district, String lat, String lng, Instant syncedAt) {
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
				"Monday",
				"09:00",
				"18:00",
				"09:00",
				"17:00",
				null,
				null,
				100,
				"20260621"
		), syncedAt);
		return library;
	}

	private RealtimeReadingRoomItem realtimeRoom(Instant observedAt) {
		return new RealtimeReadingRoomItem(
				"1168000000",
				"LIB-1",
				"Seat Library",
				"ROOM-1",
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
