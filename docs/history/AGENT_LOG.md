## [2025-11-19 21:30] 개발 규칙 초안 정리(BaseEntity/UUID 포함)

### Type
DESIGN

### Summary
- 백엔드/프런트엔드 스타일 가이드 초안을 추가하고, BaseEntity(및 UUID/createdAt/modifiedAt)를 스펙과 일치하도록 반영했다.

### Details
- 작업 사유
  - 첫 TODO(코드 컨벤션 정의)를 구현하고 `docs/spec/v1.0.md`의 BaseEntity + UUID 결정을 문서에 반영하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/plan/dev-standards_plan.md (BaseEntity/UUID 정책 추가)
  - docs/standards/java-style.md (신규)
  - docs/standards/ts-react-style.md (신규)
  - docs/todo/v1.0.md (작업 상태를 In Progress로 설정)
- 다음 단계
  - 스타일 가이드 초안을 리뷰/승인
  - 승인 후 포매터/린터(Spotless/Checkstyle, ESLint/Prettier)를 별도 작업으로 추가

## [2025-11-19 21:45] domain/global 패키징으로 기준 정렬

### Type
DESIGN

### Summary
- `global`과 `domain`만 최상위 패키지로 사용하도록 dev-standards 계획과 Java 스타일 가이드를 수정했다.

### Details
- 작업 사유
  - 모놀리식 계층화를 피하고 `domain`/`global` 기반으로 표준화하자는 사용자 결정을 반영.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/plan/dev-standards_plan.md
  - docs/standards/java-style.md
- 다음 단계
  - 조정된 패키징 규칙을 리뷰하고 `domain.<feature>` 하위 패키지(web, application, model, repository)를 확정

## [2025-11-19 22:00] 한국어 README 추가

### Type
DESIGN

### Summary
- 프로젝트 개요, 아키텍처, 워크플로, 현재 코드 규칙을 담은 README.md(한국어)를 추가했다.

### Details
- 작업 사유
  - 이해관계자가 목표/스택/규칙/문서 위치를 한눈에 확인할 수 있는 진입점을 제공하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - README.md (신규)
- 다음 단계
  - 작업 진행에 따라 README를 동기화하고, 설치/실행, API 링크, 스크린샷을 추후 추가

## [2025-11-19 22:05] TODO 업데이트: 코드 컨벤션 완료, 커밋/브랜치 시작

### Type
TODO_UPDATE

### Summary
- "Define code conventions (Java, TS)"를 완료(✅)하고 "Define commit convention / branch strategy"를 진행 중(🔄)으로 설정, 대응 계획을 작성했다.

### Details
- 작업 사유
  - Phase 1 순서를 따라 다음 작업으로 넘어가기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.0.md (상태 업데이트)
  - docs/plan/commit-standards_plan.md (신규)
- 다음 단계
  - 커밋/브랜치 계획을 리뷰/승인 후 `docs/standards/commit-branch.md` 작성

## [2025-11-19 22:15] Commitlint 워크플로와 MCP-GitHub 가이드 추가

### Type
STRUCTURAL

### Summary
- GitHub Actions에 Conventional Commits 검증을 추가하고, MCP + GitHub 연동 가이드를 문서화했다.

### Details
- 작업 사유
  - PR 단에서 커밋 규칙을 강제하고, MCP 에이전트가 브랜치/PR을 만들 수 있도록 하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - commitlint.config.cjs (신규)
  - .github/workflows/commitlint.yml (신규)
  - .github/commit-title-lint.json (신규)
  - .github/PULL_REQUEST_TEMPLATE.md (신규)
  - docs/setup/mcp-github-setup.md (신규)
  - README.md (CI + MCP 안내 링크 추가)
- 다음 단계
  - GH 인증 수단(PAT 혹은 App)과 시크릿을 제공하고, 필요 시 스코프 목록을 확정

## [2025-11-19 22:25] 커밋/브랜치 기준 문서 추가

### Type
DESIGN

