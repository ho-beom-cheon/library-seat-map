package com.libraryseatmap.publicapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceKeyMaskerTest {

	@Test
	void masksLongServiceKey() {
		assertThat(ServiceKeyMasker.mask("abcdef1234567890ghijkl"))
				.isEqualTo("abcdef...ghijkl");
	}

	@Test
	void masksShortServiceKey() {
		assertThat(ServiceKeyMasker.mask("abc123"))
				.isEqualTo("******");
	}
}
