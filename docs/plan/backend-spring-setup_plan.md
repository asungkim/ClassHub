# Feature: Backend Spring 초기 설정

## 1. Problem Definition

- 현재 backend 모듈에는 Spring Boot 프로젝트 구조, Gradle 설정, 기본 의존성, 공용 패키징 규칙(global/domain)이 정의되어 있지 않다.
- 환경 분리(local/dev/prod)와 필수 보안 설정(.env, application.yml 템플릿), 관측 로깅(SLF4J) 기준이 없어서 Phase 2 엔티티 작업을 착수할 수 없다.

## 2. Requirements

### Functional

- Spring Boot 4.0.0 / Java 21 기반 Gradle 프로젝트를 초기화한다 (settings.gradle, build.gradle 포함). not using kts
- Configuration YAML 사용
- `backend/domain`과 `backend/global`만 최상위 패키지로 사용하고, Domain Feature별 하위 구조(domain.<feature>.web/app/model/repository)를 위한 샘플 패키지를 만든다.
- 공통 설정(global):
  - `global.config`에 Spring `@Configuration` (CORS, Jackson, Locale, Timezone)을 정의.
  - `global.error` 패키지에 `ApiErrorResponse`, `GlobalExceptionHandler` 골격을 추가.
  - `global.common.BaseEntity`와 `BaseTimeEntity` 등 추상 클래스/인터페이스를 정의.
- 환경 관리:
  - `application.yml` + `application-local.yml` + `.env.example` 작성, DB/Redis 접속 정보는 Placeholder로 둔다.
  - `SPRING_PROFILES_ACTIVE`를 `.env`로 제어하고, `docker-compose` 연동을 고려한 `local` 프로필을 기본값으로 설정.
- 운영 편의:
  - `Actuator` 의존성 추가 후 `/actuator/health` 만 기본 노출.
  - `Slf4j` + `logback-spring.xml` 기본 패턴 구성.
  - `Testcontainers` 의존성(선택) 혹은 H2 in-memory profile로 테스트 환경을 준비.

### Non-functional

- Kotlin 대신 Java 기반, Gradle Kotlin DSL을 사용한다.
- CI(Commitlint)와 충돌 없도록 Gradle wrapper를 포함하고, 빌드 시 `./gradlew build` 기준으로 통과해야 한다.
- 설정/도메인 파일은 모두 한국어 주석이 아닌 의미 있는 클래스/패키지명으로 설명한다.
- 보안 비밀값(DB, JWT 등)은 `.env`/프로필에 Placeholder만 두고 깃에 실데이터를 포함하지 않는다.

## 3. API Design (Draft)

- 초기 설정 단계에서는 공개 API가 없으며, 검증용으로 Actuator `/actuator/health`만 활성화한다.
- 추후 HELLO 엔드포인트가 필요하면 `domain.sample` 패키지에 임시 Controller를 추가해 context-load 테스트를 한다.

## 4. Domain Model (Draft)

- `global.common.BaseEntity` : `id (UUID)`, `createdAt`, `updatedAt`.
- `global.common.BaseTimeEntity` : `createdAt`, `updatedAt`만 관리하는 추상 클래스.
- `global.error.ApiErrorResponse` : `{code, message, detail}`.
- `global.config.JpaConfig` : Auditing 활성화.
- `domain.sample` : Controller + Service + DTO 골격 (빌드/테스트 검증용) – 최종 기능 구현 시 제거 가능.

## 5. TDD Plan

1. `./gradlew clean build` 로컬 실행 시 성공해야 하며, 최소 `contextLoads` 테스트를 작성한다.
2. `Actuator /health` 엔드포인트에 대한 MockMvc 또는 WebTestClient 테스트를 작성해 200 OK를 검증한다.
3. BaseEntity Auditing이 동작하는지 JPA 테스트(예: H2)로 확인한다.
4. 환경별 프로필이 올바르게 로딩되는지 `SPRING_PROFILES_ACTIVE=local` 과 `test` 에서 각각 로깅/DB 속성이 적용되는지 통합 테스트로 검증한다.
