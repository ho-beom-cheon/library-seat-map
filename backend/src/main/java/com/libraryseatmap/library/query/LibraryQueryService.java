package com.libraryseatmap.library.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.libraryseatmap.api.dto.LibraryQueryDto.LibraryDetailResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.LibraryListResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.LibrarySummaryResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.NearbyLibrariesResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.NearbyLibraryResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.ReadingRoomListResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.ReadingRoomSeatResponse;
import com.libraryseatmap.library.domain.Library;
import com.libraryseatmap.library.domain.ReadingRoom;
import com.libraryseatmap.library.domain.RoomSeatLatest;
import com.libraryseatmap.library.repository.LibraryRepository;
import com.libraryseatmap.library.repository.ReadingRoomRepository;
import com.libraryseatmap.library.repository.RoomSeatLatestRepository;

@Service
@Transactional(readOnly = true)
public class LibraryQueryService {

	private static final int DEFAULT_PAGE_SIZE = 50;
	private static final int MAX_PAGE_SIZE = 100;
	private static final int DEFAULT_RADIUS_METERS = 3_000;
	private static final int MAX_RADIUS_METERS = 10_000;
	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 100;
	private static final double WALK_METERS_PER_MINUTE = 80.0;
	private static final double EARTH_RADIUS_METERS = 6_371_000.0;

	private final LibraryRepository libraryRepository;
	private final ReadingRoomRepository readingRoomRepository;
	private final RoomSeatLatestRepository roomSeatLatestRepository;
	private final Clock clock;

	@Autowired
	public LibraryQueryService(LibraryRepository libraryRepository, ReadingRoomRepository readingRoomRepository,
			RoomSeatLatestRepository roomSeatLatestRepository) {
		this(libraryRepository, readingRoomRepository, roomSeatLatestRepository, Clock.systemUTC());
	}

	LibraryQueryService(LibraryRepository libraryRepository, ReadingRoomRepository readingRoomRepository,
			RoomSeatLatestRepository roomSeatLatestRepository, Clock clock) {
		this.libraryRepository = libraryRepository;
		this.readingRoomRepository = readingRoomRepository;
		this.roomSeatLatestRepository = roomSeatLatestRepository;
		this.clock = clock;
	}

	public LibraryListResponse findLibraries(String district, boolean includeNoSeat, boolean onlyWithSeats, int page,
			int size) {
		int safePage = Math.max(page, 0);
		int safeSize = clamp(size, 1, MAX_PAGE_SIZE, DEFAULT_PAGE_SIZE);
		List<Library> libraries = sortedLibraries().stream()
				.filter(library -> !hasText(district) || district.equals(library.getDistrict()))
				.toList();
		Map<String, LibrarySeatAggregate> aggregates = aggregatesByLibraryIds(libraryIds(libraries));

		List<LibrarySummaryResponse> filtered = libraries.stream()
				.map(library -> toSummary(library, aggregateFor(library, aggregates)))
				.filter(summary -> shouldInclude(summary.dataFreshness(), includeNoSeat, onlyWithSeats))
				.toList();

		int fromIndex = Math.min(safePage * safeSize, filtered.size());
		int toIndex = Math.min(fromIndex + safeSize, filtered.size());
		return new LibraryListResponse(filtered.subList(fromIndex, toIndex), safePage, safeSize, filtered.size());
	}

