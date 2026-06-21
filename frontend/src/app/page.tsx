export default function Home() {
  return (
    <main className="app-shell">
      <header className="topbar">
        <input
          className="search-input"
          aria-label="지역 또는 도서관 검색"
          placeholder="강남구 또는 도서관명 검색"
        />
        <button className="location-button" type="button">
          내 위치
        </button>
      </header>

      <section className="map-stage" aria-label="도서관 지도 영역">
        <div className="map-placeholder">
          지역을 검색하면 공공도서관 위치와 최근 갱신 기준 좌석 현황이 표시됩니다.
        </div>
      </section>

      <section className="panel" aria-label="추천 도서관 목록">
        <h1>열람실 Now</h1>
        <p>
          최근 갱신 기준 좌석 여유와 거리를 함께 보고, 도착 후 앉을 가능성이 높은
          도서관을 찾는 지도 기반 서비스입니다.
        </p>
      </section>
    </main>
  );
}
