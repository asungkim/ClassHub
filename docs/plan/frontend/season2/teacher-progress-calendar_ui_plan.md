# Feature: Teacher Progress Management & Student Calendar UI

## 1. Problem Definition
- 선생님이 수업 직후 반 공통 진도(CourseProgress)와 학생별 개인 진도(PersonalProgress)를 한 번에 작성할 수 있는 UI가 없다.
- 진도 관리 페이지(반별/개인)와 학생별 캘린더 뷰가 없어, 누적된 학습 기록을 빠르게 조회·관리하기 어렵다.
- 조회 기능은 조교도 사용하므로, 목록/필터 UI는 **공통 컴포넌트로 분리**해 Teacher/Assistant 화면에서 재사용할 수 있어야 한다.
- 목표: 대시보드 흐름에 통합 수업 작성 모달 + 진도 관리 페이지 + 학생별 캘린더 페이지를 연결해 작성/조회/캘린더 확인까지 한 화면에서 이어지도록 만든다.

## 2. User Flows & Use Cases
- **통합 수업 작성 모달**
  1. `/teacher` 대시보드에서 “수업 작성” 버튼 클릭 → 모달 오픈.
  2. 반 목록에서 단일 반 선택 → CourseProgress 필드(제목, 내용) 입력.
  3. 선택된 반의 학생 목록 표시 → 체크박스로 학생 선택 → 각 학생별 PersonalProgress 입력 필드(제목, 내용) 노출.
  4. “작성 완료” 클릭 → 학생 선택 0명일 때는 CourseProgress 단건 API, 학생 선택이 있으면 compose API로 저장.
- **진도 관리 (반별 진도)**
  1. Sidebar에서 “진도 관리” 클릭 → 아코디언 메뉴로 “반별 진도/개인 진도” 노출.
  2. 반 드롭다운 선택.
  3. 최신순 카드 리스트로 CourseProgress 기록 확인, 필요 시 “더 보기”로 커서 페이지 추가 로드.
- **진도 관리 (개인 진도)**
  1. Sidebar에서 “진도 관리” 클릭 → 아코디언 메뉴로 “반별 진도/개인 진도” 노출.
  2. 학생 드롭다운 선택.
  3. 최신순 카드 리스트로 PersonalProgress 기록 확인, 필요 시 “더 보기”로 커서 페이지 추가 로드.
- **학생별 캘린더**
  1. Sidebar에서 “학생별 캘린더” 클릭.
  2. 학생 자동완성 검색 → 선택 시 학생 정보 카드와 월간 캘린더 로드.
  3. 날짜 클릭 → 해당 날짜 상세 모달(공통/개인/클리닉 기록 목록) 표시.
  4. 캘린더에서는 새 기록 생성이 아닌, 기존 기록의 수정/삭제만 가능하도록 동작.

## 3. Page & Layout Structure
- **Teacher 대시보드(`/teacher`)**
  - 상단 소개 섹션 유지.
  - “Teacher Dashboard” 카드(헤더 박스) 우측 상단에 “수업 작성” 버튼 배치.
  - 클릭 시 통합 수업 작성 모달 표시.
- **진도 관리 루트(`/teacher/progress`)**
  - Sidebar의 “진도 관리” 아코디언으로 반별/개인 진도 라우팅 제공.
  - 실제 콘텐츠는 `/teacher/progress/course`, `/teacher/progress/personal`로 분기.
- **반별 진도 페이지(`/teacher/progress/course`)**
  - 필터 영역: 반 선택 드롭다운.
  - 목록 영역: 카드 리스트 (날짜, 제목, 내용 요약, 작성자) + “더 보기” 버튼.
  - 빈 상태: “선택한 반에 기록이 없습니다” 안내.
- **개인 진도 페이지(`/teacher/progress/personal`)**
  - 필터 영역: 학생 검색 입력 + 학생 선택 드롭다운.
  - 선택된 학생이 여러 반이면 “반 선택” 드롭다운 추가 노출, 반 1개면 자동 선택.
  - 목록 영역: 카드 리스트 (날짜, 제목, 내용 요약, 작성자) + “더 보기” 버튼.
  - 빈 상태: “선택한 학생의 기록이 없습니다” 안내.
