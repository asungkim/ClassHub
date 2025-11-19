# 커밋 컨벤션 & 브랜치 전략

이 문서는 저장소의 커밋 메시지 형식, 브랜치 네이밍, PR/머지 정책을 정의하며 CI(commitlint) 설정과 일치한다.

## 1. Conventional Commits

- 형식: `<type>(<scope>): <subject>`
- 타입
  - feat: 사용자 행동 변화
  - fix: 버그 수정
  - refactor: 행동 변화 없는 구조 변경
  - docs: 문서만 변경
  - chore: 빌드/도구/인프라 변경
  - test: 테스트 추가/수정
  - perf: 성능 개선
  - style: 포매팅/공백 등 (행동 변화 없음)
- 스코프 예시
  - `backend:domain-<feature>`, `backend:global`, `frontend:<feature>`, `infra`, `docs`
- Subject 규칙
  - 명령형, 72자 이하, 마침표 금지
  - Body(선택): 무엇과 이유를 설명
  - Footer(선택): BREAKING CHANGE, 이슈 참조 등

예시

- `feat(backend:domain-course): add create course API`
- `refactor(backend:domain-student): extract repository interfaces`
- `docs(backend:global): add commit/branch standards`

## 2. 브랜치 전략

- 기본 브랜치
  - `main`: 배포 가능한 상태, 보호 설정 적용
- 작업 브랜치
  - `feature/<short-scope>-<short-desc>`
  - `fix/<short-scope>-<short-desc>`
  - `refactor/<short-scope>-<short-desc>`
  - `chore/<short-scope>-<short-desc>`
  - `docs/<short-desc>`

예시

- `feature/member-auth-login`
- `refactor/domain-student-repository`
- `docs/readme-intro`

## 3. PR & 머지 정책

- 가시성을 위해 초기에 Draft PR을 연다.
- CI가 녹색이고 최소 1건 이상의 승인을 받아야 한다.
- 머지 방식: PR당 이력을 깨끗하게 유지하기 위해 Squash를 선호한다.
- PR 제목은 반드시 Conventional Commits 형식을 따른다.
- PR 변경량은 가능하면 300 LOC 이하로 작게 유지한다.

## 4. 구조 vs 행동 변경

- STRUCTURAL(refactor/style)은 행동 변화를 포함하면 안 되며, 별도 커밋으로 관리한다.
- BEHAVIORAL(feat/fix)은 테스트와 구현을 함께 포함한다.

## 5. 보호 & CI

- `main` 브랜치는 필수 상태 체크와 강제 푸시 금지를 설정한다.
- CI의 commitlint가 PR 커밋과 제목을 검증한다.
