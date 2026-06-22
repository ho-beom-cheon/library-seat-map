# 데모 가이드

이 문서는 `library-seat-map`을 포트폴리오와 로컬 시연 환경에서 보여주기 위한 절차입니다. 모든 좌석 수치는 최근 갱신 데이터 기준의 참고 정보로 설명합니다.

## 데모 목표

- 지도에서 공공도서관 목록과 좌석 상태를 확인한다.
- 현재 위치 또는 선택 지역 기준으로 가까운 도서관을 비교한다.
- 최근 갱신 시각, 좌석 여유 가능성, 운영 시간 정보를 함께 보여준다.
- 공공데이터 키가 프론트엔드로 노출되지 않는 구조를 설명한다.

## 시작 전 점검

1. `.env`가 준비되어 있고 실제 키가 Git 변경사항에 포함되지 않았는지 확인합니다.
2. PostgreSQL/PostGIS가 실행 중인지 확인합니다.
3. 백엔드가 `http://localhost:8080`에서 실행 중인지 확인합니다.
4. 프론트엔드가 `http://localhost:3000`에서 실행 중인지 확인합니다.
5. 로컬 DB에 공공도서관 기본 정보와 열람실 좌석 데이터가 적재되어 있는지 확인합니다.

기본 헬스 체크:

```powershell
curl.exe "http://localhost:8080/api/health"
```

데이터가 비어 있으면 지도와 추천 결과가 비어 보일 수 있습니다. 이 경우 공공데이터 서비스키와 동기화 설정을 먼저 확인합니다.

## 로컬 실행 순서

저장소 루트에서 시작합니다.

```powershell
Copy-Item .env.example .env
docker compose up -d postgres
```

백엔드:

```powershell
Set-Location backend
.\gradlew.bat test
.\gradlew.bat bootRun
```

프론트엔드:

```powershell
Set-Location frontend
npm ci
npm run lint
npm run build
npm run dev
```

브라우저에서 `http://localhost:3000`을 엽니다.

## 시연 흐름

### 1. 지도 기본 화면

- 도서관 마커가 지도 위에 표시되는지 확인합니다.
- 좌석 정보가 있는 도서관과 없는 도서관이 구분되는지 확인합니다.
- 목록에는 도서관명, 행정구, 거리, 좌석 상태, 최근 갱신 시각이 표시되어야 합니다.

확인 API:

```powershell
curl.exe "http://localhost:8080/api/libraries?district=%EA%B0%95%EB%82%A8%EA%B5%AC&includeNoSeat=true&page=0&size=20"
```

### 2. 현재 위치 기준 주변 검색

- 위치 권한을 허용한 뒤 주변 도서관 목록이 거리 기준으로 갱신되는지 확인합니다.
- 추천 정렬에서는 거리뿐 아니라 최근 좌석 데이터와 운영 시간도 함께 반영된다고 설명합니다.
- 사용자의 현재 위치 좌표는 저장하지 않고 요청 처리에만 사용한다고 설명합니다.

확인 API:

```powershell
curl.exe "http://localhost:8080/api/libraries/nearby?lat=37.501&lng=127.001&radiusMeters=3000&sort=recommend&includeNoSeat=true&limit=20"
```

### 3. 추천 결과 확인

- 좌석 데이터가 최근 갱신된 도서관이 추천 대상에 포함되는지 확인합니다.
- 열람실 좌석 정보가 없는 도서관은 추천 결과에서 제외될 수 있다고 설명합니다.
- 추천 결과는 확정 안내가 아니라 최근 데이터 기반의 우선순위라고 설명합니다.

확인 API:

```powershell
curl.exe "http://localhost:8080/api/recommendations/libraries?district=%EA%B0%95%EB%82%A8%EA%B5%AC&minimumStudyMinutes=60&limit=10"
```

### 4. 도서관 상세 확인

- 목록이나 마커에서 도서관을 선택합니다.
- 주소, 운영 시간, 열람실별 좌석 현황, 최근 갱신 시각을 확인합니다.
- 오래된 좌석 데이터는 최신성이 낮은 정보로 설명합니다.

확인 API:

```powershell
curl.exe "http://localhost:8080/api/libraries/{libraryId}"
curl.exe "http://localhost:8080/api/libraries/{libraryId}/rooms"
```

`{libraryId}`는 목록 조회 응답의 도서관 ID로 바꿉니다.

## 스크린샷 체크리스트

실제 이미지 파일을 추가할 때는 키, 개인 위치, 브라우저 프로필 정보가 노출되지 않는지 확인합니다.

- `docs/screenshots/main-map.png`
  - 지도
  - 도서관 목록
  - 좌석 상태 배지
  - 최근 갱신 시각
- `docs/screenshots/detail-card.png`
  - 도서관 상세 정보
  - 열람실별 좌석 현황
  - 데이터 기준 시각

## 실패 상황 설명

- 공공 API 키가 없으면 데이터 동기화가 실패할 수 있습니다.
- 로컬 DB가 비어 있으면 검색 결과가 비어 보일 수 있습니다.
- 공공 API 응답 구조나 제공 행 수는 시점에 따라 달라질 수 있습니다.
- 30분 이상 지난 좌석 데이터는 추천 신뢰도를 낮게 처리합니다.
- `/prst_info_v2`는 MVP 필수 범위에서 제외되어도 핵심 시연에는 영향이 없습니다.

## 검증 명령

문서 변경 후에는 다음 명령으로 범위를 확인합니다.

```powershell
git diff --name-only
git diff --check
git diff --name-only -- src build.gradle settings.gradle package.json package-lock.json pnpm-lock.yaml yarn.lock
```

민감정보 검색은 저장소의 `AGENTS.md`에 정의된 정규식 기준으로 별도 실행합니다. 검색 결과가 문서의 검증 예시나 `.env.example` 같은 예시 변수만 가리키는지, 실제 값이 섞이지 않았는지 확인합니다.

애플리케이션 검증이 필요한 경우:

```powershell
Set-Location backend
.\gradlew.bat test
```

```powershell
Set-Location frontend
npm run lint
npm run build
```

## 릴리스 노트 후보

- README와 데모 가이드에 로컬 실행, API 예시, 시연 흐름을 정리했습니다.
- 공공데이터 키, 지도 SDK 키, 사용자 위치 정보 관리 원칙을 문서화했습니다.
- 좌석 현황을 최근 갱신 기준의 참고 정보로 표현하는 기준을 정리했습니다.
