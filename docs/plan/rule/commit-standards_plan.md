# Feature: 커밋 컨벤션 & 브랜치 전략

## 1. Problem Definition

- 일관된 커밋/브랜치 규칙이 없으면 히스토리가 지저분해지고 리뷰/릴리스 속도가 느려진다. 작은 단위의 고품질 변경을 지원할 명확하고 가벼운 규칙이 필요하다.

## 2. Requirements

### Functional

- 저장소에 맞는 스코프를 포함한 Conventional Commits 형식을 정의한다.
- feature/bugfix/chore/docs/refactor 등 작업 유형에 대한 브랜치 네이밍 전략을 정의한다.
- PR 및 머지 정책(Squash/Rebase/Merge)과 사용 조건을 규정한다.
- CI를 지원하기 위한 기본 브랜치 보호(main 규칙)를 정의한다.

### Non-functional

- 기억/채택이 쉬워야 하며, 간단한 도구로 강제 가능해야 한다.
- 작은 단위의 집중된 커밋을 장려하고 STRUCTURAL vs BEHAVIORAL 분리를 지원한다.

## 3. TDD Plan

- 문서 작업이므로 리뷰/승인으로 검증한다.

## 4. Proposed Standards (Draft)

### 4.1 Conventional Commits

- 형식: `<type>(<scope>): <subject>`
- 타입
  - feat: 사용자 행동 변화
  - fix: 버그 수정
  - refactor: 구조 변경(행동 변화 없음)
  - docs: 문서 변경
  - chore: 빌드/도구/인프라
  - test: 테스트 추가/수정
  - perf: 성능 개선
  - style: 포매팅/공백 등
- 스코프 예시
  - `backend:domain-<feature>`, `backend:global`, `frontend:<feature>`, `infra`, `docs`
- Subject 규칙
  - 명령형, 72자 이내, 마침표 금지
  - Body(선택): 무엇/왜
  - Footer(선택): Breaking change, 이슈 참조

### 4.2 브랜치 전략

- 기본 브랜치: `main`(배포 가능, 보호)
- 작업 브랜치 예시
  - `feature/<short-scope>-<short-desc>`
  - `fix/<short-scope>-<short-desc>`
  - `chore/<short-scope>-<short-desc>`
  - `docs/<short-desc>`
  - `refactor/<short-scope>-<short-desc>`
- 예시: `feature/member-auth-login`, `refactor/domain-student-repository`, `docs/readme-intro`

### 4.3 PR & 머지 정책

- 가시성을 위해 초기에 Draft PR을 연다.
- CI 통과 + 최소 1회 승인 필수.
- 머지: Squash 선호, 필요 시 Rebase, Merge commit은 지양.
- PR 제목은 Conventional Commits 형식을 따른다.
- PR 규모는 300 LOC 이하로 유지하는 것을 목표로 한다.

### 4.4 구조 vs 행동 정렬

- STRUCTURAL(refactor/style) 변경은 행동 변화를 포함하지 않으며 별도 커밋으로 분리한다.
- BEHAVIORAL(feat/fix) 변경은 테스트와 구현을 함께 포함한다.

### 4.5 보호 & CI 훅

- `main`은 필수 상태 체크와 강제 푸시 제한을 설정한다.
- Commitlint + 포매팅 검증을 Pre-commit/CI에 추가할 계획이다.

## 5. Deliverables

- 최종 규칙을 담은 `docs/standards/commit-branch.md`

## 6. Acceptance Criteria

- 커밋 타입/스코프, 브랜치 이름, PR/머지 정책을 모두 다룬다.
- 예시와 함께 규칙을 명확하게 제시한다.
- 사용자 승인을 받는다.
