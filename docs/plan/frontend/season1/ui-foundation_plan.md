# Feature: Frontend UI Foundation (Layout · Session · Error Handling)

## 1. Problem Definition
- Next.js 루트 레이아웃은 NavigationBar/Footer 적용까지만 되어 있어, 페이지마다 다시 작성할 필요가 있는 상태 관리/시각적 가이드를 제공하지 못한다.
- 인증된 사용자 정보를 저장/배포할 `SessionProvider`가 없어서 토큰 주입·재검증·세션 만료 처리 방식을 통일할 수 없다.
- API 실패/세션 만료/빈 데이터 등 공통 에러 상황을 사용자에게 어떻게 보여줄지 정의되어 있지 않아 화면마다 일관성이 깨질 위험이 있다.

## 2. Requirements

### Functional
1. **공통 레이아웃 확장**
   - `RootLayout`에서 NavigationBar/Footer/배경 외에 Notification 영역과 전역 모달/토스트 포털을 노출할 수 있는 레이어를 추가한다.
   - 페이지 기본 패딩, 섹션 폭, 폰트 설정을 `layout.tsx`와 `globals.css` 수준에서 규정한다.
2. **Session Provider & Hook**
   - `SessionProvider`가 React Query를 활용해 `GET /api/v1/auth/me`를 호출하고, 성공 시 Member/roles/토큰 메타를 Context로 제공한다.
   - `useSession()` 훅은 `status: "authenticated" | "unauthenticated" | "loading"` 과 `member`, `refresh()` 등을 반환한다.
   - `.env.local`에 설정한 `NEXT_PUBLIC_MOCK_TOKEN`이 있을 때는 기본 Authorization 헤더로 주입해 수동 테스트가 가능해야 한다.
3. **에러 처리 컴포넌트**
   - `ErrorState`, `InlineError`, `RetryButton`, `EmptyState` 등 최소 2종 이상의 재사용 가능한 컴포넌트를 `src/components/ui/`에 추가한다.
   - React Query `QueryClient`에 global `onError` 핸들러를 두어 인증 오류(401/419) 시 세션을 초기화하고 ErrorState를 노출한다.
   - Layout 레벨에서 `ErrorBoundary`를 적용해 예기치 못한 렌더링 오류를 잡고 ErrorState로 graceful degrade 한다.

### Non-functional
- React Query와 Context는 모두 TypeScript strict mode에서 동작하도록 제네릭을 명시한다.
- 세션/에러 UI는 모바일/태블릿에서 깨지지 않도록 360px 폭에서도 동작해야 한다.
- 토큰/세션 정보는 브라우저 storage에 persist하지 않고, Provider 메모리에만 존재하도록 하여 보안 리스크를 줄인다.

## 3. API Design (Draft)
- `GET /api/v1/auth/me`
  - **Headers**: `Authorization: Bearer <access-token>`
  - **Response 200**
    ```json
    {
      "code": 1000,
      "data": {
        "id": "UUID",
        "name": "홍길동",
        "email": "teacher@classhub.dev",
        "role": "TEACHER"
      }
    }
    ```
  - **Response 401/419**: `RsData` 코드 `2001` 등 → Provider가 세션을 초기화하고 로그인 페이지로 이동.
- `POST /api/v1/auth/logout`
  - 세션 컨텍스트에서 사용자가 명시적으로 로그아웃할 때 호출 (추후 구현).

## 4. Domain Model (Draft)
- **Session**
  - `status`: loading/authenticated/unauthenticated
  - `member`: `MemberSummary` (id, name, email, role, permissions?)
  - `tokenMeta`: 현재 access token, 만료 시각(옵션)
- **ErrorState**
  - `type`: `network`, `authorization`, `empty`, `unknown`
  - `title`, `description`, `ctaLabel`, `onRetry`
- **LayoutState**
  - 전역 토스트/알림 큐 (React Context 혹은 simple store)

## 5. Test Plan
1. **SessionProvider Hook Test**
   - React Testing Library로 `SessionProvider` + mocked API 클라이언트를 이용해 `status` 변화를 검증한다.
   - 401 응답 시 `status`가 `unauthenticated`로 바뀌고 `member`가 `null`이 되는지 확인한다.
2. **ErrorState Component Snapshot**
   - `ErrorState`/`InlineError` 렌더링을 스냅샷 테스트로 커버해 props 조합별 UI가 깨지지 않는지 확인한다.
3. **Integration Smoke**
   - `app/page.tsx`에서 `useSession` 사용 시 로딩 → 성공 → 에러 흐름을 Mock Service Worker(MSW) 또는 jest.mock으로 시뮬레이션한다.
4. **Manual Verification**
   - `NEXT_PUBLIC_MOCK_TOKEN` 미설정 상태에서 `/` 접근 시 ErrorState + 로그인 CTA 노출.
   - 토큰 세팅 후 `/components` 페이지에서 NavigationBar의 사용자 정보/CTA 상태가 업데이트되는지 확인.
