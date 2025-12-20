# Feature: Course 다역할 뷰 (Admin / Assistant / Student)

## 1. Problem Definition
- Admin/Assistant/Student가 사용할 Course 전용 화면이 없어, 새로 추가된 역할별 API(`GET /api/v1/admin/courses`, `DELETE /api/v1/admin/courses/{courseId}`, `GET /api/v1/assistants/me/courses`, `GET /api/v1/courses/public`)를 소비할 수 없다.
- SuperAdmin은 대시보드에서 전체 반을 검색하고 필요 시 하드 삭제를 수행해야 하며, 필터 옵션이 많아 관리 UI 구조가 필요하다.
- Assistant는 활성 TeacherAssistantAssignment에 연결된 선생님의 Course를 쉽게 조회해야 하며, 선생님 선택/상태 필터/검색 기능이 필요하다.
- Student는 대시보드 ‘반 검색’ 메뉴에서 공개 Course를 검색하고, 별도 ‘내 수업’ 페이지에서 신청/승인 상태를 확인할 수 있어야 한다(현재는 버튼/플레이스홀더만 노출).

## 2. User Flows & Use Cases
- **SuperAdmin**
  1. 대시보드 사이드바 → “반 관리” 메뉴 진입.
  2. 상단 툴바형 필터에서 Teacher/Company/Branch/Status/Keyword 선택 → `GET /api/v1/admin/courses`.
  3. 테이블에서 Course 행 확인, 필요 시 행 우측 “삭제” 버튼 → 확인 모달 → `DELETE /api/v1/admin/courses/{courseId}` 호출.
- **Assistant**
  1. 대시보드 사이드바 → “반 목록” 메뉴 진입.
  2. 상단에서 연결된 Teacher 드롭다운(활성 Assignment만)과 Status/Keyword 필터 선택 → `GET /api/v1/assistants/me/courses`.
  3. 리스트 카드/테이블에서 Course 정보 확인(Teacher 이름 포함). 추후 상세 모달 전환 준비.
- **Student**
  1. 학생 대시보드 사이드바 → “반 검색”.
  2. 상단 툴바에서 Company/Branch/Teacher 선택 또는 검색어 입력 → `GET /api/v1/courses/public`.
  3. 결과 카드에서 companyName+branchName, Course 이름, Teacher 이름, scheduleSummary, description 확인 후 “등록 요청” 버튼 클릭(향후 Enrollment API 연결 예정, 현재는 비활성 또는 Toast).
  4. 사이드바 “내 수업” 페이지에서 추후 Enrollment 승인/대기 목록을 확인(현재는 EmptyState 안내 및 ‘반 검색’ 이동 버튼만 제공).

## 3. Page & Layout Structure
- **공통 레이아웃**
  - 상단 헤더 + 좌측 대시보드 사이드바 유지.
  - 본문 상단에 역할별 제목과 설명.
- **SuperAdmin Course Management**
  - `Toolbar`: Teacher Select, Company Select, Branch Select, Status Segmented Control(전체/활성/비활성), Keyword Search + 검색 / 초기화 버튼.
  - `Table`: columns = Course명, 회사/지점, Teacher, 기간, 상태, 액션(삭제). 하단 pagination.
  - `Delete Modal`: Course 이름/지점 표시 + “완전 삭제” 확인 체크박스.
- **Assistant Course Overview**
  - `Toolbar`: Teacher Select(Assignment 기반), Status Toggle(전체/활성/비활성), Keyword Input.
  - `List`: 카드 또는 간단 테이블. 각 카드에 Course 이름, Teacher 이름, 회사/지점, 기간, 스케줄 요약. 추후 상세 버튼 placeholder.
- **Student Course Search (반 검색)**
  - `Toolbar`: Company Select, Branch Select(Company 선택 시만 활성), Teacher Select, Keyword Search, onlyVerified 스위치(기본 true).
  - `Card Grid`: 각 카드에 company+branch, Course 이름, Teacher 이름, scheduleSummary, description, “등록 요청” 버튼.
  - 빈 상태/에러 상태 컴포넌트 재사용.
- **Student My Courses (내 수업)**
  - 승인/대기 목록이 들어갈 컨테이너 카드 + EmptyState로 구성.
  - 반 검색으로 이어지는 주요 버튼을 제공해 새로운 UI 흐름을 안내.
  - 향후 Enrollment 데이터가 준비되면 리스트/탭 형태로 확장 예정.

## 4. Component Breakdown
- `CourseFilterToolbar` (역할별 variant)
  - Props: `filters`, `onChange`, `role`.
  - Admin variant: Teacher/Company/Branch/Status/Keyword.
  - Assistant variant: Teacher/Status/Keyword.
  - Student variant: Company/Branch/Teacher/Keyword/onlyVerified.
  - 공통적으로 debounce 검색 입력 + reset 버튼 포함.
- `CourseTable` (Admin)
  - Props: `courses`, `loading`, `onDelete`.
  - 반응형: 모바일에서 카드 형태 전환.
- `CourseListCard` (Assistant/Student)
  - Props: `course`, `role`.
  - role에 따라 footer 버튼(Assistant: 상세 보기 placeholder, Student: 등록 요청 버튼).
- `CourseDeleteModal`
  - Props: `course`, `open`, `onConfirm`, `onCancel`.
- 데이터 Fetch 래퍼 Hook
  - `useAdminCoursesQuery`, `useAssistantCoursesQuery`, `usePublicCoursesQuery` (TanStack Query + OpenAPI types).

## 5. State & Data Flow
- **전역 상태**: TanStack Query 캐시를 활용해 역할별 리스트 상태 관리.
- **필터 상태**: 각 페이지에서 `useReducer` 또는 `useState`로 관리, URL query sync (예: `/admin/courses?teacherId=...`).
- **API 계약**
  - Admin: `GET /api/v1/admin/courses` with query params, 응답 `PageResponse<CourseResponse>`.
  - Assistant: `GET /api/v1/assistants/me/courses`, 응답 `PageResponse<CourseWithTeacherResponse>`.
  - Student: `GET /api/v1/courses/public`, 응답 `PageResponse<PublicCourseResponse>`.
  - 삭제: `DELETE /api/v1/admin/courses/{courseId}`.
- **에러/로딩 처리**: 공통 Loader/Empty/Toast 컴포넌트 활용. 삭제 성공/실패 토스트.

## 6. Interaction & UX Details
- 필터 변경 시 즉시 API 호출(debounce 300ms).
- SuperAdmin 삭제: 체크박스 “정말 삭제합니다” 활성화 시에만 확인 버튼 활성.
- Assistant 페이지: Teacher 드롭다운에 Assignment 활성 여부 표시, 비활성 Assignment는 선택 불가(tooltip).
- Student 페이지: 등록 요청 버튼 클릭 시 아직 구현되지 않은 기능 안내 Toast (“다음 업데이트에서 신청이 완료됩니다.”).
- 접근성: 모든 Select/Button은 키보드 포커스 가능, 삭제 모달 ESC로 닫기.

## 7. Test & Verification Plan
- 단위 테스트: 필터 state reducer, schedule summary 형식화 유틸.
- 통합(프론트) 테스트: React Testing Library로
  - Admin: 필터 입력 → fetch 호출 파라미터 검증, 삭제 모달 열림/확인.
  - Assistant: Teacher 선택 → API 호출 teacherId 포함 여부.
  - Student: 카드 렌더링 시 필수 필드 표시 및 “등록 요청” 버튼 클릭 시 안내 Toast 노출.
- 수동 QA: 역할별 페이지 네비게이션, 빈 상태/에러 상태 확인, pagination 작동.