	public NearbyLibrariesResponse findNearby(Double lat, Double lng, int radiusMeters, String sort,
			boolean includeNoSeat, int limit) {
		double sourceLat = coordinate(lat, "lat", -90.0, 90.0);
		double sourceLng = coordinate(lng, "lng", -180.0, 180.0);
		int safeRadius = clamp(radiusMeters, 1, MAX_RADIUS_METERS, DEFAULT_RADIUS_METERS);
		int safeLimit = clamp(limit, 1, MAX_LIMIT, DEFAULT_LIMIT);

		List<Library> libraries = sortedLibraries().stream()
				.filter(library -> library.getLatitude() != null && library.getLongitude() != null)
				.toList();
		Map<String, LibrarySeatAggregate> aggregates = aggregatesByLibraryIds(libraryIds(libraries));

		List<NearbyLibraryResponse> nearby = libraries.stream()
				.map(library -> toNearby(library, aggregateFor(library, aggregates), sourceLat, sourceLng, safeRadius))
				.filter(response -> response.distanceMeters() <= safeRadius)
				.filter(response -> includeNoSeat || !DataFreshness.NO_DATA.name().equals(response.dataFreshness()))
				.sorted(nearbyComparator(sort))
				.limit(safeLimit)
				.toList();

		return new NearbyLibrariesResponse(nearby);
	}

