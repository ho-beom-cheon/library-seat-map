package com.libraryseatmap.library.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.libraryseatmap.library.domain.ReadingRoom;

public interface ReadingRoomRepository extends JpaRepository<ReadingRoom, UUID> {

	List<ReadingRoom> findByLibraryId(String libraryId);

	Optional<ReadingRoom> findByLibraryIdAndRoomExternalId(String libraryId, String roomExternalId);
}
