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

## [2025-11-26 18:48] TODO v1.3: Phase 1 작업 추가

### Type
TODO_UPDATE

### Summary
- Phase 1 요구에 따라 `백엔드 Spring 초기 설정`, `global 도메인 공통 구성 정의` 작업을 포함한 `docs/todo/v1.3.md`를 생성했다.

### Details
- 작업 사유
  - Phase 2로 넘어가기 전 백엔드 초기화와 global 도메인 설계 작업이 필요하다는 요청을 반영.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.3.md (신규)
- 다음 단계
  - 새로 추가된 작업을 우선 수행한 뒤 Phase 2를 시작한다.

## [2025-11-26 18:49] TODO 상태: Spring 초기 설정 착수

### Type
TODO_UPDATE

### Summary
- Phase 1의 `백엔드 Spring 초기 설정` 작업을 진행 중(🔄)으로 전환했다.

### Details
- 작업 사유
  - TODO v1.3 순서를 따라 Phase 1의 다음 작업을 수행하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.3.md
- 다음 단계
  - Spring 초기 설정에 대한 PLAN 문서를 작성하고 승인 과정을 거친 뒤 구현을 진행한다.

## [2025-11-26 18:50] Spring 초기 설정 계획 작성

### Type
DESIGN

### Summary
- Phase 1 TODO(백엔드 Spring 초기 설정)를 위해 요구사항/구조/TDD를 정의한 `docs/plan/backend-spring-setup_plan.md`를 추가했다.

### Details
- 작업 사유
  - backend/global 구조, 환경 설정, 기본 테스트 범위를 명확히 한 뒤 구현 단계로 넘어가기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/plan/backend-spring-setup_plan.md (신규)
- 다음 단계
  - 계획을 기준으로 백엔드 초기 설정을 구현하고 TODO 상태를 업데이트한다.

## [2025-11-26 21:26] 스프링 부트 초기 설정 및 글로벌 골격 구성

### Type
STRUCTURAL

### Summary
- backend 모듈을 Spring Boot 4.0.0 / Java21 기준으로 재구성하고 global/domain 패키지, BaseEntity, 예외/설정, 샘플 API, 프로필·로그 설정을 추가했다.

