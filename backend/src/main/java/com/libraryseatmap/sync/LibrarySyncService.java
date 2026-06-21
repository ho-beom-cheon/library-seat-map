package com.libraryseatmap.sync;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
import com.libraryseatmap.publicapi.PublicLibraryApiException;
import com.libraryseatmap.publicapi.dto.LibraryInfoItem;
import com.libraryseatmap.publicapi.dto.PublicLibraryApiHeader;
import com.libraryseatmap.publicapi.dto.PublicLibraryApiPage;
import com.libraryseatmap.publicapi.dto.RealtimeReadingRoomItem;

@Service
public class LibrarySyncService {

	private static final String INFO_ENDPOINT = "/info_v2";
	private static final String REALTIME_ROOM_ENDPOINT = "/rlt_rdrm_info_v2";
	private static final String SUCCESS = "SUCCESS";
	private static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
	private static final String FAILED_AUTH = "FAILED_AUTH";
	private static final String FAILED_RATE_LIMIT = "FAILED_RATE_LIMIT";
	private static final String FAILED_PROVIDER = "FAILED_PROVIDER";
	private static final String FAILED_INTERNAL = "FAILED_INTERNAL";

	private final PublicLibraryApiClient apiClient;
	private final LibrarySyncProperties properties;
	private final LibraryRepository libraryRepository;
	private final ReadingRoomRepository readingRoomRepository;
	private final SeatSnapshotRepository seatSnapshotRepository;
	private final RoomSeatLatestRepository roomSeatLatestRepository;
	private final SyncLogRepository syncLogRepository;
	private final Clock clock;

	@Autowired
	public LibrarySyncService(PublicLibraryApiClient apiClient, LibrarySyncProperties properties,
			LibraryRepository libraryRepository, ReadingRoomRepository readingRoomRepository,
			SeatSnapshotRepository seatSnapshotRepository, RoomSeatLatestRepository roomSeatLatestRepository,
			SyncLogRepository syncLogRepository) {
		this(apiClient, properties, libraryRepository, readingRoomRepository, seatSnapshotRepository,
				roomSeatLatestRepository, syncLogRepository, Clock.systemUTC());
	}

	LibrarySyncService(PublicLibraryApiClient apiClient, LibrarySyncProperties properties,
			LibraryRepository libraryRepository, ReadingRoomRepository readingRoomRepository,
			SeatSnapshotRepository seatSnapshotRepository, RoomSeatLatestRepository roomSeatLatestRepository,
			SyncLogRepository syncLogRepository, Clock clock) {
		this.apiClient = apiClient;
		this.properties = properties;
		this.libraryRepository = libraryRepository;
		this.readingRoomRepository = readingRoomRepository;
		this.seatSnapshotRepository = seatSnapshotRepository;
		this.roomSeatLatestRepository = roomSeatLatestRepository;
		this.syncLogRepository = syncLogRepository;
		this.clock = clock;
	}

	@Transactional
	public List<SyncRunResult> syncLibraryInfoForAllDistricts() {
		return properties.targetDistricts().stream()
				.map(this::syncLibraryInfo)
				.toList();
	}

	@Transactional
	public List<SyncRunResult> syncRealtimeRoomsForAllDistricts() {
		return properties.targetDistricts().stream()
				.map(this::syncRealtimeRooms)
				.toList();
	}

	@Transactional
	public SyncRunResult syncLibraryInfo(SyncTargetDistrict target) {
		Instant startedAt = clock.instant();
		int processedRows = 0;
		int skippedRows = 0;
		String status = SUCCESS;
		PublicLibraryApiHeader header = null;
		String errorMessage = null;

		try {
			PublicLibraryApiPage<LibraryInfoItem> page = apiClient.fetchLibraries(target.stdgCd());
			header = page.header();
			if (!isSuccessHeader(header)) {
				status = FAILED_PROVIDER;
			}
			else {
				for (LibraryInfoItem item : page.items()) {
					if (!hasText(item.libraryId()) || !hasText(item.name())) {
						skippedRows++;
						continue;
					}
					upsertLibrary(item);
					processedRows++;
				}
				status = skippedRows == 0 ? SUCCESS : PARTIAL_SUCCESS;
			}
		}
		catch (RuntimeException ex) {
			status = failureStatus(ex);
			errorMessage = safeMessage(ex);
		}

		saveSyncLog(INFO_ENDPOINT, target, status, header, processedRows, startedAt, errorMessage);
		return new SyncRunResult(INFO_ENDPOINT, target, status, processedRows, skippedRows);
	}

