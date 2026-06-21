CREATE INDEX IF NOT EXISTS idx_libraries_district ON libraries(district);
CREATE INDEX IF NOT EXISTS idx_libraries_source_stdg_cd ON libraries(source_stdg_cd);
CREATE INDEX IF NOT EXISTS idx_libraries_location ON libraries USING GIST(location);

CREATE INDEX IF NOT EXISTS idx_reading_rooms_library_id ON reading_rooms(library_id);
CREATE INDEX IF NOT EXISTS idx_reading_rooms_room_type ON reading_rooms(room_type);

CREATE INDEX IF NOT EXISTS idx_seat_snapshots_room_time ON seat_snapshots(room_id, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_seat_snapshots_library_time ON seat_snapshots(library_id, observed_at DESC);
CREATE INDEX IF NOT EXISTS idx_seat_snapshots_collected_at ON seat_snapshots(collected_at DESC);

CREATE INDEX IF NOT EXISTS idx_room_seat_latest_library_id ON room_seat_latest(library_id);
CREATE INDEX IF NOT EXISTS idx_room_seat_latest_freshness ON room_seat_latest(freshness_status);
CREATE INDEX IF NOT EXISTS idx_room_seat_latest_available ON room_seat_latest(available_seats DESC);

CREATE INDEX IF NOT EXISTS idx_sync_logs_endpoint_time ON sync_logs(endpoint, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_sync_logs_status_time ON sync_logs(status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_sync_logs_stdg_time ON sync_logs(stdg_cd, started_at DESC);
