package com.libraryseatmap.library.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.libraryseatmap.library.recommendation.FreshnessPolicy.FreshnessState;
import com.libraryseatmap.library.recommendation.OperationTimeService.OperationTimeConfidence;
import com.libraryseatmap.library.recommendation.RecommendationScoringService.ScoreInput;

class RecommendationScoringServiceTest {

	private final RecommendationScoringService scoringService = new RecommendationScoringService();

	@Test
	void usesSeatDistanceUsageAndOperationScores() {
		RecommendationScoringService.ScoreResult result = scoringService.score(new ScoreInput(
				50,
				100,
				new BigDecimal("0.8000"),
				0L,
				FreshnessState.FRESH,
				15,
				OperationTimeConfidence.HIGH
		));

		assertThat(result.seatScore()).isEqualTo(40.0);
		assertThat(result.distanceScore()).isEqualTo(25.0);
		assertThat(result.usageScore()).isEqualTo(4.0);
		assertThat(result.finalScore()).isEqualTo(84.0);
	}

	@Test
	void staleDataAppliesFreshnessMultiplierAndLowSeatPenalty() {
		RecommendationScoringService.ScoreResult result = scoringService.score(new ScoreInput(
				3,
				100,
				new BigDecimal("0.9700"),
				1_000L,
				FreshnessState.STALE,
				12,
				OperationTimeConfidence.HIGH
		));

		assertThat(result.riskPenalty()).isEqualTo(10);
		assertThat(result.finalScore()).isLessThan(20.0);
	}

	@Test
	void lowOperationConfidenceAppliesPenaltyWithoutHardFilter() {
		RecommendationScoringService.ScoreResult result = scoringService.score(new ScoreInput(
				20,
				100,
				new BigDecimal("0.5000"),
				null,
				FreshnessState.FRESH,
				0,
				OperationTimeConfidence.LOW
		));

		assertThat(result.distanceScore()).isZero();
		assertThat(result.riskPenalty()).isEqualTo(5);
		assertThat(result.finalScore()).isEqualTo(21.0);
	}
}
