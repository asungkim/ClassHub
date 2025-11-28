# Feature: 로그아웃 API (Refresh 토큰 무효화)

## 1. Problem Definition
- 현재 로그인/Refresh 기능은 제공되지만, 사용자가 명시적으로 로그아웃하거나 토큰 탈취가 의심될 때 Refresh 토큰을 무효화할 수단이 없다.
- Redis 기반 세션 스토어를 도입하기 전까지는 Refresh 토큰을 서버 측에 저장하지 않으므로, 임시로 토큰 서명을 블랙리스트에 추가하는 방식으로 로그아웃을 처리해야 한다.

## 2. Requirements

### Functional
- `POST /api/v1/auth/logout`
  - Request: `{ "refreshToken": "" }`
  - Flow:
    1. 입력 Validation (`@NotBlank`).
    2. JWT 서명 검증 + 만료 여부 확인.
    3. 유효한 토큰이면 블랙리스트(In-Memory) 저장소에 등록하여 추후 Refresh 요청 시 차단한다.
    4. 다중 기기 대응을 위해 `logoutAll` 옵션을 제공하여 동일한 사용자 ID의 Refresh 요청을 모두 차단할 수 있는 Hook 제공(해당 옵션은 현재 동작을 정의하거나 TODO로 남긴다).
  - Response: `RsData<Void>` (성공 시 200). 이미 만료/등록된 토큰이라도 멱등성을 위해 SUCCESS 반환.
- `POST /api/v1/auth/refresh`
  - 기존 로직 유지 + 블랙리스트 조회 후 차단된 토큰이면 `UNAUTHENTICATED`.

### Non-functional
- Refresh 블랙리스트는 `ConcurrentHashMap<String, LocalDateTime>` 등 In-Memory 자료구조로 구현(서버 재시작 시 초기화).
- Map 크기 제한 + 만료된 항목 정리를 위한 간단한 Clean-up 로직(스케줄러 혹은 Refresh 호출 시 Lazy cleanup) 구현.
- 실제 Redis 도입 시 동일 인터페이스(`RefreshTokenStore`)를 Redis 구현으로 교체할 수 있도록 추상화.
- 로그/예외 시 토큰 전체 문자열을 출력하지 말고 앞부분만 마스킹.

## 3. API Design (Draft)
| Method | URL | Request | Response | Notes |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/logout` | `{ refreshToken, logoutAll? }` | `RsData<Void>` | 멱등; logoutAll=true 시 전체 블랙리스트 처리(현재는 TODO) |

## 4. Domain Model (Draft)
- Interface: `RefreshTokenStore`
  - `void blacklist(String token, LocalDateTime expiresAt)`
  - `boolean isBlacklisted(String token)`
  - `void blacklistAllForMember(UUID memberId)` (logoutAll 옵션 대비, 기본 구현은 TODO)
- Implementation: `InMemoryRefreshTokenStore`
  - 구조: `ConcurrentHashMap<String, LocalDateTime>` (token → expiresAt)
  - Cleanup: 만료된 항목 제거 메서드 `evictExpired()`
- AuthService
  - `logout(RefreshRequest, boolean logoutAll)`
  - `refresh` 호출 시 `isBlacklisted` 확인 후 예외

## 5. TDD Plan
1. **Store 단위 테스트**
   - `blacklist`/`isBlacklisted` 동작, 만료 후 `evictExpired` 실행 시 제거 여부, `blacklistAllForMember`는 TODO로 pending 처리.
2. **Service 테스트**
   - 로그아웃 호출 시 Store에 토큰이 추가되는지 검증.
   - Refresh 시 블랙리스트 토큰이면 `UNAUTHENTICATED`.
3. **Controller 테스트 (Validation/응답)**
   - `/auth/logout`: 정상 요청 200, Validation 실패 400.
   - logoutAll 옵션은 현재 구현 여부에 따라 응답/메시지를 검증하거나 TODO로 남긴다.
