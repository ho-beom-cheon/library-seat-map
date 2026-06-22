package com.libraryseatmap.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.libraryseatmap.api.dto.RecommendationDto.RecommendationLibrariesResponse;
import com.libraryseatmap.library.recommendation.RecommendationService;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping("/libraries")
	public RecommendationLibrariesResponse libraries(
			@RequestParam(required = false) Double lat,
			@RequestParam(required = false) Double lng,
			@RequestParam(required = false) String district,
			@RequestParam(defaultValue = "5000") int radiusMeters,
			@RequestParam(defaultValue = "60") int minimumStudyMinutes,
			@RequestParam(defaultValue = "20") int limit) {
		return recommendationService.recommendLibraries(lat, lng, district, radiusMeters, minimumStudyMinutes, limit);
	}
}
