# library-seat-map

`library-seat-map`은 공공데이터포털의 공공도서관 열람실 좌석 데이터를 기반으로, 최근 갱신 기준 좌석 여유와 위치를 함께 보여주는 지도 기반 서비스입니다.

## 프로젝트 구성

- `backend`: Spring Boot 기반 API 서버
- `frontend`: Next.js 기반 웹 애플리케이션
- `docker-compose.yml`: 로컬 PostgreSQL/PostGIS, Redis 실행 환경

## 사전 준비

- Java 17 이상
- Node.js 20.9 이상
- Docker Desktop 또는 Docker Compose

## 환경변수

루트의 `.env.example`을 참고해 로컬 환경변수를 설정합니다.

공공데이터포털 서비스키와 지도 SDK 키는 실제 값으로 커밋하지 않습니다.

## 로컬 인프라 실행

```bash
docker compose up -d postgres redis
```

## 백엔드

```bash
cd backend
./gradlew test
./gradlew bootRun
```

Windows PowerShell에서는 다음 명령을 사용할 수 있습니다.

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat bootRun
```

기본 헬스 체크 엔드포인트:

```text
GET http://localhost:8080/api/health
```

## 프론트엔드

```bash
cd frontend
npm install
npm run lint
npm run build
npm run dev
```

기본 개발 서버:

```text
http://localhost:3000
```

## 유지보수 원칙

- 모든 작업은 이슈 기반으로 진행합니다.
- 전용 브랜치에서 작은 단위로 변경합니다.
- PR에는 검증 결과와 릴리스 노트 후보를 포함합니다.
- 서비스키, 토큰, 위치정보 원본 좌표 같은 민감정보를 커밋하지 않습니다.
