package com.libraryseatmap.sync;

public record SyncTargetDistrict(String name, String stdgCd) {

	static SyncTargetDistrict parse(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		String[] colonSeparated = trimmed.split(":", 2);
		if (colonSeparated.length == 2) {
			return fromParts(colonSeparated[0], colonSeparated[1]);
		}

		String[] whitespaceSeparated = trimmed.split("\\s+");
		if (whitespaceSeparated.length >= 2) {
			return fromParts(whitespaceSeparated[0], whitespaceSeparated[1]);
		}
		return new SyncTargetDistrict(null, trimmed);
	}

	private static SyncTargetDistrict fromParts(String name, String stdgCd) {
		String normalizedStdgCd = stdgCd == null ? "" : stdgCd.trim();
		if (normalizedStdgCd.isBlank()) {
			return null;
		}
		String normalizedName = name == null || name.isBlank() ? null : name.trim();
		return new SyncTargetDistrict(normalizedName, normalizedStdgCd);
	}
}
