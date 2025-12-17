# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

ClassHub는 학원 강사가 수업, 학생, 조교, 커뮤니케이션을 관리하는 플랫폼입니다.

- **백엔드**: Spring Boot 4.0.0, Java 21, Gradle 8, Spring Data JPA, Spring Security, JWT
- **데이터베이스**: MySQL 8 (Docker), Redis 7 (추후 Refresh 토큰 저장으로 확장 예정)
- **프론트엔드**: Next.js 16, React 19, TypeScript 5 (계획 중)
- **아키텍처**: Monorepo 구조 (`backend/`, `frontend/`, `infra/`, `docs/`)

## 빌드 & 테스트 명령어

모든 명령어는 저장소 루트에서 실행:

```bash
# 백엔드 빌드
./gradlew build

# 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.classhub.domain.auth.application.AuthServiceTest"

# 특정 테스트 메서드 실행
./gradlew test --tests "com.classhub.domain.auth.application.AuthServiceTest.shouldLoginSuccessfully_whenValidCredentials"

# 클린 빌드
./gradlew clean build

# 애플리케이션 실행 (데이터베이스 필요)
./gradlew bootRun

# 테스트 없이 빌드만 확인
./gradlew assemble
```

## 환경 설정

1. `backend/` 디렉토리에서 `.env.example`을 `.env`로 복사
2. 데이터베이스 자격증명과 JWT 시크릿 설정
3. MySQL이 실행 중인지 확인 (Docker 또는 로컬)
4. 필요한 환경 변수는 `backend/.env.example` 참고

## 코드 아키텍처

### 패키지 구조

백엔드는 **도메인 중심** 구조로 두 개의 최상위 패키지를 사용:

- **`global/`**: 횡단 관심사 (보안, 설정, 예외, 공통 엔티티, JWT, 응답 포맷)
- **`domain/<feature>/`**: 기능별 코드, 다음과 같이 구성:
  - `web/`: 컨트롤러와 REST DTO
  - `application/`: 서비스, 유스케이스, 트랜잭션 조율
  - `model/`: 엔티티, 값 객체, 도메인 모델
  - `repository/`: Spring Data JPA 리포지토리

예시: `domain.auth`는 다음을 포함:

- `web/AuthController.java`
- `application/AuthService.java`
- `dto/LoginRequest.java`, `LoginResponse.java`
- `token/RefreshTokenStore.java`

### BaseEntity 패턴

모든 JPA 엔티티는 `BaseEntity`를 상속 (`BaseTimeEntity`를 확장):

- **ID**: UUID, DB에는 `BINARY(16)`으로 저장, API에서는 문자열로 노출
- **타임스탬프**: `createdAt`과 `updatedAt`은 JPA Auditing으로 관리
- `@UuidGenerator`와 `@EntityListeners(AuditingEntityListener.class)` 사용

### 응답 포맷

모든 API 응답은 `RsData<T>` 구조 사용:

```json
{
  "code": 1000,
  "message": "성공 메시지",
  "data": { ... }
}
```

응답 코드와 메시지는 `global/response/RsCode.java`에 정의. HTTP 상태 코드는 `ResponseAspect`가 자동으로 설정.

### 인증 & 보안

- JWT 기반 인증 (Access + Refresh 토큰)
- Refresh 토큰은 `RefreshTokenStore`에 저장 (인메모리, Redis로 마이그레이션 가능)
- 보안 설정: `global/config/SecurityConfig.java`
- JWT 로직: `global/jwt/JwtProvider.java`
- 역할 기반 인가: `TEACHER`, `ASSISTANT`, `STUDENT`, `SUPER_ADMIN`

## 테스트 가이드라인

- **네이밍**: `shouldDoX_whenY()` 형식
- **단위 테스트**: 의존성 모킹, 도메인/서비스 로직 테스트
- **웹 테스트**: `@WebMvcTest`와 MockMvc 사용
- **통합 테스트**: `@SpringBootTest`와 Testcontainers 또는 H2 사용
- 테스트는 `src/test/java/`에서 소스 구조를 미러링

## 작업 워크플로

이 프로젝트는 `docs/todo/v1.8.md`에 정의된 구조화된 계획 워크플로를 따름:

1. 작업은 Phase/Epic/Task 계층으로 구성
2. 각 작업마다 `docs/plan/<feature>_plan.md`에 계획 문서 작성
3. 백엔드는 TDD(레드-그린-리팩터)로 구현
4. 구조적 변경(refactor)과 행위 변경(feat/fix)을 별도 커밋으로 분리
5. 의미 있는 이벤트는 `docs/history/AGENT_LOG.md`에 기록

