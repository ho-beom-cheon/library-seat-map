package com.libraryseatmap.publicapi;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.libraryseatmap.publicapi.dto.LibraryInfoItem;
import com.libraryseatmap.publicapi.dto.PublicLibraryApiHeader;
import com.libraryseatmap.publicapi.dto.PublicLibraryApiPage;
import com.libraryseatmap.publicapi.dto.RealtimeReadingRoomItem;

@Component
public class PublicLibraryResponseParser {

	private final PublicLibraryDateTimeParser dateTimeParser;

	public PublicLibraryResponseParser(PublicLibraryDateTimeParser dateTimeParser) {
		this.dateTimeParser = dateTimeParser;
	}

	public <T> PublicLibraryApiPage<T> parse(JsonNode root, Function<JsonNode, T> itemMapper) {
		PublicLibraryApiHeader header = extractHeader(root);
		List<T> items = extractItems(root).stream().map(itemMapper).toList();
		return new PublicLibraryApiPage<>(header, items, totalCount(root));
	}

	public PublicLibraryApiHeader extractHeader(JsonNode root) {
		JsonNode header = root.path("response").path("header");
		return new PublicLibraryApiHeader(text(header, "resultCode"), text(header, "resultMsg"));
	}

	public List<JsonNode> extractItems(JsonNode root) {
		JsonNode items = root.path("response").path("body").path("items").path("item");
		if (items.isMissingNode() || items.isNull()) {
			return List.of();
		}
		if (items.isArray()) {
			List<JsonNode> result = new ArrayList<>();
			items.forEach(result::add);
			return result;
		}
		if (items.isObject()) {
			return List.of(items);
		}
		return List.of();
	}

	public LibraryInfoItem toLibraryInfoItem(JsonNode item) {
		return new LibraryInfoItem(
				text(item, "pblibId"),
				text(item, "pblibNm"),
				text(item, "stdgCd"),
				text(item, "ctpvNm"),
				text(item, "sggNm"),
				text(item, "pblibTypeNm"),
				text(item, "pblibRoadNmAddr"),
				text(item, "operInstNm"),
				text(item, "pblibTelno"),
				text(item, "siteUrlAddr"),
				decimal(item, "lat"),
				decimal(item, "lot"),
				text(item, "clsrInfoExpln"),
				text(item, "wkdyOperBgngTm"),
				text(item, "wkdyOperEndTm"),
				text(item, "wkndOperBgngTm"),
				text(item, "wkndOperEndTm"),
				text(item, "lhldyOperBgngTm"),
				text(item, "lhldyOperEndTm"),
				integer(item, "tseatCnt"),
				text(item, "totCrtrYmd")
		);
	}

	public RealtimeReadingRoomItem toRealtimeReadingRoomItem(JsonNode item) {
		String rawObservedAt = text(item, "totDt");
		Optional<Instant> observedAt = dateTimeParser.parse(rawObservedAt);
		return new RealtimeReadingRoomItem(
				text(item, "stdgCd"),
				text(item, "pblibId"),
				text(item, "pblibNm"),
				text(item, "rdrmId"),
				text(item, "rdrmNo"),
				text(item, "rdrmNm"),
				text(item, "rdrmTypeNm"),
				text(item, "bldgFlrExpln"),
				integer(item, "nowVstrCnt"),
				integer(item, "tseatCnt"),
				integer(item, "useSeatCnt"),
				integer(item, "rsvtSeatCnt"),
				integer(item, "rmndSeatCnt"),
				rawObservedAt,
				observedAt
		);
	}

	private Integer totalCount(JsonNode root) {
		JsonNode totalCount = root.path("response").path("body").path("totalCount");
		if (totalCount.isMissingNode() || totalCount.isNull()) {
			return null;
		}
		if (totalCount.isNumber()) {
			return totalCount.asInt();
		}
		try {
			return Integer.valueOf(totalCount.asText());
		}
		catch (NumberFormatException ignored) {
			return null;
		}
	}

	private String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		if (value.isMissingNode() || value.isNull()) {
			return null;
		}
		String text = value.asText();
		return text.isBlank() ? null : text.trim();
	}

	private Integer integer(JsonNode node, String field) {
		String value = text(node, field);
		if (value == null) {
			return null;
		}
		try {
			return Integer.valueOf(value);
		}
		catch (NumberFormatException ignored) {
			return null;
		}
	}

	private BigDecimal decimal(JsonNode node, String field) {
		String value = text(node, field);
		if (value == null) {
			return null;
		}
		try {
			return new BigDecimal(value);
		}
		catch (NumberFormatException ignored) {
			return null;
		}
	}
}
