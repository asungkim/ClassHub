# Feature: Teacher Assistant Management UI

## 1. Problem Definition
- 선생님이 조교 초대 없이 이메일 기반으로 바로 연결해야 하는 Season2 플로우를 UI에서 지원하지 못한다.
- 대시보드에서 조교 목록을 확인하고 활성/비활성 토글은 있으나, 검색/등록 모달과 API 연동이 없어 업무가 막힌다.
- 선생님 역할은 빠르게 조교 명단을 확인하고 필요한 인원을 즉시 연결/차단할 수 있는 직관적 UI를 요구한다.

## 2. User Flows & Use Cases
1. **조교 관리 진입**
   - 사이드바 → “조교 관리” 클릭 → 조교 목록 페이지 렌더.
2. **조교 검색 및 등록**
   - 상단 우측 “조교 검색 및 등록” 버튼 → 모달 오픈.
   - 모달 입력창에 이메일 일부 입력 → `GET /api/v1/teachers/me/assistants/search?email=` 호출.
   - 검색 결과 선택 후 “등록” 버튼 → `POST /api/v1/teachers/me/assistants` 호출.
   - 성공 시 모달 닫고 목록 재조회.
3. **조교 목록 확인**
   - 초기/등록 후 → `GET /api/v1/teachers/me/assistants` 로 페이지네이션 목록 로드.
4. **활성/비활성 토글**
   - 각 카드/행에서 스위치 토글 → `PATCH /api/v1/teachers/me/assistants/{assignmentId}` Body `{ enabled: boolean }`.
   - 상태 변경 후 UI 즉시 반영(Optimistic update) + 실패 시 롤백.

## 3. Page & Layout Structure
- **메인 컨테이너**
  - 헤더: 제목 + “조교 검색 및 등록” 버튼.
  - 필터 바: 상태 필터(전체/활성/비활성), 검색어 표시(선택사항).
  - 목록 영역: Card/Grid 또는 Table 기반, 각 조교 정보 + 토글 + 연결 일시.
  - 빈 상태/로딩/에러 섹션.
- **모달 구조**
  - 제목/설명.
  - 이메일 입력 필드 + 실시간 검색 결과 리스트.
  - 결과 선택 상태 표시 + 등록 버튼.
  - 취소 버튼.
- **반응형**
  - >=1024px: 3열 카드 혹은 테이블.
  - <768px: 1열 카드, 모달 전폭 90%.

## 4. Component Breakdown
- `AssistantManagementPage`
  - 상태: assignments, statusFilter, pagination, loading/error flags.
  - 효과: 초기/필터 변경 시 목록 fetch.
- `AssistantList`
  - Props: assignments, onToggle, isLoading.
  - 테이블 혹은 카드 리스트 구현, 각 행에서 `AssistantToggle`.
- `AssistantToggle`
  - Props: assignmentId, enabled, onChange.
  - UI: Switch 컴포넌트 재사용.
- `AssistantSearchModal`
  - State: query, results, selectedAssistant, fetch status.
  - 이벤트: 입력 debounced fetch, 항목 선택, 등록 버튼.
  - Props: isOpen, onClose, onSuccess (목록 재조회).
- `AssistantSearchResultItem`
  - 표시: 이름/이메일/상태 Badge (NOT_ASSIGNED/ACTIVE/INACTIVE).
- 공통: Alert/Toast API 에러 표시(기존 hook 재사용).

## 5. State & Data Flow
- **전역 상태**: 없음. 페이지 단위 로컬 상태 + React Query/TanStack Query(사용 중이라면) 또는 커스텀 fetch hook.
- **API 계약**
  - Search: `GET /api/v1/teachers/me/assistants/search?email=` → `AssistantSearchResponse[]`.
  - Assign: `POST /api/v1/teachers/me/assistants` Body `{ assistantMemberId }`.
  - List: `GET /api/v1/teachers/me/assistants?status=&page=&size=` → `PageResponse<AssistantAssignmentResponse>`.
  - Toggle: `PATCH /api/v1/teachers/me/assistants/{assignmentId}` Body `{ enabled }`.
- **상태 전파**
  - 목록 상태는 page component가 보유, 검색 모달은 props 콜백으로 성공 시 목록 refetch 요청.
  - 토글 성공 시 해당 assignment만 갱신(optimistic) 또는 전체 refetch.
  - 오류 시 toast + 이전 상태 복구.

## 6. Interaction & UX Details
- 버튼/토글 모두 로딩 중 disabled 처리.
- 검색 입력은 300ms debounce, 최소 2글자 입력 시 호출.
- 검색 결과 없는 경우 “일치하는 조교가 없습니다” 메시지.
- 이미 연결된 조교를 선택하면 상태 Badge로 ACTIVE/INACTIVE 표시, 등록 버튼은 비활성화.
- API 실패 시 오류 메시지를 모달/토스트로 표시하고 focus 유지.
- 키보드: 모달 내 ESC 닫기, Enter로 등록, 검색 목록 화살표 이동 지원(선택).
- 접근성: 모달에 aria-labelledby/aria-modal 적용.

## 7. Test & Verification Plan
- **타입 검증**: `cd frontend && npm run build -- --webpack`.
- **수동 QA 시나리오**
  1. 로그인 → 조교 관리 → 목록 로딩 상태/빈 상태 확인.
  2. 검색 모달 띄우기 → 이메일 입력 → 결과 표시 → NOT_ASSIGNED 항목 등록 → 목록 반영 확인.
  3. ACTIVE 상태 조교 토글 OFF/ON → API 성공/실패 케이스 각각 확인.
  4. 검색 시 이미 연결된 조교 선택 → 등록 버튼 비활성 확인.
  5. 모바일 뷰 width <768px 에서 모달/목록 레이아웃 확인.
- **에러 케이스**
  - 서버 500/네트워크 에러: toast + 상태 복구.
  - API 404/409: 메시지 표준화.
- **향후 자동화**
  - React Testing Library로 모달 인터랙션 스냅샷.
  - Playwright로 전체 플로우 E2E (대시보드 → 검색 → 등록 → 토글).

---

### 계획 요약 (한국어)
- 대시보드 “조교 관리” 화면에 검색/등록 모달과 목록 토글을 순차적으로 구현해 선생님이 이미 가입한 조교를 이메일로 찾아 바로 연결하고, 목록에서 상태를 즉시 변경할 수 있도록 설계했습니다. \
  페이지 레이아웃(헤더/버튼/목록/모달), 컴포넌트 역할, 상태/데이터 흐름, UX 디테일, 검증 절차(타입·수동 QA)를 세분화해 구현 전에 검토 가능한 기준을 마련했습니다.