### Summary
- `docs/standards/commit-branch.md`를 작성하고 README에 링크했다.

### Details
- 작업 사유
  - 다음 TODO(커밋 컨벤션 & 브랜치 전략)를 완료하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/standards/commit-branch.md (신규)
  - README.md (링크 추가)
- 다음 단계
  - GH PAT이 준비되면 Conventional Commits 제목으로 PR 생성

## [2025-11-19 22:29] TODO 업데이트: GitHub Issue 템플릿 작업 시작

### Type
TODO_UPDATE

### Summary
- 커밋/브랜치 작업을 완료 처리하고, "Create GitHub Issue template"을 진행 중(🔄)으로 이동했다.

### Details
- 작업 사유
  - 커밋/브랜치 기준과 툴링이 완료되어 다음 TODO를 시작해야 함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.0.md (상태 업데이트)
- 다음 단계
  - GitHub Issue 템플릿 계획을 작성해 리뷰를 준비

## [2025-11-19 22:30] GitHub Issue 템플릿 계획 초안

### Type
DESIGN

### Summary
- 버그/기능/작업 이슈 폼 요구사항과 검증 전략을 정의한 계획 문서를 추가했다.

### Details
- 작업 사유
  - `.github/ISSUE_TEMPLATE` 자산을 작성하기 전에 계획을 수립한다는 AGENT 프로세스를 따르기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/github-issue-template_plan.md (신규)
- 다음 단계
  - 계획을 리뷰/승인한 뒤 YAML Issue Form과 관련 문서 업데이트를 진행

## [2025-11-19 22:40] 저장소 지침 한국어화

### Type
DESIGN

### Summary
- 루트/백엔드 AGENTS.md를 한국어로 재작성하고 모든 Markdown을 한국어로 작성하도록 지침을 갱신했다.

### Details
- 작업 사유
  - 사용자가 영어 지침을 제거하고 한국어 모드로 전환할 것을 요구함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - AGENTS.md
  - backend/AGENTS.md
- 다음 단계
  - 하위 문서를 모두 한국어로 정비하고 새 지침을 준수한다.

## [2025-11-19 22:41] 기존 문서 한국어 번역

### Type
DESIGN

### Summary
- TODO/스펙/설계/규칙/세팅 문서를 전부 한국어로 번역하고 최신 상태로 맞췄다.

### Details
- 작업 사유
  - “모든 문서를 한국어로 작성” 요구를 충족하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/standards/ts-react-style.md
  - docs/standards/java-style.md
  - docs/standards/commit-branch.md
  - docs/history/AGENT_LOG.md (기존 로그 번역 + 신규 기록)
  - docs/todo/v1.0.md
  - docs/spec/v1.0.md
- docs/setup/mcp-github-setup.md
- docs/plan/dev-standards_plan.md
- docs/plan/commit-standards_plan.md
- docs/plan/github-issue-template_plan.md
- 다음 단계
  - 향후 생성되는 모든 문서도 한국어로 유지한다.

## [2025-11-24 23:11] AGENT 지침 정리 및 MCP 가이드 제거

### Type
DESIGN

### Summary
- 루트 AGENTS.md의 중복 규칙을 정리하고, 더 이상 필요 없는 MCP + GitHub 연동 가이드를 제거했다.

### Details
- 작업 사유
  - 최신 지침과 중복되는 내용을 줄이고, 외부 연동 가이드가 다른 문서와 겹쳐 혼동을 주는 문제를 해소하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - AGENTS.md
  - docs/setup/mcp-github-setup.md (삭제)
- 다음 단계
  - MCP 설정 안내가 필요하면 README 등 다른 문서에서 최신 흐름으로 재작성한다.

## [2025-11-26 18:30] 요구사항 및 스펙 v1.2 작성

### Type
DESIGN

### Summary
- 리서치·요구사항을 정리해 `docs/requirement/v1.2.md`를 초안으로 추가했다.
- 요구사항을 토대로 `docs/spec/v1.2.md`를 작성해 아키텍처, 도메인, API 명세를 1.2 버전으로 확장했다.

