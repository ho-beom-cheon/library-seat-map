"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";

type MarkerState =
  | "AVAILABLE"
  | "MODERATE"
  | "CROWDED"
  | "FULL_RISK"
  | "FULL"
  | "CLOSED"
  | "NO_DATA"
  | "STALE"
  | string;

type LibraryItem = {
  libraryId: string;
  name: string;
  district: string | null;
  address: string | null;
  lat: number | string | null;
  lng: number | string | null;
  distanceMeters?: number | null;
  estimatedWalkMinutes?: number | null;
  markerState: MarkerState;
  availableSeats: number | null;
  totalSeats: number | null;
  usageRate: number | string | null;
  recommendScore?: number | null;
  lastSyncedAt: string | null;
  dataFreshness: string | null;
  operationStatus?: string | null;
  operationTimeConfidence?: string | null;
  recommendReason?: string | null;
};

type LibraryDetail = LibraryItem & {
  phone: string | null;
  homepageUrl: string | null;
  weekdayOpenTime: string | null;
  weekdayCloseTime: string | null;
  weekendOpenTime: string | null;
  weekendCloseTime: string | null;
  closedInfo: string | null;
  operationStatus: string | null;
  rooms: ReadingRoom[];
};

type ReadingRoom = {
  roomId: string;
  roomName: string;
  floorInfo: string | null;
  totalSeats: number | null;
  usedSeats: number | null;
  availableSeats: number | null;
  markerState: MarkerState;
  dataFreshness: string | null;
  lastSyncedAt: string | null;
};

type NearbyResponse = {
  items: LibraryItem[];
};

type LibraryListResponse = {
  items: LibraryItem[];
  total: number;
};

type RecommendationResponse = {
  items: LibraryItem[];
};

type ViewMode = "nearby" | "district";

type UiState = {
  loading: boolean;
  error: string | null;
  locationDenied: boolean;
  selectedId: string | null;
  detail: LibraryDetail | null;
};

const DEFAULT_DISTRICT = "강남구";
const SEOUL_CENTER = { lat: 37.5665, lng: 126.978 };
const INITIAL_STATE: UiState = {
  loading: true,
  error: null,
  locationDenied: false,
  selectedId: null,
  detail: null,
};

const MARKER_LABELS: Record<string, string> = {
  AVAILABLE: "여유",
  MODERATE: "보통",
  CROWDED: "혼잡",
  FULL_RISK: "만석 임박",
  FULL: "만석",
  CLOSED: "운영 종료",
  NO_DATA: "정보 없음",
  STALE: "갱신 지연",
};

