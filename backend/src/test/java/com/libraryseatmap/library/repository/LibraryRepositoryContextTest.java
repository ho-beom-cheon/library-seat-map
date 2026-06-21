package com.libraryseatmap.library.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class LibraryRepositoryContextTest {

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

	@Test
	void repositoriesLoad() {
		assertThat(libraryRepository).isNotNull();
		assertThat(readingRoomRepository).isNotNull();
		assertThat(seatSnapshotRepository).isNotNull();
		assertThat(roomSeatLatestRepository).isNotNull();
		assertThat(syncLogRepository).isNotNull();
	}
}
