package com.libraryseatmap.sync;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "library.sync")
public class LibrarySyncProperties {

	private boolean enabled;
	private String infoCron = "0 10 3 * * *";
	private long realtimeDelayMs = 120_000L;
	private boolean operationStatusEnabled;
	private String districts = "1168000000";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getInfoCron() {
		return infoCron;
	}

	public void setInfoCron(String infoCron) {
		this.infoCron = infoCron;
	}

	public long getRealtimeDelayMs() {
		return realtimeDelayMs;
	}

	public void setRealtimeDelayMs(long realtimeDelayMs) {
		this.realtimeDelayMs = realtimeDelayMs;
	}

	public boolean isOperationStatusEnabled() {
		return operationStatusEnabled;
	}

	public void setOperationStatusEnabled(boolean operationStatusEnabled) {
		this.operationStatusEnabled = operationStatusEnabled;
	}

	public String getDistricts() {
		return districts;
	}

	public void setDistricts(String districts) {
		this.districts = districts;
	}

	public List<SyncTargetDistrict> targetDistricts() {
		if (districts == null || districts.isBlank()) {
			return List.of();
		}
		return Arrays.stream(districts.split(","))
				.map(SyncTargetDistrict::parse)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
	}
}
