package com.libraryseatmap.library.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_seat_latest")
public class RoomSeatLatest {

	@Id
	@Column(name = "room_id")
	private UUID roomId;

	@Column(name = "library_id", nullable = false, length = 80)
	private String libraryId;

	@Column(name = "observed_at", nullable = false)
	private Instant observedAt;

	@Column(name = "collected_at", nullable = false)
	private Instant collectedAt;

	@Column(name = "current_visitor_count")
	private Integer currentVisitorCount;

	@Column(name = "total_seats", nullable = false)
	private Integer totalSeats;

	@Column(name = "used_seats", nullable = false)
	private Integer usedSeats;

	@Column(name = "reserved_seats", nullable = false)
	private Integer reservedSeats = 0;

	@Column(name = "available_seats", nullable = false)
	private Integer availableSeats;

	@Column(name = "usage_rate", nullable = false, precision = 5, scale = 4)
	private BigDecimal usageRate;

	@Column(name = "freshness_status", nullable = false, length = 30)
	private String freshnessStatus;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	protected RoomSeatLatest() {
	}

	public RoomSeatLatest(UUID roomId, String libraryId, Instant observedAt, Instant collectedAt, Integer totalSeats,
			Integer usedSeats, Integer availableSeats, BigDecimal usageRate, String freshnessStatus) {
		this.roomId = roomId;
		this.libraryId = libraryId;
		this.observedAt = observedAt;
		this.collectedAt = collectedAt;
		this.totalSeats = totalSeats;
		this.usedSeats = usedSeats;
		this.availableSeats = availableSeats;
		this.usageRate = usageRate;
		this.freshnessStatus = freshnessStatus;
	}

	public void updateSeatState(Instant observedAt, Instant collectedAt, Integer currentVisitorCount,
			Integer totalSeats, Integer usedSeats, Integer reservedSeats, Integer availableSeats,
			BigDecimal usageRate, String freshnessStatus) {
		this.observedAt = observedAt;
		this.collectedAt = collectedAt;
		this.currentVisitorCount = currentVisitorCount;
		this.totalSeats = totalSeats;
		this.usedSeats = usedSeats;
		this.reservedSeats = reservedSeats;
		this.availableSeats = availableSeats;
		this.usageRate = usageRate;
		this.freshnessStatus = freshnessStatus;
	}

	public UUID getRoomId() {
		return roomId;
	}

	public String getLibraryId() {
		return libraryId;
	}

	public Instant getObservedAt() {
		return observedAt;
	}

	public Instant getCollectedAt() {
		return collectedAt;
	}

	public Integer getCurrentVisitorCount() {
		return currentVisitorCount;
	}

	public Integer getTotalSeats() {
		return totalSeats;
	}

	public Integer getUsedSeats() {
		return usedSeats;
	}

	public Integer getReservedSeats() {
		return reservedSeats;
	}

	public Integer getAvailableSeats() {
		return availableSeats;
	}

	public BigDecimal getUsageRate() {
		return usageRate;
	}

	public String getFreshnessStatus() {
		return freshnessStatus;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