### Details
- 작업 사유
  - Phase 1 설계 흐름에 따라 최신 요구사항과 이를 반영한 스펙 버전을 준비하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/requirement/v1.2.md (신규)
  - docs/spec/v1.2.md (신규)
- 다음 단계
  - 스펙에 맞춘 PLAN 문서를 작성하고 구현 단계의 우선순위를 정한다.

## [2025-11-26 18:36] TODO v1.2 구조 업데이트

### Type
TODO_UPDATE

### Summary
- spec v1.2 내용을 반영한 새 TODO 버전(`docs/todo/v1.2.md`)을 작성해 엔티티/기능/프런트 계획을 재정렬했다.

### Details
- 작업 사유
  - 문서화된 요구사항/스펙 1.2에 맞춰 Phase/Epic/Task 구성을 최신화하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.2.md (신규)
- 다음 단계
  - Phase 1 진행 중인 \"GitHub Issue 템플릿 생성\" 작업을 계속 진행한다.

## [2025-11-26 18:40] TODO 상태: Issue 템플릿 완료, PR 템플릿 착수

### Type
TODO_UPDATE

### Summary
- Phase 1에서 `GitHub Issue 템플릿 생성`을 완료 처리하고, 다음 작업인 `GitHub PR 템플릿 생성`을 진행 중(🔄)으로 전환했다.

### Details
- 작업 사유
  - 사용자가 Issue 템플릿 작업을 마무리했다고 알렸고, TODO 우선순위에 따라 PR 템플릿 작업을 즉시 시작해야 함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.2.md
- 다음 단계
  - `GitHub PR 템플릿 생성`에 대한 PLAN 문서를 작성하고 승인 절차를 진행한다.

## [2025-11-26 18:42] GitHub PR 템플릿 계획 초안

### Type
DESIGN

### Summary
- Phase 1의 다음 TODO를 위해 `docs/plan/github-pr-template_plan.md`를 작성하고 섹션/체크리스트 요건을 정의했다.

### Details
- 작업 사유
  - Issue 템플릿에 이어 PR 템플릿도 AGENT 프로세스를 강제하도록 설계가 필요했기 때문.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/plan/github-pr-template_plan.md (신규)
- 다음 단계
  - 계획을 리뷰/승인받은 뒤 실제 `.github/PULL_REQUEST_TEMPLATE.md`를 업데이트한다.

## [2025-11-26 18:44] Issue/PR 템플릿 적용

### Type
STRUCTURAL

### Summary
- GitHub Issue Form 3종(버그/기능/작업)과 PR 템플릿을 AGENT 계획에 맞춰 추가해 리포지토리 규칙을 자동화했다.

### Details
- 작업 사유
  - 리포트/PR 단계에서 PLAN→TODO→LOG 검증과 필수 정보를 일관되게 수집하기 위함.
- 영향받은 테스트
  - N/A (템플릿 작업)
- 수정한 파일
  - .github/ISSUE_TEMPLATE/bug-report.yml (신규)
  - .github/ISSUE_TEMPLATE/feature-request.yml (신규)
  - .github/ISSUE_TEMPLATE/task.yml (신규)
  - .github/ISSUE_TEMPLATE/config.yml (신규)
  - .github/PULL_REQUEST_TEMPLATE.md
- 다음 단계
  - 템플릿을 사용해 실제 Issue/PR을 생성하며 개선점을 피드백한다.

## [2025-11-26 18:44] TODO 상태: PR 템플릿 완료

### Type
TODO_UPDATE

### Summary
- Phase 1의 `GitHub PR 템플릿 생성` 작업을 완료(✅) 처리하고, 남은 작업 없이 다음 Phase로 넘어갈 수 있도록 갱신했다.

### Details
- 작업 사유
  - 새 템플릿을 레포에 반영했으므로 TODO 진행 상황을 최신화하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.2.md
- 다음 단계
  - Phase 2 착수 전, 필요 시 PLAN 문서/우선순위를 재검토한다.
