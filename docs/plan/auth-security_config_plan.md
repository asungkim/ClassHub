# Feature: Spring Security Config + JWT Provider

## 1. Problem Definition
- Auth 엔티티/PLAN은 존재하지만, 실제로 HTTP 요청을 보호하고 JWT 발급/검증을 담당할 Security 설정이 없다.
- Password 암호화, JWT Provider, AuthenticationEntryPoint/AccessDeniedHandler를 표준화하지 않으면 이후 API 개발 시 매번 재구현해야 한다.

## 2. Requirements
### Functional
- Spring Security Filter Chain:
  - Stateless(JWT) 설정, CSRF 비활성화, 특정 경로(`/auth/**`, `/actuator/health`)만 permitAll.
  - 나머지 API는 인증 필요, Role에 따라 접근 제어.
- PasswordEncoder: BCrypt 기반 Bean 제공.
- JWT Provider:
  - Access/Refresh 토큰 생성/검증, 만료 시간 설정, 시크릿 키 로딩(환경 변수/설정 파일).
  - JWT Claim에 memberId, role 포함.
- AuthenticationEntryPoint/AccessDeniedHandler: 공통 RsData 응답 사용.
- SecurityContext에 Member 정보를 주입하기 위한 Custom Authentication Filter/Resolver.

### Non-functional
- 설정 값(secret, 만료 시간 등)은 `application.yml` + `.env`에서 관리.
- 단일 책임 원칙: Config, JWT Provider, TokenService를 분리.
- 테스트: Spring Security `@WithMockUser` 또는 커스텀 Security 테스트로 필터 동작 검증.

## 3. API Design (Draft)
- 이 PLAN은 Config/Provider에 집중하므로 API 정의 없음. Auth API는 `auth-core_plan.md` 참조.

## 4. Domain Model (Draft)
- JwtTokenProvider: `generateAccessToken(memberId, role)`, `generateRefreshToken(memberId)`, `parseClaims(token)`.
- JwtAuthenticationFilter: Authorization 헤더 → Token 검증 → SecurityContext 저장.
- SecurityConfig: filter chain + permitAll/role 설정.

## 5. TDD Plan
1. JwtTokenProvider 테스트: Access/Refresh 생성/검증, 만료 처리.
2. SecurityConfig 통합 테스트: `/auth/login` permitAll vs 보호된 엔드포인트 401/403 검증.
3. PasswordEncoder Bean 테스트: encode/ matches.