- **학생별 캘린더(`/teacher/calendar`)**
  - 헤더: 제목/설명 + 학생 검색(자동완성).
  - 학생 정보 카드: 이름, 연락처, 소속 반 목록.
  - 범례: 공통/개인/클리닉 색상 표시.
  - 캘린더: 월간 그리드 + 이전/다음 월 네비게이션.
  - 날짜 클릭 시 상세 모달 (공통/개인/클리닉 리스트 + 작성 버튼).

## 4. Component Breakdown
- `TeacherLessonComposeModal`
  - **왜 필요한지**: 공통/개인 진도를 한 번에 작성하는 핵심 입력 UI.
  - **어떻게 동작**: 반 선택 시 CourseProgress 입력 영역 활성화, 학생 체크 시 PersonalProgress 입력 카드가 동적으로 추가됨.
  - **어디에 붙는지**: `/teacher` 대시보드 “수업 작성” 버튼에서 호출.
- `CourseProgressFormSection`
  - **왜 필요한지**: 공통 진도 입력 필드 묶음(필수 입력 + 선택 메모)을 구조화.
  - **어떻게 동작**: 날짜 기본값 오늘, 필수 입력 검증(진도 내용/학습 범위/과제).
  - **어디에 붙는지**: `TeacherLessonComposeModal` 상단.
- `PersonalProgressPicker`
  - **왜 필요한지**: 학생 선택과 개인 진도 입력을 연결하기 위한 UI.
  - **어떻게 동작**: 학생 체크박스 리스트 → 선택된 학생마다 입력 카드 렌더.
  - **어디에 붙는지**: `TeacherLessonComposeModal` 하단.
- `ProgressFilterBar`
  - **왜 필요한지**: 반/학생/반 선택 필터를 일관된 UX로 제공하고 Teacher/Assistant에서 재사용.
  - **어떻게 동작**: 드롭다운 변경 시 목록 재조회.
  - **어디에 붙는지**: 반별/개인 진도 페이지 상단 (Teacher/Assistant 공통).
- `ProgressCardList`
  - **왜 필요한지**: 진도 데이터를 카드 형태로 정렬해 빠른 스캔 제공, 역할과 무관하게 재사용.
  - **어떻게 동작**: 날짜/제목/내용 요약 렌더, 빈 상태/로딩 상태 표시, “더 보기” 버튼 렌더.
  - **어디에 붙는지**: 반별/개인 진도 페이지 목록 영역 (Teacher/Assistant 공통).
- `StudentCalendarHeader`
  - **왜 필요한지**: 학생 검색을 캘린더 상단에 고정해 빠른 전환을 돕기 위함.
  - **어떻게 동작**: 검색 입력(자동완성)만 제공, 월 이동은 캘린더 카드 내부에서 제어.
  - **어디에 붙는지**: `/teacher/calendar`, `/assistant/calendar` 공통 캘린더 페이지 상단.
- `StudentInfoCard`
  - **왜 필요한지**: 선택된 학생 정보를 요약 표시.
  - **어떻게 동작**: 이름/연락처/반 목록 렌더, “변경” 버튼은 검색창에 포커스.
  - **어디에 붙는지**: `/teacher/calendar`, `/assistant/calendar` 공통 캘린더 헤더 하단.
- `MonthlyCalendarGrid`
  - **왜 필요한지**: 날짜별 이벤트(공통/개인/클리닉)를 시각화.
  - **어떻게 동작**: 월간 그리드 렌더, 하루 이벤트는 색상 바 + “+N” 표시.
  - **어디에 붙는지**: `/teacher/calendar`, `/assistant/calendar` 공통 캘린더 본문.
- `CalendarDayDetailModal`
  - **왜 필요한지**: 특정 날짜의 상세 기록을 분류해서 보여주기 위함.
  - **어떻게 동작**: 날짜 클릭 시 공통/개인/클리닉 리스트 렌더, 수정/삭제 액션 제공.
  - **어디에 붙는지**: `/teacher/calendar`, `/assistant/calendar` 공통 캘린더 페이지.

> 컴포넌트는 `Modal`, `Button`, `Card`, `Select`, `TextField`, `Checkbox`, `Tabs` 등 기존 공통 UI를 우선 사용한다. 새로운 공통 컴포넌트가 필요하면 사전에 사용자 승인 후 추가한다.

