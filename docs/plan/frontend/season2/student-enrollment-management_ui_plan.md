# Feature: Student Enrollment Management UI

## 1. Problem Definition
- 관리자, 선생님/조교, 학생이 각각 자신에게 필요한 Enrollment 흐름을 한 화면에서 관리해야 하지만 Season2 프론트에는 해당 UI가 없다.
- 백엔드(`docs/plan/backend/season2/student-enrollment-management_plan.md`, `docs/spec/v1.3.md`)에서 제공하는 API를 기반으로 역할별 페이지를 설계하고, 공통 상태/컴포넌트 구조를 정의해야 한다.
- 목표는 반 등록 신청 → 승인/거절 → 수강생 정보 조회 → 학생 측 수업 목록/신청 내역 시나리오를 완결하는 UI를 제공하는 것.

## 2. User Flows & Use Cases
### 관리자 (SUPER_ADMIN)
1. 대시보드 메뉴에서 **학생 요청 관리** 클릭.
2. 상단 필터(Teacher, Course, Status, 학생 이름)에 값을 지정해 `GET /api/v1/admin/student-enrollment-requests` 호출.
3. 테이블로 전체 신청 내역을 확인하고, 필요한 경우 상세 모달에서 Course/학생 요약을 점검한다(읽기 전용).

### 선생님/조교
1. 대시보드 메뉴 **학생 관리** 클릭 시 서브 탭 `학생 목록`, `신청 처리`가 나타난다.
2. `학생 목록` 탭
   - 검색/필터(코스, 활성/비활성, 키워드)를 지정해 `GET /api/v1/student-courses` 호출.
   - 행을 클릭하면 상세 모달(`GET /api/v1/student-courses/{recordId}`)에서 StudentInfo + StudentCourseRecord를 확인.
   - Teacher는 추후 수정 모달(`PATCH /api/v1/student-courses/{recordId}`)로 담당 조교/클리닉 슬롯/노트를 변경할 수 있도록 확장.
3. `신청 처리` 탭
   - `GET /api/v1/student-enrollment-requests?courseId&status&studentName`로 대기 목록을 불러오고, 체크박스로 여러 건을 선택.
   - 선택된 항목에 대해 승인(`PATCH /{id}/approve`) 또는 거절(`PATCH /{id}/reject`) 요청을 각각 전송.
   - 조교는 연결된 Teacher 범위만 조회/처리 가능.

### 학생
1. 대시보드 메뉴 **내 수업** 진입 시 탭 `수업 목록` / `신청 내역`.
2. `수업 목록` 탭: `GET /api/v1/students/me/courses` 결과를 카드/테이블로 노출, 검색창으로 코스 이름 필터링.
3. `신청 내역` 탭: `GET /api/v1/student-enrollment-requests/me?status`로 PENDING/REJECTED/APPROVED 내역을 확인.
   - PENDING 항목은 `PATCH /api/v1/student-enrollment-requests/{id}/cancel` 버튼을 제공.
4. **수업 등록 요청**: 별도 `반 검색` 페이지(기존 Course 공개 검색 UI 활용)에서 Course를 선택하고 `POST /api/v1/student-enrollment-requests` 호출 후 토스트 알림.

## 3. Page & Layout Structure
### 공통
- 대시보드 레이아웃 내 좌측 사이드바에 역할별 메뉴 추가.
- 상단 헤더에 역할 배지와 빠른 필터 Drawer 진입 버튼 배치.

### 관리자 – 학생 요청 관리
1. **Filter Toolbar**: Teacher 선택 셀렉트, Course 셀렉트, Status 멀티선택, 학생 이름 검색, 새로고침 버튼.
2. **Result Table**: 컬럼(신청일, 학생, Course, 상태, 메시지) / 행 클릭 시 Detail Drawer.
3. **Detail Drawer**: Course 정보, 학생 프로필, 신청 메시지, 처리 결과(승인자/시간).

### 선생님/조교 – 학생 관리
1. 상단 탭(Tabs component) → `학생 목록`, `신청 처리`.
2. **학생 목록 탭**
   - Filter Row: Course 선택, 상태 토글(활성/비활성/전체), 검색어 인풋, 페이지네이션.
   - Table: 학생 이름, 연락처, 학교/학년, 반 이름, 담당 조교 배지, 상태.
   - 행 클릭 시 Detail Modal: StudentInfo, Course 정보, Teacher Notes, ClinicSlot 등 표시하며 **해당 모달과 수정 버튼은 Teacher만 접근 가능**(Assistant는 읽기 전용 목록).
3. **신청 처리 탭**
   - Filter: Course, Status, 검색어.
   - Table + Checkbox: 동일 신청 리스트.
   - Bulk Action Bar: “선택 승인”, “선택 거절”, 개별 승인/거절 버튼.

