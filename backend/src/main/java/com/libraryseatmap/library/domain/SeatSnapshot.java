package com.libraryseatmap.library.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "seat_snapshots",
		uniqueConstraints = @UniqueConstraint(name = "uq_seat_snapshots_room_observed", columnNames = {"room_id", "observed_at"})
)
public class SeatSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "library_id", nullable = false, length = 80)
	private String libraryId;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "observed_at", nullable = false)
	private Instant observedAt;

	@Column(name = "collected_at", insertable = false)
	private Instant collectedAt;

	@Column(name = "raw_observed_value", length = 100)
	private String rawObservedValue;

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

	@Column(name = "data_source", nullable = false, length = 50)
	private String dataSource = "PLR_API";

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected SeatSnapshot() {
	}

	public SeatSnapshot(String libraryId, UUID roomId, Instant observedAt, Integer totalSeats, Integer usedSeats,
			Integer availableSeats, BigDecimal usageRate) {
		this.libraryId = libraryId;
		this.roomId = roomId;
		this.observedAt = observedAt;
		this.totalSeats = totalSeats;
		this.usedSeats = usedSeats;
		this.availableSeats = availableSeats;
		this.usageRate = usageRate;
	}

	public SeatSnapshot(String libraryId, UUID roomId, Instant observedAt, String rawObservedValue,
			Integer currentVisitorCount, Integer totalSeats, Integer usedSeats, Integer reservedSeats,
			Integer availableSeats, BigDecimal usageRate) {
		this(libraryId, roomId, observedAt, totalSeats, usedSeats, availableSeats, usageRate);
		this.rawObservedValue = rawObservedValue;
		this.currentVisitorCount = currentVisitorCount;
		this.reservedSeats = reservedSeats;
	}

	public Long getId() {
		return id;
	}

	public String getLibraryId() {
		return libraryId;
	}

	public UUID getRoomId() {
		return roomId;
	}

	public Instant getObservedAt() {
		return observedAt;
	}

	public Instant getCollectedAt() {
		return collectedAt;
	}

	public String getRawObservedValue() {
		return rawObservedValue;
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

	public String getDataSource() {
		return dataSource;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
