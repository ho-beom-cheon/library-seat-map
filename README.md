# library-seat-map

`library-seat-map`은 사용자의 현재 위치나 선택 지역을 기준으로 공공도서관 열람실의 최근 좌석 현황과 접근 정보를 지도에서 확인하는 MVP 프로젝트입니다. 좌석 정보는 공공데이터포털에서 수집한 최근 갱신값을 기반으로 하며, 실제 착석 가능 여부를 확정하지 않습니다.

상세 시연 순서는 [docs/demo-guide.md](docs/demo-guide.md)를 참고합니다.

## 핵심 기능

- 공공도서관 기본 정보와 열람실 좌석 현황 수집
- PostgreSQL/PostGIS 기반 도서관 위치, 열람실, 좌석 스냅샷 저장
- 지역, 현재 위치 반경, 보유 좌석 정보 여부 기준의 도서관 조회
- 최근 갱신 시각과 이용률을 고려한 도서관 추천
- Next.js 지도 화면에서 도서관 목록, 거리, 좌석 상태 표시
- 공공 API 키와 지도 SDK 키를 환경변수로 분리
- 사용자 현재 위치 좌표를 저장하지 않고 요청 처리에만 사용

## 데이터 출처

- 공공데이터포털 공공도서관 API
  - `/info_v2`: 도서관 기본 정보
  - `/rlt_rdrm_info_v2`: 열람실 실시간 좌석 정보
  - `/prst_info_v2`: MVP 필수 범위에서 제외
- 공공데이터 서비스키는 `.env` 또는 배포 환경변수에만 둡니다.
- 프론트엔드는 공공데이터 API를 직접 호출하지 않고 백엔드 API만 호출합니다.

## 구조

```text
frontend/                  Next.js 지도 UI
backend/                   Spring Boot API, 동기화, 추천 로직
backend/src/main/resources/db/migration/
                           Flyway DB 마이그레이션
docs/                      유지보수 문서, 릴리스 계획, 데모 가이드
docker-compose.yml         로컬 PostgreSQL/PostGIS, Redis
.env.example               로컬 환경변수 예시
```

요청 흐름은 다음과 같습니다.

```text
Browser
  -> Next.js frontend
  -> Spring Boot backend
  -> PostgreSQL/PostGIS

Spring scheduler
  -> Public library API
  -> PostgreSQL/PostGIS
```

Redis는 선택 사항입니다. MVP의 기본 로컬 실행은 PostgreSQL/PostGIS만으로 동작합니다.

## 사전 준비

- Java 17
- Node.js 20.9 이상
- Docker Desktop 또는 로컬 PostgreSQL 16 + PostGIS
- 공공데이터포털 서비스키
- Kakao Maps 또는 Naver Maps 브라우저 SDK 키

## 빠른 실행

Windows PowerShell 기준입니다.

1. 환경변수 파일을 준비합니다.

```powershell
Copy-Item .env.example .env
```

2. `.env`에 로컬 값을 입력합니다.

```text
DATA_GO_KR_SERVICE_KEY=<공공데이터포털 서비스키>
DATA_GO_KR_KEY_IS_ENCODED=false
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/libraryseat
SPRING_DATASOURCE_USERNAME=libraryseat
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
NEXT_PUBLIC_MAP_PROVIDER=kakao
NEXT_PUBLIC_KAKAO_MAP_KEY=<브라우저 지도 SDK 키>
```

로컬 DB 비밀번호 변수도 `.env`에서 함께 채웁니다. `.env`에는 실제 키가 들어가므로 Git에 추가하지 않습니다.

3. Docker로 PostgreSQL/PostGIS를 실행합니다.

```powershell
docker compose up -d postgres
```

Redis까지 함께 확인하려면 다음 명령을 사용합니다.

```powershell
docker compose up -d postgres redis
```

4. 백엔드를 검증하고 실행합니다.

```powershell
Set-Location backend
.\gradlew.bat test
.\gradlew.bat bootRun
```

백엔드는 기본적으로 `http://localhost:8080`에서 실행됩니다.

5. 새 PowerShell 창에서 프론트엔드를 검증하고 실행합니다.

```powershell
Set-Location frontend
npm ci
npm run lint
npm run build
npm run dev
```

프론트엔드는 `http://localhost:3000`에서 실행됩니다.

## 주요 환경변수

