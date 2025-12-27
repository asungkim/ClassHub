# Feature: Security Hardening (Auth & Token)

## 1. Problem Definition
- 현재 Refresh 토큰 검증이 **토큰 타입 구분 없이** `isValidToken`만 통과하면 성공한다. 이 구조는 **Access 토큰을 Refresh로 오용**할 수 있는 위험이 있다.
- Refresh 토큰이 서버에 **저장/추적되지 않기 때문에** 탈취 시 만료 전까지 재발급이 가능하고, `logoutAll`이 실질적으로 동작하지 않는다.
- Refresh 토큰 블랙리스트가 In-Memory라 서버 재시작 시 무효화 정보가 사라진다.
- Refresh 시 **회원 비활성화(삭제) 체크가 없다**.
- 프런트는 401 재시도에서 **request body 재사용 문제**가 있고, `forceLogout`이 서버 로그아웃을 호출하지 않는다.

## 2. Requirements

### Functional
1. **토큰 타입 분리**
   - Access/Refresh 토큰에 `tokenType` 클레임을 추가한다.
   - Refresh API는 `tokenType=REFRESH`만 허용하고, Access 토큰은 거부한다.
   - JWT 인증 필터는 `tokenType=ACCESS`만 인증으로 사용한다.
2. **Refresh 토큰 저장/회수**
   - Refresh 토큰에 `jti`(토큰 식별자)를 추가하고 서버에 저장한다.
   - Refresh 시 **토큰 회전(rotation)**: 기존 Refresh 토큰 폐기 + 새로운 Refresh 토큰 발급.
   - 재사용된 Refresh 토큰은 거부하고 보안 이벤트로 기록한다.
3. **로그아웃/전체 로그아웃**
   - `logout`은 해당 `jti`를 폐기한다.
   - `logoutAll`은 해당 사용자 소유 Refresh 토큰 모두 폐기한다.
4. **회원 상태 체크**
   - Refresh 시 `member.isDeleted()`이면 `MEMBER_INACTIVE`로 차단한다.
5. **쿠키/보안 설정**
   - Refresh 쿠키는 `HttpOnly`, `Secure`, `SameSite` 설정을 환경별로 명확히 관리한다.
   - 운영 환경에서는 `Secure=true`, 도메인/경로를 명시한다.
6. **프런트 연동 개선**
   - `forceLogout`는 백엔드 로그아웃을 best-effort로 호출한다.
   - 401 재시도 시 `request.body` 재사용 문제를 해결한다.
   - Refresh 재시도 횟수 제한을 둔다.
   - Mock token은 dev 환경에서만 사용한다.

### Non-functional
- Refresh 토큰 저장소는 Redis 우선(이미 인프라 스펙에 포함). 개발 환경은 In-Memory로 대체 가능.
- 토큰 회전 후 기존 Refresh 토큰 재사용 탐지 시 로그 기록(보안 감사).
- 모든 변경은 RsData/RsCode 규칙을 유지한다.

## 3. API Design (Draft)
- 기존 API 유지
  - `POST /api/v1/auth/refresh`
  - `POST /api/v1/auth/logout`
- 동작 변경(스펙 변경 없이 내부 로직 개선)
  - Refresh: `tokenType=REFRESH` 검증 + rotation 적용
  - Logout: `jti` 기반 폐기
  - LogoutAll: 사용자 소유 jti 전체 폐기

## 4. Domain Model (Draft)

### JWT
- Access 토큰 클레임: `{ id, role, tokenType=ACCESS }`
- Refresh 토큰 클레임: `{ id, tokenType=REFRESH, jti }`

### RefreshTokenStore (Redis 기반 권장)
- `save(jti, memberId, expiresAt)`
- `isRevoked(jti)` / `revoke(jti, expiresAt)`
- `revokeAll(memberId)`
- 저장 구조 예시
  - `refresh:{jti}` → memberId (TTL = 만료 시각)
  - `refresh:member:{memberId}` → set(jti) (TTL 동기화)

### AuthService 변경
- `refresh(token)`에서:
  - `tokenType=REFRESH` 검증
  - `jti` 유효성 확인 및 회전
  - `member.isDeleted()` 체크
- `logoutAll` 구현

### 프런트 변경(연동)
- `forceLogout`는 서버 로그아웃 호출 후 정리
- 401 재시도 시 request clone/버퍼 사용
- refresh 재시도 제한

## 5. TDD Plan
1. **JwtProvider Test**
   - Access/Refresh 생성 시 `tokenType` 클레임 확인
   - Refresh 토큰에 `jti` 포함 확인
2. **AuthService Test**
   - Refresh가 `tokenType=REFRESH`만 허용
   - 비활성화 회원 refresh 차단
   - rotation 시 기존 jti 재사용 차단
3. **RefreshTokenStore Test**
   - `save`/`revoke`/`revokeAll` 동작 검증 (Redis/Memory 구현별)
4. **Controller Test**
   - refresh/logout 플로우 통합 검증
5. **프런트 검증**
   - refresh 실패/로그아웃 시 재시도 제한 동작 확인
   - request 재시도 body 유지 확인

---

### 계획 요약 (한국어)
- Access/Refresh 토큰을 `tokenType`으로 구분하고, Refresh 토큰에는 `jti`를 부여해 서버에서 추적/회전한다.
- Refresh/Logout 로직을 강화해 탈취/재사용 위험을 줄이고, 비활성화 계정의 refresh를 차단한다.
- 프런트는 재시도/로그아웃 로직을 보완해 백엔드 정책과 정합성을 맞춘다.
