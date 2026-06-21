package com.libraryseatmap.library.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.libraryseatmap.library.recommendation.FreshnessPolicy.FreshnessState;
import com.libraryseatmap.library.recommendation.OperationTimeService.OperationTimeConfidence;

@Service
public class RecommendationScoringService {

	public ScoreResult score(ScoreInput input) {
		double seatScore = Math.min(input.availableSeats() / 50.0, 1.0) * 40.0;
		double distanceScore = input.distanceMeters() == null
				? 0.0
				: Math.max(0.0, 1.0 - input.distanceMeters() / 5_000.0) * 25.0;
		double usageScore = Math.max(0.0, 1.0 - input.usageRate().doubleValue()) * 20.0;
		double baseScore = seatScore + distanceScore + usageScore + input.timeScore();
		int riskPenalty = riskPenalty(input.availableSeats(), input.operationTimeConfidence());
		double finalScore = Math.max(0.0, baseScore * freshnessMultiplier(input.freshnessState()) - riskPenalty);
		return new ScoreResult(
				roundOneDecimal(finalScore),
				roundOneDecimal(seatScore),
				roundOneDecimal(distanceScore),
				roundOneDecimal(usageScore),
				input.timeScore(),
				riskPenalty
		);
	}

	private int riskPenalty(int availableSeats, OperationTimeConfidence operationTimeConfidence) {
		int penalty = 0;
		if (availableSeats <= 5) {
			penalty += 10;
		} else if (availableSeats <= 10) {
			penalty += 5;
		}
		if (operationTimeConfidence == OperationTimeConfidence.LOW) {
			penalty += 5;
		}
		return penalty;
	}

	private double freshnessMultiplier(FreshnessState freshnessState) {
		return switch (freshnessState) {
			case FRESH -> 1.0;
			case USABLE -> 0.85;
			case STALE -> 0.5;
			case EXPIRED, NO_DATA -> 0.0;
		};
	}

	private double roundOneDecimal(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

	public record ScoreInput(
			int availableSeats,
			int totalSeats,
			BigDecimal usageRate,
			Long distanceMeters,
			FreshnessState freshnessState,
			int timeScore,
			OperationTimeConfidence operationTimeConfidence
	) {
	}

	public record ScoreResult(
			double finalScore,
			double seatScore,
			double distanceScore,
			double usageScore,
			int timeScore,
			int riskPenalty
	) {
	}
}
