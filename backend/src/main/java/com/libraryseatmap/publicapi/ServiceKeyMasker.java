package com.libraryseatmap.publicapi;

public final class ServiceKeyMasker {

	private ServiceKeyMasker() {
	}

	public static String mask(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}

		String trimmed = value.trim();
		if (trimmed.length() <= 12) {
			return "*".repeat(trimmed.length());
		}

		return trimmed.substring(0, 6) + "..." + trimmed.substring(trimmed.length() - 6);
	}
}
