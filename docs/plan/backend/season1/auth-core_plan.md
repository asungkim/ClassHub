# Feature: Auth 코어 (Spring Security + JWT)

## 1. Problem Definition
- Teacher/Assistant/Student/SuperAdmin 역할로 로그인/회원가입/초대 코드 검증을 처리하는 인증 흐름이 필요하다.
- Member/Invitation 엔티티는 존재하지만, 실제 로그인/토큰 발급/보안 구성을 담당하는 서비스 계층이 없다.

## 2. Requirements
### Functional
- Teacher 회원가입: 이메일/비밀번호(암호화)/이름 입력 → Member 생성 → 기본 role=TEACHER.
- 로그인: 이메일/비밀번호 검증 → JWT Access/Refresh 발급.
- 로그아웃: Refresh 토큰 무효화(추후 Redis 등 고려).
- 초대 코드 검증: Invitation code 유효성 체크(PENDING, 만료 x) → inviteeRole 반환.
- 초대 기반 회원가입: 유효한 초대 코드/역할에 맞춰 Member 생성, Invitation 상태 ACCEPTED.
- Refresh 토큰 기반 Access 재발급.
- `GET /auth/me`: Authorization 헤더의 Access 토큰을 검증하고, 현재 로그인한 회원의 요약 정보(id, name, email, role)를 반환. 프런트 세션 컨텍스트 및 권한 체크 기준으로 사용.

### Non-functional
- Spring Security 설정: JWT 필터, AuthenticationEntryPoint/AccessDeniedHandler, PasswordEncoder(BCrypt).
- JwtProvider: Access/Refresh 토큰 생성/검증, 비밀키는 .env/환경 변수로 관리.
- 테스트는 MockMvc + Slice 또는 통합 테스트로 로그인/가입/초대 절차 검증.
- 로그 기록 및 실패 사유 표준화.

## 3. API Design (Draft)
- `POST /auth/register/teacher`
- `POST /auth/login`
- `POST /auth/logout`
- `POST /auth/verify-invitation`
- `POST /auth/register/by-invitation`
- `POST /auth/refresh`
- `GET /auth/me`
  - Request: `Authorization: Bearer <access-token>`
  - Response 200: `{ "code": 1000, "data": { "id": "UUID", "name": "...", "email": "...", "role": "TEACHER" } }`
  - Response 401: `RsCode.UNAUTHENTICATED` (토큰 누락/만료) → SecurityConfig에서 필터가 처리

## 4. Domain Model (Draft)
- AuthRequest/Response DTO (login/register/by-invitation)
- TokenResponse {accessToken, refreshToken}
- InvitationVerificationResponse {inviteeRole, status}
- MeResponse {id, email, name, role}

## 5. TDD Plan
1. 로그인 성공/실패 테스트(MockMvc) – 올바른 이메일/비밀번호 조합 vs 오류.
2. Teacher 회원가입 테스트 – Member 저장 + 암호화된 패스워드 검증.
3. 초대 코드 검증/회원가입 – Invitation 상태 전환 + 생성된 Member role 확인.
4. JWT 토큰 재발급 테스트 – 유효 Refresh → 새 Access 발급.
5. Security 필터 테스트 – 보호된 API 접근 시 Role 기반 인가 동작 확인.
6. `GET /auth/me` 테스트 – Authorization 헤더가 있을 때 현재 사용자 요약 정보 반환, 헤더가 없거나 만료된 토큰이면 401 응답.
