import { describe, expect, it } from "vitest";

import {
  freshnessText,
  freshnessTone,
  formatDistance,
  formatDistanceValue,
  formatTravel,
  formatWalk,
  getLibraryPoint,
  getMapBounds,
  markerLabel,
  markerText,
  navigationUrlFor,
  noticeText,
  operationStatusLabel,
  riskLevelLabel,
  roomSubText,
  seatSummary,
  stateTone,
} from "./library-display";
import type { LibraryItem } from "./library-display";

const baseLibrary: LibraryItem = {
  libraryId: "LIB-1",
  name: "강남도서관",
  district: "강남구",
  address: "서울 강남구",
  lat: 37.5,
  lng: 127.0,
  markerState: "AVAILABLE",
  availableSeats: 12,
  totalSeats: 40,
  usageRate: 70,
  lastSyncedAt: "2026-06-22T12:00:00Z",
  dataFreshness: "FRESH",
};

describe("map marker state display", () => {
  it("maps known marker states to user-facing labels and tones", () => {
    expect(markerLabel("AVAILABLE")).toBe("여유");
    expect(markerLabel("FULL_RISK")).toBe("만석 임박");
    expect(markerLabel("UNKNOWN")).toBe("확인 필요");

    expect(stateTone("AVAILABLE")).toBe("good");
    expect(stateTone("MODERATE")).toBe("normal");
    expect(stateTone("CROWDED")).toBe("warn");
    expect(stateTone("FULL")).toBe("danger");
    expect(stateTone("NO_DATA")).toBe("muted");
  });

  it("uses available seat count for marker text when seat data exists", () => {
    expect(markerText(baseLibrary)).toBe("12");
    expect(markerText({ ...baseLibrary, availableSeats: null, markerState: "NO_DATA" })).toBe(
      "정보 없음",
    );
  });

  it("projects valid coordinates and falls back for missing coordinates", () => {
    const bounds = getMapBounds([baseLibrary], null);
    const projected = getLibraryPoint(baseLibrary, bounds, 0);
    const fallback = getLibraryPoint({ ...baseLibrary, lat: null, lng: null }, bounds, 5);

    expect(projected.x).toBeGreaterThanOrEqual(6);
    expect(projected.x).toBeLessThanOrEqual(94);
    expect(projected.y).toBeGreaterThanOrEqual(8);
    expect(projected.y).toBeLessThanOrEqual(92);
    expect(fallback).toEqual({ x: 38, y: 38 });
  });
});

describe("library card display text", () => {
  it("summarizes seats as recent possibility text instead of certainty", () => {
    expect(seatSummary({ availableSeats: null, totalSeats: null })).toBe("좌석 정보 없음");
    expect(seatSummary({ availableSeats: 8, totalSeats: null })).toBe("최근 기준 8석 가능성");
    expect(seatSummary({ availableSeats: 8, totalSeats: 30 })).toBe(
      "최근 기준 8/30석 가능성",
    );
  });

  it("formats freshness and risk labels for detail cards", () => {
    expect(freshnessText("FRESH")).toBe("최근 갱신된 좌석 데이터입니다.");
    expect(freshnessText("STALE")).toBe("갱신 지연 데이터입니다.");
    expect(freshnessText("EXPIRED")).toBe("오래된 데이터라 현장 상황과 다를 수 있습니다.");
    expect(freshnessText("NO_DATA")).toBe("좌석 데이터가 아직 없습니다.");

    expect(freshnessTone("FRESH")).toBe("good");
    expect(freshnessTone("STALE")).toBe("warn");
    expect(freshnessTone("EXPIRED")).toBe("danger");
    expect(freshnessTone(null)).toBe("muted");

    expect(operationStatusLabel("OPEN")).toBe("현재 상태: 운영 중");
    expect(operationStatusLabel("CLOSING_SOON")).toBe("현재 상태: 운영 종료 임박");
    expect(operationStatusLabel("CLOSED")).toBe("현재 상태: 운영 종료");
    expect(operationStatusLabel(null)).toBe("현재 상태: 확인 필요");

    expect(riskLevelLabel("AVAILABLE")).toBe("낮음");
    expect(riskLevelLabel("FULL_RISK")).toBe("매우 높음");
    expect(riskLevelLabel("CLOSED")).toBe("운영 상태 확인");
  });

  it("formats distance and navigation links", () => {
    expect(formatDistance(null)).toBe("");
    expect(formatDistance(850)).toBe(" · 850m");
    expect(formatDistance(1250)).toBe(" · 1.3km");
    expect(formatDistanceValue(null)).toBe("거리 정보 없음");
    expect(formatWalk(7)).toBe("약 7분");
    expect(formatTravel(1250, 16)).toBe("1.3km / 도보 약 16분");

    expect(navigationUrlFor(baseLibrary)).toBe(
      "https://map.kakao.com/link/to/%EA%B0%95%EB%82%A8%EB%8F%84%EC%84%9C%EA%B4%80,37.5,127",
    );
    expect(navigationUrlFor({ ...baseLibrary, lat: null, lng: null })).toBe(
      "https://map.kakao.com/?q=%EA%B0%95%EB%82%A8%EB%8F%84%EC%84%9C%EA%B4%80%20%EC%84%9C%EC%9A%B8%20%EA%B0%95%EB%82%A8%EA%B5%AC",
    );
  });

  it("combines room type and floor without hiding missing values", () => {
    expect(roomSubText({ roomType: "일반", floorInfo: "2층" })).toBe("일반 · 2층");
    expect(roomSubText({ roomType: null, floorInfo: null })).toBe("열람실 정보");
  });
});

describe("empty and notice states", () => {
  it("uses the Seoul default bounds when there are no coordinates", () => {
    const bounds = getMapBounds([], null);

    expect(bounds.minLat).toBeCloseTo(37.5365);
    expect(bounds.maxLat).toBeCloseTo(37.5965);
    expect(bounds.minLng).toBeCloseTo(126.938);
    expect(bounds.maxLng).toBeCloseTo(127.018);
  });

  it("prioritizes location, error, stale, and default notices", () => {
    expect(noticeText({ locationDenied: true, error: null }, [])).toBe(
      "위치 권한이 없어 지역 검색으로 전환했습니다.",
    );
    expect(noticeText({ locationDenied: false, error: "fail" }, [])).toBe(
      "API 연결 상태를 확인해 주세요.",
    );
    expect(
      noticeText({ locationDenied: false, error: null }, [
        { dataFreshness: "STALE" },
      ]),
    ).toBe("일부 도서관은 갱신 지연 상태입니다.");
    expect(noticeText({ locationDenied: false, error: null }, [])).toBe(
      "좌석 상태는 보장 정보가 아니라 최근 갱신 기준의 가능성 정보입니다.",
    );
  });
});
