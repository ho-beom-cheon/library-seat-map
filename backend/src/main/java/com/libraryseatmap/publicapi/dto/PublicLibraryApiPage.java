package com.libraryseatmap.publicapi.dto;

import java.util.List;

public record PublicLibraryApiPage<T>(
		PublicLibraryApiHeader header,
		List<T> items,
		Integer totalCount
) {
}
