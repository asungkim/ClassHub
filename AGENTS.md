# 1. 프로젝트 개요

- **목표:** 학원 강사들이 모든 업무 관련 작업을 한곳에서 관리하고 원활히 소통할 수 있는 플랫폼 구축

# 2. 기술 스택

## 2.1 프론트엔드

- **프레임워크**: Next.js **16.0.3**
- **라이브러리**: React **19.2.0**
- **언어**: TypeScript **5.8.3**
- **스타일링**: Tailwind CSS **4.1.17**

## 2.2 백엔드

- **프레임워크**: Spring Boot **4.0.0**
- **언어**: Java **21 (LTS)**
- **빌드 도구**: Gradle **8.10**
- **ORM**: Spring Data JPA **3.3.x**, Hibernate **6.6.x**
- **데이터베이스**: MySQL **8.4.5**
- **보안**: Spring Security **6.3.x**, JWT **0.12.x**
- **API 문서화**: SpringDoc OpenAPI **2.7.x**
- **캐시 / 메시징**: Redis **7.4.x**
- **테스트**
  - JUnit **5.10.x**
  - MockMvc (Spring Boot 3.5.x 내장)
  - Spring Boot Test **3.5.7**
- **유틸리티**: Lombok **1.18.34**

## 2.3 인프라

- **컨테이너**
  - Docker Engine **27.x**
  - Docker Compose **2.29.x**
- **CI/CD**: GitHub Actions (최신)
- **클라우드**: AWS EC2 (최신)
- **리버스 프록시 / 라우팅**: Nginx Proxy Manager **2.11.x**

## 2.4 적용 범위 및 우선순위

- 이 AGENTS.md는 저장소 전역 규칙과 관례를 정의한다.
- 작업 시작 전 모든 AGENTS.md를 다시 읽고 지침을 준수한다.

# 3. 문서 및 이력 기록 규칙

모든 개발 활동을 `docs/history/AGENT_LOG.md`에 자동으로 기록한다.

## 3.1 기록 대상 이벤트

- 신규 테스트 작성
- 테스트 통과 이후 기능 구현(행동 변화)
- Tidy First 기반 구조적 변경(행동 변화 없음)
- 버그 재현 및 수정
- 설계 문서 작성 및 검토 과정
- TODO 상태 업데이트
- 버전 변경 또는 릴리스

## 3.2 로그 포맷

```
## [YYYY-MM-DD HH:mm] <작업 요약>

### Type
STRUCTURAL | BEHAVIORAL | DESIGN | TODO_UPDATE | BUGFIX | RELEASE

### Summary
- 수행한 작업 요약

### Details
- 작업 사유
- 영향받은 테스트
- 수정한 파일
- 다음 단계
```

## 3.3 기록 규칙

- `docs/history/AGENT_LOG.md`는 항상 Append-only다. 첫 기록 시 파일을 생성한다.
- 의미 있는 이벤트 단위로 기록하며, 유형(Type)을 통해 STRUCTURAL/BEHAVIORAL/등으로 분류한다.
- STRUCTURAL과 BEHAVIORAL 변경은 같은 커밋/로그에 섞지 않는다.
- TODO 상태나 버전 변경이 발생하면 반드시 로그를 남긴다.

# 4. TODO 시스템 연동 (docs/todo)

프로젝트 TODO는 `docs/todo` 하위 버전 파일로 관리한다.

```
docs/
 └── todo/
      ├── v1.0.md
      ├── v1.1.md
      └── ...
```

## 4.1 버전 규칙

- TODO 구조(작업 추가/삭제, 순서 변경 등)가 바뀌면 새 버전 파일을 생성한다.
- 각 버전 파일은 **Phase → Epic → Task** 구조를 따른다.
- 작업 상태 이모지

```
✅ 완료
🔄 진행 중
⚪ 대기
⛔ 차단됨
```

## 4.2 AGENT 책임

- 작업 시작 시: 🔄
- 작업 완료 및 검증 후: ✅
- 차단 이슈 발생 시: ⛔
- 미착수 작업: 기본 ⚪
- 상태 이모지 변경만 있을 경우 버전 업 없이 해당 파일에서 직접 수정한다.
- 구조 변경 시(예: 작업 이동) 새 버전 파일을 만든다.

## 4.3 작업 선택 규칙

