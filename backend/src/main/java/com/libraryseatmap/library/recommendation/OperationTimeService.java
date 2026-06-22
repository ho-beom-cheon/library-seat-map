package com.libraryseatmap.library.recommendation;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.libraryseatmap.library.domain.Library;

@Service
public class OperationTimeService {

	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
	private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})");
	private static final Map<DayOfWeek, List<String>> CLOSED_DAY_TOKENS = Map.of(
			DayOfWeek.MONDAY, List.of("월요일", "(월)", "Monday", "(Mon)"),
			DayOfWeek.TUESDAY, List.of("화요일", "(화)", "Tuesday", "(Tue)"),
			DayOfWeek.WEDNESDAY, List.of("수요일", "(수)", "Wednesday", "(Wed)"),
			DayOfWeek.THURSDAY, List.of("목요일", "(목)", "Thursday", "(Thu)"),
			DayOfWeek.FRIDAY, List.of("금요일", "(금)", "Friday", "(Fri)"),
			DayOfWeek.SATURDAY, List.of("토요일", "(토)", "Saturday", "(Sat)"),
			DayOfWeek.SUNDAY, List.of("일요일", "(일)", "Sunday", "(Sun)")
	);

	private final Clock clock;

	public OperationTimeService() {
		this(Clock.systemUTC());
	}

	OperationTimeService(Clock clock) {
		this.clock = clock;
	}

	public OperationTimeResult evaluate(Library library, Integer estimatedWalkMinutes, int minimumStudyMinutes) {
		LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), SEOUL_ZONE);
		LocalDateTime arrivalAt = now.plusMinutes(Math.max(estimatedWalkMinutes == null ? 0 : estimatedWalkMinutes, 0));
		int safeMinimumStudyMinutes = Math.max(minimumStudyMinutes, 0);

		if (isClosedToday(library.getClosedInfo(), now.getDayOfWeek())) {
			return OperationTimeResult.closed(OperationStatus.CLOSED);
		}

		TimeRange timeRange = timeRange(library, now.getDayOfWeek());
		if (timeRange == null) {
			return OperationTimeResult.unknown();
		}

		LocalDate operationDate = operationDate(now, timeRange);
		LocalDateTime openAt = LocalDateTime.of(operationDate, timeRange.openTime());
		LocalDateTime closeAt = LocalDateTime.of(operationDate, timeRange.closeTime());
		if (!closeAt.isAfter(openAt)) {
			closeAt = closeAt.plusDays(1);
		}

		if (arrivalAt.isBefore(openAt) || !arrivalAt.isBefore(closeAt)) {
			return OperationTimeResult.closed(OperationStatus.CLOSED);
		}

		long remainingMinutes = Duration.between(arrivalAt, closeAt).toMinutes();
		if (remainingMinutes < safeMinimumStudyMinutes) {
			return new OperationTimeResult(OperationStatus.CLOSING_SOON, OperationTimeConfidence.HIGH,
					remainingMinutes, timeScore(remainingMinutes), true);
		}
		return new OperationTimeResult(OperationStatus.OPEN, OperationTimeConfidence.HIGH,
				remainingMinutes, timeScore(remainingMinutes), false);
	}

	private LocalDate operationDate(LocalDateTime now, TimeRange timeRange) {
		if (timeRange.closeTime().isAfter(timeRange.openTime())) {
			return now.toLocalDate();
		}
		if (now.toLocalTime().isBefore(timeRange.closeTime())) {
			return now.toLocalDate().minusDays(1);
		}
		return now.toLocalDate();
	}

	private TimeRange timeRange(Library library, DayOfWeek dayOfWeek) {
		boolean weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
		String open = weekend ? library.getWeekendOpenTime() : library.getWeekdayOpenTime();
		String close = weekend ? library.getWeekendCloseTime() : library.getWeekdayCloseTime();
		LocalTime openTime = parseTime(open);
		LocalTime closeTime = parseTime(close);
		if (openTime == null || closeTime == null) {
			return null;
		}
		return new TimeRange(openTime, closeTime);
	}

	private LocalTime parseTime(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		Matcher matcher = TIME_PATTERN.matcher(value);
		if (!matcher.find()) {
			return null;
		}
		int hour = Integer.parseInt(matcher.group(1));
		int minute = Integer.parseInt(matcher.group(2));
		if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
			return null;
		}
		return LocalTime.of(hour, minute);
	}

	private boolean isClosedToday(String closedInfo, DayOfWeek dayOfWeek) {
		if (closedInfo == null || closedInfo.isBlank()) {
			return false;
		}
		if (closedInfo.contains("연중무휴") || closedInfo.contains("없음")) {
			return false;
		}
		return CLOSED_DAY_TOKENS.getOrDefault(dayOfWeek, List.of()).stream()
				.anyMatch(closedInfo::contains);
	}

	private int timeScore(long remainingMinutes) {
		if (remainingMinutes >= 180) {
			return 15;
		}
		if (remainingMinutes >= 120) {
			return 12;
		}
		if (remainingMinutes >= 60) {
			return 8;
		}
		if (remainingMinutes >= 30) {
			return 3;
		}
		return 0;
	}

	private record TimeRange(LocalTime openTime, LocalTime closeTime) {
	}

	public enum OperationStatus {
		OPEN,
		CLOSED,
		CLOSING_SOON,
		UNKNOWN
	}

	public enum OperationTimeConfidence {
		HIGH,
		LOW
	}

	public record OperationTimeResult(
			OperationStatus status,
			OperationTimeConfidence confidence,
			Long remainingMinutes,
			int timeScore,
			boolean excluded
	) {
		private static OperationTimeResult closed(OperationStatus status) {
			return new OperationTimeResult(status, OperationTimeConfidence.HIGH, 0L, 0, true);
		}

		private static OperationTimeResult unknown() {
			return new OperationTimeResult(OperationStatus.UNKNOWN, OperationTimeConfidence.LOW, null, 0, false);
		}
	}
}