	public LibraryDetailResponse findLibrary(String libraryId) {
		Library library = libraryRepository.findById(libraryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Library not found."));
		List<ReadingRoomSeatResponse> rooms = roomResponses(libraryId);
		LibrarySeatAggregate aggregate = aggregateFromRooms(rooms);
		return new LibraryDetailResponse(
				library.getLibraryId(),
				library.getName(),
				library.getDistrict(),
				library.getAddress(),
				library.getPhone(),
				library.getHomepageUrl(),
				library.getLatitude(),
				library.getLongitude(),
				library.getWeekdayOpenTime(),
				library.getWeekdayCloseTime(),
				library.getWeekendOpenTime(),
				library.getWeekendCloseTime(),
				library.getClosedInfo(),
				"UNKNOWN",
				markerState(aggregate).name(),
				aggregate.availableSeatsOrNull(),
				aggregate.totalSeatsOrNull(),
				aggregate.usageRateOrNull(),
				aggregate.dataFreshness().name(),
				aggregate.lastSyncedAt(),
				rooms
		);
	}

	public ReadingRoomListResponse findRooms(String libraryId) {
		if (!libraryRepository.existsById(libraryId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Library not found.");
		}
		return new ReadingRoomListResponse(roomResponses(libraryId));
	}

	private List<ReadingRoomSeatResponse> roomResponses(String libraryId) {
		List<ReadingRoom> rooms = readingRoomRepository.findByLibraryId(libraryId);
		Map<UUID, RoomSeatLatest> latestByRoomId = roomSeatLatestRepository.findAllById(roomIds(rooms)).stream()
				.collect(Collectors.toMap(RoomSeatLatest::getRoomId, Function.identity()));
		return rooms.stream()
				.sorted(Comparator.comparing(ReadingRoom::getRoomName, Comparator.nullsLast(String::compareTo)))
				.map(room -> toRoomResponse(room, latestByRoomId.get(room.getId())))
				.toList();
	}

	private ReadingRoomSeatResponse toRoomResponse(ReadingRoom room, RoomSeatLatest latest) {
		LibrarySeatAggregate aggregate = aggregateFromLatest(List.of(latest).stream()
				.filter(value -> value != null)
				.toList());
		return new ReadingRoomSeatResponse(
				room.getId().toString(),
				room.getRoomName(),
				room.getRoomType(),
				room.getFloorInfo(),
				aggregate.totalSeatsOrNull(),
				aggregate.usedSeatsOrNull(),
				aggregate.reservedSeatsOrNull(),
				aggregate.availableSeatsOrNull(),
				aggregate.usageRateOrNull(),
				markerState(aggregate).name(),
				aggregate.dataFreshness().name(),
				latest == null ? null : latest.getObservedAt(),
				aggregate.lastSyncedAt()
		);
	}

	private LibrarySummaryResponse toSummary(Library library, LibrarySeatAggregate aggregate) {
		return new LibrarySummaryResponse(
				library.getLibraryId(),
				library.getName(),
				library.getDistrict(),
				library.getAddress(),
				library.getLatitude(),
				library.getLongitude(),
				markerState(aggregate).name(),
				aggregate.availableSeatsOrNull(),
				aggregate.totalSeatsOrNull(),
				aggregate.usageRateOrNull(),
				aggregate.lastSyncedAt(),
				aggregate.dataFreshness().name()
		);
	}

	private NearbyLibraryResponse toNearby(Library library, LibrarySeatAggregate aggregate, double lat, double lng,
			int radiusMeters) {
		long distanceMeters = Math.round(distanceMeters(lat, lng, library.getLatitude().doubleValue(),
				library.getLongitude().doubleValue()));
		return new NearbyLibraryResponse(
				library.getLibraryId(),
				library.getName(),
				library.getDistrict(),
				library.getAddress(),
				library.getLatitude(),
				library.getLongitude(),
				distanceMeters,
				estimatedWalkMinutes(distanceMeters),
				markerState(aggregate).name(),
				aggregate.availableSeatsOrNull(),
				aggregate.totalSeatsOrNull(),
				aggregate.usageRateOrNull(),
				recommendScore(aggregate, distanceMeters, radiusMeters),
				aggregate.lastSyncedAt(),
				aggregate.dataFreshness().name(),
				recommendReason(aggregate)
		);
	}

	private Map<String, LibrarySeatAggregate> aggregatesByLibraryIds(Collection<String> libraryIds) {
		if (libraryIds.isEmpty()) {
			return Map.of();
		}
		return roomSeatLatestRepository.findByLibraryIdIn(libraryIds).stream()
				.collect(Collectors.groupingBy(RoomSeatLatest::getLibraryId,
						Collectors.collectingAndThen(Collectors.toList(), this::aggregateFromLatest)));
	}

	private LibrarySeatAggregate aggregateFor(Library library, Map<String, LibrarySeatAggregate> aggregates) {
		return aggregates.getOrDefault(library.getLibraryId(), LibrarySeatAggregate.noData());
	}

	private LibrarySeatAggregate aggregateFromRooms(List<ReadingRoomSeatResponse> rooms) {
		List<RoomSeatProjection> projections = rooms.stream()
				.filter(room -> !DataFreshness.NO_DATA.name().equals(room.dataFreshness()))
				.map(room -> new RoomSeatProjection(room.totalSeats(), room.usedSeats(), room.reservedSeats(),
						room.availableSeats(), room.lastSyncedAt(), DataFreshness.valueOf(room.dataFreshness())))
				.toList();
		if (projections.isEmpty()) {
			return LibrarySeatAggregate.noData();
		}

		int totalSeats = projections.stream().mapToInt(RoomSeatProjection::totalSeats).sum();
		int usedSeats = projections.stream().mapToInt(RoomSeatProjection::usedSeats).sum();
		int reservedSeats = projections.stream().mapToInt(RoomSeatProjection::reservedSeats).sum();
		int availableSeats = projections.stream().mapToInt(RoomSeatProjection::availableSeats).sum();
		Instant lastSyncedAt = projections.stream()
				.map(RoomSeatProjection::lastSyncedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);
		DataFreshness dataFreshness = projections.stream()
				.map(RoomSeatProjection::dataFreshness)
				.max(Comparator.comparingInt(DataFreshness::severity))
				.orElse(DataFreshness.NO_DATA);
		return new LibrarySeatAggregate(totalSeats, usedSeats, reservedSeats, availableSeats,
				usageRate(totalSeats, usedSeats), lastSyncedAt, dataFreshness);
	}

	private LibrarySeatAggregate aggregateFromLatest(List<RoomSeatLatest> latestRows) {
		if (latestRows.isEmpty()) {
			return LibrarySeatAggregate.noData();
		}

		int totalSeats = latestRows.stream().mapToInt(RoomSeatLatest::getTotalSeats).sum();
		int usedSeats = latestRows.stream().mapToInt(RoomSeatLatest::getUsedSeats).sum();
		int reservedSeats = latestRows.stream().mapToInt(RoomSeatLatest::getReservedSeats).sum();
		int availableSeats = latestRows.stream().mapToInt(RoomSeatLatest::getAvailableSeats).sum();
		Instant lastSyncedAt = latestRows.stream()
				.map(RoomSeatLatest::getCollectedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);
		DataFreshness dataFreshness = latestRows.stream()
				.map(this::dataFreshness)
				.max(Comparator.comparingInt(DataFreshness::severity))
				.orElse(DataFreshness.NO_DATA);
		return new LibrarySeatAggregate(totalSeats, usedSeats, reservedSeats, availableSeats,
				usageRate(totalSeats, usedSeats), lastSyncedAt, dataFreshness);
	}

	private DataFreshness dataFreshness(RoomSeatLatest latest) {
		Instant observedAt = latest.getObservedAt();
		if (observedAt == null) {
			return DataFreshness.NO_DATA;
		}
		Duration age = Duration.between(observedAt, clock.instant());
		if (age.isNegative() || age.compareTo(Duration.ofMinutes(3)) <= 0) {
			return DataFreshness.FRESH;
		}
		if (age.compareTo(Duration.ofMinutes(10)) <= 0) {
			return DataFreshness.USABLE;
		}
		if (age.compareTo(Duration.ofMinutes(30)) <= 0) {
			return DataFreshness.STALE;
		}
		return DataFreshness.EXPIRED;
	}

	private MarkerState markerState(LibrarySeatAggregate aggregate) {
		if (aggregate.dataFreshness() == DataFreshness.NO_DATA) {
			return MarkerState.NO_DATA;
		}
		if (aggregate.dataFreshness() == DataFreshness.STALE || aggregate.dataFreshness() == DataFreshness.EXPIRED) {
			return MarkerState.STALE;
		}
		if (aggregate.availableSeats() == 0) {
			return MarkerState.FULL;
		}
		if (aggregate.availableSeats() <= 5 || greaterThanOrEqual(aggregate.usageRate(), "0.9000")) {
			return MarkerState.FULL_RISK;
		}
		if (greaterThanOrEqual(aggregate.usageRate(), "0.7500")) {
			return MarkerState.CROWDED;
		}
		if (greaterThanOrEqual(aggregate.usageRate(), "0.5000")) {
			return MarkerState.MODERATE;
		}
		if (aggregate.availableSeats() > 10) {
			return MarkerState.AVAILABLE;
		}
		return MarkerState.MODERATE;
	}

	private Comparator<NearbyLibraryResponse> nearbyComparator(String sort) {
		if ("distance".equalsIgnoreCase(sort)) {
			return Comparator.comparingLong(NearbyLibraryResponse::distanceMeters)
					.thenComparing(NearbyLibraryResponse::name);
		}
		if ("availableSeats".equalsIgnoreCase(sort)) {
			return Comparator.comparing((NearbyLibraryResponse response) -> Optional
							.ofNullable(response.availableSeats()).orElse(-1))
					.reversed()
					.thenComparingLong(NearbyLibraryResponse::distanceMeters);
		}
		return Comparator.comparingDouble(NearbyLibraryResponse::recommendScore)
				.reversed()
				.thenComparingLong(NearbyLibraryResponse::distanceMeters);
	}

	private double recommendScore(LibrarySeatAggregate aggregate, long distanceMeters, int radiusMeters) {
		if (aggregate.dataFreshness() == DataFreshness.NO_DATA || aggregate.dataFreshness() == DataFreshness.EXPIRED) {
			return 0.0;
		}
		double seatScore = Math.min(aggregate.availableSeats() / 50.0, 1.0) * 60.0;
		double distanceScore = Math.max(0.0, (radiusMeters - distanceMeters) / (double) radiusMeters) * 40.0;
		double stalePenalty = aggregate.dataFreshness() == DataFreshness.STALE ? 0.5 : 1.0;
		return roundOneDecimal((seatScore + distanceScore) * stalePenalty);
	}

	private String recommendReason(LibrarySeatAggregate aggregate) {
		if (aggregate.dataFreshness() == DataFreshness.NO_DATA) {
			return "Seat data has not been collected yet.";
		}
		if (aggregate.dataFreshness() == DataFreshness.EXPIRED) {
			return "Seat data refresh is delayed.";
		}
		return "Calculated from seat availability, distance, and data freshness.";
	}

	private List<Library> sortedLibraries() {
		List<Library> libraries = new ArrayList<>(libraryRepository.findAll());
		libraries.sort(Comparator.comparing(Library::getName, Comparator.nullsLast(String::compareTo))
				.thenComparing(Library::getLibraryId));
		return libraries;
	}

	private List<String> libraryIds(List<Library> libraries) {
		return libraries.stream().map(Library::getLibraryId).toList();
	}

	private List<UUID> roomIds(List<ReadingRoom> rooms) {
		return rooms.stream().map(ReadingRoom::getId).toList();
	}

	private boolean shouldInclude(String dataFreshness, boolean includeNoSeat, boolean onlyWithSeats) {
		boolean hasSeatData = !DataFreshness.NO_DATA.name().equals(dataFreshness);
		if (onlyWithSeats) {
			return hasSeatData;
		}
		return includeNoSeat || hasSeatData;
	}

	private BigDecimal usageRate(int totalSeats, int usedSeats) {
		if (totalSeats <= 0) {
			return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
		}
		return BigDecimal.valueOf(usedSeats).divide(BigDecimal.valueOf(totalSeats), 4, RoundingMode.HALF_UP);
	}

	private boolean greaterThanOrEqual(BigDecimal value, String threshold) {
		return value.compareTo(new BigDecimal(threshold)) >= 0;
	}

	private double coordinate(Double value, String name, double min, double max) {
		if (value == null || value < min || value > max) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " is out of range.");
		}
		return value;
	}

