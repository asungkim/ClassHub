# Feature: Assistant Progress/Attendance UI Enhancements

## 1. Problem Definition
- 조교가 `/assistant/clinics/attendance`에서 학생 추가를 시도하면 권한 문제로 목록 로딩이 실패해 실사용이 막혔었다. 백엔드 권한 확장 이후 프런트 동작을 검증/보완해야 한다.
- 조교도 공통/개인 진도를 기록할 수 있게 되었으나, 현재 UI는 선생님 전용 흐름(모달/버튼)으로 설계되어 조교가 작업을 시작하기 어렵다.
- 조교가 수정/삭제할 수 있는 범위는 “본인 작성 기록만”이므로, UI에서도 버튼 노출을 작성자 기준으로 제어해야 한다.
- 학생 캘린더 상세 모달은 현재 `canEdit`이 역할 기반이라 조교/작성자 기준 제어가 불가능하다.

## 2. User Flows & Use Cases
- **조교 대시보드 통합 작성 모달**
  1. `/assistant` 대시보드 상단 카드 오른쪽에 “+ 수업 내용 작성” 버튼 표시.
  2. 클릭 시 통합 수업 작성 모달 오픈.
  3. 연결된 선생님들의 모든 반 목록 로딩 → 반 선택.
  4. 공통 진도(제목/내용/날짜) 입력 + 학생 체크 후 개인 진도 입력.
  5. 저장 시: 공통만이면 CourseProgress 생성, 개인 포함 시 compose API.
- **조교 반별/개별 진도 수정/삭제**
  1. `/assistant/progress/course`, `/assistant/progress/personal` 진도 카드 리스트 표시.
  2. 각 카드의 작성자 ID가 본인과 일치할 때만 “수정/삭제” 버튼 노출.
  3. 버튼 클릭 시 기존 수정/삭제 모달 흐름 그대로 사용.
- **조교 캘린더 수정/삭제**
  1. `/assistant/calendar`에서 날짜 상세 모달 진입.
  2. 공통/개인/클리닉 기록 각각에 대해 작성자 ID가 본인일 때만 “수정/삭제” 버튼 노출.
  3. 버튼 클릭 시 기존 수정/삭제 모달 흐름으로 연결.
- **오늘의 출석부 학생 추가**
  1. `/assistant/clinics/attendance`에서 “학생 추가” 버튼 클릭.
  2. 반 선택 후 학생 목록 로딩 → 출석 추가 정상 동작 확인.

## 3. Page & Layout Structure
- **조교 대시보드(`/assistant`)**
  - 상단 소개 카드 오른쪽에 “+ 수업 내용 작성” 버튼 추가.
  - 버튼 위치/스타일은 `/teacher`와 동일한 구조 유지.
- **조교 반별 진도(`/assistant/progress/course`)**
  - 기존 레이아웃 유지.
  - 카드 액션은 “작성자 본인”일 때만 노출.
- **조교 개인 진도(`/assistant/progress/personal`)**
  - 기존 레이아웃 유지.
  - 카드 액션은 “작성자 본인”일 때만 노출.
- **조교 학생 캘린더(`/assistant/calendar`)**
  - 날짜 상세 모달에서 각 항목별 수정/삭제 버튼 조건을 개별 계산하도록 변경.
- **조교 출석부(`/assistant/clinics/attendance`)**
  - UI 변경 최소화. 학생 추가 흐름 정상 작동 여부 확인.

## 4. Component Breakdown
- `TeacherLessonComposeModal` (확장)
  - **왜 필요한지**: 조교도 통합 수업 작성 기능이 필요하지만 기존 모달은 교사용 API에 고정됨.
  - **어떻게 동작하는지**: `role` prop 추가로 Teacher/Assistant에 따라 반 목록 API를 분기하고 동일한 입력 UI를 재사용.
  - **어디에 붙는지**: `/teacher`, `/assistant` 대시보드 상단 버튼에서 공통 사용.
