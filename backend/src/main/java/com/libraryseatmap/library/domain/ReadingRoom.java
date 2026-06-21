package com.libraryseatmap.library.domain;

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
		name = "reading_rooms",
		uniqueConstraints = @UniqueConstraint(name = "uq_reading_rooms_source", columnNames = {"library_id", "room_external_id"})
)
public class ReadingRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "library_id", nullable = false, length = 80)
	private String libraryId;

	@Column(name = "room_external_id", nullable = false, length = 100)
	private String roomExternalId;

	@Column(name = "room_no", length = 50)
	private String roomNo;

	@Column(name = "room_name", nullable = false, length = 255)
	private String roomName;

	@Column(name = "room_type", length = 100)
	private String roomType;

	@Column(name = "floor_info", length = 100)
	private String floorInfo;

	@Column(name = "source_stdg_cd", length = 20)
	private String sourceStdgCd;

	@Column(name = "total_seats")
	private Integer totalSeats;

	@Column(name = "last_synced_at")
	private Instant lastSyncedAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	protected ReadingRoom() {
	}

	public ReadingRoom(String libraryId, String roomExternalId, String roomName) {
		this.libraryId = libraryId;
		this.roomExternalId = roomExternalId;
		this.roomName = roomName;
	}

	public UUID getId() {
		return id;
	}

	public String getLibraryId() {
		return libraryId;
	}

	public String getRoomExternalId() {
		return roomExternalId;
	}

	public String getRoomNo() {
		return roomNo;
	}

	public String getRoomName() {
		return roomName;
	}

	public String getRoomType() {
		return roomType;
	}

	public String getFloorInfo() {
		return floorInfo;
	}

	public String getSourceStdgCd() {
		return sourceStdgCd;
	}

	public Integer getTotalSeats() {
		return totalSeats;
	}

	public Instant getLastSyncedAt() {
		return lastSyncedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
