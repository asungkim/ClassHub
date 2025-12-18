# Feature: Role Dashboard Routing & Guards

## 1. Problem Definition
- `/dashboard/{superadmin,teacher,assistant,student}` 페이지는 생성됐지만, 현재 누구나 URL을 입력하면 접근 가능하고 로그인 후 자동 라우팅도 없다.
- 로그인 성공 시 `member.role`에 맞춰 자동으로 해당 대시보드로 이동시키고, 권한이 맞지 않는 사용자는 접근하지 못하도록 페이지 가드를 적용해야 한다.

## 2. Requirements

### Functional
1. **Role → Route 매핑**
   - `SUPERADMIN → /dashboard/superadmin`
   - `TEACHER → /dashboard/teacher`
   - `ASSISTANT → /dashboard/assistant`
   - `STUDENT → /dashboard/student`
   - enum/레코드 형태로 중앙에서 관리한다.
2. **로그인 후 리다이렉트**
   - 홈 로그인/Teacher 회원가입 성공 시, `member.role`을 확인한 뒤 위 매핑을 이용해 `router.push(roleRoute[role])`.
   - 세션이 이미 존재할 경우 홈/로그인/회원가입 페이지에 접근하면 자동으로 해당 대시보드로 이동.
3. **페이지 가드**
   - 각 `/dashboard/*` 페이지 내부에서 `useSession()`을 호출해 인증 상태를 확인한다.
   - `status === "loading"`이면 로딩 UI를 보여주고, `unauthenticated`면 `/`로 리다이렉트.
   - `member.role`이 해당 페이지의 역할과 다르면 “권한 없음” 메시지 혹은 홈으로 리다이렉트.
4. **Fallback/에러 처리**
   - 세션 에러 시 공통 ErrorState를 보여주고 홈으로 돌아갈 수 있는 CTA 제공.
5. **Context Hook**
   - 라우팅에 반복 사용될 로직을 `useRoleRedirect` 또는 `RedirectGuard` 같은 커스텀 훅/컴포넌트로 추출해 중복을 줄인다.

### Non-functional
- CSR 기반 라우팅만으로도 동작해야 하므로 Next.js App Router의 클라이언트 컴포넌트로 구현한다.
- 미래에 SSR 보호가 필요하면 middleware로 확장 가능하도록 Route 매핑과 Guard 로직을 모듈화한다.
- 잘못된 역할 문자열이 들어온 경우에도 graceful하게 홈으로 되돌린다.

## 3. API Design (Draft)
- 기존 `GET /api/v1/auth/me`만 사용. 추가 API 호출 없음.

## 4. Domain Model (Draft)
- **RoleRouteMap**
  - `{ SUPERADMIN: "/dashboard/superadmin", ... }`
- **RoleGuardResult**
  - `status: "loading" | "redirecting" | "granted" | "unauthorized"`
  - `targetRoute?: string`
- **DashboardPageProps**
  - `requiredRole: MemberRole`

## 5. Test Plan
1. **Hook/Helper Unit Test**
   - `getRouteByRole(role)` 함수가 정확한 경로를 반환/undefined 처리하는지 Jest로 테스트.
2. **React Testing Library Page Guard Test**
   - Dashboard 페이지에서 SessionProvider를 mock하여 role mismatch 시 redirect/메시지가 뜨는지 검증.
3. **Manual Verification**
   - 실제 로그인 후 role별 CTA를 클릭해 각 `/dashboard/*` 페이지로 이동하는지 확인.
   - URL을 직접 입력해 다른 역할 페이지에 접근하려고 할 때 홈으로 돌아가는지 확인.
