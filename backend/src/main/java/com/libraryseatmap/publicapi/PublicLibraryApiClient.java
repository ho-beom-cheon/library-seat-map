package com.libraryseatmap.publicapi;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.libraryseatmap.publicapi.dto.LibraryInfoItem;
import com.libraryseatmap.publicapi.dto.PublicLibraryApiPage;
import com.libraryseatmap.publicapi.dto.RealtimeReadingRoomItem;

@Component
public class PublicLibraryApiClient {

	private static final String INFO_ENDPOINT = "/info_v2";
	private static final String REALTIME_ROOM_ENDPOINT = "/rlt_rdrm_info_v2";
	private static final String OPERATION_STATUS_ENDPOINT = "/prst_info_v2";

	private final RestClient restClient;
	private final PublicLibraryApiProperties properties;
	private final PublicLibraryResponseParser parser;

	public PublicLibraryApiClient(RestClient.Builder restClientBuilder, PublicLibraryApiProperties properties,
			PublicLibraryResponseParser parser) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
		this.parser = parser;
	}

	public PublicLibraryApiPage<LibraryInfoItem> fetchLibraries(String stdgCd) {
		JsonNode response = get(INFO_ENDPOINT, stdgCd);
		return parser.parse(response, parser::toLibraryInfoItem);
	}

	public PublicLibraryApiPage<RealtimeReadingRoomItem> fetchRealtimeRooms(String stdgCd) {
		JsonNode response = get(REALTIME_ROOM_ENDPOINT, stdgCd);
		return parser.parse(response, parser::toRealtimeReadingRoomItem);
	}

	public JsonNode fetchOperationStatus(String stdgCd) {
		return get(OPERATION_STATUS_ENDPOINT, stdgCd);
	}

	private JsonNode get(String endpoint, String stdgCd) {
		properties.requireServiceKey();

		return restClient.get()
				.uri(buildUri(endpoint, stdgCd))
				.retrieve()
				.body(JsonNode.class);
	}

	URI buildUri(String endpoint, String stdgCd) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getBaseUrl() + endpoint);

		builder.query("serviceKey=" + serviceKeyQueryValue());

		builder.queryParam("pageNo", 1)
				.queryParam("numOfRows", properties.getNumOfRows())
				.queryParam("type", properties.getResponseType())
				.queryParam("stdgCd", stdgCd);

		return builder.build(true).toUri();
	}

	private String serviceKeyQueryValue() {
		if (properties.isKeyEncoded()) {
			return properties.getServiceKey();
		}
		return URLEncoder.encode(properties.getServiceKey(), StandardCharsets.UTF_8).replace("+", "%20");
	}
}