	@Transactional
	public SyncRunResult syncRealtimeRooms(SyncTargetDistrict target) {
		Instant startedAt = clock.instant();
		Instant collectedAt = startedAt;
		int processedRows = 0;
		int skippedRows = 0;
		String status = SUCCESS;
		PublicLibraryApiHeader header = null;
		String errorMessage = null;

		try {
			PublicLibraryApiPage<RealtimeReadingRoomItem> page = apiClient.fetchRealtimeRooms(target.stdgCd());
			header = page.header();
			if (!isSuccessHeader(header)) {
				status = FAILED_PROVIDER;
			}
			else {
				for (RealtimeReadingRoomItem item : page.items()) {
					Optional<SeatCounts> counts = SeatCounts.from(item);
					if (!isValidRealtimeItem(item) || counts.isEmpty()) {
						skippedRows++;
						continue;
					}
					upsertRealtimeRoom(item, counts.get(), collectedAt);
					processedRows++;
				}
				status = skippedRows == 0 ? SUCCESS : PARTIAL_SUCCESS;
			}
		}
		catch (RuntimeException ex) {
			status = failureStatus(ex);
			errorMessage = safeMessage(ex);
		}

		saveSyncLog(REALTIME_ROOM_ENDPOINT, target, status, header, processedRows, startedAt, errorMessage);
		return new SyncRunResult(REALTIME_ROOM_ENDPOINT, target, status, processedRows, skippedRows);
	}

	private void upsertLibrary(LibraryInfoItem item) {
		Library library = libraryRepository.findById(item.libraryId())
				.orElseGet(() -> new Library(item.libraryId(), item.name()));
		library.applyLibraryInfo(item, clock.instant());
		libraryRepository.save(library);
	}

	private void upsertRealtimeRoom(RealtimeReadingRoomItem item, SeatCounts counts, Instant collectedAt) {
		Library library = libraryRepository.findById(item.libraryId())
				.orElseGet(() -> new Library(item.libraryId(), fallbackName(item.libraryName(), item.libraryId())));
		library.applyRealtimeSeed(item.libraryName(), item.sourceStdgCd());
		libraryRepository.save(library);

		ReadingRoom room = readingRoomRepository.findByLibraryIdAndRoomExternalId(item.libraryId(), item.roomExternalId())
				.orElseGet(() -> new ReadingRoom(item.libraryId(), item.roomExternalId(), item.roomName()));
		room.applyRealtimeRoom(item, counts.totalSeats(), collectedAt);
		ReadingRoom savedRoom = readingRoomRepository.save(room);

		Instant observedAt = item.observedAt().orElse(collectedAt);
		appendSnapshotIfAbsent(savedRoom.getId(), item, counts, observedAt);
		upsertLatest(savedRoom.getId(), item, counts, observedAt, collectedAt);
	}

	private void appendSnapshotIfAbsent(UUID roomId, RealtimeReadingRoomItem item, SeatCounts counts, Instant observedAt) {
		if (seatSnapshotRepository.findByRoomIdAndObservedAt(roomId, observedAt).isPresent()) {
			return;
		}
		seatSnapshotRepository.save(new SeatSnapshot(item.libraryId(), roomId, observedAt, item.rawObservedAt(),
				item.currentVisitorCount(), counts.totalSeats(), counts.usedSeats(), counts.reservedSeats(),
				counts.availableSeats(), counts.usageRate()));
	}

	private void upsertLatest(UUID roomId, RealtimeReadingRoomItem item, SeatCounts counts, Instant observedAt,
			Instant collectedAt) {
		String freshnessStatus = freshnessStatus(observedAt, collectedAt);
		RoomSeatLatest latest = roomSeatLatestRepository.findById(roomId)
				.orElseGet(() -> new RoomSeatLatest(roomId, item.libraryId(), observedAt, collectedAt,
						counts.totalSeats(), counts.usedSeats(), counts.availableSeats(), counts.usageRate(),
						freshnessStatus));
		latest.updateSeatState(observedAt, collectedAt, item.currentVisitorCount(), counts.totalSeats(),
				counts.usedSeats(), counts.reservedSeats(), counts.availableSeats(), counts.usageRate(),
				freshnessStatus);
		roomSeatLatestRepository.save(latest);
	}