## 5. State & Data Flow
- **데이터 소스 (OpenAPI 타입 기반)**
  - `CourseProgress`: `paths["/api/v1/courses/{courseId}/course-progress"]["get"]` + `components["schemas"]["CourseProgressResponse"]`.
  - `PersonalProgress`: `paths["/api/v1/student-courses/{recordId}/personal-progress"]["get"]` + `components["schemas"]["PersonalProgressResponse"]`.
  - Compose: `paths["/api/v1/courses/{courseId}/course-progress/compose"]["post"]`.
  - Calendar: `paths["/api/v1/students/{studentId}/calendar"]["get"]`.
  - 학생 목록: `paths["/api/v1/student-courses"]["get"]` (courseId/keyword 필터 활용).
- **상태 관리**
  - `TeacherLessonComposeModal`: `selectedCourseId`, `selectedStudentIds`, 입력 폼 상태, submit/loading/error.
  - 진도 페이지: 선택 값(반/학생), cursor 상태, loading/empty 상태. (Teacher/Assistant 공통 사용)
  - 학생 캘린더: `selectedStudent`, `currentYearMonth`, `calendarData`, `selectedDate`.
- **커서 기반 목록 처리**
  - 커서 기반 API(`cursorCreatedAt`, `cursorId`, `limit`)로 최신순 목록을 가져온다.
  - 기본 limit만 조회하고, 필요 시 “더 보기” 버튼으로 추가 커서를 요청한다.
- **필드 매핑 규칙 (UI ↔ 백엔드)**
  - 백엔드 필드가 `title`, `content`만 있으므로 UI 입력을 다음과 같이 합친다.
    - `title`: “진도 내용”
    - `content`: “학습 범위/과제/메모”를 줄바꿈 섹션으로 결합
  - PersonalProgress도 동일하게 `title`(진도 내용), `content`(학습 내용/메모)로 저장.
  - 이 매핑 방식은 UX 문구와 맞는지 사용자 확인 후 확정.
- **에러 처리**
  - 모든 API 실패는 `getApiErrorMessage`/`getFetchError`로 메시지 추출 후 토스트 표시.
  - 필수 입력 누락 시 필드 단위 에러 메시지 출력.
- **로딩 전략**
  - 목록 페이지는 skeleton 카드 표시.
  - 모달 제출 시 버튼 로딩 + 비활성화, 성공 시 모달 닫고 목록 갱신.

## 6. Interaction & UX Details
- 반/학생 선택 전에는 입력 섹션을 비활성화하고 안내 문구 노출.
- 학생 체크박스 선택 시 하단에 개인 진도 입력 카드가 “선택 순서대로” 추가됨.
- 모달 닫기 시 입력값 초기화, 작성 중일 때는 확인 다이얼로그로 이탈 방지.
- 필터 변경 시 목록 재조회 + 스크롤 최상단 이동.
- 캘린더 월 이동은 현재 월 기준 ±3개월까지만 활성화 (백엔드 제한과 동일).
- 날짜 셀에 이벤트가 많을 경우 `+N` 표기, 상세 모달에서 전체 목록 확인.

## 7. Test & Verification Plan
- **타입/빌드 검증 (필수)**
  - `cd frontend && npm run build -- --webpack`
- **수동 QA 체크리스트**
  1. `/teacher` 대시보드에서 “수업 작성” 모달 열림 확인.
  2. 반 선택 → 공통 진도 입력 노출, 학생 선택 → 개인 진도 입력 노출 확인.
  3. 작성 완료 후 목록/캘린더에 즉시 반영되는지 확인.
  4. 반별/개인 진도 페이지에서 “더 보기” 동작 및 카드 리스트 최신순 확인.
  5. 학생 캘린더: 학생 검색 → 정보 카드/캘린더 로딩 → 날짜 클릭 상세 모달 확인.
  6. 월 이동 제한(±3개월) 및 에러 토스트 확인.
- **추가 검증**
  - 기존 공통 컴포넌트로 해결되지 않는 UI가 있다면 사용자 승인 후 새 컴포넌트 추가.
