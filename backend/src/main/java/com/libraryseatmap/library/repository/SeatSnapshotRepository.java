package com.libraryseatmap.library.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.libraryseatmap.library.domain.SeatSnapshot;

public interface SeatSnapshotRepository extends JpaRepository<SeatSnapshot, Long> {

	List<SeatSnapshot> findByRoomIdOrderByObservedAtDesc(UUID roomId);

	Optional<SeatSnapshot> findByRoomIdAndObservedAt(UUID roomId, Instant observedAt);
}
