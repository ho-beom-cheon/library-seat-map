package com.libraryseatmap.publicapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "library.public-api")
public class PublicLibraryApiProperties {

	private String baseUrl = "https://apis.data.go.kr/B551982/plr_v2";
	private String responseType = "json";
	private String serviceKey = "";
	private boolean keyEncoded;
	private int numOfRows = 500;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = trimTrailingSlash(baseUrl);
	}

	public String getResponseType() {
		return responseType;
	}

	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}

	public String getServiceKey() {
		return serviceKey;
	}

	public void setServiceKey(String serviceKey) {
		this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
	}

	public boolean isKeyEncoded() {
		return keyEncoded;
	}

	public void setKeyEncoded(boolean keyEncoded) {
		this.keyEncoded = keyEncoded;
	}

	public int getNumOfRows() {
		return numOfRows;
	}

	public void setNumOfRows(int numOfRows) {
		this.numOfRows = numOfRows;
	}

	void requireServiceKey() {
		if (serviceKey == null || serviceKey.isBlank()) {
			throw new PublicLibraryApiException("DATA_GO_KR_SERVICE_KEY is required.");
		}
	}

	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return baseUrl;
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
