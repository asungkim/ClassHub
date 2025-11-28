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