### Details
- 작업 사유
  - Phase 1 TODO(백엔드 Spring 초기 설정) 요구를 충족하고 이후 엔티티 작업을 시작할 수 있는 공용 골격이 필요했음.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/build.gradle, settings.gradle
  - backend/src/main/java/com/classhub/** (main 클래스, global/common/config/error, domain sample 패키지 등)
  - backend/src/main/resources/application*.yml, logback-spring.xml, .env.example
  - backend/src/test/java/com/classhub/** (context, health, auditing 테스트)
  - .gitignore
- 다음 단계
  - global 도메인 공통 구성 정의 TODO를 마무리하며 필요 시 추가 공통 컴포넌트를 확장한다.

## [2025-11-26 21:26] TODO 상태: Spring 초기 설정 완료

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.3.md`에서 "백엔드 Spring 초기 설정"을 ✅, "global 도메인 공통 구성 정의"를 🔄로 갱신했다.

### Details
- 작업 사유
  - 초기 설정 작업이 완료되어 다음 TODO(글로벌 도메인 구성)으로 넘어가기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.3.md
- 다음 단계
  - 글로벌 공통 모듈 세부 구성을 확정하고 TODO를 완료 처리한다.

## [2025-11-26 21:54] Sample API 응답을 RsData 포맷으로 통일

### Type
STRUCTURAL

### Summary
- `SampleController`가 `RsData`를 직접 반환하도록 수정해 ResponseAspect/Global 응답 규칙과 일치시켰다.

### Details
- 작업 사유
  - 기존 ResponseEntity 기반 응답이 전역 RsData 포맷과 달라 일관성이 필요했음.
- 영향받은 테스트
  - `./backend/gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/sample/web/SampleController.java
- 다음 단계
  - 추후 구현되는 도메인 API도 동일한 RsData 규칙을 사용한다.

## [2025-11-26 21:57] 문서 동기화: Spring 초기 설정/응답 규칙

### Type
DESIGN

### Summary
- README, AGENTS, backend-spring-setup 계획 문서를 최신 스택(boot 4.0.0, RsData 응답, global/entity 패키지) 기준으로 갱신했다.

### Details
- 작업 사유
  - 코드 구조와 전역 응답 포맷을 문서와 일치시키고, 새 TODO/PLAN 흐름을 명확히 하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - README.md
  - AGENTS.md
  - docs/plan/backend-spring-setup_plan.md
- 다음 단계
  - 문서 기준에 맞춰 global 공통 구성 TODO를 마저 진행한다.

## [2025-11-26 23:33] Global 공통 구성 계획 작성

### Type
DESIGN

### Summary
- Phase 1 TODO(글로벌 도메인 공통 구성 정의)를 위해 `docs/plan/global-domain-common_plan.md`를 작성하고 공통 엔티티/응답/예외/CSR 요구사항을 정리했다.

### Details
- 작업 사유
  - Phase 2 도메인 구현 전에 통합된 global 모듈 계약을 명문화하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/plan/global-domain-common_plan.md (신규)
- 다음 단계
  - 계획에 따라 global 패키지를 검증/정리하고 TODO 상태를 업데이트한다.

## [2025-11-26 23:36] TODO 상태: 글로벌 공통 구성 완료

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.3.md`에서 "global 도메인 공통 구성 정의"를 완료(✅) 처리했다.

### Details
- 작업 사유
  - global 모듈(BaseEntity/RsData/ResponseAspect/예외) 검증과 테스트(`./backend/gradlew test`)를 통해 계획 요구 사항을 충족했기 때문.
- 영향받은 테스트
  - `./backend/gradlew test`
- 수정한 파일
  - docs/todo/v1.3.md
- 다음 단계
  - Phase 2 엔티티 작업으로 진입하거나 추가 PLAN을 작성한다.

## [2025-11-27 21:31] 요구사항-스펙-프로세스 정비

### Type
DESIGN

### Summary
- `docs/spec/v1.2.md`에 Requirement v1.2 대응 표(FR-001~FR-027)를 추가하고, AGENTS.md에 Requirement→Spec→TODO 절차 규칙을 명문화했다.

### Details
- 작업 사유
  - Requirement 문서와 스펙/PLAN/TODO 간 일관된 버전 관리가 필요했음.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/spec/v1.2.md
  - AGENTS.md
- 다음 단계
  - TODO를 스펙의 FR 매핑에 맞춰 갱신한다.

## [2025-11-27 21:31] TODO v1.3 요구사항 매핑

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.3.md`에 Requirement/Spec 버전 주석을 추가하고, 각 Task에 해당 FR ID를 명시했다.

### Details
- 작업 사유
  - TODO 항목이 어떤 요구사항을 충족하는지 추적하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/todo/v1.3.md
- 다음 단계
  - Phase 2 착수 시 해당 FR 태그를 참고해 구현 우선순위를 정한다.

## [2025-11-27 21:40] Requirement/Spec/AGENT 동기화

### Type
DESIGN

### Summary
- Requirement v1.2에 PLAN 기반 기능 정의 방식을 명시하고, spec v1.2 및 AGENTS 규칙을 동일한 프로세스로 정리했다.

### Details
- 작업 사유
  - 기능 요구사항을 TODO/PLAN에서 세분화하는 현 방식과 문서를 일치시키기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/requirement/v1.2.md
  - docs/spec/v1.2.md
  - AGENTS.md
- 다음 단계
  - TODO를 새 요구사항 설명에 맞춰 유지한다.

## [2025-11-27 21:40] TODO 메타 정보 정비

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.3.md`에 Requirement/Spec 버전 설명을 갱신하고, 작업 설명에서 구 FR ID를 제거했다.

### Details
- 작업 사유
  - TODO 항목이 PLAN 기반으로 변화될 때 혼선을 줄이기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.3.md
- 다음 단계
  - Phase 2 작업 시 PLAN 문서에서 기능 요구를 정의한다.

## [2025-11-27 21:43] Member 엔티티 PLAN 초안

### Type
DESIGN

### Summary
- Member 엔티티 속성/제약/테스트 계획을 정의한 `docs/plan/member-entity_plan.md`를 작성했다.

### Details
- 작업 사유
  - Phase 2 첫 작업으로 Member 엔티티 구현을 준비하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/plan/member-entity_plan.md (신규)
- 다음 단계
  - PLAN을 검토/승인받고 실제 Member 엔티티를 구현한다.

## [2025-11-27 21:50] TODO 상태: Member 엔티티 작업 착수

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.3.md`에서 Member 엔티티 작업을 🔄로 전환하고, PLAN을 최신 조건(전화번호 제외, Lombok 생성자 규칙)으로 갱신했다.

### Details
- 작업 사유
  - Phase 2 첫 작업(Member 엔티티 구현)을 시작하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/todo/v1.3.md
  - docs/plan/member-entity_plan.md
- 다음 단계
  - PLAN에 맞춰 Member 엔티티를 구현하고 테스트를 작성한다.

## [2025-11-27 21:52] Member 엔티티 구현

### Type
STRUCTURAL

### Summary
- Member 엔티티/Role/Repository를 추가하고, unique email 제약·teacherId 필드를 포함한 테스트를 작성했다.

### Details
- 작업 사유
  - Phase 2 첫 엔티티 작업(사용자 관리)을 코드에 반영하기 위함.
- 영향받은 테스트
  - `./backend/gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/member/model/Member.java
  - backend/src/main/java/com/classhub/domain/member/model/MemberRole.java
  - backend/src/main/java/com/classhub/domain/member/repository/MemberRepository.java
  - backend/src/test/java/com/classhub/domain/member/MemberRepositoryTest.java
- 다음 단계
  - Member 관련 서비스/DTO/Controller PLAN을 작성하고 구현을 확장한다.

## [2025-11-27 21:52] TODO 상태: Member 엔티티 완료

### Type
TODO_UPDATE

### Summary
- Member 엔티티 작업을 ✅로 전환했다.

### Details
- 작업 사유
  - Member 엔티티 및 테스트가 마무리되었기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.3.md
- 다음 단계
  - 다음 엔티티(Invitation 등) PLAN/구현을 진행한다.

## [2025-11-27 21:59] Invitation 엔티티 PLAN 초안

### Type
DESIGN

### Summary
- 초대 도메인 요구사항을 정의한 `docs/plan/invitation-entity_plan.md`를 추가했다.

### Details
- 작업 사유
  - Phase 2 두 번째 엔티티(Invitation) 구현을 준비하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/plan/invitation-entity_plan.md (신규)
- 다음 단계
  - PLAN 검토 후 Invitation 엔티티/테스트를 구현한다.

## [2025-11-27 22:10] Invitation PLAN 업데이트

### Type
DESIGN

### Summary
- Invitation 엔티티에서 courseId를 제거하고 Teacher→Assistant 초대는 senderId로만 표현하도록 PLAN을 수정했다.

### Details
- 작업 사유
  - 실제 요구(Teacher에 종속)와 PLAN의 필드 구성을 일치시키기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/invitation-entity_plan.md
- 다음 단계
  - PLAN 기반으로 Invitation 엔티티를 구현한다.

## [2025-11-27 22:13] TODO 상태: Invitation 엔티티 착수

### Type
TODO_UPDATE

### Summary
- Invitation 엔티티 작업을 🔄로 설정해 Phase 2 두 번째 작업을 시작했다.

### Details
- 작업 사유
  - Member 엔티티 완료 후 다음 우선순위인 Invitation 구현을 진행하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.3.md
- 다음 단계
  - PLAN에 따라 Invitation 엔티티를 구현한다.

## [2025-11-27 22:14] Invitation 엔티티 구현

### Type
STRUCTURAL

### Summary
- Invitation 엔티티/Role/Status/Repository를 추가하고 초대 코드 고유값, 상태 전환(expire/accept/revoke) 로직을 구현했다.

### Details
- 작업 사유
  - 초대 기반 Auth/조직 연결을 위한 도메인 모델을 마련하기 위함.
- 영향받은 테스트
  - `./backend/gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/invitation/model/Invitation.java
  - backend/src/main/java/com/classhub/domain/invitation/model/InvitationRole.java
  - backend/src/main/java/com/classhub/domain/invitation/model/InvitationStatus.java
  - backend/src/main/java/com/classhub/domain/invitation/repository/InvitationRepository.java
  - backend/src/test/java/com/classhub/domain/invitation/InvitationRepositoryTest.java
  - docs/spec/v1.2.md (Invitation 필드 갱신)
- 다음 단계
  - 초대 서비스/컨트롤러 PLAN을 작성하고 흐름(accept/revoke)을 구현한다.

## [2025-11-27 22:14] TODO 상태: Invitation 엔티티 완료

### Type
TODO_UPDATE

### Summary
- Invitation 엔티티 작업을 ✅로 전환했다.

### Details
- 작업 사유
  - Invitation 도메인/테스트 구현이 완료되었기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.3.md
- 다음 단계
  - 다음 엔티티(Notice 등) PLAN/구현을 진행한다.

## [2025-11-27 22:16] TODO v1.4 재구성

### Type
TODO_UPDATE

### Summary
- 엔티티 일괄 구현 방식에서 기능 중심(Member/Auth → Teacher/Assistant → Student/Lesson → Course)으로 전환한 `docs/todo/v1.4.md`를 작성했다.

### Details
- 작업 사유
  - 기능 진행 상황에 맞춰 필요한 엔티티/관계를 단계적으로 설계하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md (신규)
- 다음 단계
  - Phase 2 작업을 v1.4 순서대로 진행한다.

## [2025-11-27 22:22] Auth 코어 PLAN 작성

### Type
DESIGN

### Summary
- Spring Security + JWT 기반 로그인/회원가입/초대 검증 흐름을 정의한 `docs/plan/auth-core_plan.md`를 추가했다.

### Details
- 작업 사유
  - Member/Invitation을 활용할 인증 서비스를 설계하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/auth-core_plan.md (신규)
- 다음 단계
  - PLAN을 기준으로 Auth 서비스를 구현하고 TODO 상태를 갱신한다.

## [2025-11-27 22:23] TODO 상태: Auth 서비스 착수

### Type
TODO_UPDATE

### Summary
- Phase 2에서 "Auth 서비스 (Spring Security + JWT + 초대 검증)" 작업을 🔄로 전환했다.

### Details
- 작업 사유
  - Auth PLAN이 준비되어 구현을 바로 이어가기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - PLAN에 따라 Auth 서비스를 구현한다.

## [2025-11-27 22:27] TODO 세분화: Auth 작업 분할

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.4.md`의 Auth 작업을 PLAN 보완, Security 구성, 회원가입/로그인/토큰/초대 검증 등 세부 단계로 쪼갰다.

### Details
- 작업 사유
  - Auth 범위를 작은 단위로 나눠 진행 상황을 명확히 하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - 세분화된 순서대로 PLAN/구현을 진행한다.

## [2025-11-27 22:29] Spring Security Config PLAN 작성

### Type
DESIGN

### Summary
- JWT Provider, Security Filter Chain, PasswordEncoder 구성을 다루는 `docs/plan/auth-security_config_plan.md`를 추가했다.

### Details
- 작업 사유
  - Auth 작업을 세분화한 첫 단계로 Security/Token 레이어 요구사항을 명확히 하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/auth-security_config_plan.md (신규)
- 다음 단계
  - 해당 PLAN을 바탕으로 Security 구성을 구현하고, 이어서 회원가입/로그인/초대 검증 PLAN을 작성한다.

## [2025-11-27 22:29] TODO 상태: Auth PLAN 보완 완료

### Type
TODO_UPDATE

### Summary
- Auth 세분화 작업 중 첫 항목(PLAN 보완)을 ✅로 표시했다.

### Details
- 작업 사유
  - Security/Token PLAN을 작성했으므로 다음 구현 단계로 이동하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - Spring Security Config + JWT Provider 구현을 진행한다.

## [2025-11-27 23:28] Spring Security 구성 TDD 검증

### Type
STRUCTURAL

### Summary
- JwtProvider 단위 테스트, Security 통합 테스트, PasswordEncoder Bean 테스트를 추가해 PLAN의 검증 항목을 충족했다.

### Details
- 작업 사유
  - Auth Security PLAN에서 정의한 토큰/필터/암호화 요구사항을 코드로 검증하기 위함.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/test/java/com/classhub/global/jwt/JwtProviderTest.java
  - backend/src/test/java/com/classhub/global/config/SecurityIntegrationTest.java
  - backend/src/test/java/com/classhub/global/config/PasswordEncoderBeanTest.java
- 다음 단계
  - Auth 서비스 세부 API(회원가입/로그인 등)를 PLAN 순서에 따라 구현한다.

## [2025-11-27 23:29] TODO 상태: Security Config 검증 완료

### Type
TODO_UPDATE

### Summary
- Phase 2 - Auth 서비스 세부 작업 중 "Spring Security Config + PasswordEncoder + JWT Provider 뼈대"를 완료(✅)로 표시했다.

### Details
- 작업 사유
  - Security 구성과 테스트를 마무리해 PLAN 기준을 충족했기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - 다음 작업(Teacher 회원가입 API) 착수 여부를 결정하고 PLAN/구현을 준비한다.

## [2025-11-27 23:35] Teacher 회원가입 PLAN 작성

### Type
DESIGN

### Summary
- Teacher self-onboarding 흐름을 다루는 `docs/plan/auth-teacher-register_plan.md`를 추가해 API/검증/TDD 절차를 정의했다.

### Details
- 작업 사유
  - Phase 2에서 다음 TODO(Teacher 회원가입 API)를 착수하기 전에 요구/테스트 범위를 명확히 하기 위함.
- 영향받은 테스트
  - N/A (문서 작업)
- 수정한 파일
  - docs/plan/auth-teacher-register_plan.md (신규)
- 다음 단계
  - PLAN을 기준으로 Controller/Service/DTO/Test를 구현하고 TODO 상태를 업데이트한다.

## [2025-11-27 23:38] Teacher 회원가입 PLAN 보완

### Type
DESIGN

### Summary
- Teacher register PLAN의 응답을 Member 요약으로 한정하고, DTO Validation(특수문자 포함 8자 이상 패스워드 등) 요구사항을 명시했다.

### Details
- 작업 사유
  - 사용자 피드백에 따라 회원가입 응답에는 토큰을 포함하지 않고, Bean Validation 규칙을 구체적으로 정의해야 했음.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/auth-teacher-register_plan.md
- 다음 단계
  - 갱신된 PLAN에 맞춰 구현/TDD를 진행한다.

## [2025-11-27 23:40] TODO 상태: Teacher 회원가입 작업 착수

### Type
TODO_UPDATE

### Summary
- Phase 2 - Auth 서비스 세부 작업 중 "Teacher 회원가입 API"를 진행 중(🔄)으로 전환했다.

### Details
- 작업 사유
  - PLAN 승인 후 실제 구현/TDD를 시작하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - Teacher 회원가입 API를 PLAN에 따라 구현한다.

## [2025-11-28 00:30] Teacher 회원가입 API 구현

### Type
BEHAVIORAL

### Summary
- `/auth/register/teacher` 엔드포인트를 추가해 이메일 형식/비밀번호 정책을 검증하고 Role=TEACHER 계정을 생성한 뒤 요약 정보를 반환하도록 구현했다.
- 등록, 중복 이메일, 비밀번호 검증 실패를 다루는 통합 테스트를 추가했다.

### Details
- 작업 사유
  - Phase 2 Auth TODO 중 Teacher self-onboarding 흐름을 제공해 이후 기능 검증에 사용할 계정을 만들기 위함.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/auth/web/AuthController.java
  - backend/src/main/java/com/classhub/domain/auth/application/AuthApplicationService.java
  - backend/src/main/java/com/classhub/domain/auth/dto/TeacherRegisterRequest.java
  - backend/src/main/java/com/classhub/domain/auth/dto/TeacherRegisterResponse.java
  - backend/src/main/java/com/classhub/domain/member/repository/MemberRepository.java
  - backend/src/main/java/com/classhub/global/response/RsCode.java
  - backend/src/main/java/com/classhub/global/exception/GlobalExceptionHandler.java
  - backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java
- 다음 단계
  - TODO에 반영하고 다음 Auth 세부 작업(로그인 등)을 준비한다.

## [2025-11-28 00:31] TODO 상태: Teacher 회원가입 완료

### Type
TODO_UPDATE

### Summary
- Phase 2 "Teacher 회원가입 API" 항목을 완료(✅) 처리했다.

### Details
- 작업 사유
  - 엔드포인트 및 TDD 검증이 끝났기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - 다음 Auth 세부 작업(로그인 API 등)으로 진행한다.

## [2025-11-28 00:36] Auth 서비스 계층 테스트 추가

### Type
STRUCTURAL

### Summary
- AuthApplicationService에 대한 단위 테스트를 추가해 Teacher 등록 성공/중복 이메일 예외를 검증했다.

### Details
- 작업 사유
  - Service 계층에서 비즈니스 로직을 독립적으로 검증해야 한다는 규칙을 반영하기 위함.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/test/java/com/classhub/domain/auth/application/AuthApplicationServiceTest.java
- 다음 단계
  - 같은 기준을 다른 기능에도 적용한다.

## [2025-11-28 00:37] 백엔드 AGENT 테스트 계층 지침 추가

### Type
DESIGN

### Summary
- backend/AGENTS.md에 Repository→Service→Controller 순서로 테스트를 작성하고 각 계층의 책임을 명시하는 규칙을 추가했다.

### Details
- 작업 사유
  - 사용자 요청에 따라 테스트 작성 순서와 책임 분리를 명문화하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - backend/AGENTS.md
- 다음 단계
  - 새 규칙을 모든 향후 작업에 적용한다.

## [2025-11-28 00:39] AuthService 네이밍 정리

### Type
STRUCTURAL

### Summary
- Service 클래스 명을 `AuthService`로 통일하고 Controller/Test 참조를 모두 갱신했다.

### Details
- 작업 사유
  - 도메인명+Service 패턴을 유지하겠다는 요구 사항을 반영.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/auth/application/AuthService.java
  - backend/src/main/java/com/classhub/domain/auth/web/AuthController.java
  - backend/src/test/java/com/classhub/domain/auth/application/AuthServiceTest.java
- 다음 단계
  - 동일한 명명 규칙을 향후 서비스에도 적용한다.

## [2025-11-28 01:20] 백엔드 AGENT 테스트 규칙 보완

### Type
DESIGN

### Summary
- Service 테스트는 Mockito 기반 순수 단위 테스트로 작성하고, SpringBootTest/통합테스트는 Controller에서만 사용한다는 지침을 AGENTS에 추가했다.

### Details
- 작업 사유
  - 테스트 계층별 책임을 명확히 하고, Service 테스트에서 불필요한 컨텍스트 로딩을 방지하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - backend/AGENTS.md
- 다음 단계
  - 규칙을 다른 기능에도 일관되게 적용한다.

## [2025-11-28 01:22] AuthService 단위 테스트 Mock 기반 전환

### Type
STRUCTURAL

### Summary
- `AuthServiceTest`를 Mockito Extension 기반으로 바꿔 Repository/PasswordEncoder를 Mock하고, 저장 시 Reflection으로 ID/타임스탬프를 주입하도록 수정했다.

### Details
- 작업 사유
  - Service 테스트는 Spring Context 없이 Mock으로만 작성해야 한다는 신규 규칙을 적용하기 위함.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/test/java/com/classhub/domain/auth/application/AuthServiceTest.java
- 다음 단계
  - 동일 방식으로 다른 Service 테스트도 정비한다.

## [2025-11-28 01:32] Service 테스트 전략 롤백

### Type
STRUCTURAL

### Summary
- 사용자 요청에 따라 Service 테스트를 다시 SpringBootTest 통합 방식으로 되돌리고, AGENT 지침도 원상복구했다.

### Details
- 작업 사유
  - 당장 컨텍스트를 띄운 테스트가 필요하다는 요구 반영.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/test/java/com/classhub/domain/auth/application/AuthServiceTest.java
  - backend/AGENTS.md
- 다음 단계
  - 필요 시 추후 다시 분리한다.

## [2025-11-28 01:55] API Base Path를 /api/v1로 통일

### Type
BEHAVIORAL

### Summary
- 모든 엔드포인트를 `/api/v1` prefix로 노출하도록 AuthController, Security 설정, 통합 테스트를 수정했다.

### Details
- 작업 사유
  - 사용자 요청에 따라 API 경로 규칙을 `/api/v1`로 일관되게 맞추기 위함.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/auth/web/AuthController.java
  - backend/src/main/java/com/classhub/global/config/SecurityConfig.java
  - backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java
  - backend/src/test/java/com/classhub/global/config/SecurityIntegrationTest.java
- 다음 단계
  - 향후 추가되는 Controller도 동일한 prefix 규칙을 따른다.

## [2025-11-28 02:00] 로그인/토큰 발급 PLAN 작성

### Type
DESIGN

### Summary
- 로그인 및 Access/Refresh 토큰 발급·재발급 흐름을 정의한 `docs/plan/auth-login_plan.md`를 추가했다.

### Details
- 작업 사유
  - Phase 2 Auth TODO의 다음 항목(로그인 API)을 진행하기 전에 요구사항/테스트 전략을 명확히 하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/auth-login_plan.md (신규)
- 다음 단계
  - PLAN 승인 후 로그인/Refresh API를 TDD로 구현한다.

## [2025-11-28 02:01] TODO 상태: 로그인 API 착수

### Type
TODO_UPDATE

### Summary
- Auth 서비스 세부 작업 중 "로그인 API + Access/Refresh 발급" 항목을 진행 중(🔄)으로 전환했다.

### Details
- 작업 사유
  - PLAN 작성이 완료되어 다음 구현 단계에 착수하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - PLAN에 따라 로그인 API와 Refresh 발급을 구현한다.

## [2025-11-28 02:05] 로그인 PLAN 업데이트 (응답/테스트 범위 보완)

### Type
DESIGN

### Summary
- LoginResponse에서 email/authority를 제외하고, Controller/Service 테스트 책임을 명확히 한 PLAN으로 갱신했다.

### Details
- 작업 사유
  - 사용자 피드백에 따라 로그인 응답을 토큰/만료 정보에 집중시키고 테스트 전략을 재정의해야 했음.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/auth-login_plan.md
- 다음 단계
  - 갱신된 PLAN에 맞춰 구현/TDD를 진행한다.

## [2025-11-28 13:00] 로그인/Refresh API 구현

### Type
BEHAVIORAL

### Summary
- `/api/v1/auth/login`과 `/api/v1/auth/refresh`를 추가해 이메일/비밀번호 검증, JWT Access/Refresh 발급·재발급, 만료 시간 응답을 구현했다.
- Service/Controller 테스트를 작성해 성공/실패, Validation, Refresh 흐름을 검증했다.

### Details
- 작업 사유
  - Phase 2 Auth TODO에서 로그인/토큰 발급 기능을 제공해야 이후 도메인 API 접근이 가능하기 때문.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/auth/application/AuthService.java
  - backend/src/main/java/com/classhub/domain/auth/web/AuthController.java
  - backend/src/main/java/com/classhub/domain/auth/dto/LoginRequest.java
  - backend/src/main/java/com/classhub/domain/auth/dto/LoginResponse.java
  - backend/src/main/java/com/classhub/domain/auth/dto/RefreshRequest.java
  - backend/src/test/java/com/classhub/domain/auth/application/AuthServiceTest.java
  - backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java
  - backend/src/test/java/com/classhub/global/config/SecurityIntegrationTest.java (경로 검증 반영 시 이미 수정됨)
- 다음 단계
  - Refresh 토큰 저장/로그아웃 처리, 초대 기반 가입 등 남은 Auth 작업을 이어간다.

## [2025-11-28 13:01] TODO 상태: 로그인 API 완료

### Type
TODO_UPDATE

### Summary
- Phase 2의 "로그인 API + Access/Refresh 발급" 작업을 완료(✅) 처리했다.

### Details
- 작업 사유
  - LOGIN/REFRESH API 구현 및 테스트가 마무리되었기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - 다음 세부 작업(Refresh 재발급/로그아웃 처리 등)을 계획한다.

## [2025-11-28 13:05] Refresh/로그아웃 PLAN 작성

### Type
DESIGN

### Summary
- Refresh 토큰 블랙리스트 기반 로그아웃 요구사항을 정의한 `docs/plan/auth-refresh_logout_plan.md`를 추가했다.

### Details
- 작업 사유
  - TODO 항목(Refresh 토큰 재발급/로그아웃 처리)을 구현하기 전 엔티티/서비스/API/TDD 범위를 명확히 하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/auth-refresh_logout_plan.md (신규)
- 다음 단계
  - PLAN을 기준으로 RefreshToken 엔티티/서비스/컨트롤러를 구현한다.

## [2025-11-28 13:05] TODO 상태: Refresh/로그아웃 작업 착수

### Type
TODO_UPDATE

### Summary
- Phase 2의 "Refresh 토큰 재발급/로그아웃 처리" 작업을 진행 중(🔄)으로 전환했다.

### Details
- 작업 사유
  - PLAN 작성과 함께 실제 구현 준비를 시작하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - PLAN에 맞춰 RefreshToken 저장소/로그아웃 API를 구현한다.

## [2025-11-28 23:15] Refresh 블랙리스트 + 로그아웃 API 구현

### Type
BEHAVIORAL

### Summary
- In-Memory RefreshTokenStore를 추가하고, `/api/v1/auth/logout` API와 Refresh 블랙리스트 검사 로직을 구현했다.
- 로그아웃 이후 해당 Refresh 토큰으로는 재발급이 불가능하며, Controller/Service 테스트로 검증했다.

### Details
- 작업 사유
  - TODO 항목(Refresh 토큰 재발급/로그아웃 처리)을 완료하고, 명시적 로그아웃 시 토큰을 폐기하기 위함.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/auth/application/AuthService.java
  - backend/src/main/java/com/classhub/domain/auth/web/AuthController.java
  - backend/src/main/java/com/classhub/domain/auth/dto/LogoutRequest.java
  - backend/src/main/java/com/classhub/domain/auth/dto/RefreshRequest.java (재사용)
  - backend/src/main/java/com/classhub/domain/auth/token/RefreshTokenStore.java (신규)
  - backend/src/main/java/com/classhub/domain/auth/token/InMemoryRefreshTokenStore.java (신규)
  - backend/src/main/java/com/classhub/global/jwt/JwtProvider.java
  - backend/src/test/java/com/classhub/domain/auth/application/AuthServiceTest.java
  - backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java
  - backend/src/test/java/com/classhub/domain/auth/token/InMemoryRefreshTokenStoreTest.java (신규)
- 다음 단계
  - logoutAll/Redis 기반 확장은 후속 작업으로 남겨둔다.

## [2025-11-28 23:16] TODO 상태: Refresh/로그아웃 작업 완료

### Type
TODO_UPDATE

### Summary
- Phase 2 - Auth 서비스 세부 작업 중 "Refresh 토큰 재발급/로그아웃 처리"를 완료(✅)로 표시했다.

### Details
- 작업 사유
  - 블랙리스트/로그아웃 기능 구현 및 테스트가 완료되었기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - 초대 코드 검증/가입 흐름 등 남은 Auth 작업을 진행한다.

## [2025-11-28 23:18] 초대 코드 검증/가입 PLAN 작성

### Type
DESIGN

### Summary
- 초대 코드 검증 및 초대 기반 회원가입 API 요구사항을 정의한 `docs/plan/auth-invitation_signup_plan.md`를 추가했다.

### Details
- 작업 사유
  - Phase 2 Auth TODO의 다음 항목(초대 코드 검증/가입)을 구현하기 전 문제 정의/테스트 전략을 명확히 하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/auth-invitation_signup_plan.md (신규)
- 다음 단계
  - PLAN을 기준으로 Service/Controller/Repository 변경을 진행한다.

## [2025-11-28 23:18] TODO 상태: 초대 코드 검증 작업 착수

### Type
TODO_UPDATE

### Summary
- "초대 코드 검증 API + 초대 기반 회원가입" 작업을 진행 중(🔄)으로 전환했다.

### Details
- 작업 사유
  - PLAN 작성이 완료되어 구현 단계를 시작하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - Invitation 검증/가입 API를 PLAN에 따라 개발한다.

## [2025-11-29 00:04] 초대 코드 검증/가입 API 구현

### Type
BEHAVIORAL

### Summary
- `/api/v1/auth/invitations/verify`와 `/api/v1/auth/register/invited` 엔드포인트를 추가하고, InvitationAuthService/DTO/테스트를 통해 초대 기반 회원가입 흐름을 완성했다.

### Details
- 작업 사유
  - Phase 2 Auth TODO의 마지막 세부 작업(초대 코드 검증 + 가입)을 완료해 Teacher→Assistant, Assistant→Student 초대 플로우를 지원하기 위함.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/auth/application/InvitationAuthService.java (신규)
  - backend/src/main/java/com/classhub/domain/auth/application/AuthService.java
  - backend/src/main/java/com/classhub/domain/auth/web/AuthController.java
  - backend/src/main/java/com/classhub/domain/auth/dto/InvitationVerifyRequest.java (신규)
  - backend/src/main/java/com/classhub/domain/auth/dto/InvitationVerifyResponse.java (신규)
  - backend/src/main/java/com/classhub/domain/auth/dto/InvitationRegisterRequest.java (신규)
  - backend/src/main/java/com/classhub/domain/auth/dto/LoginRequest.java (재사용)
  - backend/src/main/java/com/classhub/domain/invitation/repository/InvitationRepository.java
  - backend/src/main/java/com/classhub/global/response/RsCode.java
  - backend/src/test/java/com/classhub/domain/auth/application/InvitationAuthServiceTest.java (신규)
  - backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java
  - docs/plan/auth-invitation_signup_plan.md (기존 계획에 부합)
- 다음 단계
  - 초대 생성/관리 API 필요 시 별도 PLAN으로 진행한다.

## [2025-11-29 00:05] TODO 상태: 초대 코드 검증 작업 완료

### Type
TODO_UPDATE

### Summary
- "초대 코드 검증 API + 초대 기반 회원가입" 작업을 완료(✅)로 표시했다.

### Details
- 작업 사유
  - 초대 검증/가입 API 및 테스트가 완료되었기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - Phase 2 완료 후 다음 Phase로 넘어갈 준비를 한다.

## [2025-11-29 00:10] TODO 상태: Auth Epic 정리

### Type
TODO_UPDATE

### Summary
- Phase 2의 "Auth 서비스 (Spring Security + JWT + 초대 검증)" Epic과 하위 항목, 그리고 Auth & Security Epic의 “Teacher 회원가입/로그인 API”를 완료(✅)로 정리했다.

### Details
- 작업 사유
  - 회원가입/로그인/Refresh/로그아웃/초대 기반 가입 등 Auth 관련 작업이 모두 끝났기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - Phase 3 혹은 다른 우선순위 작업을 선택한다.

## [2025-11-29 00:15] Auth & Security 초대 API 상위 PLAN

### Type
DESIGN

### Summary
- Auth & Security Epic 관점에서 초대 기반 가입/검증 API 요구와 확장 포인트를 정리한 `docs/plan/auth-invitation_api_plan.md`를 작성했다.

### Details
- 작업 사유
  - TODO 항목(Invitation 기반 가입/검증 API)을 상위 Epic에서 관리하기 위한 기준 문서를 마련.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/auth-invitation_api_plan.md (신규)
- 다음 단계
  - 계획에 맞춰 초대 생성/관리 API PLAN을 이어서 작성한다.

## [2025-11-29 00:20] TODO 업데이트: 초대 생성/프런트 PoC 작업 추가

### Type
TODO_UPDATE

### Summary
- Phase 2에 “Invitation 생성/관리 API” Epic을 추가하고, Auth & Security Epic 항목을 완료 처리했다. 또한 Auth 문서/UX 섹션에 초대 기반 E2E 프런트 PoC 작업을 추가했다.

### Details
- 작업 사유
  - Requirement/Spec v1.2 기반 초대 생성/취소 API와 초대 흐름 프런트 검증 작업이 아직 남아 있기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - 새로운 Epic에 대한 PLAN을 수립하고 구현을 진행한다.

## [2025-11-29 00:25] Invitation 생성/관리 PLAN 작성

### Type
DESIGN

### Summary
- Teacher/Assistant 초대 생성/목록/취소 API 요구사항을 정의한 `docs/plan/invitation-management_plan.md`를 작성했다.

### Details
- 작업 사유
  - TODO에 추가된 “Invitation 생성/관리 API” Epic을 시작하기 이전에 기능 범위와 TDD 전략을 명확히 하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/invitation-management_plan.md (신규)
- 다음 단계
  - PLAN에 따라 실제 초대 생성/목록/취소 API를 구현한다.

## [2025-11-29 00:27] Invitation PLAN: 입력 필드 정리

### Type
DESIGN

### Summary
- 초대 생성 API에서 note/만료 기간 입력을 제거하고, 시스템 기본 만료값만 사용하도록 PLAN을 수정했다.

### Details
- 작업 사유
  - 사용자 피드백에 따라 초대 요청 시 별도 만료 기간이나 메모를 받지 않도록 설계를 단순화.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/invitation-management_plan.md
- 다음 단계
  - 변경된 요구사항에 맞춰 구현을 진행한다.

## [2025-11-29 00:29] Invitation PLAN: 역할별 입력 조건 보완

### Type
DESIGN

### Summary
- Teacher→Assistant 초대는 email만 입력하고, Assistant→Student 초대는 `studentProfileId` + email을 요구하도록 PLAN을 수정했다.

### Details
- 작업 사유
  - UI/요구사항에 따라 학생 초대 시 특정 StudentProfile과 이메일을 매칭해야 함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/invitation-management_plan.md
- 다음 단계
  - 수정된 요구사항에 맞춰 구현을 진행한다.

## [2025-11-29 01:25] Invitation PLAN: 학생 초대 권한/검증 수정

### Type
DESIGN

### Summary
- 학생 초대 API를 Teacher와 Assistant 모두 사용할 수 있도록 하고, StudentProfile 이메일 일치 검증 요구를 제거하도록 PLAN을 업데이트했다.

### Details
- 작업 사유
  - 사용자 요청에 따라 학생 초대 절차에서 이메일 매칭을 강제하지 않고 Teacher도 직접 학생을 초대할 수 있어야 함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/invitation-management_plan.md
- 다음 단계
  - 구현 로직과 테스트를 해당 요구에 맞게 조정한다.

## [2025-11-29 01:21] Invitation 생성/관리 API 구현

### Type
BEHAVIORAL

### Summary
- Teacher/Assistant 초대 생성·목록·취소 API를 추가하고, InvitationService/Controller/DTO, 테스트를 구현했다.

### Details
- 작업 사유
  - Requirement/Spec에 정의된 Invitation CRUD 흐름을 실제 API로 제공하기 위함.
- 영향받은 테스트
  - `./gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/invitation/application/InvitationService.java (신규)
  - backend/src/main/java/com/classhub/domain/invitation/web/InvitationController.java (신규)
  - backend/src/main/java/com/classhub/domain/invitation/dto/** (신규)
  - backend/src/main/java/com/classhub/domain/invitation/repository/InvitationRepository.java
  - backend/src/main/java/com/classhub/global/response/RsCode.java
  - backend/src/test/java/com/classhub/domain/invitation/application/InvitationServiceTest.java (신규)
  - backend/src/test/java/com/classhub/domain/invitation/web/InvitationControllerTest.java (신규)
  - backend/src/test/java/com/classhub/domain/auth/application/InvitationAuthServiceTest.java (간접 영향 없음)
- 다음 단계
  - 초대 만료 정리 로직 및 프런트 PoC 작업을 준비한다.

## [2025-11-29 01:22] TODO 상태: Invitation 생성/관리 API 완료

### Type
TODO_UPDATE

### Summary
- Invitation 생성/관리 Epic에서 초대 생성/목록/취소 작업을 완료(✅)로 표시하고 Auth & Security Epic도 전부 ✅로 정리했다.

### Details
- 작업 사유
  - 초대 관련 API 구현이 끝났기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - 남은 "초대 만료/정리 로직" 및 Auth 문서/프런트 작업을 진행한다.

## [2025-12-03 15:20] TODO 업데이트: Invitation & Auth E2E 준비

### Type
TODO_UPDATE

### Summary
- Phase 2의 초대/인증 관련 Epic을 E2E 준비 방향으로 재구성하고, StudentProfile / 프런트 PoC / Playwright 데모 작업을 추가했다.

### Details
- 작업 사유
  - 초대→로그인→회원가입→로그아웃 흐름을 실제 프런트/테스트로 검증하기 위해 StudentProfile 선행 작업과 데모 준비가 필요하다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.4.md
- 다음 단계
  - StudentProfile PLAN 작성 및 구현부터 시작한다.

## [2025-12-03 15:25] TODO v1.5 정리

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.5.md`를 추가해 Phase 2를 StudentProfile/E2E 검증 중심으로 재편하고, Phase3~6 로드맵(핵심 기능, 리팩토링, 배포, 프론트 디자인)을 새롭게 정의했다.

### Details
- 작업 사유
  - Auth/Invitation 기능을 실제 프론트에서 검증하기 위한 우선순위를 반영하고, 이후 Phase 작업 순서를 명확히 하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.5.md (신규)
- 다음 단계
  - Phase 2의 StudentProfile PLAN을 작성하고 구현에 착수한다.

## [2025-12-03 16:01] TODO 상태: StudentProfile & PersonalLesson 작업 착수

### Type
TODO_UPDATE

### Summary
- Phase 2의 "StudentProfile 엔티티 + Repository + CRUD (Teacher 전용), PersonalLesson 개발" 작업을 진행 상태(🔄)로 변경하고, 다음 단계 계획을 명확히 했다.

### Details
- 작업 사유
  - "go" 명령에 따라 Phase 2의 첫 번째 대기 작업을 시작했음을 기록하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.5.md
- 다음 단계
  - StudentProfile & PersonalLesson 설계 문서를 작성하고 사용자 검토를 기다린다.

## [2025-12-03 16:01] StudentProfile & PersonalLesson 설계 문서 초안

### Type
DESIGN

### Summary
- StudentProfile/PersonalLesson 요구사항과 API/도메인/TDD 전략을 정의한 `docs/plan/studentprofile-personallesson_plan.md` 초안을 작성했다.

### Details
- 작업 사유
  - TODO에서 정의된 기능 구현 전에 PLAN 작성이 선행되어야 하므로 요구/스펙을 구조화하였다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/studentprofile-personallesson_plan.md (신규)
- 다음 단계
  - 설계 문서 리뷰/승인 후 TDD 및 구현을 진행한다.

## [2025-12-03 16:10] StudentProfile PLAN 요구사항 반영

### Type
DESIGN

### Summary
- StudentProfile 필수/선택 필드를 사용자 요청대로 조정하고, 담당 조교/학번/초대 연동 정책을 설계 문서(`docs/plan/studentprofile-personallesson_plan.md`)에 반영했다.

### Details
- 작업 사유
  - 부모 이름 필수 제거, 학생 번호·담당 조교 필수 추가, memberId/defaultClinicSlotId 동작 명시 등 요구사항을 수용하기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/studentprofile-personallesson_plan.md
- 다음 단계
  - 문서 승인을 받은 뒤 StudentProfile/PersonalLesson TDD를 시작한다.

## [2025-12-03 16:24] TODO 상태: StudentProfile & PersonalLesson 구현 완료

### Type
TODO_UPDATE

### Summary
- Phase 2의 "StudentProfile 엔티티 + Repository + CRUD (Teacher 전용), PersonalLesson 개발" 작업을 완료(✅)로 표시하고 후속 데이터 시드/프런트 작업을 준비할 수 있게 했다.

### Details
- 작업 사유
  - StudentProfile/PersonalLesson 백엔드 구현 및 테스트를 마쳤기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.5.md
- 다음 단계
  - 남은 Phase 2 작업(시드/프런트 PoC 등)을 진행한다.

## [2025-12-03 16:24] StudentProfile & PersonalLesson 도메인/CRUD 구현

### Type
BEHAVIORAL

### Summary
- Course/StudentProfile/PersonalLesson 엔티티·레포·서비스·컨트롤러를 추가하고 Teacher 중심 CRUD + 검색/PersonalLesson 기록 기능을 구현했다.

### Details
- 작업 사유
  - 초대 이후 학생 정보를 관리하고 개별 진도 기록을 남길 수 있는 Requirement v1.2 기능을 제공하기 위함.
- 영향받은 테스트
  - `./gradlew test` (Gradle 배포본 다운로드 시 네트워크 제한으로 실행 불가, 수동 검증 필요)
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/course/model/Course.java (신규)
  - backend/src/main/java/com/classhub/domain/course/repository/CourseRepository.java (신규)
  - backend/src/main/java/com/classhub/domain/studentprofile/** (신규 서비스/엔티티/DTO/컨트롤러)
  - backend/src/main/java/com/classhub/domain/personallesson/** (신규 서비스/엔티티/DTO/컨트롤러)
  - backend/src/main/java/com/classhub/global/response/PageResponse.java (신규)
  - backend/src/main/java/com/classhub/global/response/RsCode.java
  - backend/src/test/java/com/classhub/domain/studentprofile/** (신규)
  - backend/src/test/java/com/classhub/domain/personallesson/** (신규)
- 다음 단계
  - Gradle 배포본이 준비된 환경에서 테스트를 실행하고, 프런트 데모/시드 데이터 작업으로 이어간다.

## [2025-12-03 16:55] PersonalLesson↔StudentProfile 연관관계 및 DTO 패키지 정리

### Type
STRUCTURAL

### Summary
- PersonalLesson이 StudentProfile을 `@ManyToOne`으로 참조하도록 리팩터링하고, StudentProfile/PersonalLesson DTO를 request/response 패키지로 분리했다.

### Details
- 작업 사유
  - PersonalLesson이 학생 프로필과 강하게 결합돼 있어 FK 무결성과 Lazy 로딩을 활용할 수 있도록 엔티티 연관관계를 명확히 하고, DTO 패키지 구조를 일관성 있게 정리하기 위함.
- 영향받은 테스트
  - `GRADLE_USER_HOME=./.gradle ./gradlew test` (Gradle 배포본 다운로드 시 네트워크 차단으로 실행 불가)
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/personallesson/** (엔티티, 레포, 서비스, 컨트롤러, DTO)
  - backend/src/main/java/com/classhub/domain/studentprofile/dto/** (패키지 구조 변경)
  - backend/src/test/java/com/classhub/domain/personallesson/**, backend/src/test/java/com/classhub/domain/studentprofile/** (패키지/관계 변경 반영)
- 다음 단계
  - 네트워크가 허용되는 환경에서 테스트를 실행하고 이후 기능 개발을 이어간다.

## [2025-12-03 17:08] 테스트 실행 환경 정비 및 Gradle 테스트 통과

### Type
BUGFIX

### Summary
- 보안 필터 없이 `@AuthenticationPrincipal`을 모킹할 수 있도록 MockMvc 커스텀 필터와 RequestPostProcessor를 추가하고, 수동 JSON 직렬화로 `ObjectMapper` 의존 없이 컨트롤러 테스트를 재작성하여 `./gradlew test`가 통과하도록 했다.

### Details
- 작업 사유
  - Gradle 테스트가 `AutoConfigureMockMvc`/Jackson 미존재 및 인증 주입 실패로 컴파일/실행되지 않아 CI를 막고 있었음.
- 영향받은 테스트
  - `GRADLE_USER_HOME=./.gradle ./gradlew test` (성공)
- 수정한 파일
  - backend/src/test/java/com/classhub/domain/studentprofile/web/StudentProfileControllerTest.java
  - backend/src/test/java/com/classhub/domain/personallesson/web/PersonalLessonControllerTest.java
  - backend/src/main/java/com/classhub/domain/personallesson/web/PersonalLessonController.java (PatchMapping import)
- 다음 단계
  - 향후 테스트 작성 시 동일한 MockMvc 헬퍼를 재사용하고, 필요 시 실제 JWT 인증 흐름으로 전환한다.

## [2025-12-03 17:16] StudentProfile 필드 정리 및 나이/연락처 스펙 반영

### Type
STRUCTURAL

### Summary
- StudentProfile에서 `parentName`, `memo`, `studentNumber`를 정리하고 `phoneNumber` 및 `age`를 도입했으며, DTO/서비스/테스트/PLAN 문서를 모두 일관되게 업데이트했다.

### Details
- 작업 사유
  - 최신 요구에 맞춰 학생 고유 식별을 전화번호로 통일하고, 나이 정보를 추가하며 불필요한 필드를 제거하기 위함.
- 영향받은 테스트
  - `GRADLE_USER_HOME=./.gradle ./gradlew test` (성공)
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/studentprofile/** (모델/DTO/서비스/리포지토리/컨트롤러 응답)
  - backend/src/main/java/com/classhub/global/response/RsCode.java
  - backend/src/test/java/com/classhub/domain/studentprofile/** 및 backend/src/test/java/com/classhub/domain/personallesson/** (요청/검증 업데이트)
  - docs/plan/studentprofile-personallesson_plan.md
- 다음 단계
  - 새로운 필드에 맞춰 프런트/기타 도메인 연동 시나리오를 점검한다.

## [2025-12-03 17:47] TODO 업데이트: Swagger 구축 작업 추가

### Type
TODO_UPDATE

### Summary
- Phase 2의 Invitation & Auth E2E 준비 목록에 SpringDoc 기반 Swagger UI 구축 작업을 추가했다.

### Details
- 작업 사유
  - 각 API를 직접 호출/검증할 수 있도록 문서화 및 테스트 UI가 필요하다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.5.md
- 다음 단계
  - Swagger 설정 PLAN/TODO를 작성하고 구현을 진행한다.

## [2025-12-03 17:49] TODO 착수 변경: Swagger 구축

### Type
TODO_UPDATE

### Summary
- Phase 2에서 Swagger 구축 작업을 진행 상태(🔄)로, StudentProfile 데이터 시드 작업은 다시 대기로 조정했다.

### Details
- 작업 사유
  - 사용자가 “다음 작업”으로 Swagger를 지목했기 때문에 우선순위를 재정렬했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.5.md
- 다음 단계
  - Swagger PLAN 문서를 작성하고 구현을 시작한다.

## [2025-12-03 17:50] Swagger 구축 PLAN 초안

### Type
DESIGN

### Summary
- SpringDoc 기반 Swagger UI 구성 목표, 요구사항, 설정 전략, TDD 계획을 정리한 `docs/plan/swagger_plan.md`를 작성했다.

### Details
- 작업 사유
  - API를 직접 호출/검증할 수 있는 자동 문서/테스트 UI가 필요해 단계별 계획이 요구되었다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/swagger_plan.md (신규)
- 다음 단계
  - 계획에 따라 SpringDoc 의존성과 설정을 추가하고, 테스트/보안 정책을 구현한다.

## [2025-12-03 17:59] Swagger UI 및 OpenAPI 문서화 구현

### Type
BEHAVIORAL

### Summary
- SpringDoc(OpenAPI 3) 의존성을 추가하고 `SwaggerConfig`, Security 허용 정책, 컨트롤러 Tag/Operation 메타를 구성해 Swagger UI에서 모든 API를 호출할 수 있도록 했다.

### Details
- 작업 사유
  - API를 직접 호출·검증하려는 요구에 맞춰 자동 문서와 테스트 UI를 제공하기 위함.
- 영향받은 테스트
  - `GRADLE_USER_HOME=./.gradle ./gradlew test`
- 수정한 파일
  - backend/build.gradle
  - backend/src/main/java/com/classhub/global/config/SwaggerConfig.java (신규)
  - backend/src/main/java/com/classhub/global/config/SecurityConfig.java
  - backend/src/main/java/com/classhub/domain/**/web/*.java (Auth, Invitation, StudentProfile, CourseStudent, PersonalLesson, StudentPersonalLesson, Sample)
- 다음 단계
  - 필요 시 운영 프로필 접근 제어/문서 자동 배포 정책을 정비한다.

## [2025-12-03 17:59] TODO 완료: Swagger 구축

### Type
TODO_UPDATE

### Summary
- Phase 2의 "SpringDoc 기반 Swagger UI 구축 및 API 스펙 자동화" 작업을 ✅로 표시했다.

### Details
- 작업 사유
  - Swagger 구축 구현이 완료되었기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.5.md
- 다음 단계
  - 다음 우선순위인 StudentProfile 시드/업데이트 흐름 작업을 준비한다.

## [2025-12-03 18:15] Swagger All 그룹 추가

### Type
STRUCTURAL

### Summary
- SwaggerConfig에 모든 엔드포인트를 한 번에 보여주는 `All` 그룹을 추가해 Swagger UI에서 전체 API를 한 화면에서 볼 수 있도록 했다.

### Details
- 작업 사유
  - 각 그룹을 개별로 클릭하지 않고 전체 엔드포인트를 빠르게 확인하고자 하는 요구를 반영했다.
- 영향받은 테스트
  - N/A (구성 변경)
- 수정한 파일
  - backend/src/main/java/com/classhub/global/config/SwaggerConfig.java
- 다음 단계
  - 필요 시 UI 기본 그룹을 `All`로 설정하거나 문서 가이드를 추가한다.

## [2025-12-03 18:28] DTO 패키지 분리 및 Sample 패키지 제거

### Type
STRUCTURAL

### Summary
- 모든 도메인의 DTO를 request/response 패키지로 이동하고, 사용하지 않는 `domain.sample` 패키지와 관련 테스트를 제거했다.

### Details
- 작업 사유
  - DTO 위치를 일관성 있게 정리하고 불필요한 샘플 코드를 정리해 유지보수를 단순화하기 위함.
- 영향받은 테스트
  - `GRADLE_USER_HOME=./.gradle ./gradlew test`
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/auth/** (DTO 패키지, 서비스, 컨트롤러)
  - backend/src/main/java/com/classhub/domain/invitation/**
  - backend/src/test/java/com/classhub/domain/auth/**, backend/src/test/java/com/classhub/domain/invitation/**
  - backend/src/test/java/com/classhub/global/entity/BaseEntityAuditingTest.java
  - backend/src/main/java/com/classhub/global/config/SwaggerConfig.java (Sample 그룹 제거)
  - backend/src/main/java/com/classhub/domain/sample/**, backend/src/test/java/com/classhub/domain/sample/** (삭제)
- 다음 단계
  - 남은 TODO(데이터 시드 등) 작업으로 진행한다.

## [2025-12-03 18:28] TODO 완료: DTO 패키지 분리 + sample 삭제

### Type
TODO_UPDATE

### Summary
- Phase 2의 “모든 dto 패키지 request/response로 분리 + domain/sample 패키지 삭제” 항목을 완료로 표시했다.

### Details
- 작업 사유
  - DTO 재조직 및 샘플 코드 제거가 완료되었기 때문.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.5.md
- 다음 단계
  - StudentProfile 시드/업데이트 작업을 준비한다.

## [2025-12-03 18:34] Bootstrap Data PLAN 초안

### Type
DESIGN

### Summary
- StudentProfile 기반 데이터 시드/업데이트 흐름의 범위를 정의하고, Member/Teacher/Assistant/StudentProfile/PersonalLesson을 자동 주입할 `BootstrapDataRunner` 설계를 `docs/plan/bootstrap-data_plan.md`로 정리했다.

### Details
- 작업 사유
  - TODO Phase 2에서 요구한 seed/업데이트 작업을 시작하려면 어떤 데이터 세트를 언제/어떻게 생성할지 먼저 문서로 확정해야 한다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/bootstrap-data_plan.md (신규)
- 다음 단계
  - PLAN 내용을 기반으로 Runner/Fixture 구현 및 TODO 진행 상황을 업데이트한다.

## [2025-12-03 18:41] Bootstrap Data PLAN 피드백 반영

### Type
DESIGN

### Summary
- global.init 패키지/`BaseInitData` 구성, Teacher별 Assistant 3명·Course 3개·StudentProfile 30명·PersonalLesson 3건씩 생성 요구, Course 단순 조회 용도 등의 피드백을 `docs/plan/bootstrap-data_plan.md`에 반영했다.

### Details
- 작업 사유
  - 사용자 요청에 따라 seed 데이터 규모와 패키지 구조를 구체화해야 다음 구현 단계가 명확해졌다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/bootstrap-data_plan.md
- 다음 단계
  - 확정된 PLAN에 맞춰 global.init Runner/Fixture를 구현한다.

## [2025-12-03 18:47] Bootstrap Runner 순서 정비

### Type
DESIGN

### Summary
- Invitation이 StudentProfile 정보를 참조하도록 명확히 하기 위해 `docs/plan/bootstrap-data_plan.md`에서 Runner 실행 순서를 Member → Course → StudentProfile → Invitation → PersonalLesson으로 조정했다.

### Details
- 작업 사유
  - 초대 데이터가 StudentProfile을 참조해야 해 이전 순서대로면 FK 매핑이 불가능했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/bootstrap-data_plan.md
- 다음 단계
  - 조정된 순서를 기준으로 Runner/Fixture 구현을 진행한다.

## [2025-12-03 18:53] Bootstrap 데이터 러너 구현

### Type
BEHAVIORAL

### Summary
- `global.init` 패키지에 BaseInitData/Runner/Context와 Member·Course·StudentProfile·Invitation·PersonalLesson 시드를 추가하고, local/dev 프로필에서만 실행되는 부트스트랩 플로우를 완성했다.

### Details
- 작업 사유
  - Phase 2의 StudentProfile 기반 데이터 시드/업데이트 흐름을 자동화해 Swagger/프런트/E2E 검증용 데이터를 즉시 확보해야 했다.
- 영향받은 테스트
  - `GRADLE_USER_HOME=../.gradle ./gradlew test` (Gradle 배포본 다운로드가 네트워크 차단으로 실패)
- 수정한 파일
  - backend/src/main/java/com/classhub/domain/** (Member/StudentProfile/Invitation 모델, Course/StudentProfile/PersonalLesson Repository)
  - backend/src/main/java/com/classhub/global/init/** (BaseInitData, Runner, Seed Context 및 도메인별 InitData)
  - backend/src/main/resources/application-local.yml, README.md
- 다음 단계
  - 네트워크 접근이 가능한 환경에서 테스트를 실행해 시드 로직을 검증하고, 필요 시 추가 Fixture나 force 옵션을 조정한다.

## [2025-12-03 18:53] TODO 완료: 기초 데이터 저장

### Type
TODO_UPDATE

### Summary
- Invitation & Auth E2E 준비 Epic의 “기초 데이터 저장” 작업을 완료로 표시했다.

### Details
- 작업 사유
  - Bootstrap Runner 구현으로 요구된 시드/업데이트 흐름이 완료되었다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.5.md
- 다음 단계
  - 후속 프런트 데모/Playwright 시나리오 작업을 이어간다.

## [2025-12-03 20:04] Frontend Bootstrap PLAN 작성

### Type
DESIGN

### Summary
- Next.js + Tailwind 기반 프런트 스캐폴딩 요구를 정의한 `docs/plan/frontend-bootstrap_plan.md`를 추가했다.

### Details
- 작업 사유
  - Phase 2 TODO “간단한 프런트(Next.js)로 현재까지 만든 기능 테스트” 진행을 위한 설계 문서가 필요했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/frontend-bootstrap_plan.md (신규)
- 다음 단계
  - PLAN에 따라 frontend 프로젝트 생성 및 공용 API/Tailwind 설정을 구현한다.

## [2025-12-03 20:06] Frontend Next.js 스캐폴딩 구현

### Type
BEHAVIORAL

### Summary
- `frontend/` 디렉터리에 Next.js 16 + React 19 + Tailwind 4 기반 프로젝트 구조를 추가하고, 공용 API 클라이언트/환경 변수 예시/문서화를 완료했다.

### Details
- 작업 사유
  - Phase 2 TODO의 첫 하위 작업(프런트 스캐폴딩)을 완료해 이후 Auth/Invitation/StudentProfile 화면을 개발할 기반을 마련.
- 영향받은 테스트
  - N/A (Next.js 기본 구조, 실행은 로컬에서 `npm run dev`)
- 수정한 파일
  - frontend/** (package.json, tsconfig, app, src/lib/apiClient.ts, README 등)
  - README.md (프런트 실행 가이드)
  - docs/todo/v1.5.md (하위 작업 상태)
- 다음 단계
  - Auth/Invitation 화면과 StudentProfile/PersonalLesson UI를 추가하고 smoke 테스트/문서화를 이어간다.

## [2025-12-03 22:30] PLAN 디렉터리 Backend/Frontend 분리

### Type
STRUCTURAL

### Summary
- `docs/plan`을 backend/frontend 작업 큐로 분리하고 공통 문서는 루트에 유지하도록 README를 추가했다.

### Details
- 작업 사유
  - TODO Phase 2의 프런트 진행 전, PLAN 문서를 영역별로 정리하여 추적성을 높이기 위함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/backend/** (기존 backend 관련 PLAN 이동)
  - docs/plan/frontend/** (frontend PLAN 이동)
  - docs/plan/README.md (신규)
- 다음 단계
  - 분리된 PLAN 구조를 기반으로 Auth/Invitation UI PLAN을 작성하고 구현을 이어간다.
## [2025-12-03 22:33] Auth & Invitation UI PLAN 작성

### Type
DESIGN

### Summary
- 로그인/회원가입/초대 코드 검증 화면 요구사항을 정의한 `docs/plan/frontend/auth-invitation-ui_plan.md`를 추가했다.

### Details
- 작업 사유
  - Phase 2 TODO의 Auth·Invitation UI를 구현하기 전에 입력 흐름, 상태 모델, 테스트 전략을 명확히 하기 위함.
- 영향받은 테스트
  - N/A (설계 문서 작성)
- 수정한 파일
  - docs/plan/frontend/auth-invitation-ui_plan.md (신규)
- 다음 단계
  - PLAN 내용에 따라 컴포넌트/페이지를 구현하고, 테스트/문서화를 진행한다.

## [2025-12-03 22:33] TODO 상태 업데이트 - Auth & Invitation UI 착수

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.5.md`에서 Auth·Invitation UI 작업을 ⚪ → 🔄로 전환해 진행 중 상태를 반영했다.

### Details
- 작업 사유
  - PLAN 작성과 동시에 실제 구현 준비가 시작되어 TODO 상태 동기화가 필요했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/todo/v1.5.md
- 다음 단계
  - Auth·Invitation UI 구현 진행 상황에 따라 TODO 상태를 다시 갱신하고, 완료 시 ✅로 변경한다.
## [2025-12-03 22:40] Auth & Invitation UI PLAN 경로 보정

### Type
DESIGN

### Summary
- OpenAPI 스펙(`/api/v1/...`)과 일치하도록 Auth & Invitation UI PLAN의 로그인/Teacher 회원가입 API 경로를 명시적으로 수정했다.

### Details
- 작업 사유
  - `frontend/src/types/openapi.d.ts` 기준 모든 Auth 엔드포인트가 `/api/v1` prefix를 포함하므로 PLAN 요구사항도 동일한 경로를 가리키도록 보정이 필요했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/frontend/auth-invitation-ui_plan.md
- 다음 단계
  - 수정된 PLAN에 맞춰 UI 구현 시 올바른 경로를 사용한다.
## [2025-12-03 22:43] Auth & Invitation UI PLAN 접근 제약 명시

### Type
DESIGN

### Summary
- Teacher 전용 일반 회원가입과 초대 기반 가입 경로를 구분해 PLAN에 공개 범위/접근 제약을 명시했다.

### Details
- 작업 사유
  - 서비스 정책상 공개 회원가입은 Teacher만 가능하며, 초대 가입 URL은 검증을 거친 사용자만 접근해야 하므로 PLAN 요구사항에 이를 반영했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/frontend/auth-invitation-ui_plan.md
- 다음 단계
  - 구현 시 초대 검증 상태 없이는 초대 가입 페이지에 접근할 수 없도록 라우팅 가드를 적용한다.
## [2025-12-03 22:44] Auth & Invitation UI PLAN - 가입 정책 정교화

### Type
DESIGN

### Summary
- Teacher 공개 가입은 '선생님 회원가입' 카피로 명시하고, 초대 링크 진입 시 자동 검증/가입 흐름을 상세화했다.

### Details
- 작업 사유
  - 서비스 정책에 따라 일반 가입자는 Teacher로 등록되며, Assistant/Student는 초대 링크를 통해서만 가입해야 하므로 UI PLAN에 정책을 반영했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/frontend/auth-invitation-ui_plan.md
- 다음 단계
  - 구현 시 Teacher 공개 가입 CTA와 초대 링크 기반 라우팅 가드를 정확히 반영한다.
## [2025-12-03 22:46] Auth & Invitation UI PLAN - 초대 링크 역할 분기 강화

### Type
DESIGN

### Summary
- 초대 링크로 진입한 사용자만 해당 역할(조교/학생) 가입 화면으로 이동하고, 역할별 카피/가드가 잠기도록 PLAN 요구사항을 보강했다.

### Details
- 작업 사유
  - Teacher가 발급한 초대 링크를 통해서만 Assistant/Student가 회원가입하도록 보장해야 하므로 검증/가입 단계에 역할 잠금과 UI 구분을 명시했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/frontend/auth-invitation-ui_plan.md
- 다음 단계
  - 구현 시 verify 단계에서 role을 컨텍스트에 저장하고, invited 등록 페이지가 해당 role에 맞춰 렌더링되도록 구성한다.
## [2025-12-03 22:49] Auth & Invitation UI PLAN - UI 지침 추가

### Type
DESIGN

### Summary
- Auth/Invitation 화면이 최신 SaaS 스타일 카드형 레이아웃과 반응형 디자인을 따르도록 Non-functional 요구 사항을 보강했다.

### Details
- 작업 사유
  - 사용자 경험을 현대적으로 유지하고 직관적인 UI를 제공하기 위해 Tailwind 기반 레이아웃/타이포/컬러 지침을 명시적으로 정의했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/frontend/auth-invitation-ui_plan.md
- 다음 단계
  - 구현 시 해당 UI 지침을 반영해 카드형 레이아웃과 명확한 대비를 적용한다.
## [2025-12-03 22:50] Auth & Invitation UI PLAN - 테마 전환 요구 추가

### Type
DESIGN

### Summary
- 라이트/다크 모드를 지원하는 ThemeProvider와 토글 요구 사항을 Non-functional 항목에 추가했다.

### Details
- 작업 사유
  - 최신 UI 기대치에 맞춰 시스템 테마 감지 + 수동 전환을 제공해야 하므로 Auth/Invitation PLAN에 명시적으로 포함했다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - docs/plan/frontend/auth-invitation-ui_plan.md
- 다음 단계
  - 구현 시 ThemeProvider와 토글 컴포넌트를 추가하고 각 화면이 두 모드에서 동일한 사용자 경험을 제공하도록 한다.
## [2025-12-03 23:03] Auth · Invitation UI 1차 구현 완료

### Type
BEHAVIORAL

### Summary
- Next.js 앱에 글로벌 테마/세션/쿼리 프로바이더를 구성하고 로그인·선생님 가입·초대 검증·초대 가입 UI 흐름을 구현했다.

### Details
- 작업 사유
  - Phase 2 TODO의 첫 하위 작업을 마무리해 백엔드 Auth/Invitation API를 프런트에서 즉시 검증할 수 있도록 함.
- 영향받은 테스트
  - `npm run lint` 시도 시 Next CLI가 `lint` 디렉터리를 찾으려 해 실패(추후 Next lint 설정 확인 필요).
- 수정한 파일
  - frontend/app/** (layout, providers, globals.css, 메인/인증 페이지 일괄 추가)
  - frontend/src/components/** (AuthLayout, Theme/Session/Invitation 컨텍스트, UI 컴포넌트 등)
  - frontend/src/hooks/useAuthForm.ts, src/lib/api.ts, src/lib/cn.ts
  - docs/todo/v1.5.md
- 다음 단계
  - StudentProfile/PersonalLesson UI, 공통 에러 처리, smoke 테스트/실행 가이드를 이어서 구현한다.

## [2025-12-03 23:12] Tailwind/PostCSS 및 Auth 세션 핸들러 수정

### Type
BEHAVIORAL

### Summary
- Tailwind 4가 요구하는 `@tailwindcss/postcss` 플러그인을 추가하고 PostCSS 구성을 업데이트했으며, AuthSessionProvider가 올바른 토큰 헬퍼를 import하도록 수정했다.

### Details
- 작업 사유
  - Next dev 실행 시 Tailwind PostCSS 플러그인 에러와 존재하지 않는 함수 import 에러가 발생했다.
- 영향받은 테스트
  - `npm run lint` 여전히 Next.js CLI가 `/frontend/lint` 디렉터리를 찾으려다 실패(기존 이슈 지속).
- 수정한 파일
  - frontend/postcss.config.cjs
  - frontend/src/components/auth/auth-session-provider.tsx
- 다음 단계
  - Next lint 명령이 잘못된 디렉터리를 가리키는 원인을 추적하고, Auth/Invitation UI의 추가 기능 개발을 이어간다.
## [2025-12-03 23:25] ESLint 스크립트 및 상태 관리 개선

### Type
BEHAVIORAL

### Summary
- Next 16 CLI에는 `next lint` 명령이 없어 ESLint 실행 실패가 발생해 `eslint.config.mjs` + `eslint .` 스크립트로 대체하고, Auth/Theme Provider 상태 초기화를 렌더 단계에서 처리해 lint 오류를 해결했다.

### Details
- 작업 사유
  - `npm run lint`가 `next lint` 명령 부재로 실패했고, ESLint 적용 후 React 훅 규칙 위반이 발생했다.
- 영향받은 테스트
  - `npm run lint` (성공)
- 수정한 파일
  - frontend/package.json
  - frontend/eslint.config.mjs (신규)
  - frontend/src/components/auth/auth-session-provider.tsx
  - frontend/src/components/theme/theme-provider.tsx
- 다음 단계
  - Theme/Auth 컨텍스트를 사용하는 UI를 최신 API에 맞게 연동하고, lint 경고를 지속적으로 모니터링한다.

## [2025-12-03 23:27] JWT 필터 화이트리스트 적용

### Type
BUGFIX

### Summary
- JWT 인증 필터가 permitAll 경로에서도 실행돼 회원가입/로그인 요청이 막히는 문제를 해결하기 위해 인증이 필요 없는 Auth 엔드포인트를 `shouldNotFilter` 화이트리스트로 분리했다.

### Details
- 작업 사유
  - `/api/v1/auth/register/teacher` 등 무토큰 API가 `JwtAuthenticationFilter`에서 401로 차단됐다.
- 영향받은 테스트
  - 별도 테스트 미실행(구성 변경)
- 수정한 파일
  - backend/src/main/java/com/classhub/global/jwt/JwtAuthenticationFilter.java
- 다음 단계
  - 필요한 다른 오픈 엔드포인트가 생기면 화이트리스트를 업데이트하고, 보호가 필요한 Auth 경로는 별도 세분화한다.

## [2025-12-03 23:39] Auth 공개 API 호출 시 토큰 제거

### Type
BEHAVIORAL

### Summary
- 토큰이 자동 첨부되어 공개 Auth 엔드포인트가 차단되는 문제를 막기 위해 토큰 미첨부 클라이언트(`publicApi`)를 도입하고 로그인/회원가입/초대 검증 흐름에서 이를 사용하도록 수정했다.

### Details
- 작업 사유
  - AuthSessionProvider가 설정한 Authorization 헤더가 회원가입/로그인 요청에도 붙어 JWT 필터에서 401이 발생했다.
- 영향받은 테스트
  - `npm run lint`
- 수정한 파일
  - frontend/src/lib/api.ts
  - frontend/app/auth/login/page.tsx
  - frontend/app/auth/register/teacher/page.tsx
  - frontend/app/auth/register/invited/page.tsx
  - frontend/src/components/auth/invitation-flow-provider.tsx
- 다음 단계
  - README 등 개발 문서에서도 publicApi 사용처 안내를 검토하고, 보호된 API 호출부는 기존 `api`를 계속 활용한다.

## [2025-12-04 09:37] API 경로 정리 및 publicApi 제거

### Type
STRUCTURAL

### Summary
- BASE_URL에 `/api/v1`를 포함하도록 바꾸고 모든 요청 경로에서 중복된 `/api/v1` 접두어를 제거했으며, publicApi를 없애고 단일 클라이언트를 사용하도록 정리했다.

### Details
- 작업 사유
  - 백엔드 로그에서 `/api/v1/api/v1/...`가 찍히고 클라이언트가 두 개라 혼란이 있어 요청 경로 체계를 단순화했다.
- 영향받은 테스트
  - `npm run lint`
- 수정한 파일
  - frontend/src/lib/api.ts
  - frontend/app/auth/login/page.tsx
  - frontend/app/auth/register/teacher/page.tsx
  - frontend/app/auth/register/invited/page.tsx
  - frontend/src/components/auth/invitation-flow-provider.tsx
  - frontend/README.md
- 다음 단계
  - 환경 변수(`NEXT_PUBLIC_API_BASE_URL`)를 호스트+`/api/v1` 형태로 유지하고, 향후 다른 경로 추가 시 동일한 규칙을 따른다.

## [2025-12-04 09:38] ThemeToggle Hydration 대응

### Type
BUGFIX

### Summary
- SSR과 CSR 간 테마 아이콘이 달라 발생하던 Hydration mismatch를 방지하기 위해 ThemeToggle 아이콘을 클라이언트 마운트 이후에만 실제 테마 상태로 렌더링하도록 수정했다.

### Details
- 작업 사유
  - 서버 렌더 시 항상 Sun, 클라이언트에서는 시스템 테마에 따라 Moon/Sun이 달라져 Hydration 오류가 발생했다.
- 영향받은 테스트
  - N/A (UI 컴포넌트)
- 수정한 파일
  - frontend/src/components/theme/theme-toggle.tsx
- 다음 단계
  - 다른 컴포넌트에서도 SSR/CSR 불일치가 없는지 점검한다.

## [2025-12-04 10:15] Next.js 부트스트랩 및 API 클라이언트 재구성

### Type
STRUCTURAL

### Summary
- 삭제된 프런트엔드 스캐폴딩을 Next.js 16 + Tailwind 4 조합으로 다시 생성하고 공용 API/환경 변수 유틸을 정비했다.
- 팀에서 사용하지 않기로 한 ESLint/`npm run lint` 구성을 제거해 의존성과 스크립트를 단순화했다.

### Details
- 작업 사유
  - TODO의 “Next.js + Tailwind 기본 프로젝트 세팅 및 공용 API 클라이언트/환경 변수 구성” 항목이 다시 필요해 최소 실행 가능한 프런트 구조와 문서를 복원했다.
- 영향받은 테스트
  - `npm run build` (Turbopack가 CSS 빌드 시 sandbox 포트 바인딩 제한으로 실패)
- 수정한 파일
  - frontend/package.json, frontend/package-lock.json, frontend/.gitignore, frontend/.env.local.example
  - frontend/src/app/{layout.tsx,providers.tsx,page.tsx,globals.css}, frontend/src/lib/{env.ts,api.ts}
  - frontend/README.md, docs/todo/v1.5.md
- 다음 단계
  - Auth/Invitation/StudentProfile UI를 추가하고 `npm run openapi`로 실제 타입을 생성해 API 호출을 구현한다.

## [2025-12-04 10:45] Components 데모 페이지 및 테마 토큰 적용

### Type
STRUCTURAL

### Summary
- 제공된 ClassHub 테마 JSON을 기반으로 Tailwind 4 전역 토큰을 재정의하고, 공통 UI 컴포넌트(Button, Card, TextField, Checkbox 등)를 모듈화했다.
- `/components` 페이지를 추가해 색상/타이포/폼/통계 카드 등을 한 번에 검증할 수 있는 쇼케이스를 구성했다.

### Details
- 작업 사유
  - 반복적으로 사용할 UI 요소를 중앙화해 이후 Auth/Invitation 화면 개발 시 재사용성을 확보하고, 사용자에게 시각적 확인용 페이지를 제공하기 위함.
- 영향받은 테스트
  - `npm run build` (sandbox 환경에서 CSS 처리 시 Turbopack가 별도 프로세스 포트 바인딩을 시도해 실패)
- 수정한 파일
  - frontend/src/app/{globals.css,layout.tsx,page.tsx,components/page.tsx}, frontend/src/app/providers.tsx
  - frontend/src/components/{ui/button.tsx,ui/card.tsx,ui/checkbox.tsx,ui/logo.tsx,ui/section-heading.tsx,ui/text-field.tsx,showcase/components-showcase.tsx}
  - frontend/src/theme/classhub-theme.ts, frontend/package.json, frontend/package-lock.json
  - docs/todo/v1.5.md
- 다음 단계
  - Components 페이지를 참고해 Auth/Invitation 페이지를 구체화하고, 상태 관리/에러 핸들링 컴포넌트로 확장한다.

## [2025-12-04 10:58] Carousel 공통 컴포넌트 및 이미지 카드 확장

### Type
STRUCTURAL

### Summary
- 재사용 가능한 Carousel UI를 추가해 추천 프로그램/공지 등을 슬라이드 형태로 표시할 수 있도록 했고, Card 컴포넌트가 media 슬롯을 받아 썸네일/배너를 포함할 수 있도록 확장했다.
- `/components` 쇼케이스에 Carousel 섹션과 이미지 카드 예제를 추가해 공통 디자인 자산을 한눈에 검증할 수 있게 했다.

### Details
- 작업 사유
  - 프로젝트 전반에서 반복적으로 쓰일 Carousel/이미지 카드 패턴을 미리 정의해 추후 화면 개발 속도를 높이고 일관된 시각 언어를 유지하려 함.
- 영향받은 테스트
  - `npm run build` (sandbox 환경 포트 제한으로 Turbopack CSS 처리 실패, 이전과 동일)
- 수정한 파일
  - frontend/src/components/ui/{card.tsx,carousel.tsx}
  - frontend/src/components/showcase/components-showcase.tsx
  - docs/history/AGENT_LOG.md
- 다음 단계
  - Carousel/이미지 카드 기반으로 실제 도메인 화면(Auth/Invitation/StudentProfile) 디자인을 구체화한다.

## [2025-12-04 11:10] Navbar · Footer · Hero 공통 컴포넌트 추가

### Type
STRUCTURAL

### Summary
- NavigationBar, Footer, Hero 컴포넌트를 제작해 레이아웃 전반에서 반복 사용하는 요소를 중앙 집중화했다.
- `/components` 페이지에 Hero, 네비게이션, 푸터 데모 섹션을 추가하고 실제 레이아웃에서도 새 Navbar/Footers를 적용했다.

### Details
- 작업 사유
  - 공통 레이아웃 요소를 모듈화해 향후 페이지 개발 시 재활용하고 디자인 일관성을 유지하려 함.
- 영향받은 테스트
  - `npm run build` (Turbopack CSS 처리 시 sandbox 포트 제한으로 실패, 기존과 동일)
- 수정한 파일
  - frontend/src/components/ui/{button.tsx,hero.tsx,navigation-bar.tsx,footer.tsx}
  - frontend/src/components/showcase/components-showcase.tsx
  - frontend/src/app/layout.tsx
  - frontend/package.json, frontend/package-lock.json
- 다음 단계
  - 새 Navbar/Footer/Hero를 기준으로 실제 Auth/Invitation 페이지 레이아웃을 구성한다.
## [2025-12-04 11:25] OpenAPI 타입 기반 API 클라이언트 적용

### Type
STRUCTURAL

### Summary
- `npm run openapi`로 생성된 `paths` 타입을 `frontend/src/lib/api.ts`에서 사용하도록 업데이트해 API 호출의 타입 안전성을 확보했다.

### Details
- 작업 사유
  - placeholder 타입을 유지하면 엔드포인트/파라미터 자동 완성이 동작하지 않아 구현 시 실수가 잦을 수 있었다.
- 영향받은 테스트
  - N/A (타입 정의 교체)
- 수정한 파일
  - frontend/src/lib/api.ts
- 다음 단계
  - 생성된 타입을 활용해 Auth/Invitation UI 구현 시 API 스펙을 그대로 참조한다.

## [2025-12-04 12:17] PLAN 디렉터리 백엔드/프런트 분리 및 구조 가이드 정비

### Type
DESIGN

### Summary
- 기존 `docs/plan` 루트에 혼재돼 있던 PLAN 문서를 backend/frontend/rule로 재구성해 담당 영역별 문서를 명확히 분리했다.
- 새 구조와 사용 규칙을 정리한 `docs/plan/README.md`를 추가해 향후 문서 작성 시 따라야 할 절차를 명시했다.

### Details
- 작업 사유
  - PLAN 문서가 단일 디렉터리에 섞여 있어 필요한 설계 자료를 찾기 어렵고 영역 간 책임 구분이 불명확했다.
- 영향받은 테스트
  - N/A (문서 개편)
- 수정한 파일
  - `docs/plan/README.md`
  - `docs/plan/backend/*`, `docs/plan/frontend/*`, `docs/plan/rule/*`
- 다음 단계
  - 각 기능 작업 시 해당 영역(backend/frontend)에 PLAN 문서를 추가하고 README 프로세스를 준수한다.

## [2025-12-04 12:17] TODO 상태 업데이트 - 공통 레이아웃/세션 컴포넌트

### Type
TODO_UPDATE

### Summary
- Phase 2 하위 “공통 레이아웃·세션 상태·에러 처리 컴포넌트 정비” 작업을 🔄 상태로 전환해 현재 집중 작업임을 명시했다.
- 종료된 “간단한 프런트 테스트” 플레이스홀더 항목을 정리해 TODO를 최신 스코프로 유지했다.

### Details
- 작업 사유
  - Next.js 기반 공통 레이아웃/상태 컴포넌트를 작업에 착수했으므로 진행 중 상태를 반영하고, 중복 항목을 제거해 추적성을 높이고자 함.
- 영향받은 테스트
  - N/A (TODO 문서만 수정)
- 수정한 파일
  - `docs/todo/v1.5.md`
- 다음 단계
  - 공통 컴포넌트 정비 완료 후 Auth/Invitation 화면 구현 항목을 이어서 진행한다.

## [2025-12-04 12:25] Frontend UI Foundation PLAN 작성

### Type
DESIGN

### Summary
- Phase 2의 “공통 레이아웃·세션 상태·에러 처리 컴포넌트 정비” 작업을 위한 `ui-foundation_plan.md`를 작성해 레이아웃 확장, SessionProvider, 에러 컴포넌트 요구사항을 정리했다.

### Details
- 작업 사유
  - 세션/에러 처리 구현 범위가 명확하지 않아 기능 개발 전에 목표와 API 의존성을 정의할 필요가 있었다.
- 영향받은 테스트
  - N/A (설계 문서)
- 수정한 파일
  - `docs/plan/frontend/ui-foundation_plan.md`
- 다음 단계
  - PLAN 승인 후 SessionProvider/에러 컴포넌트 구현을 진행하고 TODO 상태를 업데이트한다.

## [2025-12-04 12:26] 프런트 커뮤니케이션 지침 강화

### Type
DESIGN

### Summary
- 사용자의 프런트 이해도를 감안해, AGENTS.md와 CLAUDE.md에 “프런트 작업은 항상 세분화하고 단계별로 아주 상세히 설명”해야 한다는 지침을 추가했다.

### Details
- 작업 사유
  - 프런트 관련 대화가 추상적으로 흐르면서 확인이 어렵다는 요청이 있어, 문서상으로 상세 설명/분할 규칙을 명시했다.
- 영향받은 테스트
  - N/A (문서)
- 수정한 파일
  - `AGENTS.md`
  - `CLAUDE.md`
- 다음 단계
  - 프런트 PLAN/구현 공유 시 새로운 지침을 준수하며 단계별 설명을 제공한다.

## [2025-12-04 12:37] Auth PLAN 업데이트 - GET /auth/me 추가

### Type
DESIGN

### Summary
- 프런트 세션 컨텍스트 요구에 맞춰 `auth-core_plan.md`에 `GET /api/v1/auth/me` 기능을 추가하고, API/TDD 항목에 해당 내용을 반영했다.

### Details
- 작업 사유
  - React Query 기반 SessionProvider가 현재 사용자 정보를 표준 방식으로 조회할 엔드포인트가 필요해 기존 Auth PLAN에 self-lookup API를 명시했다.
- 영향받은 테스트
  - N/A (계획 수정)
- 수정한 파일
  - `docs/plan/backend/auth-core_plan.md`
- 다음 단계
  - PLAN을 기준으로 AuthController/Service에 `/auth/me` 엔드포인트를 구현하고 관련 테스트를 추가한다.

## [2025-12-04 12:43] Auth self-lookup API 추가

### Type
BEHAVIORAL

### Summary
- `GET /api/v1/auth/me` 엔드포인트를 도입해 Access 토큰 기준으로 현재 로그인한 사용자의 식별자/역할 정보를 제공한다.
- AuthService에 현재 사용자 조회 메서드와 `MeResponse` DTO를 추가하고, MockMvc 테스트로 성공/401 흐름을 검증했다.

### Details
- 작업 사유
  - 프런트 SessionProvider가 백엔드에서 현재 사용자를 표준 방식으로 조회할 API가 없어 세션 동기화가 불가능했다.
- 영향받은 테스트
  - `AuthControllerTest` (MockMvc) — ✅ (새 케이스 작성, Gradle wrapper 부재로 로컬 실행은 생략)
- 수정한 파일
  - `backend/src/main/java/com/classhub/domain/auth/{application/AuthService.java,dto/response/MeResponse.java,web/AuthController.java}`
  - `backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java`
- 다음 단계
  - 프런트 `SessionProvider`가 `/auth/me` 응답을 사용하도록 UI foundation 작업을 진행한다.

## [2025-12-04 15:55] 프런트 UI Foundation 1차 구현

### Type
BEHAVIORAL

### Summary
- 루트 레이아웃에 공통 Notification/Portal 레이어와 ErrorBoundary를 추가해 모든 페이지가 동일한 구조에서 렌더링되도록 했다.
- React Query 기반 `SessionProvider`/`useSession` 훅을 구현해 `/api/v1/auth/me` 응답을 컨텍스트에 보관하고, mock 토큰/토큰 갱신 API와 연동할 수 있게 했다.
- ErrorState·InlineError 컴포넌트와 글로벌 QueryCache onError 핸들러를 추가해 인증 오류/예상치 못한 예외를 일관되게 처리한다.

### Details
- 작업 사유
  - TODO Phase 2의 “공통 레이아웃·세션 상태·에러 처리 컴포넌트 정비” 항목을 완료해 이후 Auth/Invitation 화면 구현 시 재사용 가능한 기반을 마련하려 함.
- 영향받은 테스트
  - N/A (프런트 빌드/테스트 미실행, Next dev 환경에서 수동 확인 예정)
- 수정한 파일
  - `frontend/src/app/{layout.tsx,providers.tsx}`
  - `frontend/src/components/session/session-provider.tsx`
  - `frontend/src/components/ui/{app-error-boundary.tsx,error-state.tsx,inline-error.tsx}`
  - `docs/todo/v1.5.md`
- 다음 단계
  - Auth/Invitation / StudentProfile 페이지에서 `useSession`과 에러 컴포넌트를 활용하도록 UI 작업을 진행하고, 필요 시 토큰 저장/로그인 흐름을 세분화한다.

## [2025-12-04 16:12] 프런트 PLAN Test Plan 명시

### Type
DESIGN

### Summary
- `docs/plan/README.md`에 프런트 PLAN은 5번 섹션을 Test Plan으로 작성해 구체적인 테스트 전략을 담아야 한다는 규칙을 추가했다.
- `ui-foundation_plan.md`의 `TDD Plan` 섹션명을 `Test Plan`으로 변경해 실제 테스트 항목을 명확히 했다.

### Details
- 작업 사유
  - 사용자 요청에 따라 프런트 개발도 TDD에 준하는 테스트 계획을 문서 단계에서 명시하도록 지침을 업데이트했다.
- 영향받은 테스트
  - N/A (문서)
- 수정한 파일
  - `docs/plan/README.md`
  - `docs/plan/frontend/ui-foundation_plan.md`
- 다음 단계
  - 이후 모든 프런트 PLAN 문서에서 Test Plan 섹션에 단위/통합/E2E 테스트 전략을 구체적으로 작성한다.

## [2025-12-04 16:15] 프런트 테스트 보고 지침 강화

### Type
DESIGN

### Summary
- `frontend/AGENTS.md`에 프런트 작업을 단위별로 나누고 자동/수동 테스트 결과를 반드시 기록하도록 명시했다.
- `CLAUDE.md`에도 동일한 요구사항을 추가해 프런트 작업 보고 시 실행한 테스트 종류와 확인 절차를 항상 포함하도록 했다.

### Details
- 작업 사유
  - 프런트 개발 시 검증 과정을 일관되게 공유해달라는 요청을 문서화해 에이전트 모두가 동일한 기준을 따르도록 하기 위함.
- 영향받은 테스트
  - N/A (문서)
- 수정한 파일
  - `frontend/AGENTS.md`
  - `CLAUDE.md`
- 다음 단계
  - 프런트 작업 보고 시 각 단계별 테스트/확인 내용을 빠짐없이 기록한다.

## [2025-12-04 16:17] Frontend Home Page PLAN 작성

### Type
DESIGN

### Summary
- Phase 2의 “Auth·Invitation 화면 구현”을 세분화해, 홈 페이지에서 세션 상태/주요 액션을 안내하는 `home-page_plan.md`를 작성했다.

### Details
- 작업 사유
  - `/` 화면을 실제 업무 플로우 진입점으로 만들기 위해 Hero, 세션 카드, Quick Action 카드, Env 정보 등을 정의할 필요가 있었다.
- 영향받은 테스트
  - N/A (설계)
- 수정한 파일
  - `docs/plan/frontend/home-page_plan.md`
- 다음 단계
  - PLAN을 기준으로 홈 페이지 구현과 테스트(React Testing Library, 수동 확인)를 진행한다.

## [2025-12-04 16:26] Frontend Home Page PLAN 보완

### Type
DESIGN

### Summary
- 홈 화면을 Glassmorphism 기반 로그인 카드 + Hero 레이아웃으로 재정의하고, Teacher 가입/초대 안내 등 구체적 UI 요구를 PLAN에 반영했다.

### Details
- 작업 사유
  - 사용자 피드백에 따라 `/` 페이지를 실제 서비스 메인처럼 보이게 구성하고, Nav/Footer는 별도 대시보드에서 적용할 예정이라 독립 레이아웃으로 수정했다.
- 영향받은 테스트
  - N/A (설계 업데이트)
- 수정한 파일
  - `docs/plan/frontend/home-page_plan.md`
- 다음 단계
  - PLAN 기준으로 홈 페이지 구현 시 로그인 폼/CTA/애니메이션/테스트를 차례로 진행한다.

## [2025-12-04 16:28] Home Page PLAN - 초대 안내 제외

### Type
DESIGN

### Summary
- 홈 페이지 PLAN에서 조교/학생 초대 안내 섹션을 제거하고, 기본 Hero + Login + Teacher 가입 CTA 중심으로 단순화했다.

### Details
- 작업 사유
  - 초대 기반 가입은 별도 링크 진입 시 처리하기로 하여, 기본 홈 화면에는 노출하지 않도록 요구가 변경되었다.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - `docs/plan/frontend/home-page_plan.md`
- 다음 단계
  - 홈 화면 구현 시 초대 안내는 별도 초대 링크 페이지에서 처리한다.

## [2025-12-04 16:33] 홈 진입 페이지 구현 및 레이아웃 분기

### Type
BEHAVIORAL

### Summary
- 루트 레이아웃에 `AppChrome`을 도입해 `/`에서는 Nav/Footer를 숨기고, 다른 페이지는 기존 NavigationBar/Footer/Portal 구조를 유지하도록 분기했다.
- 홈 페이지를 Glassmorphism Hero + 로그인 카드 UI로 재구성하고, `useSession`/`api.POST("/auth/login")`를 연결해 로그인 후 토큰을 SessionProvider에 주입하도록 했다.
- TODO에 “홈 진입 페이지” 세부 항목을 추가해 완료 상태를 표시했다.

### Details
- 작업 사유
  - 사용자가 제안한 메인 디자인 가이드를 적용해 실제 서비스 느낌의 첫 화면을 제공하고, 앞으로 역할별 대시보드 진입 전에 로그인 흐름을 확정하려 함.
- 영향받은 테스트
  - `npm run lint` (frontend) → 스크립트가 정의돼 있지 않아 실행 불가 (package.json 상 lint 명령 없음)
  - 수동 확인: mock 토큰 없이 브라우저 렌더링 시 로그인 폼/세션 배너 노출, 이메일/비밀번호 입력 및 패스워드 토글, Teacher 가입 버튼 라우팅을 확인
- 수정한 파일
  - `frontend/src/components/ui/app-chrome.tsx`
  - `frontend/src/app/layout.tsx`
  - `frontend/src/app/page.tsx`
  - `docs/todo/v1.5.md`
- 다음 단계
  - Auth/Invitation 페이지를 동일한 SessionProvider 흐름으로 구현하고, 로그인/초대 페이지별 Test Plan을 정리한다.

## [2025-12-04 16:35] 홈 페이지 Hero 카피 정리

### Type
BEHAVIORAL

### Summary
- `/` 페이지 상단의 Hero 설명/목록 섹션을 제거해 로그인 카드에 집중하도록 단순화했다.

### Details
- 작업 사유
  - 사용자 요청으로 홈 화면을 로그인 카드 중심으로 유지하고, 상단 카피/리스트는 추후 대시보드에서 다루기로 했다.
- 영향받은 테스트
  - N/A (UI 텍스트 제거)
- 수정한 파일
  - `frontend/src/app/page.tsx`
- 다음 단계
  - 나머지 Auth/Invitation 페이지를 동일한 세션 흐름으로 구현한다.

## [2025-12-04 16:38] 홈 페이지 세션 배너 UX 조정

### Type
BUGFIX

### Summary
- 초 방문 시 `/auth/me`가 실패하더라도 붉은 경고가 뜨지 않도록 SessionBanner의 에러 표시를 완화하고, 기본 안내 메시지로 대체했다.

### Details
- 작업 사유
  - 백엔드가 꺼져 있거나 로그인 전 상태에서 에러 메시지가 보여 사용자 경험이 어색했다.
- 영향받은 테스트
  - N/A (UI 조건 변경)
- 수정한 파일
  - `frontend/src/app/page.tsx`
- 다음 단계
  - 실제 로그인 플로우 구현 시 성공/실패 상태를 별도 토스트나 폼 에러로 안내한다.

## [2025-12-04 16:41] 홈 로그인 API 타입 참조 적용

### Type
STRUCTURAL

### Summary
- 홈 페이지 로그인 호출 시 `components["schemas"]["LoginRequest"]`/`LoginResponse` 타입을 활용해 OpenAPI 기반 Request/Response 검증을 적용했다.

### Details
- 작업 사유
  - API 스펙 변경과 무관하게 타입 안전성을 확보하고, 임의 string 기반 구현을 방지하기 위함.
- 영향받은 테스트
  - N/A (타입/구현 정비)
- 수정한 파일
  - `frontend/src/app/page.tsx`
- 다음 단계
  - 다른 프런트 API 호출부도 동일한 방식으로 OpenAPI 타입을 명시한다.

## [2025-12-04 16:41] 프런트 OpenAPI 사용 지침 추가

### Type
DESIGN

### Summary
- `frontend/AGENTS.md`, `CLAUDE.md`에 “프런트 API 구현 시 반드시 `src/types/openapi` 타입을 참고하고 Request/Response를 명시하라”는 규칙을 추가했다.

### Details
- 작업 사유
  - 사용자 요청에 따라 앞으로의 프런트 작업에서도 OpenAPI 스키마를 단일 출처로 삼도록 명문화.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - `frontend/AGENTS.md`
  - `CLAUDE.md`
- 다음 단계
  - 향후 PLAN/구현 공유 시 해당 지침을 준수하고 테스트 보고를 병행한다.

## [2025-12-04 16:46] 홈 페이지 Remember Me 옵션 제거

### Type
BEHAVIORAL

### Summary
- `/` 로그인 카드의 “로그인 상태 유지” 체크박스를 제거하고, 추후 쿠키 기반 토큰 보관 기능을 설계할 때 다시 추가하기로 했다.

### Details
- 작업 사유
  - 현재 세션 저장 전략이 없어 UI만 남아 있어 혼란을 준다는 사용자 피드백을 반영함.
- 영향받은 테스트
  - N/A
- 수정한 파일
  - `frontend/src/app/page.tsx`
- 다음 단계
  - 토큰 persist 전략이 확정되면 remember me 기능을 재도입한다.
