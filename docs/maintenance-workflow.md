# 유지보수 워크플로우

이 문서는 `library-seat-map` 저장소의 이슈, 브랜치, PR 운영 기준을 정리합니다.

## 운영 원칙

- 모든 작업은 이슈 기반으로 진행합니다.
- 관련 이슈가 없으면 구현 전에 이슈 초안을 먼저 제안합니다.
- 작업은 전용 브랜치에서 진행합니다.
- 변경은 작은 단위로 나누고 요청 범위 밖의 파일은 수정하지 않습니다.
- PR에는 요약, 관련 이슈, 변경 내용, 검증 결과, 릴리스 노트 후보를 포함합니다.
- 의미 없는 커밋, 빈 커밋, 활동량만 늘리기 위한 변경은 하지 않습니다.
- 회사, 고객사, 내부정보, 민감정보는 문서와 코드에 포함하지 않습니다.
- 사용자 요청 없이 기존 변경사항을 되돌리지 않습니다.

## 브랜치 규칙

이슈 번호가 있으면 다음 형식을 사용합니다.

```text
codex/issue-<number>-<short-task-name>
```

이슈가 아직 없고 제안 또는 준비 작업을 하는 경우에는 다음 형식을 사용합니다.

```text
codex/proposed-<short-task-name>
```

## 커밋 규칙

커밋 메시지는 영어 Conventional Commits 형식을 사용합니다.

```text
docs: add maintenance workflow
feat: implement public library API client
fix: handle stale seat data
test: add recommendation scoring tests
ci: add backend build workflow
chore: update local development settings
```

한 커밋에는 한 가지 목적만 담고, 문서 변경과 애플리케이션 소스 변경을 섞지 않습니다.

## PR 작성 기준

PR 제목은 한국어로 작성합니다.

```text
문서: 유지보수 워크플로우 추가
기능: 공공도서관 조회 API 구현
테스트: 추천 점수 계산 테스트 추가
```

PR 본문에는 다음 항목을 포함합니다.

- 요약
- 관련 이슈
- 변경 내용
- 검증 결과
- 릴리스 노트 후보
- 유지보수자 참고사항

## 검증

문서 변경에서는 다음을 확인합니다.

```bash
git diff --name-only
git diff --check
rg -n "AKIA|SECRET|PASSWORD=|TOKEN=|PRIVATE KEY|BEGIN RSA|REPO_PASS|REPO_USER|internal|고객사|내부정보" .
```

애플리케이션 소스 변경이 없어야 하는 작업에서는 다음 명령으로 범위를 확인합니다.

```bash
git diff --name-only -- src build.gradle settings.gradle package.json package-lock.json pnpm-lock.yaml yarn.lock
```
