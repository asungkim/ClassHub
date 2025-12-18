# Feature: Teacher Registration API

## 1. Problem Definition

- Requirement v1.3 (섹션 “👩‍🏫 Teacher”)는 이메일/비밀번호/이름/전화번호 기반의 선생님 회원가입을 필수 단계로 정의하고 있으나, Season2 코드베이스에는 `/api/v1/members/register/teacher`가 제거된 상태라 신규 Teacher가 직접 계정을 만들 수 없다.
- Spec v1.3에서도 Auth 리소스가 “역할별 회원가입”을 제공한다고 명시하지만 실제 API/서비스/테스트 구현이 부재해 로그인/대시보드 진입선이 단절되어 있다.
- TEACHER Role을 확보하지 못하면 이후 Company/Branch 등록, Course 생성, Invitation 발송 등 Phase 4~5 작업 흐름이 모두 차단되므로, 안전한 회원가입 API를 재설계·구현해야 한다.
- 장차 조교/학생 가입도 동일한 흐름에서 처리할 예정이라, 역할 공통 로직을 캡슐화한 `RegisterService`를 마련하고 이번 작업에서는 `registerTeacher(...)`부터 구현해 토대를 만든다.

## 2. Requirements

### Functional

1. **엔드포인트**: `POST /api/v1/members/register/teacher` (인증 불필요)
   - Request Body: `{ email, password, name, phoneNumber }`. 모든 필드는 `@NotBlank`, email은 RFC-5322 검증, phone은 숫자와 `-`만 허용하며 `+` 기호는 금지한다.
2. **정규화 및 중복 검사**
   - 이메일은 `trim().toLowerCase()` 후 사용하며, 이미 존재하면 `RsCode.DUPLICATE_EMAIL`로 거절한다.
   - 전화번호는 숫자만 남긴 뒤 `-` 구분자로 재조립해 일관된 포맷을 유지한다.
3. **비밀번호 정책**
   - 최소 8자, 대/소문자, 숫자, 특수문자 중 2종 이상 포함 여부를 Bean Validation(커스텀 `@PasswordRule`)로 검사하고, `PasswordEncoder`로 해시해 저장한다.
4. **Teacher Member 생성**
   - `Member` 엔티티에 `role=MemberRole.TEACHER`, `name`, `phoneNumber`, `email`, `password`를 채워 저장한다. Soft-delete 상태 회원이 존재하면 `RsCode.MEMBER_INACTIVE`로 응답한다.
5. **토큰 발급 및 응답**
   - 가입 직후 `AuthService.issueTokens` (또는 login 재사용)으로 Access/Refresh 토큰을 발급하고 `LoginResponse`(memberId, accessToken, accessTokenExpiresAt)만 반환한다.
   - `RefreshTokenCookieProvider`로 HttpOnly 쿠키를 세팅해 Refresh 토큰을 쿠키로만 전달한다.
6. **RegisterService 토대 마련**
   - `RegisterService`를 도입해 `registerTeacher(RegisterTeacherRequest request)`를 우선 구현한다.  
   - 추후 `registerAssistant`/`registerStudent`를 동일 서비스 내에서 확장할 수 있도록 입력 DTO와 검증 로직을 모듈화한다.

### Non-functional

- **보안**: Rate Limiting/Recaptcha는 인프라에서 처리하지만, Controller 레벨에서 실패 사유를 모호하게(`UNAUTHENTICATED`, `DUPLICATE_EMAIL`) 유지해 계정 추측을 어렵게 한다.
- **트랜잭션 무결성**: Member 저장과 Refresh 토큰 처리(블랙리스트 초기화)는 한 트랜잭션에서 수행하고, 중간 예외 발생 시 새 Member가 남지 않도록 롤백한다.

## 3. API Design (Draft)

| Method | URL                             | Request                                                                                                                                   | Response/Status                                                                                                                          | Error Codes                                                                                  |
| ------ | ------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| POST   | `/api/v1/members/register/teacher` | `json\n{ \"email\": \"teacher@classhub.com\", \"password\": \"Classhub!1\", \"name\": \"김선생\", \"phoneNumber\": \"010-1234-5678\" }\n` | `200 OK` + `RsData<LoginResponse>` (memberId, role=TEACHER, accessToken, accessTokenExpiresAt, refreshTokenExpiresAt); Refresh 쿠키 세팅 | `DUPLICATE_EMAIL`, `INVALID_PHONE_FORMAT`, `WEAK_PASSWORD`, `MEMBER_INACTIVE`, `BAD_REQUEST` |

