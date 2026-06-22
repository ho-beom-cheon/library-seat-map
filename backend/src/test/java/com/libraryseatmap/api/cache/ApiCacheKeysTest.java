package com.libraryseatmap.api.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiCacheKeysTest {

	@Test
	void nearbyKeyRoundsCoordinatesToThreeDecimals() {
		String key = ApiCacheKeys.nearby(37.512345, 127.045678, 3000, "recommend", true, 50);

		assertThat(key).contains("37.512", "127.046");
		assertThat(key).doesNotContain("37.512345", "127.045678");
	}
}
