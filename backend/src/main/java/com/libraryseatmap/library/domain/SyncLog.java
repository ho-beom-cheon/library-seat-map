package com.libraryseatmap.library.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sync_logs")
public class SyncLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String provider = "PLR_API";

	@Column(nullable = false, length = 100)
	private String endpoint;

	@Column(name = "stdg_cd", length = 20)
	private String stdgCd;

	@Column(length = 80)
	private String district;

	@Column(nullable = false, length = 30)
	private String status;

	@Column(name = "http_status")
	private Integer httpStatus;

	@Column(name = "result_code", length = 100)
	private String resultCode;

	@Column(name = "result_message", columnDefinition = "TEXT")
	private String resultMessage;

	@Column(name = "row_count", nullable = false)
	private Integer rowCount = 0;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at", nullable = false)
	private Instant finishedAt;

	@Column(name = "duration_ms")
	private Integer durationMs;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected SyncLog() {
	}

	public SyncLog(String endpoint, String status, Instant startedAt, Instant finishedAt) {
		this.endpoint = endpoint;
		this.status = status;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
	}

	public Long getId() {
		return id;
	}

	public String getProvider() {
		return provider;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getStdgCd() {
		return stdgCd;
	}

	public String getDistrict() {
		return district;
	}

	public String getStatus() {
		return status;
	}

	public Integer getHttpStatus() {
		return httpStatus;
	}

	public String getResultCode() {
		return resultCode;
	}

	public String getResultMessage() {
		return resultMessage;
	}

	public Integer getRowCount() {
		return rowCount;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

	public Integer getDurationMs() {
		return durationMs;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