### 사용자가 "go"라고 하면

1. `docs/todo/v1.7.md`에서 다음 작업 확인 (🔄 진행 중 우선, 없으면 첫 번째 ⚪ 대기)
2. 계획이 없으면 `docs/plan/<feature>_plan.md` 작성 후 한국어 요약 제공 및 승인 요청
3. 승인 후 테스트와 함께 구현
4. TODO 상태와 AGENT_LOG 업데이트

## 커밋 & 브랜치 표준

**Conventional Commits** 준수 (GitHub Actions로 강제):

- **타입**: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `perf`, `style`
- **스코프**: `backend:domain-<feature>`, `backend:global`, `frontend:<feature>`, `infra`, `docs`
- **형식**: `<type>(<scope>): <subject>` (명령형, ≤72자)
- **브랜치**: `feature/<scope>-<desc>`, `fix/<scope>-<desc>`, `refactor/<scope>-<desc>`

예시:

- `feat(backend:domain-auth): add teacher registration API`
- `refactor(backend:domain-member): extract repository interfaces`
- `test(backend:domain-invitation): add service unit tests`

자세한 내용은 `docs/standards/commit-branch.md` 참고.

## 주요 문서

- **스펙**: `docs/spec/v1.3.md` - 기술 스펙, 도메인 엔티티, 관계
- **TODO**: `docs/todo/v1.9.md` - 현재 작업 항목 (Phase별 구성)
- **표준**:
  - `docs/standards/java-style.md` - Java 코딩 규칙
  - `docs/standards/commit-branch.md` - Git 워크플로
- **계획**: `docs/plan/*_plan.md` - 기능 구현 계획서
- **히스토리**: `docs/history/AGENT_LOG.md` - 개발 이벤트 로그

## 도메인 모델 (현재 상태)

구현된 주요 엔티티:

- **Member**: 역할을 가진 사용자 (Teacher/Assistant/Student/SuperAdmin)
  - Teacher는 Course를 생성하고 Assistant를 초대
  - Assistant는 Student를 초대
- **Invitation**: 역할 기반 회원가입을 위한 초대 코드
  - 상태: PENDING, ACCEPTED, CANCELLED, EXPIRED
  - 유형: Assistant 초대 (Teacher가 생성), Student 초대 (Assistant가 생성)

계획된 엔티티 (`docs/spec/v1.3.md` 참고):

- Course, ClinicSlot, ClinicSession, SharedLesson, PersonalLesson
- StudentProfile, Notice, WorkLog, ClinicRecord

## 프런트엔드 컴포넌트 사용 지침

- `/components` 페이지에 데모된 공통 컴포넌트(Button, Card, Carousel, Hero, NavigationBar, Footer, TextField 등)는 앞으로 구현될 모든 프런트 기능에서 기본적으로 사용한다.
- 기존 컴포넌트로 요구사항을 충족하기 어렵다면, **새 컴포넌트를 추가하기 전에** 사용자에게 아래 내용을 반드시 질문한다:
  1. 어떤 추가 컴포넌트를 만들 계획인지 (이름/역할)
  2. 왜 기존 공통 컴포넌트로는 해결이 어려운지
  3. 새 컴포넌트를 어느 화면/기능에 사용할 예정인지
- 사용자의 확인/승인 없이 새 UI 컴포넌트를 도입하지 말고, 가능한 한 기존 라이브러리(components page 기준)를 재활용한다.

### 프런트 작업 설명 & 분할 규칙

- 사용자는 프런트엔드 컨텍스트에 익숙하지 않으므로, 모든 프런트 작업 안내/리뷰/결과 공유 시 **레이아웃 → 상태 관리 → 컴포넌트 단위**로 흐름을 적어가며 설명한다.
- 구현 계획과 실행은 가능한 한 **작은 단계**(예: SessionProvider 세팅, ErrorState 컴포넌트, 훅 작성 등)로 나누고, 각 단계가 끝나면 어떤 테스트/검증을 했는지까지 보고한다.
- 새 작업을 시작할 때는 “목표 → 세부 단계 → 예상 결과” 구조를 명시해 사용자가 진행 상황을 쉽게 따라올 수 있게 한다.
- 각 단계 보고 시 반드시 **어떤 자동 테스트(React Testing Library, Storybook 시각 확인, Playwright 등)를 돌렸는지와 수동 확인(페이지 경로, mock 토큰 등)을 어떻게 했는지**를 적어야 한다.
- 프런트에서 API를 호출하거나 타입을 선언할 때는 `frontend/src/types/openapi.d.ts` 기반 타입(`paths`, `operations`, `components`)을 참고해 Request/Response 스키마를 그대로 사용해야 한다. 문자열 상수나 임의 타입 대신 오픈API에서 파생된 타입을 명시하고, 필요 시 코드에 `type LoginRequestBody = components["schemas"]["LoginRequest"]` 같은 alias를 둔다.
- API 호출 시 `/api/v1/...` 전체 경로를 넘기고, 에러 메시지는 `frontend/src/lib/api-error.ts`의 헬퍼(`getApiErrorMessage`, `getFetchError`)로 처리해 TypeScript가 `never`로 추론되지 않도록 한다.
- Next `Link`/라우터는 `Route` 타입을 지켜야 하므로, 내부 경로 판별 유틸(`isInternalRoute`)을 사용하고 외부 URL은 `<a>` 태그로 명확히 분기한다.
- macOS 환경에서는 Turbopack이 권한 문제를 일으킬 수 있으니, 프런트 변경 후에는 **반드시 `cd frontend && npm run build -- --webpack`**으로 타입/빌드 검증을 수행하고 결과를 공유한다.

