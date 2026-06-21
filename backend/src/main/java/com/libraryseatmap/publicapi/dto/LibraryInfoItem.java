package com.libraryseatmap.publicapi.dto;

import java.math.BigDecimal;

public record LibraryInfoItem(
		String libraryId,
		String name,
		String sourceStdgCd,
		String city,
		String district,
		String libraryType,
		String address,
		String operatorName,
		String phone,
		String homepageUrl,
		BigDecimal latitude,
		BigDecimal longitude,
		String closedInfo,
		String weekdayOpenTime,
		String weekdayCloseTime,
		String weekendOpenTime,
		String weekendCloseTime,
		String holidayOpenTime,
		String holidayCloseTime,
		Integer totalSeatsReported,
		String baseDate
) {
}