- 사용자가 "go"라고 명령하면 `docs/todo/vX.Y.md`의 문서 순서를 엄격히 따른다.
  - 🔄 상태 작업이 있으면 가장 먼저 등록된 작업을 계속 진행한다.
  - 그렇지 않으면 첫 번째 ⚪ 작업을 선택한다.
  - ⛔ 작업은 해제 전까지 건너뛴다.
- 명시 지시 없이 작업 순서를 바꾸지 않는다. 필요 시 새 버전 파일에서 반영한다.

# 5. PLAN 시스템 연동 (docs/plan)

모든 기능 개발은 `docs/plan`의 설계 문서를 기반으로 수행한다.

## 5.1 구조

```
docs/
 └── plan/
       ├── member/
       │     ├── member-auth.md
       │     ├── member-profile.md
       │     └── member-permission.md
       │
       ├── course/
       │     ├── course-crud.md
       │     ├── course-session.md
       │     └── course-assistant.md
       └── ...
```

- 기능별로 `<feature>_plan.md` 네이밍을 사용한다. (예: `course-crud_plan.md`, `member-auth_plan.md`)

## 5.2 절차

1. 사용자가 “go”라고 하면 4.3 규칙에 따라 다음 작업을 선택한다.
2. 해당 작업에 대한 설계 문서를 `docs/plan`에 `<feature>_plan.md` 이름으로 작성한다.
3. 계획 내용의 목적, 범위, 핵심 결정을 한국어로 짧게 설명하여 리뷰를 돕는다.
4. 사용자가 검토하고 피드백을 주면 승인될 때까지 반복한다.
5. 승인 후에만 TDD/구현을 시작한다. 승인 전에는 코드 작성 금지.

## 5.3 설계 문서 템플릿

```
# Feature: <기능 이름>

## 1. Problem Definition
- 해결해야 할 문제를 기술한다.

## 2. Requirements
### Functional
- …

### Non-functional
- …

## 3. API Design (Draft)
- Method / URL / Request / Response

## 4. Domain Model (Draft)
- Entity, Relation, Validation

## 5. TDD Plan
- 작성할 테스트의 순서를 정의한다.
```

## 5.4 커뮤니케이션

- 새로운 설계 문서를 만들거나 수정할 때마다 계획 의도와 범위를 한국어로 간결히 요약해 전달한다.

# 6. 백엔드 공통 규칙 (global/domain)

- 패키지 최상위는 `global`과 `domain`만 사용한다. 기능별 패키지는 `domain.<feature>.web|application|model|repository` 구조를 따른다.
- 모든 엔티티는 `global.entity.BaseEntity`를 상속해 `UUID id`, `createdAt`, `updatedAt` 필드를 공유하며, Spring Data JPA Auditing을 활성화한다.
- 예외와 응답은 `global.response.RsData` + `global.response.RsCode` 포맷을 사용한다. `ResponseAspect`가 RsData의 코드로 HTTP Status를 설정하고, `global.exception.BusinessException/GlobalExceptionHandler`가 비즈니스/일반 예외를 일관되게 처리한다.
- Controller는 가능한 한 `RsData`를 직접 반환하고, 서비스/도메인 로직은 `domain.<feature>.application` 계층에서 조율한다.
- 공통 설정(`global.config`)에는 Locale/Timezone, CORS, JPA Auditing 등을 포함하고, 프로필별 설정(`application*.yml`, `.env.example`)을 통해 환경을 분리한다.

# 7. 요구사항 → 스펙 → TODO 동기화 절차

1. **Requirements First**: 기능 요구 변경 시 `docs/requirement/vX.Y.md`를 우선 업데이트/리뷰한다. Requirement 버전은 단일 출처(Single Source of Truth)다.
2. **Spec Update**: 요구사항 확정 후 `docs/spec/vX.Y.md`를 같은 버전으로 올리고, 어떤 Requirement 버전에서 파생되었는지 명시한다(FR ID 매핑 포함).
3. **TODO Update**: 스펙이 확정되면 `docs/todo/vX.Y.md`를 새 버전으로 만들어 Phase/Epic/Task를 재정렬한다. 각 Task에 대응하는 FR/스펙 섹션을 적어두어야 한다.
4. **Logging**: Requirement → Spec → TODO 순으로 변경할 때마다 `docs/history/AGENT_LOG.md`에 DESIGN/TODO_UPDATE 이벤트를 남기고, 변경된 문서/버전을 기록한다.
5. **Implementation Gate**: PLAN/TODO가 최신 Requirement/Spec 버전을 참조하지 않으면 구현에 착수할 수 없다.
