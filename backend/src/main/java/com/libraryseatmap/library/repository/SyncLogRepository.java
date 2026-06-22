package com.libraryseatmap.library.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.libraryseatmap.library.domain.SyncLog;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

	List<SyncLog> findTop10ByOrderByStartedAtDesc();

	List<SyncLog> findByStatusOrderByStartedAtDesc(String status);

	Optional<SyncLog> findFirstByStatusInOrderByFinishedAtDesc(List<String> statuses);
}