### 프론트엔드 테스트 & 검증 프로세스

**개발 단계별 검증**:

1. **타입 검증 (필수)**: 코드 작성 후 `cd frontend && npm run build -- --webpack` 실행, 컴파일 에러 0개 확인
2. **수동 기능 테스트 (필수)**: `npm run dev`로 개발 서버 실행 후 실제 UI에서 주요 시나리오 테스트
   - 역할별 접근 제어, API 성공/실패, 로딩/에러/빈 상태, 반응형 동작 확인
   - 테스트 경로와 확인 내용을 AGENT_LOG에 기록
3. **엣지 케이스 검증 (권장)**: 빈 데이터, API 에러, 권한 없음, 네트워크 느림 상황 확인
4. **자동 테스트 (미래)**: 현재는 수동 검증 우선. React Testing Library/Playwright 도입 시 업데이트

**기능 완료 기준**: 빌드 통과, 수동 테스트 완료, 역할별 접근 제어, 에러/로딩/빈 상태 UI, 반응형, AGENT_LOG 기록

상세 내용은 `frontend/AGENTS.md`의 "프론트엔드 테스트 & 검증 프로세스" 참고.

## 코드 스타일 참고사항

- 불변 DTO는 Java `record` 사용
- 서비스 레이어에서 트랜잭션 처리 (`@Transactional`)
- 컨트롤러는 얇게 유지 - 서비스에 위임
- JPA에서 단방향 관계 선호
- 입력 검증은 `jakarta.validation` 사용
- 예외 처리는 `@ControllerAdvice`로 중앙집중화
- 비밀번호는 `PasswordEncoder`로 BCrypt 해싱

## 중요 제약사항

- 요청 DTO에 BaseEntity 필드(id, createdAt, updatedAt)를 절대 포함하지 않음
- UUID는 Hibernate가 생성하며, 클라이언트가 제공하지 않음
- 모든 타임스탬프는 JPA Auditing으로 관리
- 보안: 회원가입 허용 전 초대 코드 검증 필수
- 테스트는 성공과 실패 시나리오 모두 검증해야 함

## MCP 활용 지침

- **GitHub MCP 우선 사용**
  - 브랜치 생성/삭제, PR 확인, 리뷰, 이슈 업데이트 등 GitHub 상호작용은 가급적 GitHub MCP로 수행한다.
  - 로컬 Git 명령을 병행할 때는 동일 작업을 MCP로 미러링하거나 결과를 기록해 차이를 추적 가능하게 유지한다.
  - push/force push처럼 원격 이력에 영향이 있는 작업은 GitHub MCP로 실행하고, 필요한 경우 사전 승인을 문서화한다.
- **Context7 MCP 기반 레퍼런스 확인**
  - Next.js 16, React 19, Spring Boot 4 등 주요 의존성 문서는 Context7 MCP에서 `resolve-library-id` → `get-library-docs` 순으로 조회한다.
  - 구현·리뷰 과정에서 참고한 문서와 버전은 가능하면 AGENT_LOG나 PR 설명에 남긴다.
  - Context7에 원하는 페이지가 없을 때만 외부 자료를 사용하고, 출처 및 사유를 기록한다.
- **작업 흐름**
  1. 작업 시작 시 GitHub MCP로 브랜치/이슈/리뷰 상태를 확인한다.
  2. 구현 중 레퍼런스가 필요하면 Context7 MCP에서 최신 문서를 조회한다.
  3. 결과 보고 시 사용한 MCP 리소스(문서/리뷰 링크 등)를 함께 언급해 추적 가능성을 높인다.
