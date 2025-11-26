# Feature: Global 도메인 공통 구성 정의

## 1. Problem Definition
- 엔티티, 응답, 예외, 설정 모듈이 흩어져 있어 도메인 별 일관된 패턴을 강제하기 어렵다.
- global 패키지에서 제공하는 베이스 클래스/Aspect/Exception을 명문화하지 않으면 Phase2 구현 시 중복·불일치가 발생한다.

## 2. Requirements
### Functional
- `global.entity`에서 BaseEntity/BaseTimeEntity를 통해 UUID + Auditing 필드를 통합한다.
- `global.response`(`RsData`, `RsCode`, `RsConstant`)로 모든 API 응답 포맷을 단일화한다.
- `global.aspect.ResponseAspect`로 RsData의 code 값과 HTTP Status를 동기화한다.
- `global.exception`(`BusinessException`, `GlobalExceptionHandler`)으로 비즈니스/일반 예외 응답을 처리한다.
- `global.config`에는 JpaConfig, WebConfig(CORS/Locale/Timezone) 등을 등록하고, `application*.yml` 프로필과 `.env.example`을 유지한다.
- 샘플 도메인(`domain.sample`)은 위 공통 모듈을 사용하는 CSR 예시로 유지하며 `/api/v1/sample/ping`에서 RsData 응답을 보여준다.

### Non-functional
- Java + Spring Boot 4.0.0 + Gradle Groovy DSL 유지.
- 모든 클래스/패키지는 영어 기반 명명, 주석 최소화.
- 응답/예외 구조는 한국어 메시지를 기본으로 하되, HTTP Status는 국제 표준을 따른다.
- Test: contextLoads, actuator health, BaseEntity auditing 등을 항상 통과해야 한다.

## 3. API Design (Draft)
- 샘플 API: `GET /api/v1/sample/ping` → `RsData` `{code, message, data:{message,timestamp}}`
- 예외 응답: `RsData` `{code, message}` (data optional)

## 4. Domain Model (Draft)
- `BaseEntity(id:UUID)` extends `BaseTimeEntity(createdAt,updatedAt)`
- `RsCode` enum: SUCCESS, CREATED, BAD_REQUEST, NOT_FOUND, FORBIDDEN, UNAUTHORIZED, INTERNAL_SERVER, TOO_MANY_REQUESTS 등
- `BusinessException(rsCode)`
- `SampleNote` entity (content)

## 5. TDD Plan
1. `./backend/gradlew test` 실행 시 contextLoads + health endpoint + BaseEntity auditing 테스트가 통과해야 한다.
2. `/api/v1/sample/ping` 통합 테스트에서 RsData 구조/HTTP 200을 검증(추후 작성 예정).
3. BusinessException 발생 시 RsData 응답/HTTP Status가 기대대로 설정되는지 통합 테스트(추후 작성 예정).
