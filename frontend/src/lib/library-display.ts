export type MarkerState =
  | "AVAILABLE"
  | "MODERATE"
  | "CROWDED"
  | "FULL_RISK"
  | "FULL"
  | "CLOSED"
  | "NO_DATA"
  | "STALE"
  | string;

export type LibraryItem = {
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

export type LibraryDetail = LibraryItem & {
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

export type ReadingRoom = {
  roomId: string;
  roomName: string;
  roomType: string | null;
  floorInfo: string | null;
  totalSeats: number | null;
  usedSeats: number | null;
  reservedSeats: number | null;
  availableSeats: number | null;
  usageRate: number | string | null;
  markerState: MarkerState;
  dataFreshness: string | null;
  observedAt: string | null;
  lastSyncedAt: string | null;
};

export type UiState = {
  loading: boolean;
  error: string | null;
  locationDenied: boolean;
  selectedId: string | null;
  detail: LibraryDetail | null;
};

export type MapBounds = {
  minLat: number;
  maxLat: number;
  minLng: number;
  maxLng: number;
};

export const DEFAULT_DISTRICT = "강남구";
export const SEOUL_CENTER = { lat: 37.5665, lng: 126.978 };

export const MARKER_LABELS: Record<string, string> = {
  AVAILABLE: "여유",
  MODERATE: "보통",
  CROWDED: "혼잡",
  FULL_RISK: "만석 임박",
  FULL: "만석",
  CLOSED: "운영 종료",
  NO_DATA: "정보 없음",
  STALE: "갱신 지연",
};

export function getMapBounds(
  items: LibraryItem[],
  position: { lat: number; lng: number } | null,
): MapBounds {
  const points = [
    ...items
      .map((item) => ({
        lat: toFiniteCoordinate(item.lat),
        lng: toFiniteCoordinate(item.lng),
      }))
      .filter((point): point is { lat: number; lng: number } => {
        return point.lat !== null && point.lng !== null;
      }),
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

export function getLibraryPoint(item: LibraryItem, bounds: MapBounds, index: number) {
  const lat = toFiniteCoordinate(item.lat);
  const lng = toFiniteCoordinate(item.lng);

  if (lat !== null && lng !== null) {
    return projectPoint(lat, lng, bounds);
  }

  return {
    x: 18 + (index % 4) * 20,
    y: 24 + Math.floor(index / 4) * 14,
  };
}

export function projectPoint(lat: number, lng: number, bounds: MapBounds) {
  const lngRange = Math.max(bounds.maxLng - bounds.minLng, 0.0001);
  const latRange = Math.max(bounds.maxLat - bounds.minLat, 0.0001);
  const x = ((lng - bounds.minLng) / lngRange) * 100;
  const y = (1 - (lat - bounds.minLat) / latRange) * 100;

  return {
    x: clamp(x, 6, 94),
    y: clamp(y, 8, 92),
  };
}

export function markerLabel(markerState: MarkerState) {
  return MARKER_LABELS[markerState] ?? "확인 필요";
}

export function markerText(item: Pick<LibraryItem, "availableSeats" | "markerState">) {
  if (item.availableSeats === null || item.availableSeats === undefined) {
    return markerLabel(item.markerState);
  }

  return `${item.availableSeats}`;
}

export function stateTone(markerState: MarkerState) {
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

export function operationStatusLabel(status?: string | null) {
  if (status === "OPEN") {
    return "현재 상태: 운영 중";
  }
  if (status === "CLOSING_SOON") {
    return "현재 상태: 운영 종료 임박";
  }
  if (status === "CLOSED") {
    return "현재 상태: 운영 종료";
  }
  return "현재 상태: 확인 필요";
}

export function riskLevelLabel(markerState: MarkerState) {
  if (markerState === "AVAILABLE") {
    return "낮음";
  }
  if (markerState === "MODERATE") {
    return "보통";
  }
  if (markerState === "CROWDED") {
    return "높음";
  }
  if (markerState === "FULL_RISK" || markerState === "FULL") {
    return "매우 높음";
  }
  if (markerState === "CLOSED") {
    return "운영 상태 확인";
  }
  return "확인 필요";
}

export function freshnessTone(dataFreshness: string | null) {
  if (dataFreshness === "FRESH") {
    return "good";
  }
  if (dataFreshness === "STALE") {
    return "warn";
  }
  if (dataFreshness === "EXPIRED") {
    return "danger";
  }
  return "muted";
}

export function seatSummary(
  item: Pick<LibraryItem | ReadingRoom, "availableSeats" | "totalSeats">,
) {
  if (item.availableSeats === null || item.availableSeats === undefined) {
    return "좌석 정보 없음";
  }

  if (item.totalSeats === null || item.totalSeats === undefined) {
    return `최근 기준 ${item.availableSeats}석 가능성`;
  }

  return `최근 기준 ${item.availableSeats}/${item.totalSeats}석 가능성`;
}

export function roomSubText(room: Pick<ReadingRoom, "roomType" | "floorInfo">) {
  const parts = [room.roomType, room.floorInfo].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : "열람실 정보";
}

export function noticeText(
  ui: Pick<UiState, "locationDenied" | "error">,
  items: Pick<LibraryItem, "dataFreshness">[],
) {
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

export function freshnessText(dataFreshness: string | null) {
  if (dataFreshness === "FRESH") {
    return "최근 갱신된 좌석 데이터입니다.";
  }
  if (dataFreshness === "STALE") {
    return "갱신 지연 데이터입니다.";
  }
  if (dataFreshness === "EXPIRED") {
    return "오래된 데이터라 현장 상황과 다를 수 있습니다.";
  }
  if (dataFreshness === "NO_DATA") {
    return "좌석 데이터가 아직 없습니다.";
  }
  return "최근 갱신 기준으로 표시합니다.";
}

export function formatDistance(distanceMeters?: number | null) {
  if (distanceMeters === null || distanceMeters === undefined) {
    return "";
  }
  if (distanceMeters >= 1000) {
    return ` · ${(distanceMeters / 1000).toFixed(1)}km`;
  }
  return ` · ${distanceMeters}m`;
}

export function formatWalk(minutes?: number | null) {
  if (minutes === null || minutes === undefined) {
    return "거리 정보 없음";
  }
  return `약 ${minutes}분`;
}

export function formatTravel(distanceMeters?: number | null, walkMinutes?: number | null) {
  const distance = formatDistanceValue(distanceMeters);
  const walk = formatWalk(walkMinutes);

  if (distance === "거리 정보 없음" && walk === "거리 정보 없음") {
    return "거리 정보 없음";
  }
  if (distance === "거리 정보 없음") {
    return walk;
  }
  if (walk === "거리 정보 없음") {
    return distance;
  }
  return `${distance} / 도보 ${walk}`;
}

export function formatDistanceValue(distanceMeters?: number | null) {
  if (distanceMeters === null || distanceMeters === undefined) {
    return "거리 정보 없음";
  }
  if (distanceMeters >= 1000) {
    return `${(distanceMeters / 1000).toFixed(1)}km`;
  }
  return `${distanceMeters}m`;
}

export function formatSyncedAt(value: string | null) {
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

export function formatClock(value: Date) {
  return new Intl.DateTimeFormat("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(value);
}

export function navigationUrlFor(
  item: Pick<LibraryItem, "name" | "address" | "lat" | "lng">,
) {
  const lat = toFiniteCoordinate(item.lat);
  const lng = toFiniteCoordinate(item.lng);
  const label = encodeURIComponent(item.name);

  if (lat !== null && lng !== null) {
    return `https://map.kakao.com/link/to/${label},${lat},${lng}`;
  }

  const query = encodeURIComponent(`${item.name} ${item.address ?? ""}`.trim());
  return `https://map.kakao.com/?q=${query}`;
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function toFiniteCoordinate(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : null;
}
