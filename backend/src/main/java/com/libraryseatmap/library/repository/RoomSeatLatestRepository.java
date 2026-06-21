package com.libraryseatmap.library.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.libraryseatmap.library.domain.RoomSeatLatest;

public interface RoomSeatLatestRepository extends JpaRepository<RoomSeatLatest, UUID> {

	List<RoomSeatLatest> findByLibraryId(String libraryId);

	List<RoomSeatLatest> findByLibraryIdIn(Collection<String> libraryIds);

	List<RoomSeatLatest> findByFreshnessStatus(String freshnessStatus);
}
