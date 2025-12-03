# Feature: SpringDoc 기반 Swagger UI 구축

## 1. Problem Definition
- Member/Auth/Invitation 및 StudentProfile API를 구현했지만, 팀원이 직접 호출해 보고 회귀 테스트에 활용할 API 문서/UI가 없다.
- Postman 컬렉션만으로는 최신 DTO/필드를 자동으로 반영하기 어렵고, 신규 엔드포인트 공유가 번거롭다.
- SpringDoc(OpenAPI)을 도입해 자동 문서화 + Swagger UI 테스트 콘솔을 제공하면 QA/프런트/운영이 즉시 API를 확인하고 호출할 수 있다.

## 2. Requirements
### Functional
1. SpringDoc(OpenAPI 3.0 이상) 의존 설정 및 `swagger-ui` 자동 노출.
2. `/v3/api-docs` JSON과 `/swagger-ui.html` UI를 기본 프로필(로컬/개발)에서 활성화하고, 운영 프로필은 필요 시 보안 적용.
3. 공통 메타 정보(제목, 버전, 연락처)와 글로벌 헤더(Authorization Bearer) 정의.
4. JWT 인증이 필요한 엔드포인트에서도 Swagger Try-Out이 동작하도록 Authorization 헤더 입력 창 제공.
5. 모든 컨트롤러/엔드포인트에 `@Tag`, `@Operation`, `@ApiResponse`를 부여해 Member/Auth/Invitation/StudentProfile/PersonalLesson 등 Tag/요약 정보를 유지한다.

### Non-functional
1. 의존성 추가 후에도 기존 빌드/테스트 파이프라인에 영향이 없어야 한다.
2. 환경별 접근 제어: dev/local에서만 UI 노출, prod에서는 비활성화 혹은 Basic Auth 적용.
3. API 문서는 빌드 시 자동 생성되며 별도 수작업이 없다.
4. 추후 Postman 컬렉션 내보내기를 고려해 OpenAPI JSON 파일을 CI 아티팩트로 저장할 수 있게 한다.

## 3. API Design (Draft)
- SpringDoc가 기존 REST Controller를 기반으로 자동 문서 생성하며, 각 클래스에 `@Tag(name="Invitation API")`, 메서드에 `@Operation(summary="", description="")`, 필요 시 `@ApiResponses`를 붙인다.
- `/v3/api-docs` : OpenAPI JSON
- `/swagger-ui.html` 또는 `/swagger-ui/index.html` : Swagger UI
- `OpenAPI` bean을 정의해 제목/description/라이선스/보안 스킴(Bearer JWT) 설정.

## 4. Domain Model (Draft)
- 설정 클래스: `SwaggerConfig` (예: `global.config`)
  - `@Configuration` + `@Profile({"local","dev"})`.
  - `OpenAPI openAPI()`에서 Info, Server, Components(SecuritySchemes/Bearer), SecurityRequirement 설정.
  - `GroupedOpenApi` Bean을 여러 개 등록해 `/api/v1/auth/**`, `/api/v1/student-profiles/**`, `/api/v1/personal-lessons/**` 등을 grouping.
  - 필요 시 `SwaggerUiConfigParameters`로 UI behavior 설정(디폴트 태그, docExpansion 등).
- Controller 수정: 각 컨트롤러에 `@Tag`, 메서드에 `@Operation`/`@ApiResponse` 부여.
- Security 연동: Swagger 리소스 경로 허용 및 JWT 헤더 입력 가능하도록 SecurityConfig에 `requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()` 추가.

## 5. TDD Plan
1. **Configuration Test**
   - `@SpringBootTest` + `TestRestTemplate`로 `/v3/api-docs` 응답이 200인지 확인(로컬/테스트 프로필 한정).
2. **Security Test**
   - Swagger 리소스 경로(`/swagger-ui/**`, `/v3/api-docs/**`)가 인증 없이 접근 가능한지 WebTestClient/MockMvc로 검증.
3. **Grouped/Annotation Test**
   - `application-test` 프로필에서 Swagger config 비활성화 여부 확인(예: bean 미생성).
   - Reflection 기반 테스트 혹은 커스텀 스캔으로 주요 컨트롤러에 `@Tag` 가 붙어 있는지 검증.
4. **Documentation Validation**
   - OpenAPI JSON 내 SecuritySchemes(Bearer), Tags, Path metadata가 있는지 스냅샷 테스트.
