# Feature: Teacher Course Management UI

## 1. Problem Definition
- 선생님이 “반 관리” 화면에서 Course 목록/캘린더를 확인하고, Course를 생성·수정·비활성화할 수 있는 UI가 필요하다.
- 기존 Season2 UI에는 Course 관리 페이지가 없어 Teacher Branch/Assistant 흐름 이후 작업이 막혀 있다.
- 목표: 목록/캘린더 이중 뷰, Course 생성·수정 모달, 활성/비활성 토글 UX를 Next.js 대시보드 구조 위에 구축한다.

## 2. User Flows & Use Cases
- **목록 뷰 기본 진입**
  1. 선생님이 대시보드 Sidebar → “반 관리” 클릭.
  2. 기본 탭은 `ACTIVE` 상태 Course, 페이지네이션+검색어/지점 필터 제공.
  3. 각 카드/행에서 활성 토글, 수정 버튼, 상세 열람 액션 제공.
- **상태 탭 전환**
  - 탭: ACTIVE / INACTIVE / ALL. 선택 시 API status 파라미터 변경.
- **Course 생성**
  1. 상단 “반 생성” 버튼 클릭 → 모달 오픈.
  2. TeacherBranchAssignment 목록을 `companyName + branchName` 라벨로 select.
  3. 이름(required), 설명(optional), 시작일/종료일, 스케줄(요일+시작/종료 시간) 한 개 이상 추가.
  4. 저장 시 POST `/api/v1/courses`, 성공 후 목록 갱신.
- **Course 수정**
  - 목록 행 or 상세 Drawer에서 “수정” → 동일 모달 재사용 (기존 값 preset), PATCH `/api/v1/courses/{courseId}`.
- **Course 활성/비활성화**
  - 목록 토글 또는 상세 모달에서 enabled on/off → PATCH `/api/v1/courses/{courseId}/status`.
- **캘린더 뷰**
  1. View 전환 스위치 (List / Calendar).
  2. Calendar는 주 단위 (월~일, 06:00~10:00) 타임블록을 grid로 표시, drag/long-press 입력 없이 read-only.
  3. 주 이동 시 `GET /api/v1/courses/schedule?startDate&endDate` 호출.
- **Course 상세 보기**
  - 목록에서 클릭 시 오른쪽 패널(또는 상세 모달)로 Course 정보+스케줄 표.

## 3. Page & Layout Structure
- **헤더 영역**
  - 페이지 타이틀 + view 전환 토글(List/Calendar) + 상태 탭 + “반 생성” 버튼.
- **필터 영역 (목록 뷰)**
  - Branch 드롭다운 (TeacherBranchAssignment 목록 재사용)
  - 검색 인풋 (name keyword)
- **목록 영역**
  - 테이블 화면: Course 이름, 지점, 기간, 스케줄 간략 표시, 활성 토글, 수정 버튼.
  - Pagination Footer (page/size).
- **캘린더 영역**
  - 주간 grid (열=요일, 행=1시간 슬롯). 각 Course는 색상 카드로 표시, hover 시 branch/company/시간 정보.
  - 상단에 주간 네비게이션(이전/다음, 오늘) 및 현재 범위 표시.
- **모달**
  - 생성/수정 공통 모달: stepless form + 스케줄 리스트(추가/삭제 버튼).
  - 스케줄 입력 Row: 요일 select + start/end time pickers.

## 4. Component Breakdown
- `CourseManagementPage`
  - 역할: 뷰 상태(필터, 탭, view 모드) 관리, 데이터 fetch, 하위 컴포넌트 조합.
  - 상태: `viewMode`, `statusTab`, `branchFilter`, `keyword`, `pagination`, `courses`, `isCreateModalOpen`, `editingCourse`.
- `CourseFilters`
  - Props: branch options, current status/tab, keyword.
  - 이벤트: branch change, keyword change, tab change.
- `CourseListTable`
  - Props: CourseResponse[], pagination info.
  - 이벤트: toggleStatus(courseId, enabled), edit(course).
- `CourseCalendar`
  - Props: weekly course 데이터.
  - 이벤트: 주간 이동(→ start/end 변경).
- `CourseFormModal`
  - Props: mode(create/edit), initialCourse, branchOptions, onSubmit.
  - 내부: 스케줄 배열 관리, 요일 select + time picker, validation 메시지.
- `CourseScheduleList`
  - 재사용 가능한 스케줄 Row 리스트 컴포넌트.
- API hooks: `useTeacherBranches`, `useCourseList`, `useCourseCalendar`, `useCreateCourse`, `useUpdateCourse`, `useToggleCourse`.

## 5. State & Data Flow
- **전역/서버 상태**
  - React Query로 Course 목록/캘린더/TeacherBranchAssignment fetch. key는 `[courses, status, branch, page, keyword]`, `[courses-calendar, start, end]`, `[teacher-branches, status=ACTIVE]`.
  - Mutation 후 `invalidateQueries`로 목록/캘린더 함께 갱신.
- **지역 상태**
  - `viewMode`, `statusTab`, `branchFilter`, `keyword`, `calendarRange`는 로컬 useState.
  - 모달 form은 react-hook-form + Zod schema (openapi 타입 기반) 사용.
- **API 계약**
  - `CourseResponse`: branchName/companyName 포함 (openapi.d.ts 참고).
  - `CourseCreateRequest`/`CourseUpdateRequest`: branchId, name, optional description, startDate/endDate, schedules[].
  - Validation: 스케줄 최소 1개, end > start, 기간 start <= end 클라이언트에서도 사전검증.
- **에러 처리**
  - 400/403/404 등 RsCode 메시지를 토스트로 표기. validation 에러는 form-field 에러로 표시.
- **로딩 전략**
  - 목록/캘린더 각각 skeleton. Mutation 시 버튼 loading state 표시.

## 6. Interaction & UX Details
- 상태 탭 클릭 시 즉시 active tab 스타일, 목록 페치 재요청.
- Branch select나 keyword 입력 → 300ms debounce 후 fetch.
- 목록 토글: optimistic update (UI 즉시 반영) 후 실패 시 롤백+토스트.
- 모달 스케줄 Row 추가: “+ 요일 추가” 버튼, 각 Row 우측 trash 버튼.
- Calendar: 카드 hover 시 Tooltip (Course 이름, 지점, 시간), 클릭 시 상세 패널 open.
- 반 생성 버튼: TeacherBranchAssignment 없으면 disabled + 안내 텍스트.
- 접근성: TabIndex/ARIA label (토글, 모달), keyboard navigation (Esc closes modal).

## 7. Test & Verification Plan
- **단위 테스트**
  - 스케줄 Form 유틸 (시간 비교, 중복 요일) 로직 jest 테스트.
  - API hook mock 테스트 (React Query)로 파라미터 구성 확인.
- **통합 테스트**
  - Cypress 또는 Playwright로 목록 필터 → API Stub → 렌더 확인.
  - 생성 모달 입력 → mock mutation → 목록 갱신 검증.
- **수동 QA 체크리스트**
  1. 상태 탭/branch 필터 전환 시 데이터와 탭 UI 동기화.
  2. Course 생성: 스케줄 1개 이상 입력하지 않으면 Validation 문구.
  3. Course 수정: 기존 스케줄/기간 그대로 노출·수정 가능.
  4. 활성/비활성 토글 시 토스트 및 상태 반영.
  5. Calendar 주간 이동 시 API 요청 파라미터(start/end) 확인, 카드 표시 맞춤.
