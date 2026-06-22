package com.libraryseatmap.api;

import java.time.Duration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.libraryseatmap.api.cache.ApiCacheKeys;
import com.libraryseatmap.api.cache.TtlResponseCache;
import com.libraryseatmap.api.dto.LibraryQueryDto.LibraryDetailResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.LibraryListResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.NearbyLibrariesResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.ReadingRoomListResponse;
import com.libraryseatmap.library.query.LibraryQueryService;

@RestController
@RequestMapping("/api/libraries")
public class LibraryQueryController {

	private static final Duration NEARBY_TTL = Duration.ofSeconds(30);
	private static final Duration DISTRICT_TTL = Duration.ofSeconds(60);
	private static final Duration DETAIL_TTL = Duration.ofSeconds(60);

	private final LibraryQueryService libraryQueryService;
	private final TtlResponseCache responseCache;

	public LibraryQueryController(LibraryQueryService libraryQueryService, TtlResponseCache responseCache) {
		this.libraryQueryService = libraryQueryService;
		this.responseCache = responseCache;
	}

	@GetMapping
	public LibraryListResponse libraries(
			@RequestParam(required = false) String district,
			@RequestParam(defaultValue = "true") boolean includeNoSeat,
			@RequestParam(defaultValue = "false") boolean onlyWithSeats,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		String key = ApiCacheKeys.district(district, includeNoSeat, onlyWithSeats, page, size);
		return responseCache.get(key, DISTRICT_TTL,
				() -> libraryQueryService.findLibraries(district, includeNoSeat, onlyWithSeats, page, size));
	}

	@GetMapping("/nearby")
	public NearbyLibrariesResponse nearby(
			@RequestParam Double lat,
			@RequestParam Double lng,
			@RequestParam(defaultValue = "3000") int radiusMeters,
			@RequestParam(defaultValue = "recommend") String sort,
			@RequestParam(defaultValue = "true") boolean includeNoSeat,
			@RequestParam(defaultValue = "50") int limit) {
		String key = ApiCacheKeys.nearby(lat, lng, radiusMeters, sort, includeNoSeat, limit);
		return responseCache.get(key, NEARBY_TTL,
				() -> libraryQueryService.findNearby(lat, lng, radiusMeters, sort, includeNoSeat, limit));
	}

	@GetMapping("/{libraryId}")
	public LibraryDetailResponse library(@PathVariable String libraryId) {
		return responseCache.get(ApiCacheKeys.libraryDetail(libraryId), DETAIL_TTL,
				() -> libraryQueryService.findLibrary(libraryId));
	}

	@GetMapping("/{libraryId}/rooms")
	public ReadingRoomListResponse rooms(@PathVariable String libraryId) {
		return responseCache.get(ApiCacheKeys.libraryRooms(libraryId), DETAIL_TTL,
				() -> libraryQueryService.findRooms(libraryId));
	}
}
