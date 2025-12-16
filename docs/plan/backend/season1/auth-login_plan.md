# Feature: 로그인 API + Access/Refresh 발급

## 1. Problem Definition
- Teacher 회원가입은 완료되었지만, 이메일/비밀번호로 로그인하여 Access/Refresh 토큰을 발급하고 재발급/로그아웃 흐름을 처리하는 API가 없다.
- 프런트엔드가 이후 Course/Notice 기능을 호출하려면 로그인 API와 토큰 발급/갱신 규칙이 필요하다.

## 2. Requirements

### Functional
- `POST /api/v1/auth/login`
  - Request: `{ email, password }`
  - Process
    1. 입력 Validation (`@Email`, `@NotBlank`)
    2. 해당 이메일 Member 조회 → 없으면 `RsCode.UNAUTHENTICATED`.
    3. PasswordEncoder.matches 검증 → 실패 시 `UNAUTHENTICATED`.
    4. JwtProvider로 Access/Refresh 발급(Claims: memberId, role)
  - Response: `RsData<LoginResponse>` (memberId, accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt)
- Refresh 토큰 재발급: `POST /api/v1/auth/refresh`
  - Request: `{ refreshToken }`
  - Flow: Refresh 유효성 검사 → 새 Access/Refresh 발급 → Response 동일.
- 로그아웃/토큰 무효화는 향후 Redis 연동 예정(이번 PLAN에서는 Refresh stubbing; 토큰 블랙리스트는 TODO로 남김).
- 실패 시 코드는 `UNAUTHENTICATED` 혹은 `BAD_REQUEST` (Validation) 사용.

### Non-functional
- JWT 비밀키/만료 시간은 기존 `custom.jwt` 설정.
- Refresh 토큰 관리는 추후 Redis 도입 전까지 In-memory map or JPA 엔티티 없이 구조만 정의(NOTE: 실제 구현에서 TODO 남김).
- Password check/토큰 발급 로직은 Service 계층에서 처리하고 로그에 민감정보는 마스킹.
- 테스트는 MockMvc + SpringBootTest 통합 테스트, Service 레벨 테스트(필요 시)로 로그인 성공/실패/Refresh 흐름을 검증.

## 3. API Design (Draft)
| Method | URL | Request | Response | Notes |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | `{ "email": "", "password": "" }` | `RsData<{ memberId, email, authority, accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt }>` | 성공 시 Access/Refresh 발급 |
| POST | `/api/v1/auth/refresh` | `{ "refreshToken": "" }` | `RsData<LoginResponse>` | Refresh 만료 시 UNAUTHENTICATED |

## 4. Domain Model (Draft)
- DTO
  - `LoginRequest(email, password)`
  - `LoginResponse(memberId, accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt)`
  - `RefreshRequest(refreshToken)`
- Service
  - `AuthService.login(LoginRequest)`
  - `AuthService.refresh(RefreshRequest)`
- Repository: `MemberRepository.findByEmail`
- JWT Provider: 기존 `JwtProvider` (만료/claim).
- Optional Refresh storage abstraction(추후 Redis) – 이번에는 서비스 내 TODO로 명시.

## 5. TDD Plan
1. **Controller 통합 테스트 (응답/Validation 위주)**
   - 로그인 성공 → 200 + `RsData` 구조/토큰 필드 확인.
   - 잘못된 비밀번호/이메일 시 401 응답 형태 확인.
   - Validation 실패(이메일 포맷, 비밀번호 공백) → 400.
   - Refresh Endpoint도 동일하게 정상/실패 응답 구조만 확인.
2. **Service 테스트 (비즈니스 로직 전체)**
   - 회원 조회 실패, 비밀번호 불일치, 비활성 사용자 등 예외 검증.
   - 로그인 성공 시 토큰/만료 값 생성 및 PasswordEncoder.matches 호출 여부 확인.
   - Refresh: 정상 토큰 → 새 Access/Refresh 발급, 잘못된/만료 토큰 → UNAUTHENTICATED.
