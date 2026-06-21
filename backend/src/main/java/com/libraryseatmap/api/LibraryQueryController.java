package com.libraryseatmap.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.libraryseatmap.api.dto.LibraryQueryDto.LibraryDetailResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.LibraryListResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.NearbyLibrariesResponse;
import com.libraryseatmap.api.dto.LibraryQueryDto.ReadingRoomListResponse;
import com.libraryseatmap.library.query.LibraryQueryService;

@RestController
@RequestMapping("/api/libraries")
public class LibraryQueryController {

	private final LibraryQueryService libraryQueryService;

	public LibraryQueryController(LibraryQueryService libraryQueryService) {
		this.libraryQueryService = libraryQueryService;
	}

	@GetMapping
	public LibraryListResponse libraries(
			@RequestParam(required = false) String district,
			@RequestParam(defaultValue = "true") boolean includeNoSeat,
			@RequestParam(defaultValue = "false") boolean onlyWithSeats,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return libraryQueryService.findLibraries(district, includeNoSeat, onlyWithSeats, page, size);
	}

	@GetMapping("/nearby")
	public NearbyLibrariesResponse nearby(
			@RequestParam Double lat,
			@RequestParam Double lng,
			@RequestParam(defaultValue = "3000") int radiusMeters,
			@RequestParam(defaultValue = "recommend") String sort,
			@RequestParam(defaultValue = "true") boolean includeNoSeat,
			@RequestParam(defaultValue = "50") int limit) {
		return libraryQueryService.findNearby(lat, lng, radiusMeters, sort, includeNoSeat, limit);
	}

	@GetMapping("/{libraryId}")
	public LibraryDetailResponse library(@PathVariable String libraryId) {
		return libraryQueryService.findLibrary(libraryId);
	}

	@GetMapping("/{libraryId}/rooms")
	public ReadingRoomListResponse rooms(@PathVariable String libraryId) {
		return libraryQueryService.findRooms(libraryId);
	}
}
