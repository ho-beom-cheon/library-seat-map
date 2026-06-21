package com.libraryseatmap.library.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.libraryseatmap.publicapi.dto.LibraryInfoItem;

@Entity
@Table(name = "libraries")
public class Library {

	@Id
	@Column(name = "library_id", length = 80)
	private String libraryId;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(name = "source_stdg_cd", length = 20)
	private String sourceStdgCd;

	@Column(length = 80)
	private String city;

	@Column(length = 80)
	private String district;

	@Column(name = "library_type", length = 100)
	private String libraryType;

	@Column(length = 500)
	private String address;

	@Column(name = "operator_name", length = 255)
	private String operatorName;

	@Column(length = 80)
	private String phone;

	@Column(name = "homepage_url", length = 1000)
	private String homepageUrl;

	@Column(precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(precision = 10, scale = 7)
	private BigDecimal longitude;

	@Column(name = "closed_info", columnDefinition = "TEXT")
	private String closedInfo;

	@Column(name = "weekday_open_time", length = 20)
	private String weekdayOpenTime;

	@Column(name = "weekday_close_time", length = 20)
	private String weekdayCloseTime;

	@Column(name = "weekend_open_time", length = 20)
	private String weekendOpenTime;

	@Column(name = "weekend_close_time", length = 20)
	private String weekendCloseTime;

	@Column(name = "holiday_open_time", length = 20)
	private String holidayOpenTime;

	@Column(name = "holiday_close_time", length = 20)
	private String holidayCloseTime;

	@Column(name = "total_seats_reported")
	private Integer totalSeatsReported;

	@Column(name = "base_date", length = 20)
	private String baseDate;

	@Column(name = "last_info_synced_at")
	private Instant lastInfoSyncedAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	protected Library() {
	}

	public Library(String libraryId, String name) {
		this.libraryId = libraryId;
		this.name = name;
	}

	public void applyLibraryInfo(LibraryInfoItem item, Instant syncedAt) {
		this.name = item.name();
		this.sourceStdgCd = item.sourceStdgCd();
		this.city = item.city();
		this.district = item.district();
		this.libraryType = item.libraryType();
		this.address = item.address();
		this.operatorName = item.operatorName();
		this.phone = item.phone();
		this.homepageUrl = item.homepageUrl();
		this.latitude = item.latitude();
		this.longitude = item.longitude();
		this.closedInfo = item.closedInfo();
		this.weekdayOpenTime = item.weekdayOpenTime();
		this.weekdayCloseTime = item.weekdayCloseTime();
		this.weekendOpenTime = item.weekendOpenTime();
		this.weekendCloseTime = item.weekendCloseTime();
		this.holidayOpenTime = item.holidayOpenTime();
		this.holidayCloseTime = item.holidayCloseTime();
		this.totalSeatsReported = item.totalSeatsReported();
		this.baseDate = item.baseDate();
		this.lastInfoSyncedAt = syncedAt;
	}

	public void applyRealtimeSeed(String name, String sourceStdgCd) {
		if (name != null && !name.isBlank()) {
			this.name = name;
		}
		if (sourceStdgCd != null && !sourceStdgCd.isBlank()) {
			this.sourceStdgCd = sourceStdgCd;
		}
	}

	public String getLibraryId() {
		return libraryId;
	}

	public String getName() {
		return name;
	}

	public String getSourceStdgCd() {
		return sourceStdgCd;
	}

	public String getCity() {
		return city;
	}

	public String getDistrict() {
		return district;
	}

	public String getLibraryType() {
		return libraryType;
	}

	public String getAddress() {
		return address;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public String getPhone() {
		return phone;
	}

	public String getHomepageUrl() {
		return homepageUrl;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public String getClosedInfo() {
		return closedInfo;
	}

	public String getWeekdayOpenTime() {
		return weekdayOpenTime;
	}

	public String getWeekdayCloseTime() {
		return weekdayCloseTime;
	}

	public String getWeekendOpenTime() {
		return weekendOpenTime;
	}

	public String getWeekendCloseTime() {
		return weekendCloseTime;
	}

	public String getHolidayOpenTime() {
		return holidayOpenTime;
	}

	public String getHolidayCloseTime() {
		return holidayCloseTime;
	}

	public Integer getTotalSeatsReported() {
		return totalSeatsReported;
	}

	public String getBaseDate() {
		return baseDate;
	}

	public Instant getLastInfoSyncedAt() {
		return lastInfoSyncedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
