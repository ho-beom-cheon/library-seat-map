package com.libraryseatmap.library.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.libraryseatmap.api.dto.RecommendationDto.RecommendationLibrariesResponse;
import com.libraryseatmap.api.dto.RecommendationDto.RecommendedLibraryResponse;
import com.libraryseatmap.library.domain.Library;
import com.libraryseatmap.library.domain.RoomSeatLatest;
import com.libraryseatmap.library.recommendation.FreshnessPolicy.FreshnessResult;
import com.libraryseatmap.library.recommendation.FreshnessPolicy.FreshnessState;
import com.libraryseatmap.library.recommendation.OperationTimeService.OperationTimeResult;
import com.libraryseatmap.library.recommendation.RecommendationScoringService.ScoreInput;
import com.libraryseatmap.library.recommendation.RecommendationScoringService.ScoreResult;
import com.libraryseatmap.library.repository.LibraryRepository;
import com.libraryseatmap.library.repository.RoomSeatLatestRepository;

@Service
@Transactional(readOnly = true)
public class RecommendationService {

	private static final int DEFAULT_RADIUS_METERS = 5_000;
	private static final int MAX_RADIUS_METERS = 10_000;
	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 100;
	private static final double WALK_METERS_PER_MINUTE = 80.0;
	private static final double EARTH_RADIUS_METERS = 6_371_000.0;

	private final LibraryRepository libraryRepository;
	private final RoomSeatLatestRepository roomSeatLatestRepository;
	private final FreshnessPolicy freshnessPolicy;
	private final OperationTimeService operationTimeService;
	private final RecommendationScoringService scoringService;

	public RecommendationService(LibraryRepository libraryRepository, RoomSeatLatestRepository roomSeatLatestRepository,
			FreshnessPolicy freshnessPolicy, OperationTimeService operationTimeService,
			RecommendationScoringService scoringService) {
		this.libraryRepository = libraryRepository;
		this.roomSeatLatestRepository = roomSeatLatestRepository;
		this.freshnessPolicy = freshnessPolicy;
		this.operationTimeService = operationTimeService;
		this.scoringService = scoringService;
	}

	public RecommendationLibrariesResponse recommendLibraries(Double lat, Double lng, String district,
			int radiusMeters, int minimumStudyMinutes, int limit) {
		boolean locationMode = lat != null || lng != null;
		if (locationMode && (lat == null || lng == null)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat and lng must be provided together.");
		}
		if (!locationMode && !hasText(district)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat/lng or district is required.");
		}

		Double sourceLat = locationMode ? coordinate(lat, "lat", -90.0, 90.0) : null;
		Double sourceLng = locationMode ? coordinate(lng, "lng", -180.0, 180.0) : null;
		int safeRadius = clamp(radiusMeters, 1, MAX_RADIUS_METERS, DEFAULT_RADIUS_METERS);
		int safeLimit = clamp(limit, 1, MAX_LIMIT, DEFAULT_LIMIT);
		int safeMinimumStudyMinutes = Math.max(minimumStudyMinutes, 0);

		List<Library> libraries = sortedLibraries().stream()
				.filter(library -> !hasText(district) || district.equals(library.getDistrict()))
				.filter(library -> !locationMode || hasCoordinates(library))
				.toList();
		Map<String, SeatAggregate> aggregates = aggregatesByLibraryIds(libraryIds(libraries));

		List<RecommendedLibraryResponse> recommendations = libraries.stream()
				.map(library -> toRecommendation(library, aggregates.get(library.getLibraryId()), sourceLat, sourceLng,
						locationMode, safeMinimumStudyMinutes))
				.flatMap(Optional::stream)
				.filter(response -> !locationMode || response.distanceMeters() <= safeRadius)
				.sorted(recommendationComparator())
				.limit(safeLimit)
				.toList();

		return new RecommendationLibrariesResponse(recommendations);
	}