### 학생 – 내 수업
1. Tabs: `수업 목록`, `신청 내역`.
2. `수업 목록`: 카드형(코스 명, 기간, 지점, 담당 선생님, 상태). 검색창 + 빈 상태 안내.
3. `신청 내역`: 리스트 + 상태 배지. PENDING 항목에는 취소 버튼, 기타 상태는 설명 텍스트.
4. `반 검색` 페이지: Course 공개 검색 UI에서 원하는 반을 선택하고 “등록 요청” 버튼을 누르면 해당 Course에 신청 여부를 한 번 더 확인하는 Confirm 모달을 띄운 뒤, 성공 시 “신청이 완료되었습니다” 토스트와 함께 `신청 내역` 탭으로 이동.

## 4. Component Breakdown
- `EnrollmentFilterToolbar`: 역할별 필터 구성을 props로 받아 렌더링. 필드 값이 변경될 때 쿼리 파라미터 업데이트.
- `EnrollmentTable`: 테이블/리스트 공통 UI. 체크박스/RowAction 슬롯 지원.
- `StudentDetailModal`: StudentCourseDetailResponse 기반 상세 정보 표시 + Teacher 전용 수정 Form.
- `RequestDetailDrawer`: Teacher/관리자 공용 요청 상세 Drawer.
- `StatusBadge`: EnrollmentStatus → 색상 매핑.
- `BulkActionBar`: 선택된 행 수, 승인/거절 버튼, 비활성화 조건 관리.

## 5. State & Data Flow
- **전역 상태**: React Query(SWR) 기반. 필터 값은 URL 쿼리 문자열로 유지해 새로고침/공유를 용이하게 함.
- **API 계약**
  - Admin: `GET /api/v1/admin/student-enrollment-requests?teacherId&courseId&status&studentName&page&size`
  - Teacher/Assistant Student List: `GET /api/v1/student-courses?courseId&status&keyword&page&size`
  - Student Detail: `GET /api/v1/student-courses/{recordId}`
  - Student Update(Teacher only): `PATCH /api/v1/student-courses/{recordId}`
  - Request List Teacher/Assistant: `GET /api/v1/student-enrollment-requests?courseId&status&studentName&page&size`
  - Approve/Reject: `PATCH /api/v1/student-enrollment-requests/{id}/approve|reject`
  - Student My Courses: `GET /api/v1/students/me/courses`
  - Student Requests: `GET /api/v1/student-enrollment-requests/me?status&page&size`
  - Student Cancel: `PATCH /api/v1/student-enrollment-requests/{id}/cancel`
  - Student Create: `POST /api/v1/student-enrollment-requests`
- 에러 처리: `RsCode`에 따른 메시지 매핑(권한 부족, 상태 오류, 중복 신청 등) → Toast.
- Optimistic Update: 승인/거절/취소는 성공 시 invalidate + 토스트, 실패 시 오류 메시지.

## 6. Interaction & UX Details
- 필터 변경 시 로딩 스피너 + Skeleton.
- Bulk 승인 시 확인 모달(선택 N건). 거절은 사유 입력 optional.
- 학생 상세 모달에서 Teacher Notes 수정 버튼 클릭 시 inline form 전환(추후 구현).
- 학생 내 수업에서 빈 상태 시 “반 검색으로 이동” CTA.
- 접근성: 체크박스/버튼에 키보드 네비게이션 지원, 상태 배지에 `aria-label`.
- 국제화: 모든 날짜/시간은 `dayjs` 한국어 포맷 사용.

## 7. Test & Verification Plan
1. **단위 테스트**
   - 필터 컴포넌트 상태 변경 → query string 업데이트.
   - Bulk Action 바: 선택 없음 시 버튼 disabled.
2. **통합 테스트 (React Testing Library)**
   - Teacher 학생 목록 화면이 API 응답을 렌더링하고 필터 적용 시 refetch 호출.
   - 학생 신청 내역 탭에서 취소 버튼 클릭 → confirm → API 호출 모킹 확인.
3. **수동 QA 체크리스트**
   - 역할별 메뉴 권한 확인(SUPER_ADMIN, TEACHER, ASSISTANT, STUDENT).
   - 승인/거절/취소 실패 시 에러 토스트 노출.
   - 쿼리 파라미터 공유 후 새로고침 시 동일 상태 유지.
   - 모바일 뷰에서 테이블이 카드 리스트로 전환.

## 8. 구현 단계 제안 (프론트)
1. **관리자 학생 요청 관리 페이지** – 필터 + 테이블 + 상세 Drawer.
2. **선생님/조교 학생 관리** – 학생 목록/신청 처리 탭, 승인/거절 UX.
3. **학생 내 수업 & 신청 내역 + 반 검색 연동** – 기존 Course 검색과 연결, 신청/취소 UX.