- `CourseProgressSection`
  - **왜 필요한지**: 카드 리스트에서 조교의 수정/삭제 권한을 명확히 해야 함.
  - **어떻게 동작하는지**: `canEditDelete` 조건을 `memberId === writerId`일 때만 true로 확장(TEACHER/ASSISTANT 공통).
  - **어디에 붙는지**: `/teacher/progress/course`, `/assistant/progress/course`.
- `PersonalProgressSection`
  - **왜 필요한지**: 개인 진도도 동일한 작성자 기준 노출 필요.
  - **어떻게 동작하는지**: `canEditDelete` 조건을 `memberId === writerId`로 확장.
  - **어디에 붙는지**: `/teacher/progress/personal`, `/assistant/progress/personal`.
- `CalendarDayDetailModal`
  - **왜 필요한지**: 현재 `canEdit` boolean만 받아 조교 작성자 조건을 구현할 수 없음.
  - **어떻게 동작하는지**: `canEditCourse(event)`, `canEditPersonal(event)`, `canEditClinic(event)` 형태의 함수 props로 전환.
  - **어디에 붙는지**: `/teacher/calendar`, `/assistant/calendar`.

> 신규 컴포넌트 추가 없이 기존 컴포넌트를 확장/조합해서 해결한다. (공통 컴포넌트 추가는 사용자 승인 전까지 금지)

## 5. State & Data Flow
- **세션 정보**
  - `useSession`으로 `member.memberId`, `member.role`을 가져와 작성자 비교에 사용.
- **작성자 기준 버튼 노출**
  - Progress 리스트: `member.memberId === item.writerId`일 때만 수정/삭제 표시.
  - Calendar 상세: `event.writerId` 또는 `event.recordSummary.writerId` 기반으로 개별 조건 계산.
- **API 의존성 (캘린더)**
  - `CourseProgressEvent`는 writerId가 이미 존재.
  - `PersonalProgressEvent`, `ClinicRecordSummary`에는 writerId가 없으므로 **백엔드 응답 확장이 필요**.
  - 확장 전까지는 조교 캘린더 수정/삭제 버튼을 숨김 처리하고, 스펙 갱신 후 활성화.
- **통합 작성 모달**
  - 역할에 따라 `fetchTeacherCourses` vs `fetchAssistantCourses`로 분기.
  - 조교는 연결된 선생님들의 모든 반 목록을 가져온다.
  - 학생 목록은 `fetchCourseStudents` 재사용 (조교 권한 확장 후 정상 동작).

## 6. Interaction & UX Details
- 조교 대시보드 버튼은 선생님과 동일한 위치/스타일을 유지해 학습 비용을 낮춘다.
- 작성자 조건에 맞지 않으면 수정/삭제 버튼 자체를 숨겨 혼란을 줄인다.
- 캘린더 상세 모달에서 수정/삭제 버튼은 항목별로 독립적으로 표시되도록 변경한다.
- 출석부에서 학생 추가 실패 시 에러 토스트를 유지하며, 정상 동작 시 추가/삭제/기록 플로우가 끊기지 않도록 확인한다.

## 7. Test & Verification Plan
- **타입/빌드 검증 (필수)**
  - `cd frontend && npm run build -- --webpack`
- **수동 QA 체크리스트**
  1. `/assistant` 대시보드 상단에 “+ 수업 내용 작성” 버튼 노출 및 모달 오픈 확인.
  2. 모달에서 반/학생 목록 로딩 및 저장 성공 확인.
  3. `/assistant/progress/course` 카드에서 본인 작성 항목만 수정/삭제 버튼 노출 확인.
  4. `/assistant/progress/personal` 카드에서 본인 작성 항목만 수정/삭제 버튼 노출 확인.
  5. `/assistant/calendar` 날짜 상세 모달에서 본인 작성 항목만 수정/삭제 버튼 노출 확인.
  6. `/assistant/clinics/attendance` 학생 추가 버튼으로 학생 목록 로딩/등록 정상 동작 확인.
- **API 의존성 확인**
  - 캘린더 응답에 `writerId`가 포함되어야 조교 편집 버튼 로직을 활성화할 수 있음.