export default function Home() {
  const [items, setItems] = useState<LibraryItem[]>([]);
  const [query, setQuery] = useState(DEFAULT_DISTRICT);
  const [activeQuery, setActiveQuery] = useState(DEFAULT_DISTRICT);
  const [mode, setMode] = useState<ViewMode>("nearby");
  const [position, setPosition] = useState<{ lat: number; lng: number } | null>(
    null,
  );
  const [lastRefresh, setLastRefresh] = useState<Date | null>(null);
  const [ui, setUi] = useState<UiState>(INITIAL_STATE);

  useEffect(() => {
    requestCurrentLocation();
    // Prompt once on first paint; later location and search updates are user-driven.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selectedItem = useMemo(
    () => items.find((item) => item.libraryId === ui.selectedId) ?? null,
    [items, ui.selectedId],
  );

  const mapBounds = useMemo(() => getMapBounds(items, position), [items, position]);

  async function loadLibraries(nextMode: ViewMode, params: URLSearchParams) {
    setUi((current) => ({ ...current, loading: true, error: null }));

    try {
      let nextItems: LibraryItem[];

      if (nextMode === "nearby") {
        const response = await fetchJson<NearbyResponse>(
          `/api/libraries/nearby?${params}`,
        );
        nextItems = response.items ?? [];
      } else {
        const district = params.get("district") ?? DEFAULT_DISTRICT;
        const recommendationParams = new URLSearchParams({
          district,
          minimumStudyMinutes: "60",
          limit: params.get("size") ?? "50",
        });
        const recommendations = await fetchJson<RecommendationResponse>(
          `/api/recommendations/libraries?${recommendationParams}`,
        );

        if (recommendations.items?.length) {
          nextItems = recommendations.items;
        } else {
          const response = await fetchJson<LibraryListResponse>(
            `/api/libraries?${params}`,
          );
          nextItems = response.items ?? [];
        }
      }

      setItems(nextItems);
      setLastRefresh(new Date());
      setUi((current) => ({
        ...current,
        loading: false,
        error: null,
        selectedId: nextItems[0]?.libraryId ?? null,
        detail: null,
      }));
    } catch {
      setItems([]);
      setUi((current) => ({
        ...current,
        loading: false,
        error:
          "도서관 정보를 불러오지 못했습니다. 잠시 후 다시 시도하거나 지역명으로 검색해 주세요.",
        selectedId: null,
        detail: null,
      }));
    }
  }

  function requestCurrentLocation() {
    if (!("geolocation" in navigator)) {
      loadByDistrict(DEFAULT_DISTRICT, true);
      return;
    }

    setMode("nearby");
    setUi((current) => ({
      ...current,
      loading: true,
      error: null,
      locationDenied: false,
    }));

    navigator.geolocation.getCurrentPosition(
      (currentPosition) => {
        const nextPosition = {
          lat: currentPosition.coords.latitude,
          lng: currentPosition.coords.longitude,
        };
        const params = new URLSearchParams({
          lat: String(nextPosition.lat),
          lng: String(nextPosition.lng),
          radiusMeters: "5000",
          sort: "recommend",
          includeNoSeat: "true",
          limit: "30",
        });

        setPosition(nextPosition);
        setMode("nearby");
        loadLibraries("nearby", params);
      },
      () => {
        loadByDistrict(DEFAULT_DISTRICT, true);
      },
      {
        enableHighAccuracy: false,
        maximumAge: 1000 * 60 * 5,
        timeout: 6000,
      },
    );
  }

  function loadByDistrict(district: string, locationDenied = false) {
    const normalizedDistrict = district.trim() || DEFAULT_DISTRICT;
    const params = new URLSearchParams({
      district: normalizedDistrict,
      includeNoSeat: "true",
      onlyWithSeats: "false",
      page: "0",
      size: "50",
    });

    setMode("district");
    setPosition(null);
    setActiveQuery(normalizedDistrict);
    setQuery(normalizedDistrict);
    setUi((current) => ({
      ...current,
      locationDenied,
      error: null,
    }));
    loadLibraries("district", params);
  }

  async function selectLibrary(library: LibraryItem) {
    setUi((current) => ({
      ...current,
      selectedId: library.libraryId,
      detail: null,
    }));

    try {
      const detail = await fetchJson<LibraryDetail>(
        `/api/libraries/${encodeURIComponent(library.libraryId)}`,
      );
      setUi((current) => ({ ...current, detail }));
    } catch {
      setUi((current) => ({ ...current, detail: null }));
    }
  }

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    loadByDistrict(query);
  }

  const panelTitle =
    mode === "nearby" && position
      ? "현재 위치 주변 추천"
      : `${activeQuery} 도서관 검색`;

  const emptyMessage = ui.error
    ? ui.error
    : ui.locationDenied
      ? "위치 권한을 사용할 수 없어 지역 검색 결과를 표시합니다."
      : "검색 조건에 맞는 도서관이 없습니다. 다른 지역명을 입력해 주세요.";

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand">
          <strong>열람석 Now</strong>
          <span>공공도서관 좌석 지도</span>
        </div>
        <form className="search-form" onSubmit={submitSearch}>
          <label className="sr-only" htmlFor="library-search">
            지역 또는 도서관 검색
          </label>
          <input
            id="library-search"
            className="search-input"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="강남구, 종로구처럼 지역명을 입력"
          />
          <button className="search-button" type="submit">
            검색
          </button>
        </form>
        <button
          className="location-button"
          type="button"
          onClick={requestCurrentLocation}
        >
          내 위치
        </button>
      </header>

      <section className="notice-strip" aria-live="polite">
        <span>{noticeText(ui, items)}</span>
        <span>{lastRefresh ? `화면 갱신 ${formatClock(lastRefresh)}` : "갱신 대기"}</span>
      </section>

      <div className="workspace">
        <section className="map-stage" aria-label="도서관 지도 영역">
          <div className="map-canvas">
            <div className="map-grid" />
            <div className="map-water" />
            <div className="map-road road-a" />
            <div className="map-road road-b" />
            <div className="map-road road-c" />

            {position ? (
              <div
                className="user-pin"
                style={{
                  left: `${projectPoint(position.lat, position.lng, mapBounds).x}%`,
                  top: `${projectPoint(position.lat, position.lng, mapBounds).y}%`,
                }}
                aria-label="현재 위치"
              />
            ) : null}

            {items.map((library, index) => {
              const point = getLibraryPoint(library, mapBounds, index);
              const selected = library.libraryId === ui.selectedId;

              return (
                <button
                  className={`library-marker marker-${stateTone(library.markerState)}${
                    selected ? " is-selected" : ""
                  }`}
                  key={library.libraryId}
                  style={{ left: `${point.x}%`, top: `${point.y}%` }}
                  type="button"
                  onClick={() => selectLibrary(library)}
                  aria-label={`${library.name} ${markerLabel(library.markerState)}`}
                >
                  <span>{markerText(library)}</span>
                </button>
              );
            })}

            {ui.loading ? (
              <div className="map-overlay">도서관 정보를 불러오는 중입니다.</div>
            ) : items.length === 0 ? (
              <div className="map-overlay">{emptyMessage}</div>
            ) : null}
          </div>
        </section>

        <aside className="results-panel" aria-label="추천 도서관 목록">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">최근 갱신 기준</p>
              <h1>{panelTitle}</h1>
            </div>
            <span className="result-count">{items.length}곳</span>
          </div>

          {items.length > 0 ? (
            <div className="library-list">
              {items.map((library) => (
                <button
                  className={`library-card${
                    library.libraryId === ui.selectedId ? " is-active" : ""
                  }`}
                  key={library.libraryId}
                  type="button"
                  onClick={() => selectLibrary(library)}
                >
                  <span className={`state-chip chip-${stateTone(library.markerState)}`}>
                    {markerLabel(library.markerState)}
                  </span>
                  <strong>{library.name}</strong>
                  <span className="library-meta">
                    {library.district ?? "지역 정보 없음"}
                    {formatDistance(library.distanceMeters)}
                  </span>
                  <span className="seat-line">{seatSummary(library)}</span>
                  <span className="reason-line">
                    {library.recommendReason ?? freshnessText(library.dataFreshness)}
                  </span>
                </button>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <strong>{ui.loading ? "조회 중" : "표시할 도서관이 없습니다."}</strong>
              <p>{ui.loading ? "잠시만 기다려 주세요." : emptyMessage}</p>
            </div>
          )}
        </aside>
      </div>

      <section className="detail-drawer" aria-label="선택한 도서관 상세 정보">
        {selectedItem ? (
          <LibraryDetailCard item={ui.detail ?? selectedItem} />
        ) : (
          <div className="detail-empty">
            지도 마커나 목록을 선택하면 도서관별 좌석 상태를 볼 수 있습니다.
          </div>
        )}
      </section>
    </main>
  );
}

function LibraryDetailCard({ item }: { item: LibraryItem | LibraryDetail }) {
  const rooms = "rooms" in item ? item.rooms : [];

  return (
    <div className="detail-content">
      <div className="detail-main">
        <span className={`state-chip chip-${stateTone(item.markerState)}`}>
          {markerLabel(item.markerState)}
        </span>
        <div>
          <h2>{item.name}</h2>
          <p>{item.address ?? "주소 정보 없음"}</p>
        </div>
      </div>

      <dl className="detail-stats">
        <div>
          <dt>좌석</dt>
          <dd>{seatSummary(item)}</dd>
        </div>
        <div>
          <dt>도보</dt>
          <dd>{formatWalk(item.estimatedWalkMinutes)}</dd>
        </div>
        <div>
          <dt>갱신</dt>
          <dd>{formatSyncedAt(item.lastSyncedAt)}</dd>
        </div>
      </dl>

      <p className="detail-copy">
        {item.recommendReason ??
          "좌석 상태는 최근 갱신 기준의 가능성 정보이며 실제 현장 상황과 다를 수 있습니다."}
      </p>

      {rooms.length > 0 ? (
        <div className="room-list" aria-label="열람실별 좌석">
          {rooms.slice(0, 4).map((room) => (
            <div className="room-row" key={room.roomId}>
              <span>
                <strong>{room.roomName}</strong>
                {room.floorInfo ? ` · ${room.floorInfo}` : ""}
              </span>
              <span>{seatSummary(room)}</span>
            </div>
          ))}
        </div>
      ) : (
        <p className="room-empty">열람실별 세부 좌석 정보는 아직 표시할 수 없습니다.</p>
      )}
    </div>
  );
}

async function fetchJson<T>(path: string): Promise<T> {
  const response = await fetch(path, {
    headers: { Accept: "application/json" },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

function getMapBounds(
  items: LibraryItem[],
  position: { lat: number; lng: number } | null,
) {
  const points = [
    ...items
      .map((item) => ({ lat: Number(item.lat), lng: Number(item.lng) }))
      .filter((point) => Number.isFinite(point.lat) && Number.isFinite(point.lng)),
    ...(position ? [position] : []),
  ];

  if (points.length === 0) {
    return {
      minLat: SEOUL_CENTER.lat - 0.03,
      maxLat: SEOUL_CENTER.lat + 0.03,
      minLng: SEOUL_CENTER.lng - 0.04,
      maxLng: SEOUL_CENTER.lng + 0.04,
    };
  }

  const lats = points.map((point) => point.lat);
  const lngs = points.map((point) => point.lng);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLng = Math.min(...lngs);
  const maxLng = Math.max(...lngs);
  const latPadding = Math.max((maxLat - minLat) * 0.18, 0.008);
  const lngPadding = Math.max((maxLng - minLng) * 0.18, 0.008);

  return {
    minLat: minLat - latPadding,
    maxLat: maxLat + latPadding,
    minLng: minLng - lngPadding,
    maxLng: maxLng + lngPadding,
  };
}

function getLibraryPoint(item: LibraryItem, bounds: ReturnType<typeof getMapBounds>, index: number) {
  const lat = Number(item.lat);
  const lng = Number(item.lng);

  if (Number.isFinite(lat) && Number.isFinite(lng)) {
    return projectPoint(lat, lng, bounds);
  }

  return {
    x: 18 + (index % 4) * 20,
    y: 24 + Math.floor(index / 4) * 14,
  };
}

function projectPoint(lat: number, lng: number, bounds: ReturnType<typeof getMapBounds>) {
  const lngRange = Math.max(bounds.maxLng - bounds.minLng, 0.0001);
  const latRange = Math.max(bounds.maxLat - bounds.minLat, 0.0001);
  const x = ((lng - bounds.minLng) / lngRange) * 100;
  const y = (1 - (lat - bounds.minLat) / latRange) * 100;

  return {
    x: clamp(x, 6, 94),
    y: clamp(y, 8, 92),
  };
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function markerLabel(markerState: MarkerState) {
  return MARKER_LABELS[markerState] ?? "확인 필요";
}

function markerText(item: LibraryItem) {
  if (item.availableSeats === null || item.availableSeats === undefined) {
    return markerLabel(item.markerState);
  }

  return `${item.availableSeats}`;
}

function stateTone(markerState: MarkerState) {
  if (markerState === "AVAILABLE") {
    return "good";
  }
  if (markerState === "MODERATE") {
    return "normal";
  }
  if (markerState === "CROWDED" || markerState === "FULL_RISK") {
    return "warn";
  }
  if (markerState === "FULL" || markerState === "CLOSED") {
    return "danger";
  }
  return "muted";
}

function seatSummary(item: Pick<LibraryItem | ReadingRoom, "availableSeats" | "totalSeats">) {
  if (item.availableSeats === null || item.availableSeats === undefined) {
    return "좌석 정보 없음";
  }

  if (item.totalSeats === null || item.totalSeats === undefined) {
    return `최근 기준 ${item.availableSeats}석 가능성`;
  }

  return `최근 기준 ${item.availableSeats}/${item.totalSeats}석 가능성`;
}

function noticeText(ui: UiState, items: LibraryItem[]) {
  if (ui.locationDenied) {
    return "위치 권한이 없어 지역 검색으로 전환했습니다.";
  }
  if (ui.error) {
    return "API 연결 상태를 확인해 주세요.";
  }
  if (items.some((item) => item.dataFreshness === "STALE")) {
    return "일부 도서관은 갱신 지연 상태입니다.";
  }
  return "좌석 상태는 보장 정보가 아니라 최근 갱신 기준의 가능성 정보입니다.";
}

function freshnessText(dataFreshness: string | null) {
  if (dataFreshness === "STALE") {
    return "갱신 지연 데이터입니다.";
  }
  if (dataFreshness === "NO_DATA") {
    return "좌석 데이터가 아직 없습니다.";
  }
  return "최근 갱신 기준으로 표시합니다.";
}

function formatDistance(distanceMeters?: number | null) {
  if (distanceMeters === null || distanceMeters === undefined) {
    return "";
  }
  if (distanceMeters >= 1000) {
    return ` · ${(distanceMeters / 1000).toFixed(1)}km`;
  }
  return ` · ${distanceMeters}m`;
}

function formatWalk(minutes?: number | null) {
  if (minutes === null || minutes === undefined) {
    return "거리 정보 없음";
  }
  return `약 ${minutes}분`;
}

function formatSyncedAt(value: string | null) {
  if (!value) {
    return "갱신 정보 없음";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatClock(value: Date) {
  return new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(value);
}
