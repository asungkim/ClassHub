# Feature: 대시보드 UI 개선 & 로그아웃 (Frontend)

## 1. Problem Definition

- 역할별 대시보드에서 홈/문서용 상단 컴포넌트가 그대로 노출되어 화면을 차지하고, 좌측에서 열리는 내비게이션/로그아웃 UI가 없다.
- 선생님·조교·학생 모두 동일한 레이아웃으로 좌측 네비게이션과 하단 로그아웃 버튼을 갖춘 대시보드를 원하지만, 현재는 버튼/레이아웃 구조가 준비되어 있지 않다.
- Footer에 불필요한 링크/내용이 많아 대시보드 화면을 단순하고 깔끔하게 유지하기 어렵다.

## 2. Requirements

### Functional

- **대시보드 공통 레이아웃**: `/dashboard/{teacher,assistant,student}` 페이지에서 공통 Shell을 사용해 좌측에서 열리는 사이드바(이름/역할/메뉴)를 제공하고, 대시보드 콘텐츠는 메인 영역에 카드·목록 형태로 배치한다. 데스크톱에서는 기본 표시, 모바일에서는 토글 버튼으로 오픈/닫기.
- **상단 홈 컴포넌트 제거**: 기존 홈/Hero/문서 안내 영역을 대시보드에서는 숨기고, 필요한 경우 간결한 헤더(제목/날짜 정도)만 남긴다. “대시보드 열기” 같은 중복 CTA는 제거한다.
- **사이드바 메뉴**: 대시보드가 기본 선택된 상태로 표시되고, 추후 확장 가능한 메뉴 슬롯을 남겨둔다(필수 라우트만 활성화). 메뉴는 `NavigationBar`/`Button`/`Card` 등 기존 UI 컴포넌트를 조합해 구현한다.
- **로그아웃 버튼**: 사이드바 하단에 `POST /api/v1/auth/logout`을 호출하는 버튼을 배치한다. `useSession().logout`을 통해 Refresh 쿠키를 포함해 서버에 로그아웃 요청을 보내고, 응답과 무관하게 액세스 토큰을 비우고 홈(`/`)으로 리다이렉트한다.
- **대시보드 콘텐츠 구조**: 상단 요약 카드(예: 인원/수업/알림/클리닉), 일정 리스트(오늘/이번 주), 진행률 박스 등 목록형 섹션을 배치해 두 번째 스크린샷처럼 한눈에 정보를 보이도록 구성한다. 아직 API가 없는 데이터는 임시 스켈레톤/빈 상태를 사용하고, 실제 API 연동 시 동일 섹션을 교체할 수 있게 컴포넌트화한다.
- **Footer 축소**: 대시보드에서는 Footer를 간단한 저작권/브랜드 정도로만 노출하고, 불필요한 링크/텍스트는 제거한다. 공통 Footer 컴포넌트에서 props/slot을 활용하거나 최소 변형으로 해결한다.

### Non-functional

- 기존 UI 컴포넌트(`frontend/src/components/ui/NavigationBar`, `Button`, `Card`, `Footer` 등)를 우선 조합·확장하고, 새 베이스 컴포넌트 추가는 피한다.
- 역할별 라우트는 `getDashboardRoute`/`Route` 타입을 그대로 사용하며, 링크/버튼 경로는 `isInternalRoute` 규칙을 따른다.
- API 타입은 `components["schemas"]["LogoutRequest"]`를 alias로 사용하고, `api.POST` 호출 시 `/api/v1/auth/logout` 전체 경로를 넘긴다.
- 반응형 레이아웃(모바일 슬라이드/오버레이, 데스크톱 고정 폭)과 접근성(포커스 이동/ARIA label)을 고려한다.
- 구현 후 `cd frontend && npm run build -- --webpack`으로 타입/빌드를 검증하고, 수동 확인 경로를 기록한다.

## 3. API Design (Draft)

| Method | URL                   | Request                                                                 | Response     | Notes                                                   |
| ------ | --------------------- | ----------------------------------------------------------------------- | ------------ | ------------------------------------------------------- |
| POST   | `/api/v1/auth/logout` | `LogoutRequest` (`refreshToken?`, `logoutAll?`; Refresh 쿠키 자동 포함) | `RsDataVoid` | 멱등 호출. 프런트에서는 `logoutAll` 기본 `false`로 호출 |

## 4. Domain Model (Draft)

- **DashboardShell**: `sidebarOpen` 상태를 관리하며 `NavigationBar`(모바일 토글), `Sidebar`, `Content` 슬롯을 묶는 레이아웃. `useSession`으로 사용자 이름/역할을 표시.
- **SidebarNavItem**: `{ label, icon, href?: Route }` 형태의 데이터로 메뉴를 구성. 아직 없는 기능은 `href` 생략/비활성 처리.
- **LogoutAction**: `useSession().logout` + `router.replace("/")`를 묶은 핸들러. 요청 실패 시에도 토큰/쿼리 리셋.
- **DashboardSections**: 요약 카드, 일정 리스트, 진행률 박스 등 섹션 컴포넌트 모음. 데이터가 없을 때는 스켈레톤/빈 상태를 렌더링하도록 분리.

## 5. TDD Plan

1. **타입/빌드 검증**: `cd frontend && npm run build -- --webpack`으로 Route/API 타입 위반 여부 확인.
2. **수동 시나리오 (역할 공통)**
   - 로그인 상태로 `/dashboard/{role}` 진입 → 좌측 사이드바가 기본/토글로 노출되는지 확인.
   - 사이드바 로그아웃 클릭 → `/api/v1/auth/logout` 호출, 세션 리셋 후 `/` 리다이렉트 확인.
   - 모바일 뷰(DevTools)에서 토글로 사이드바 열림/닫힘, 포커스 트랩/스크롤 제어 확인.
   - Footer가 축소 버전으로만 노출되는지, 상단 홈/Hero 섹션이 숨겨졌는지 확인.
3. **오류/비로그인 흐름**: 세션 만료 상태에서 대시보드 접근 시 가드로 홈 리다이렉트/에러 메시지 노출을 확인.
