# Feature: RefreshToken Cookie & Session Persistence (Backend + Frontend)

## 1. Problem Definition
- 현재 Access/Refresh 토큰은 모두 JSON 응답으로만 전달되어 브라우저 새로고침 시 세션이 유지되지 않는다.
- RefreshToken을 HttpOnly 쿠키로 저장하고, 프런트가 앱 초기화 시 자동으로 AccessToken을 재발급할 수 있도록 백엔드/프런트 전체 흐름을 재구성해야 한다.

## 2. Requirements

### Functional (Backend)
1. **쿠키 발급**
   - 로그인(`POST /api/v1/auth/login`) 및 초대/Teacher 회원가입 응답에 RefreshToken을 HttpOnly + Secure + SameSite=Lax 쿠키로 설정한다.
   - 쿠키 이름 예: `refreshToken`.
2. **재발급 엔드포인트**
   - `POST /api/v1/auth/refresh`는 요청 본문 대신 쿠키에 포함된 RefreshToken을 사용해 AccessToken/쿠키를 재발급한다.
   - 토큰이 없거나 블랙리스트면 `401`.
3. **로그아웃**
   - `POST /api/v1/auth/logout` 호출 시 RefreshToken 쿠키를 즉시 만료시킨다 (Set-Cookie Max-Age=0).
4. **보안 설정**
   - Spring `ResponseCookie`를 사용하고, 환경별 도메인/secure 속성 설정을 지원한다.

### Functional (Frontend)
1. **SessionProvider 초기화**
   - 앱 로드 시 `api.POST("/auth/refresh")`를 시도해 AccessToken을 자동 재발급하고, 성공 시 `setAuthToken`으로 세션을 세팅한다.
   - 실패 시에는 기존처럼 로그인 화면으로 유지.
2. **로그인 흐름**
   - 로그인 요청은 이전과 동일하게 AccessToken만 응답에서 받아서 setAuthToken 한다 (RefreshToken은 쿠키로 자동 처리).
3. **로그아웃**
   - `POST /auth/logout` 호출 후 `clearAuthToken()`와 Query 캐시 초기화.

### Non-functional
- 쿠키는 HttpOnly로 JS 접근을 막고, HTTPS 환경에서만 Secure=true로 동작하도록 profile 별 설정을 제공한다.
- RefreshToken rotation/블랙리스트는 기존 RefreshTokenStore 로직을 재사용한다.

## 3. API Design (Draft)
- `POST /api/v1/auth/login`: Body 동일, Response Body에서 RefreshToken 제거, 대신 Header `Set-Cookie: refreshToken=...`.
- `POST /api/v1/auth/refresh`: Body 불필요. 성공 시 AccessToken/RefreshToken(쿠키) 재발급.
- `POST /api/v1/auth/logout`: Body에 refreshToken 있을 시 기존 로직 유지하되, 쿠키 무효화 추가.

## 4. Domain Model (Draft)
- **RefreshCookieProperties**
  - `domain`, `httpOnly`, `secure`, `sameSite`, `maxAgeSeconds`.
- **RefreshCookieService**
  - `writeRefreshCookie(response, token, expiresAt)`
  - `clearRefreshCookie(response)`
- **RefreshGuardResult**
  - Backend: `token`, `memberId`, `role`.
  - Frontend: `status`, `needsRefresh`.

## 5. Test Plan
1. **Backend Unit/Integration**
   - `AuthControllerTest`: 로그인 응답에 `Set-Cookie`가 포함되는지 확인.
   - Refresh endpoint 테스트: 쿠키 기반 요청으로 AccessToken/쿠키 재발급, 없는 경우 401.
   - 로그아웃 시 쿠키 만료 여부 확인.
2. **Frontend Tests**
   - React Testing Library로 SessionProvider 초기화 시 `/auth/refresh` mock 성공/실패를 시뮬레이션해 status 변화 확인.
   - Sign-in flow는 기존과 동일하게 AccessToken만 받아 setAuthToken이 호출되는지 확인.
3. **Manual Verification**
   - 실제 브라우저에서 로그인 후 새로고침해도 대시보드가 유지되는지 확인.
   - 브라우저 개발자 도구에서 쿠키가 HttpOnly로 설정돼 있는지 확인.
