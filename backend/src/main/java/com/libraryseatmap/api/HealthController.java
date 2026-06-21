package com.libraryseatmap.api;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

	@GetMapping("/health")
	public HealthResponse health() {
		return new HealthResponse("UP", "library-seat-map-backend", Instant.now());
	}

	public record HealthResponse(String status, String service, Instant checkedAt) {
	}
}