- Validation 실패는 `400` + `RsCode.BAD_REQUEST`로 통일하고, `@ControllerAdvice`가 Bean Validation 메시지를 수집한다.
- 향후 Company/Branch 연동이 필요하면 `data`에 `setupRequired=true` 같은 플래그를 추가할 여지를 남긴다.

## 4. Domain Model (Draft)

- **Member (Aggregate Root)**
  - 필드: `id`, `email`, `password`, `name`, `phoneNumber`, `role`, `deletedAt`.
  - 동작: `static Member.createTeacher(...)` 팩토리로 role 고정, email/phone Normalization을 담당. Soft-delete 상태 재가입 시 `restore()` 호출 여부 판단.
- **MemberRole**: Enum (`TEACHER`, `ASSISTANT`, `STUDENT`, `ADMIN`, `SUPER_ADMIN`). 본 작업에서는 `TEACHER`만 사용.
- **TeacherRegisterRequest (record DTO)**
  - Bean Validation + `normalizedEmail()`, `normalizedPhoneNumber()` 헬퍼 포함.
- **RegisterService**
  - 의존성: `MemberRepository`, `PasswordEncoder`, `AuthService` (토큰 발급).
  - 책임: 공통 검증/정규화/멤버 생성 로직을 담당하며 `registerTeacher(...)`를 시작으로 역할별 가입 메서드를 순차적으로 확장한다.

## 5. TDD Plan

1. **Repository/Validator 단위 테스트 (`MemberRepositoryTest`)**
   - `shouldDetectDuplicateEmail_whenSameAddressExists`: 동일 이메일 저장 후 신규 요청이 `DUPLICATE_EMAIL` 트리거 준비가 되는지 확인한다.
   - (선택) `shouldNormalizePhone_whenSavingMember`: phone normalizer가 정규화된 값을 저장하는지 검증해 도메인 로직을 보호한다.
2. **Application 서비스 단위 테스트 (`RegisterServiceTest`)**
   - `shouldRegisterTeacher_whenInputValid`: 중복 이메일 미존재 → `RegisterService.registerTeacher`가 Member 생성 → `AuthService.login` 호출 & AuthTokens 반환.
   - `shouldFailRegisterTeacher_whenEmailAlreadyExists`: Repository에서 기존 Member를 반환하면 `BusinessException(DUPLICATE_EMAIL)` 발생.
   - `shouldNormalizeAndEncodePassword_forTeacher`: email lowercasing, phone normalizer, password encoder 호출 여부를 검증한다.
   - (필요 시) Soft-delete 회원 복구/차단 정책을 정의하고 예외 흐름을 테스트한다.
3. **Controller 통합 테스트 (`MemberControllerTest`)**
   - 성공 케이스: `POST /api/v1/members/register/teacher` 호출 시 200 + `LoginResponse` JSON + Refresh 쿠키 세팅 확인.
   - 실패 케이스: 잘못된 email/짧은 비밀번호 입력 시 `400 BAD_REQUEST` + Validation 메시지 구조 검증.
4. **회귀 테스트**
   - `JwtAuthenticationFilter` 허용 경로(`POST /api/v1/members/register/teacher`는 permitAll 대상이므로 Rule을 MemberController에 맞춰 확인) 유지 여부를 `SecurityIntegrationTest`로 확인한다.
   - `AuthServiceTest` 포함 전체 `./gradlew test`를 실행해 기존 Auth 흐름이 깨지지 않았는지 검증한다.

## 6. Implementation Steps (3단계)
1. **RegisterService 골격 + DTO/Repository 보강**
   - RegisterService와 RegisterTeacherRequest(Bean Validation/정규화 헬퍼 포함)를 추가하고, MemberRepository에 이메일 중복 검사를 위한 메서드를 확장한다.
   - 전화번호 Normalizer/Validator를 유틸 또는 값 객체로 추출해 이후 역할에서도 재사용한다.
2. **registerTeacher 유즈케이스 + 단위 테스트**
   - RegisterService.registerTeacher에서 중복 검사 → 비밀번호 해싱 → Member 생성 → AuthService.login 호출 순서를 구현하고 RegisterServiceTest로 정상/중복/정규화/Soft-delete 시나리오를 검증한다.
   - Member 팩토리/Builder를 보완해 의도를 드러내고, 필요한 도메인 메서드(예: Member.createTeacher)를 정리한다.
3. **MemberController 연동 + 회귀 검증**
   - `/api/v1/members/register/teacher`를 RegisterService에 위임하는 MemberController를 추가하고 MockMvc 테스트로 성공/Validation 실패/Refresh 쿠키 설정을 검증한다.
   - `./gradlew test`로 Auth 관련 회귀를 실행하고, 변경 사항을 AGENT_LOG에 기록한다.
