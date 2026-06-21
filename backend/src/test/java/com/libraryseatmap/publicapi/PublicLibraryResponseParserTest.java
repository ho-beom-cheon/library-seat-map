package com.libraryseatmap.publicapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libraryseatmap.publicapi.dto.LibraryInfoItem;
import com.libraryseatmap.publicapi.dto.PublicLibraryApiPage;
import com.libraryseatmap.publicapi.dto.RealtimeReadingRoomItem;

class PublicLibraryResponseParserTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final PublicLibraryResponseParser parser = new PublicLibraryResponseParser(new PublicLibraryDateTimeParser());

	@Test
	void parsesLibraryInfoItems() throws Exception {
		JsonNode root = objectMapper.readTree("""
				{
				  "response": {
				    "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
				    "body": {
				      "totalCount": "1",
				      "items": {
				        "item": [{
				          "pblibId": "LIB001",
				          "pblibNm": "강남도서관",
				          "stdgCd": "1168000000",
				          "ctpvNm": "서울특별시",
				          "sggNm": "강남구",
				          "pblibRoadNmAddr": "서울특별시 강남구",
				          "lat": "37.5000000",
				          "lot": 127.0000000,
				          "tseatCnt": "120",
				          "totCrtrYmd": "20260621"
				        }]
				      }
				    }
				  }
				}
				""");

		PublicLibraryApiPage<LibraryInfoItem> page = parser.parse(root, parser::toLibraryInfoItem);

		assertThat(page.header().resultCode()).isEqualTo("00");
		assertThat(page.totalCount()).isEqualTo(1);
		assertThat(page.items()).hasSize(1);
		LibraryInfoItem item = page.items().get(0);
		assertThat(item.libraryId()).isEqualTo("LIB001");
		assertThat(item.name()).isEqualTo("강남도서관");
		assertThat(item.latitude()).isEqualByComparingTo(new BigDecimal("37.5000000"));
		assertThat(item.longitude()).isEqualByComparingTo(new BigDecimal("127.0000000"));
		assertThat(item.totalSeatsReported()).isEqualTo(120);
	}

	@Test
	void parsesRealtimeRoomSingleItem() throws Exception {
		JsonNode root = objectMapper.readTree("""
				{
				  "response": {
				    "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
				    "body": {
				      "totalCount": 1,
				      "items": {
				        "item": {
				          "stdgCd": "1168000000",
				          "pblibId": "LIB001",
				          "pblibNm": "강남도서관",
				          "rdrmId": "ROOM001",
				          "rdrmNo": "1",
				          "rdrmNm": "일반열람실",
				          "rdrmTypeNm": "일반",
				          "bldgFlrExpln": "3층",
				          "nowVstrCnt": "34",
				          "tseatCnt": "120",
				          "useSeatCnt": "78",
				          "rsvtSeatCnt": "0",
				          "rmndSeatCnt": "42",
				          "totDt": "20260621143200"
				        }
				      }
				    }
				  }
				}
				""");

		PublicLibraryApiPage<RealtimeReadingRoomItem> page = parser.parse(root, parser::toRealtimeReadingRoomItem);

		assertThat(page.items()).hasSize(1);
		RealtimeReadingRoomItem item = page.items().get(0);
		assertThat(item.libraryId()).isEqualTo("LIB001");
		assertThat(item.roomExternalId()).isEqualTo("ROOM001");
		assertThat(item.roomName()).isEqualTo("일반열람실");
		assertThat(item.availableSeats()).isEqualTo(42);
		assertThat(item.observedAt()).contains(Instant.parse("2026-06-21T05:32:00Z"));
	}

	@Test
	void returnsEmptyItemsWhenItemNodeIsMissing() throws Exception {
		JsonNode root = objectMapper.readTree("""
				{"response": {"header": {"resultCode": "00"}, "body": {"items": {}}}}
				""");

		PublicLibraryApiPage<LibraryInfoItem> page = parser.parse(root, parser::toLibraryInfoItem);

		assertThat(page.items()).isEmpty();
	}
}
