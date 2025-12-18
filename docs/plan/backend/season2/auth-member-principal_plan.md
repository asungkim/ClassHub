# Feature: Auth MemberPrincipal Role Claim

## 1. Problem Definition
- Requirement v1.3와 Spec v1.3(섹션 2.2.2 Security 규칙)에서는 역할별 권한 제어가 핵심인데, 현재 `MemberPrincipal`이 UUID만 보유해 컨트롤러/서비스에서 인증 사용자의 역할을 즉시 활용할 수 없다.
- Access Token에는 `authority` 클레임이 있지만 `MemberPrincipal`로 전파되지 않아 `@AuthenticationPrincipal` 사용 시 다시 DB 조회를 해야 하거나 앞으로 추가될 역할 검증을 적용할 수 없다.
- Season2 기능(Company 검증, Enrollment, Clinic 등)에서 Role 기반 분기가 늘어나므로, JWT → Spring Security Authentication → Controller까지 Role 정보를 일관되게 전달하는 계약을 정의해야 한다.

## 2. Requirements

### Functional
1. Access Token 생성 시 `MemberRole` 값을 `role`/`authority` 클레임으로 명시하고, `JwtProvider#getAuthentication`이 `MemberPrincipal(id, role)`을 조립한다.
2. `MemberPrincipal`은 `UUID id`, `MemberRole role`을 보유하고 `@AuthenticationPrincipal` 주입 대상이 된다. Role이 null이면 인증 실패로 간주한다.
3. `JwtAuthenticationFilter`는 기존과 동일하게 동작하지만, SecurityContext에 저장된 `Authentication`으로부터 `SimpleGrantedAuthority(MemberRole.name())`를 얻어 method-security와 `requestMatchers().hasAuthority()` 규칙을 충족해야 한다.
4. `AuthService.issueTokens`는 `Member`에서 역할을 읽어 Access Token에 포함시키고, `AuthController` 경로(`login`, `refresh`, `me`)가 신규 계약을 이용하도록 한다.
5. 기존 테스트/컨트롤러(`MemberControllerTest`, `SecurityIntegrationTest` 등)에서 `MemberPrincipal`을 생성할 때 Role을 명시하도록 갱신한다.

### Non-functional
- 토큰 포맷 변경 이후에는 기존 토큰을 더 이상 신뢰하지 않으므로 배포 직후 재로그인을 요구한다. (Season2 초기화 구간이라 backwards compatibility 필요 없음)
- Role 값을 클레임으로 보관해도 민감 정보 노출 리스크가 없도록 Enum 값을 그대로 사용하며, 허용되지 않은 값이 들어온 경우 즉시 인증 실패를 반환한다.
- JwtProvider 단위 테스트로 만료·클레임·Authentication 구성이 안정적으로 유지됨을 보장한다.

## 3. API Design (Draft)

| Method | URL | 요청/응답 초안 | 비고 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Request: `{ email, password }` (기존과 동일). Response: `LoginResponse` (access/refresh token + 만료). Access Token은 `sub=memberId`, `claims={ "id": memberId, "role": MemberRole.name }`. | Role은 `Member.role`에서 읽어 인증 컨텍스트에 반영 |
| `POST` | `/api/v1/auth/refresh` | Request: Refresh 토큰(HttpOnly 쿠키). Response: `LoginResponse`. Access Token 갱신 시 동일한 Role 클레임을 다시 기록한다. | Refresh 토큰에는 여전히 `id`만 담아 저장 공간 최소화 |
| `GET` | `/api/v1/auth/me` | Authorization 헤더 필요. AuthenticationPrincipal로부터 `MemberPrincipal`을 주입받거나, 기존처럼 `memberId`를 통해 DB 조회한다. Role은 추후 DB 조회 전 단건 검증/로그용으로 활용한다. | 추후 Teacher/Assistant별 분기 로직 추가 예정 |

## 4. Domain Model (Draft)

- `Member` (aggregate): `id`, `email`, `password`, `name`, `role: MemberRole`, `deletedAt`. 역할 값은 `MemberRole` Enum으로 관리한다.
- `MemberRole` Enum: `TEACHER`, `ASSISTANT`, `STUDENT`, `ADMIN`, `SUPER_ADMIN` (Spec v1.3 기준으로 확장 예정). Spring Security 권한 문자열로 그대로 사용한다.
- `MemberPrincipal` (record DTO): `UUID id`, `MemberRole role`. JWT Authentication → Controller 사이의 최소 식별 정보를 전달한다.
- `Jwt Claims`:
  - Access Token: `id`, `role` (or `authority`) + 기본 registered claim(sub, iat, exp).
  - Refresh Token: `id`만 유지.

## 5. TDD Plan
1. `JwtProviderTest` 보강  
   - Access Token 생성 시 `role` 클레임을 포함하며 `getAuthentication(token)`이 `MemberPrincipal`의 `role`과 `GrantedAuthority`를 일치시킴을 검증한다.  
   - Refresh Token에는 role이 없음을 재확인한다.
2. `SecurityIntegrationTest`  
   - `generateAccessToken` 호출 시 Enum 기반 role을 넣고 `/api/v1/admin/**` 접근 권한 테스트가 계속 성공하는지 확인한다.  
   - 필요 시 `MemberPrincipal`을 노출하는 가짜 Controller를 추가해 role이 주입되는지 검증한다.
3. Controller 단위 테스트 (`MemberControllerTest` 등)  
   - `@AuthenticationPrincipal MemberPrincipal` 사용 지점에서 role을 전달하도록 헬퍼를 수정하고, role 기반 예외 흐름이 필요하면 검증한다.
4. 회귀 테스트  
   - `AuthController` 경로(`/login`, `/refresh`, `/me`)가 변경된 토큰 스펙으로도 200 응답을 반환하는지 WebMvc 테스트로 확인한다 (필요 시).

## 6. Implementation Steps (3단계)
1. **MemberPrincipal / MemberRole 정비**  
   - `MemberPrincipal`을 `record UUID id, MemberRole role` 구조로 확장하고, `MemberRole` Enum을 Spec v1.3 값(TEACHER/ASSISTANT/STUDENT/ADMIN/SUPER_ADMIN)으로 보강한다.  
   - 관련 DTO/테스트에서 새 생성자를 사용하도록 정리해 컴파일 오류를 우선 해결한다.
2. **JWT 생성·검증 계층 리팩터링**  
   - `AuthService.issueTokens`, `JwtProvider.generateAccessToken`, `JwtProvider.getAuthentication`, `JwtAuthenticationFilter`를 수정해 Access Token에 role 클레임을 넣고, Authentication의 Principal/GrantedAuthority를 일관되게 설정한다.  
   - `JwtProviderTest`, `SecurityIntegrationTest`를 업데이트해 role 포함 토큰이 정상 동작함을 검증한다.
3. **컨트롤러/회귀 검증 및 배포 준비**  
   - `AuthController`, `MemberController` 등 `@AuthenticationPrincipal`을 사용하는 진입점에서 role을 활용하도록 코드/테스트를 보정하고, 필요 시 로깅/예외 메시지를 role 기반으로 다듬는다.  
   - 변경된 토큰 포맷을 배포 노트/AGENT_LOG에 기록해 재로그인 필요성을 공유한다.
