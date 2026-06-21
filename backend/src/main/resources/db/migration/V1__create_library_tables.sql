CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS libraries (
    library_id              VARCHAR(80) PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL,
    source_stdg_cd          VARCHAR(20),
    city                    VARCHAR(80),
    district                VARCHAR(80),
    library_type            VARCHAR(100),
    address                 VARCHAR(500),
    operator_name           VARCHAR(255),
    phone                   VARCHAR(80),
    homepage_url            VARCHAR(1000),
    latitude                NUMERIC(10, 7),
    longitude               NUMERIC(10, 7),
    location                GEOGRAPHY(POINT, 4326),
    closed_info             TEXT,
    weekday_open_time       VARCHAR(20),
    weekday_close_time      VARCHAR(20),
    weekend_open_time       VARCHAR(20),
    weekend_close_time      VARCHAR(20),
    holiday_open_time       VARCHAR(20),
    holiday_close_time      VARCHAR(20),
    total_seats_reported    INTEGER,
    base_date               VARCHAR(20),
    last_info_synced_at     TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_libraries_lat CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_libraries_lng CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE TABLE IF NOT EXISTS reading_rooms (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    library_id              VARCHAR(80) NOT NULL REFERENCES libraries(library_id) ON DELETE CASCADE,
    room_external_id        VARCHAR(100) NOT NULL,
    room_no                 VARCHAR(50),
    room_name               VARCHAR(255) NOT NULL,
    room_type               VARCHAR(100),
    floor_info              VARCHAR(100),
    source_stdg_cd          VARCHAR(20),
    total_seats             INTEGER,
    last_synced_at          TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reading_rooms_source UNIQUE (library_id, room_external_id),
    CONSTRAINT chk_reading_rooms_total CHECK (total_seats IS NULL OR total_seats >= 0)
);

CREATE TABLE IF NOT EXISTS seat_snapshots (
    id                      BIGSERIAL PRIMARY KEY,
    library_id              VARCHAR(80) NOT NULL REFERENCES libraries(library_id) ON DELETE CASCADE,
    room_id                 UUID NOT NULL REFERENCES reading_rooms(id) ON DELETE CASCADE,
    observed_at             TIMESTAMPTZ NOT NULL,
    collected_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    raw_observed_value      VARCHAR(100),
    current_visitor_count   INTEGER,
    total_seats             INTEGER NOT NULL,
    used_seats              INTEGER NOT NULL,
    reserved_seats          INTEGER NOT NULL DEFAULT 0,
    available_seats         INTEGER NOT NULL,
    usage_rate              NUMERIC(5, 4) NOT NULL,
    data_source             VARCHAR(50) NOT NULL DEFAULT 'PLR_API',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_seat_snapshots_room_observed UNIQUE (room_id, observed_at),
    CONSTRAINT chk_seat_snapshot_counts CHECK (
        total_seats >= 0 AND used_seats >= 0 AND reserved_seats >= 0 AND available_seats >= 0
    ),
    CONSTRAINT chk_seat_snapshot_usage CHECK (usage_rate >= 0 AND usage_rate <= 1.5)
);

CREATE TABLE IF NOT EXISTS room_seat_latest (
    room_id                 UUID PRIMARY KEY REFERENCES reading_rooms(id) ON DELETE CASCADE,
    library_id              VARCHAR(80) NOT NULL REFERENCES libraries(library_id) ON DELETE CASCADE,
    observed_at             TIMESTAMPTZ NOT NULL,
    collected_at            TIMESTAMPTZ NOT NULL,
    current_visitor_count   INTEGER,
    total_seats             INTEGER NOT NULL,
    used_seats              INTEGER NOT NULL,
    reserved_seats          INTEGER NOT NULL DEFAULT 0,
    available_seats         INTEGER NOT NULL,
    usage_rate              NUMERIC(5, 4) NOT NULL,
    freshness_status        VARCHAR(30) NOT NULL,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_room_latest_counts CHECK (
        total_seats >= 0 AND used_seats >= 0 AND reserved_seats >= 0 AND available_seats >= 0
    )
);

CREATE TABLE IF NOT EXISTS sync_logs (
    id                      BIGSERIAL PRIMARY KEY,
    provider                VARCHAR(50) NOT NULL DEFAULT 'PLR_API',
    endpoint                VARCHAR(100) NOT NULL,
    stdg_cd                 VARCHAR(20),
    district                VARCHAR(80),
    status                  VARCHAR(30) NOT NULL,
    http_status             INTEGER,
    result_code             VARCHAR(100),
    result_message          TEXT,
    row_count               INTEGER NOT NULL DEFAULT 0,
    started_at              TIMESTAMPTZ NOT NULL,
    finished_at             TIMESTAMPTZ NOT NULL,
    duration_ms             INTEGER,
    error_message           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_libraries_updated_at ON libraries;
CREATE TRIGGER trg_libraries_updated_at
BEFORE UPDATE ON libraries
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_reading_rooms_updated_at ON reading_rooms;
CREATE TRIGGER trg_reading_rooms_updated_at
BEFORE UPDATE ON reading_rooms
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_room_seat_latest_updated_at ON room_seat_latest;
CREATE TRIGGER trg_room_seat_latest_updated_at
BEFORE UPDATE ON room_seat_latest
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION set_library_location()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.location = ST_SetSRID(ST_MakePoint(NEW.longitude::double precision, NEW.latitude::double precision), 4326)::geography;
    ELSE
        NEW.location = NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_libraries_location ON libraries;
CREATE TRIGGER trg_libraries_location
BEFORE INSERT OR UPDATE OF latitude, longitude ON libraries
FOR EACH ROW EXECUTE FUNCTION set_library_location();
