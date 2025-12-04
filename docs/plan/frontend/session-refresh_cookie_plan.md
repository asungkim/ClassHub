# Feature: Session Persistence with Refresh Cookie

## 1. Problem Definition
- 백엔드가 RefreshToken을 HttpOnly 쿠키로 내려주도록 바뀌었지만, 프런트는 새로고침 시 AccessToken을 복구하지 않아 즉시 로그아웃된다.
- SessionProvider가 앱 초기화 시 `/auth/refresh`를 호출해 AccessToken을 재발급 받고, 수동 로그아웃 시 쿠키 초기화와 상태 정리를 해야 한다.

## 2. Requirements

### Functional
1. **SessionProvider 초기화**
   - 앱 마운트 시 `/auth/refresh` 호출을 시도해 AccessToken을 재발급하고 `setAuthToken`/`useSession` 컨텍스트를 갱신한다.
   - 쿠키가 없거나 401이면 기존처럼 unauthenticated 상태로 유지한다.
2. **로그인 흐름**
   - 로그인 성공 시 AccessToken만 JSON에서 읽어 `setAuthToken` 한다 (RefreshToken 쿠키는 서버가 자동 처리).
   - 로그인 후 `useSession`을 invalidation해 `/auth/me`를 다시 호출한다.
3. **로그아웃**
   - `/auth/logout` 호출 후 React Query 캐시 무효화, AccessToken 제거, 홈 화면으로 이동시킨다.
4. **에러 처리**
   - `/auth/refresh` 실패 시 ErrorState 대신 기본 안내를 보여주고, 사용자가 로그인을 다시 시도할 수 있게 한다.

### Non-functional
1. 모든 fetch는 `credentials: "include"`로 쿠키가 전송되도록 설정한다.
2. SessionProvider는 Refresh 호출이 끝날 때까지 `status="loading"`을 유지해 깜박임을 최소화한다.
3. React Query/Hook 테스트를 통해 로딩·성공·실패 상태를 검증한다.

## 3. API Design (Draft)
- `POST /api/v1/auth/refresh`
  - Request Body 없음
  - Response: `{ accessToken, accessTokenExpiresAt }` (Refresh 쿠키 갱신)
- `POST /api/v1/auth/logout`
  - Request Body 없음
  - Response: `{ code, message }`

## 4. Domain Model (Draft)
- **RefreshState**
  - `status`: idle/loading/success/failure
  - `errorMessage`
- **SessionContext**
  - `refresh()` 함수가 새 API 호출을 추상화한다.

## 5. Test Plan
1. **SessionProvider Refresh Test**
   - React Testing Library + MSW로 `/auth/refresh` 성공/실패를 mock하여 `status` 전환을 검증한다.
2. **Logout Flow Test**
   - Logout 버튼을 클릭했을 때 `/auth/logout` mock + `clearAuthToken` 호출을 확인한다.
3. **Manual Verification**
   - 로그인 후 새로고침해도 대시보드가 유지되는지 실 브라우저에서 확인하고, 로그아웃 시 쿠키가 삭제되는지 DevTools에서 확인한다.