	private Optional<RecommendedLibraryResponse> toRecommendation(Library library, SeatAggregate aggregate,
			Double sourceLat, Double sourceLng, boolean locationMode, int minimumStudyMinutes) {
		if (aggregate == null || aggregate.hasNoSeatData() || aggregate.totalSeats() <= 0
				|| aggregate.availableSeats() <= 0 || aggregate.freshnessState() == FreshnessState.EXPIRED) {
			return Optional.empty();
		}

		Long distanceMeters = null;
		Integer estimatedWalkMinutes = null;
		if (locationMode) {
			distanceMeters = Math.round(distanceMeters(sourceLat, sourceLng, library.getLatitude().doubleValue(),
					library.getLongitude().doubleValue()));
			estimatedWalkMinutes = estimatedWalkMinutes(distanceMeters);
		}

		OperationTimeResult operationTime = operationTimeService.evaluate(library, estimatedWalkMinutes,
				minimumStudyMinutes);
		if (operationTime.excluded()) {
			return Optional.empty();
		}

		ScoreResult score = scoringService.score(new ScoreInput(
				aggregate.availableSeats(),
				aggregate.totalSeats(),
				aggregate.usageRate(),
				distanceMeters,
				aggregate.freshnessState(),
				operationTime.timeScore(),
				operationTime.confidence()
		));

		return Optional.of(new RecommendedLibraryResponse(
				library.getLibraryId(),
				library.getName(),
				library.getDistrict(),
				library.getAddress(),
				library.getLatitude(),
				library.getLongitude(),
				distanceMeters,
				estimatedWalkMinutes,
				markerState(aggregate),
				aggregate.availableSeats(),
				aggregate.totalSeats(),
				aggregate.usageRate(),
				score.finalScore(),
				aggregate.lastSyncedAt(),
				aggregate.freshnessState().name(),
				operationTime.status().name(),
				operationTime.confidence().name(),
				recommendReason(aggregate, distanceMeters)
		));
	}

	private Map<String, SeatAggregate> aggregatesByLibraryIds(Collection<String> libraryIds) {
		if (libraryIds.isEmpty()) {
			return Map.of();
		}
		return roomSeatLatestRepository.findByLibraryIdIn(libraryIds).stream()
				.collect(Collectors.groupingBy(RoomSeatLatest::getLibraryId,
						Collectors.collectingAndThen(Collectors.toList(), this::aggregateFromLatest)));
	}

	private SeatAggregate aggregateFromLatest(List<RoomSeatLatest> latestRows) {
		int totalSeats = latestRows.stream().mapToInt(RoomSeatLatest::getTotalSeats).sum();
		int usedSeats = latestRows.stream().mapToInt(RoomSeatLatest::getUsedSeats).sum();
		int reservedSeats = latestRows.stream().mapToInt(RoomSeatLatest::getReservedSeats).sum();
		int availableSeats = latestRows.stream().mapToInt(RoomSeatLatest::getAvailableSeats).sum();
		Instant lastSyncedAt = latestRows.stream()
				.map(RoomSeatLatest::getCollectedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);
		FreshnessResult freshness = latestRows.stream()
				.map(row -> freshnessPolicy.evaluate(row.getObservedAt()))
				.max(Comparator.comparingInt(result -> result.state().severity()))
				.orElse(new FreshnessResult(FreshnessState.NO_DATA, null, 0.0));
		return new SeatAggregate(totalSeats, usedSeats, reservedSeats, availableSeats,
				usageRate(totalSeats, usedSeats), lastSyncedAt, freshness.state());
	}

	private String markerState(SeatAggregate aggregate) {
		if (aggregate.availableSeats() <= 5 || greaterThanOrEqual(aggregate.usageRate(), "0.9000")) {
			return "FULL_RISK";
		}
		if (greaterThanOrEqual(aggregate.usageRate(), "0.7500")) {
			return "CROWDED";
		}
		if (greaterThanOrEqual(aggregate.usageRate(), "0.5000")) {
			return "MODERATE";
		}
		if (aggregate.availableSeats() > 10) {
			return "AVAILABLE";
		}
		return "MODERATE";
	}

	private String recommendReason(SeatAggregate aggregate, Long distanceMeters) {
		if (aggregate.availableSeats() <= 5) {
			return "가깝지만 잔여 좌석이 적어 만석 위험이 있습니다.";
		}
		if (aggregate.freshnessState() == FreshnessState.STALE) {
			return "좌석은 여유 있지만 최근 갱신 시각을 확인하세요.";
		}
		if (distanceMeters != null && distanceMeters <= 1_000 && aggregate.availableSeats() > 10) {
			return "좌석 여유가 많고 현재 위치에서 가깝습니다.";
		}
		return "좌석 여유와 이용률을 기준으로 추천합니다.";
	}

	private Comparator<RecommendedLibraryResponse> recommendationComparator() {
		return Comparator.comparingDouble(RecommendedLibraryResponse::recommendScore)
				.reversed()
				.thenComparing(response -> Optional.ofNullable(response.distanceMeters()).orElse(Long.MAX_VALUE))
				.thenComparing(RecommendedLibraryResponse::name);
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

	private boolean hasCoordinates(Library library) {
		return library.getLatitude() != null && library.getLongitude() != null;
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

	private record SeatAggregate(
			int totalSeats,
			int usedSeats,
			int reservedSeats,
			int availableSeats,
			BigDecimal usageRate,
			Instant lastSyncedAt,
			FreshnessState freshnessState
	) {
		private boolean hasNoSeatData() {
			return freshnessState == FreshnessState.NO_DATA;
		}
	}
}