	private void saveSyncLog(String endpoint, SyncTargetDistrict target, String status, PublicLibraryApiHeader header,
			int rowCount, Instant startedAt, String errorMessage) {
		Instant finishedAt = clock.instant();
		syncLogRepository.save(SyncLog.finished(endpoint, target.stdgCd(), target.name(), status, null,
				header == null ? null : header.resultCode(), header == null ? null : header.resultMessage(),
				rowCount, startedAt, finishedAt, errorMessage));
	}

	private boolean isValidRealtimeItem(RealtimeReadingRoomItem item) {
		return hasText(item.libraryId()) && hasText(item.roomExternalId()) && hasText(item.roomName());
	}

	private boolean isSuccessHeader(PublicLibraryApiHeader header) {
		if (header == null || header.resultCode() == null) {
			return true;
		}
		String resultCode = header.resultCode().trim();
		return "00".equals(resultCode) || "NORMAL_CODE".equalsIgnoreCase(resultCode);
	}

	private String failureStatus(RuntimeException ex) {
		String message = safeMessage(ex).toLowerCase();
		if (ex instanceof PublicLibraryApiException && message.contains("service_key")) {
			return FAILED_AUTH;
		}
		if (message.contains("401") || message.contains("unauthorized") || message.contains("auth")) {
			return FAILED_AUTH;
		}
		if (message.contains("429") || message.contains("quota") || message.contains("rate")) {
			return FAILED_RATE_LIMIT;
		}
		return FAILED_INTERNAL;
	}

	private String freshnessStatus(Instant observedAt, Instant collectedAt) {
		Duration age = Duration.between(observedAt, collectedAt);
		if (age.isNegative() || age.compareTo(Duration.ofMinutes(3)) <= 0) {
			return "FRESH";
		}
		if (age.compareTo(Duration.ofMinutes(10)) <= 0) {
			return "USABLE";
		}
		if (age.compareTo(Duration.ofMinutes(30)) <= 0) {
			return "STALE";
		}
		return "EXPIRED";
	}

	private String safeMessage(RuntimeException ex) {
		String message = ex.getMessage();
		if (message == null || message.isBlank()) {
			return ex.getClass().getSimpleName();
		}
		return message;
	}

	private String fallbackName(String name, String libraryId) {
		return hasText(name) ? name : libraryId;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private record SeatCounts(int totalSeats, int usedSeats, int reservedSeats, int availableSeats,
			BigDecimal usageRate) {

		private static Optional<SeatCounts> from(RealtimeReadingRoomItem item) {
			Integer reservedSeats = defaultZero(item.reservedSeats());
			Integer totalSeats = item.totalSeats();
			Integer usedSeats = item.usedSeats();
			Integer availableSeats = item.availableSeats();

			if (totalSeats == null && usedSeats != null && availableSeats != null) {
				totalSeats = usedSeats + reservedSeats + availableSeats;
			}
			if (availableSeats == null && totalSeats != null && usedSeats != null) {
				availableSeats = totalSeats - usedSeats - reservedSeats;
			}
			if (usedSeats == null && totalSeats != null && availableSeats != null) {
				usedSeats = totalSeats - reservedSeats - availableSeats;
			}
			if (totalSeats == null || usedSeats == null || availableSeats == null) {
				return Optional.empty();
			}
			if (totalSeats < 0 || usedSeats < 0 || reservedSeats < 0 || availableSeats < 0) {
				return Optional.empty();
			}
			BigDecimal usageRate = usageRate(totalSeats, usedSeats);
			if (usageRate.compareTo(new BigDecimal("1.5000")) > 0) {
				return Optional.empty();
			}
			return Optional.of(new SeatCounts(totalSeats, usedSeats, reservedSeats, availableSeats, usageRate));
		}

		private static Integer defaultZero(Integer value) {
			return value == null ? 0 : value;
		}

		private static BigDecimal usageRate(int totalSeats, int usedSeats) {
			if (totalSeats <= 0) {
				return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
			}
			return BigDecimal.valueOf(usedSeats)
					.divide(BigDecimal.valueOf(totalSeats), 4, RoundingMode.HALF_UP);
		}
	}
}
