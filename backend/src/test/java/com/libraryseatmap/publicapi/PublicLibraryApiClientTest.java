package com.libraryseatmap.publicapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PublicLibraryApiClientTest {

	@Test
	void buildUriEncodesRawServiceKey() {
		PublicLibraryApiProperties properties = properties("abc+123/==", false);
		PublicLibraryApiClient client = client(properties);

		URI uri = client.buildUri("/info_v2", "1168000000");

		assertThat(uri.toString()).contains("serviceKey=abc%2B123%2F%3D%3D");
		assertThat(uri.toString()).contains("stdgCd=1168000000");
		assertThat(uri.toString()).contains("type=json");
	}

	@Test
	void buildUriPreservesEncodedServiceKey() {
		PublicLibraryApiProperties properties = properties("abc%2B123%2F%3D%3D", true);
		PublicLibraryApiClient client = client(properties);

		URI uri = client.buildUri("/rlt_rdrm_info_v2", "1168000000");

		assertThat(uri.toString()).contains("serviceKey=abc%2B123%2F%3D%3D");
		assertThat(uri.toString()).contains("/rlt_rdrm_info_v2");
	}

	private PublicLibraryApiClient client(PublicLibraryApiProperties properties) {
		PublicLibraryDateTimeParser dateTimeParser = new PublicLibraryDateTimeParser();
		PublicLibraryResponseParser parser = new PublicLibraryResponseParser(dateTimeParser);
		return new PublicLibraryApiClient(RestClient.builder(), properties, parser);
	}

	private PublicLibraryApiProperties properties(String serviceKey, boolean encoded) {
		PublicLibraryApiProperties properties = new PublicLibraryApiProperties();
		properties.setBaseUrl("https://apis.data.go.kr/B551982/plr_v2/");
		properties.setResponseType("json");
		properties.setServiceKey(serviceKey);
		properties.setKeyEncoded(encoded);
		properties.setNumOfRows(500);
		return properties;
	}
}