	private int clamp(int value, int min, int max, int defaultValue) {
		if (value <= 0) {
			return defaultValue;
		}
		return Math.max(min, Math.min(max, value));
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private int estimatedWalkMinutes(long distanceMeters) {
		return Math.max(1, (int) Math.ceil(distanceMeters / WALK_METERS_PER_MINUTE));
	}

	private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_METERS * c;
	}

	private double roundOneDecimal(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

	private enum MarkerState {
		AVAILABLE,
		MODERATE,
		CROWDED,
		FULL_RISK,
		FULL,
		STALE,
		NO_DATA
	}

	private enum DataFreshness {
		FRESH(0),
		USABLE(1),
		STALE(2),
		EXPIRED(3),
		NO_DATA(4);

		private final int severity;

		DataFreshness(int severity) {
			this.severity = severity;
		}

		private int severity() {
			return severity;
		}
	}

	private record LibrarySeatAggregate(
			int totalSeats,
			int usedSeats,
			int reservedSeats,
			int availableSeats,
			BigDecimal usageRate,
			Instant lastSyncedAt,
			DataFreshness dataFreshness
	) {
		private static LibrarySeatAggregate noData() {
			return new LibrarySeatAggregate(0, 0, 0, 0,
					BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), null, DataFreshness.NO_DATA);
		}

		private Integer totalSeatsOrNull() {
			return dataFreshness == DataFreshness.NO_DATA ? null : totalSeats;
		}

		private Integer usedSeatsOrNull() {
			return dataFreshness == DataFreshness.NO_DATA ? null : usedSeats;
		}

		private Integer reservedSeatsOrNull() {
			return dataFreshness == DataFreshness.NO_DATA ? null : reservedSeats;
		}

		private Integer availableSeatsOrNull() {
			return dataFreshness == DataFreshness.NO_DATA ? null : availableSeats;
		}

		private BigDecimal usageRateOrNull() {
			return dataFreshness == DataFreshness.NO_DATA ? null : usageRate;
		}
	}

	private record RoomSeatProjection(
			int totalSeats,
			int usedSeats,
			int reservedSeats,
			int availableSeats,
			Instant lastSyncedAt,
			DataFreshness dataFreshness
	) {
	}
}