| 이름 | 위치 | 설명 |
| --- | --- | --- |
| `DATA_GO_KR_SERVICE_KEY` | backend | 공공데이터포털 서비스키 |
| `DATA_GO_KR_KEY_IS_ENCODED` | backend | 서비스키가 URL 인코딩된 값인지 여부 |
| `PUBLIC_API_BASE_URL` | backend | 공공도서관 API 기본 URL |
| `LIBRARY_SYNC_ENABLED` | backend | 스케줄러 기반 동기화 사용 여부 |
| `LIBRARY_SYNC_DISTRICTS` | backend | 동기화 대상 행정구역 코드 목록 |
| `LIBRARY_SYNC_MANUAL_TRIGGER_ENABLED` | backend | 로컬 데모용 수동 동기화 API 사용 여부 |
| `SPRING_DATASOURCE_URL` | backend | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | backend | PostgreSQL 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | backend | PostgreSQL 비밀번호 |
| `REDIS_ENABLED` | backend | Redis 사용 여부 |
| `APP_LOCATION_LOGGING_ENABLED` | backend | 위치 요청 로그 기록 여부. 기본값은 `false`를 권장 |
| `NEXT_PUBLIC_API_BASE_URL` | frontend | 백엔드 API 기본 URL |
| `NEXT_PUBLIC_MAP_PROVIDER` | frontend | 지도 제공자 |
| `NEXT_PUBLIC_KAKAO_MAP_KEY` | frontend | 브라우저용 Kakao 지도 SDK 키 |

## API 예시

백엔드가 실행 중일 때 확인합니다.

```powershell
curl.exe "http://localhost:8080/api/health"
```

로컬 데모에서 데이터를 즉시 적재하려면 `.env`에 공공데이터 서비스키를 넣고 `LIBRARY_SYNC_MANUAL_TRIGGER_ENABLED=true`를 설정한 뒤 다음 명령을 실행합니다.

```powershell
curl.exe -X POST "http://localhost:8080/api/sync/run"
```

기본 설정 기준으로 도서관 기본 정보는 매일 03:10에 스케줄 동기화되고, 열람실 실시간 좌석 정보는 백엔드 실행 후 고정 주기로 갱신됩니다. 수동 동기화 API는 이 대기 시간을 줄이기 위한 로컬 데모용 트리거입니다.

동기화 상태는 다음 명령으로 확인합니다.

```powershell
curl.exe "http://localhost:8080/api/sync/status"
```

```powershell
curl.exe "http://localhost:8080/api/libraries?district=%EA%B0%95%EB%82%A8%EA%B5%AC&includeNoSeat=true&page=0&size=20"
```

```powershell
curl.exe "http://localhost:8080/api/libraries/nearby?lat=37.501&lng=127.001&radiusMeters=3000&sort=recommend&includeNoSeat=true&limit=20"
```

```powershell
curl.exe "http://localhost:8080/api/recommendations/libraries?district=%EA%B0%95%EB%82%A8%EA%B5%AC&minimumStudyMinutes=60&limit=10"
```

```powershell
curl.exe "http://localhost:8080/api/libraries/{libraryId}"
curl.exe "http://localhost:8080/api/libraries/{libraryId}/rooms"
```

`{libraryId}`는 목록 조회 응답에서 받은 도서관 ID로 바꿉니다.

## 스크린샷 자리

실제 시연 캡처는 키와 위치 정보가 노출되지 않도록 확인한 뒤 추가합니다.

- `docs/screenshots/main-map.png`: 지도와 도서관 목록
- `docs/screenshots/detail-card.png`: 도서관 상세 정보와 최근 갱신 시각

## 위험 요소와 대응

- 공공 API 장애나 응답 지연이 있으면 최신 데이터 갱신이 늦어질 수 있습니다.
- 열람실 좌석 정보가 없는 도서관은 지도에 표시할 수 있지만 추천에서는 제외할 수 있습니다.
- 최근 갱신 시각이 오래된 좌석 데이터는 추천 점수에서 제외하거나 낮게 취급합니다.
- 사용자 현재 위치 좌표는 저장하지 않고 거리 계산과 추천 요청 처리에만 사용합니다.
- 지도 SDK 키는 브라우저에서 노출될 수 있는 공개 키만 사용하고, 공공데이터 서비스키는 백엔드 환경변수로만 관리합니다.
- 좌석 문구는 항상 "최근 갱신 기준", "여유 가능성", "정보 없음"처럼 가능성 중심으로 작성합니다.

## 릴리스 노트 후보

- 서울 공공도서관 기본 정보와 열람실 좌석 현황 수집
- 지도 기반 도서관 조회와 위치 반경 검색
- 최근 갱신 시각, 이용률, 거리 기준 추천 API
- 좌석 정보가 없는 도서관의 지도 표시와 추천 제외 처리
- 공공데이터 키와 사용자 위치 정보 관리 원칙 반영
