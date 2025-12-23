# Feature: Clinic Management UI (Teacher/Assistant/Student)

## 1. Problem Definition
- Clinic 도메인(슬롯/세션/출석/기록)은 백엔드 구현이 완료되었지만, 역할별 UI 흐름이 없어 실제 운영이 불가능하다.
- Teacher/Assistant/Student가 같은 데이터(teacher/branch 기준)를 서로 다른 권한과 시점에서 다루므로, **공통 구조 + 역할별 분기**가 필요한 화면 설계가 필요하다.
- 출석 잠금(세션 시작 10분 전), 이동 제한(세션 시작 30분 전), 기본 슬롯 변경 규칙 등 **시간 기반 정책**을 UI에서 안내/제한해야 혼선을 줄일 수 있다.
- 목표: 기존 대시보드 구조에 클리닉 관리 화면을 추가하고, API 응답을 근거로 슬롯/세션/출석/기록이 끊김 없이 이어지는 UX를 제공한다.

## 2. User Flows & Use Cases
### Teacher
1. 사이드바 → 클리닉 관리 아코디언에서 하위 메뉴 선택.
2. 상단에서 **출강 지점 선택** (`GET /api/v1/teachers/me/branches`).
3. **지점별 클리닉(슬롯 관리)** (`/teacher/clinics/slots`):
   - 선택된 지점 기준 슬롯 목록 조회 (`GET /api/v1/clinic-slots?branchId=...`).
   - 신규 슬롯 생성 (`POST /api/v1/clinic-slots`).
   - 슬롯 수정/삭제 (`PATCH /api/v1/clinic-slots/{slotId}`, `DELETE /api/v1/clinic-slots/{slotId}`).
4. **주차별 클리닉(세션)** (`/teacher/clinics/sessions`):
   - 선택된 지점 + 주차 범위로 세션 조회 (`GET /api/v1/clinic-sessions?dateRange=...&branchId=...`).
   - 긴급 세션 생성 (`POST /api/v1/clinic-sessions/emergency`).
   - 세션 취소 (`PATCH /api/v1/clinic-sessions/{sessionId}/cancel`).
5. **오늘의 출석부** (`/teacher/clinics/attendance`):
   - 오늘 날짜 세션 목록 조회 → 세션 선택.
   - 출석 명단 조회 (`GET /api/v1/clinic-attendances?clinicSessionId=...`).
   - 예외 추가/삭제 (`POST /api/v1/clinic-sessions/{sessionId}/attendances`, `DELETE /api/v1/clinic-attendances/{attendanceId}`).
   - 학생 클릭 → 기록 작성/수정/삭제 (`/api/v1/clinic-records` CRUD).

### Assistant
1. 사이드바 → 클리닉 일정 아코디언에서 하위 메뉴 선택.
2. 상단에서 **Teacher + Branch 선택**:
   - `GET /api/v1/assistants/me/courses`로 teacher/branch 조합을 구성.
3. **선생님별 클리닉(슬롯 조회)** (`/assistant/clinics/slots`):
   - `GET /api/v1/clinic-slots?branchId=...&teacherId=...`.
4. **주차별 클리닉(세션)** (`/assistant/clinics/sessions`):
   - `GET /api/v1/clinic-sessions?dateRange=...&branchId=...&teacherId=...`.
   - 긴급 세션 생성 (`POST /api/v1/clinic-sessions/emergency`, body에 teacherId 포함).
   - 세션 취소.
5. **오늘의 출석부** (`/assistant/clinics/attendance`):
   - 출석 명단 조회 + 예외 추가/삭제 + 기록 작성/수정/삭제.

### Student
1. 사이드바 → 클리닉 아코디언에서 하위 메뉴 선택.
2. **클리닉 시간표(기본 슬롯 선택)** (`/student/clinics/schedule`):
   - 학생 컨텍스트 조회 (`GET /api/v1/students/me/clinic-contexts`).
   - 선생님+지점 카드 선택 → (동일 teacher+branch에 Course가 여러 개면) 반 선택 드롭다운 노출.
   - 선택된 Course 기준 슬롯 시간표 조회 (`GET /api/v1/clinic-slots?courseId=...`).
   - 기본 슬롯 선택/변경 (`PATCH /api/v1/students/me/courses/{courseId}/clinic-slot`).
3. **이번 주 클리닉 시간표** (`/student/clinics/week`):
   - 학생 컨텍스트 조회 (`GET /api/v1/students/me/clinic-contexts`).
   - 본인 참석 목록 조회 (`GET /api/v1/students/me/clinic-attendances?dateRange=...`).
   - 선택한 teacher+branch의 주간 세션 조회 (`GET /api/v1/clinic-sessions?dateRange=...&branchId=...&teacherId=...`).
   - 시간표 셀 클릭
     - **비참석 세션**: 추가 참석 신청 (`POST /api/v1/students/me/clinic-attendances`).
     - **참석 중 세션**: 동일 주차 이동 (`PATCH /api/v1/students/me/clinic-attendances`).

## 3. Page & Layout Structure
### Teacher `/teacher/clinics`
- 라우트 구조:
  - `/teacher/clinics/slots` 지점별 클리닉(슬롯)
  - `/teacher/clinics/sessions` 주차별 클리닉(세션)
  - `/teacher/clinics/attendance` 오늘의 출석부
- 사이드바 아코디언에서 하위 메뉴로 이동한다.

### Assistant `/assistant/clinics`
- 라우트 구조:
  - `/assistant/clinics/slots` 선생님별 클리닉(슬롯)
  - `/assistant/clinics/sessions` 주차별 클리닉(세션)
  - `/assistant/clinics/attendance` 오늘의 출석부
- 사이드바 아코디언에서 하위 메뉴로 이동한다.

### Student `/student/clinics`
- 라우트 구조:
  - `/student/clinics/schedule` 클리닉 시간표(기본 슬롯)
  - `/student/clinics/week` 이번 주 클리닉
- 사이드바 아코디언에서 하위 메뉴로 이동한다.
- 각 화면은 teacher+branch 카드 → 반 선택(필요 시) → 시간표 흐름을 따른다.

## 4. Component Breakdown
- `ClinicContextSelector`
  - **왜 필요한지**: Teacher/Assistant가 branch(및 teacher) 맥락을 먼저 확정해야 모든 조회가 가능하다.
  - **어떻게 동작**: Teacher는 `/api/v1/teachers/me/branches`로 지점 목록을 받아 선택, Assistant는 `/api/v1/assistants/me/courses`에서 teacher/branch 조합을 추출해 2단 드롭다운 구성.
  - **어디에 붙는지**: `/teacher/clinics`, `/assistant/clinics` 상단.
- `ClinicSlotPanel`
  - **왜 필요한지**: 슬롯 CRUD와 목록을 한 섹션으로 묶어 관리하기 위함.
  - **어떻게 동작**: 슬롯 리스트 렌더 + “슬롯 추가/수정” 모달 호출, 삭제 시 확인 다이얼로그.
  - **어디에 붙는지**: Teacher/Assistant 페이지의 “지점별 클리닉” 섹션.
- `ClinicSlotFormModal`
  - **왜 필요한지**: dayOfWeek/time/capacity 입력을 표준화해 검증과 재사용을 쉽게 하기 위함.
  - **어떻게 동작**: 요일 Select + time input + capacity 입력, 제출 시 POST/PATCH 호출.
  - **어디에 붙는지**: `ClinicSlotPanel`에서 호출.
- `ClinicSessionWeekPanel`
  - **왜 필요한지**: 주차별 세션 조회/생성을 한 시야에서 관리하기 위함.
  - **어떻게 동작**: 주차 범위 선택 → 세션 리스트 렌더, 긴급 세션 모달로 생성, 취소 버튼 제공.
  - **어디에 붙는지**: Teacher/Assistant 페이지 “주차별 클리닉”.
- `ClinicEmergencySessionModal`
  - **왜 필요한지**: 긴급 세션 생성 폼을 재사용하기 위함.
  - **어떻게 동작**: 날짜/시간/정원 입력, Assistant는 teacherId 포함하여 제출.
  - **어디에 붙는지**: `ClinicSessionWeekPanel`.
- `ClinicAttendanceBoard`
  - **왜 필요한지**: “오늘 세션 리스트”와 “출석부”를 동시에 보여야 작업 흐름이 끊기지 않는다.
  - **어떻게 동작**: 좌측 세션 선택 → 우측 출석부 로딩, 학생 클릭 시 기록 모달 열림.
  - **어디에 붙는지**: Teacher/Assistant 페이지 “오늘의 출석부”.
- `ClinicAttendanceAddModal`
  - **왜 필요한지**: 출석 예외 추가 시 학생 검색/선택이 필요하다.
  - **어떻게 동작**: `/api/v1/student-courses` 목록을 불러와 검색 후 `studentCourseRecordId`로 추가 요청.
  - **어디에 붙는지**: `ClinicAttendanceBoard` 내 “학생 추가” 버튼.
- `ClinicRecordFormModal`
  - **왜 필요한지**: 출석부에서 바로 기록 작성/수정/삭제가 가능해야 한다.
  - **어떻게 동작**: `recordId` 유무로 신규/수정 상태 분기, 저장 후 출석부 재조회.
  - **어디에 붙는지**: 출석부 학생 항목 클릭 시 호출.
- `StudentClinicContextCards`
  - **왜 필요한지**: 학생 화면에서 teacher+branch 기준으로 정보를 묶어야 슬롯/세션을 일관되게 볼 수 있다.
  - **어떻게 동작**: `GET /api/v1/students/me/clinic-contexts`로 카드 목록 생성, 같은 teacher+branch에 Course가 여러 개면 드롭다운 노출.
  - **어디에 붙는지**: `/student/clinics`의 두 탭 상단.
- `StudentClinicSlotTimetable`
  - **왜 필요한지**: 기본 슬롯 선택을 시간표 형태로 제공해 직관적으로 선택할 수 있다.
  - **어떻게 동작**: `GET /api/v1/clinic-slots?courseId=...` 결과를 시간표 그리드에 배치하고 `defaultClinicSlotId`로 강조 표시한다.
  - **어디에 붙는지**: `/student/clinics`의 “클리닉 시간표” 탭.
- `StudentClinicWeekTimetable`
  - **왜 필요한지**: 이번 주 참석/비참석 세션을 한 눈에 파악하고 즉시 액션을 이어가야 한다.
  - **어떻게 동작**: 내 attendance와 주간 세션 목록을 합쳐 시간표로 렌더링하고, 셀 클릭으로 추가/이동 흐름을 분기한다.
  - **어디에 붙는지**: `/student/clinics`의 “이번 주 클리닉” 탭.
- `StudentClinicSessionDetailPanel`
  - **왜 필요한지**: 시간표 셀 클릭 후 추가/이동/정보 확인을 한 곳에서 처리해야 한다.
  - **어떻게 동작**: 비참석 세션이면 “추가 참석” 버튼, 참석 중 세션이면 “변경 모드” 진입 버튼을 표시한다.
  - **어디에 붙는지**: `/student/clinics`의 “이번 주 클리닉” 탭 우측/하단.

> 공통 UI는 `Card`, `Table`, `Tabs`, `Select`, `Modal`, `Button`, `TextField`, `Badge` 등 기존 컴포넌트를 우선 사용한다.  
> 새 공통 컴포넌트가 필요하면 사용자 승인 후 추가한다.

## 5. State & Data Flow
### 데이터 소스 (OpenAPI 타입 기반)
- Branch/Assignment
  - Teacher: `GET /api/v1/teachers/me/branches`
  - Assistant: `GET /api/v1/assistants/me/courses`
- ClinicSlot
  - `GET /api/v1/clinic-slots?branchId=...`
  - `GET /api/v1/clinic-slots?branchId=...&teacherId=...`
  - `GET /api/v1/clinic-slots?courseId=...`
  - `POST /api/v1/clinic-slots`
  - `PATCH /api/v1/clinic-slots/{slotId}`
  - `DELETE /api/v1/clinic-slots/{slotId}`
- ClinicSession
  - `GET /api/v1/clinic-sessions?dateRange=YYYY-MM-DD,YYYY-MM-DD&branchId=...(&teacherId=...)`
  - `POST /api/v1/clinic-sessions/emergency`
  - `PATCH /api/v1/clinic-sessions/{sessionId}/cancel`
  - (선택) `POST /api/v1/clinic-slots/{slotId}/sessions` 정규 세션 수동 생성
- ClinicAttendance (Teacher/Assistant)
  - `GET /api/v1/clinic-attendances?clinicSessionId=...`
  - `POST /api/v1/clinic-sessions/{sessionId}/attendances`
  - `DELETE /api/v1/clinic-attendances/{attendanceId}`
- ClinicAttendance (Student)
  - `GET /api/v1/students/me/clinic-attendances?dateRange=...`
  - `POST /api/v1/students/me/clinic-attendances` (body: `{ clinicSessionId, courseId }`)
  - `PATCH /api/v1/students/me/clinic-attendances`
- ClinicRecord
  - `POST /api/v1/clinic-records`
  - `GET /api/v1/clinic-records?clinicAttendanceId=...`
  - `PATCH /api/v1/clinic-records/{recordId}`
  - `DELETE /api/v1/clinic-records/{recordId}`
- Student 기본 슬롯 & 컨텍스트
  - `GET /api/v1/students/me/clinic-contexts`
  - `PATCH /api/v1/students/me/courses/{courseId}/clinic-slot`
- 출석 추가용 학생 목록
  - `GET /api/v1/student-courses?courseId=&status=ACTIVE&keyword=`

### 상태 관리
- 공통: `selectedTeacherId`, `selectedBranchId`, `selectedWeekRange`, `selectedSessionId`.
- 슬롯: `slotList`, `slotFormState`, `isSlotModalOpen`.
- 세션: `sessionList`, `emergencyFormState`, `isEmergencyModalOpen`.
- 출석: `attendanceList`, `selectedAttendanceId`, `isAttendanceLocked`.
- 기록: `recordFormState`, `recordId`, `isRecordModalOpen`.
- 학생:
  - `clinicContexts`, `selectedContextKey`, `selectedCourseId`.
  - `slotsByCourseId`, `defaultSlotByCourseId`.
  - `weekSessionsByContext`, `studentAttendances`, `selectedMoveTarget`.

### API 계약/에러 처리
- `dateRange`는 `YYYY-MM-DD,YYYY-MM-DD` 형식으로 전송한다.
- 모든 에러 메시지는 `getApiErrorMessage`/`getFetchError` 헬퍼로 처리한다.
- 출석 변경은 서버에서 잠금(10분 전)과 이동 제한(30분 전)을 검증하므로, UI는 **사전 비활성화 + 서버 에러 토스트**를 모두 적용한다.

### 데이터 갱신 전략
- 슬롯 CRUD 후 `clinic-slots` 재조회.
- 세션 생성/취소 후 `clinic-sessions` 재조회.
- 출석 추가/삭제/이동 후 `clinic-attendances` 또는 `students/me/clinic-attendances` 재조회.
- 기록 저장/삭제 후 해당 세션 출석부 재조회.

## 6. Interaction & UX Details
- 슬롯 수정 시 요일/시간 변경이 감지되면 “기본 슬롯이 해제될 수 있음” 경고 문구 표시.
- 세션 카드에 `REGULAR/EMERGENCY` 배지, 취소된 세션은 흐린 스타일 + “취소됨” 표시.
- 출석부:
  - `recordId`가 없으면 “기록 작성” 버튼, 있으면 “기록 수정/삭제” 버튼 노출.
  - 세션 시작 10분 전이면 추가/삭제 버튼 비활성화 + 안내 툴팁.
- 학생 화면:
  - 기본 슬롯 선택 시 성공/실패 토스트 표시.
  - 시간표에서 **비참석 세션 클릭 → 추가 참석**, **참석 세션 클릭 → 변경 모드**로 분기한다.
  - 변경 모드에서는 이동 가능한 세션만 강조하고, 나머지는 비활성 처리한다.
  - 참석 이동은 세션 시작 30분 전까지만 가능하므로, 시간 경과 시 버튼 비활성화.
  - 추가 참석 시 세션 시간 겹침/정원 초과 에러를 명확히 안내한다.
- 로딩 상태는 Skeleton/Spinner로 처리하고, 빈 상태는 EmptyState 컴포넌트를 사용한다.

## 7. Test & Verification Plan
- **타입/빌드 검증**
  - `cd frontend && npm run build -- --webpack`
- **수동 QA 체크리스트**
  1. Teacher: 지점 선택 → 슬롯 목록/생성/수정/삭제 정상 동작.
  2. Teacher: 주차별 세션 조회 → 긴급 세션 생성/취소 동작 확인.
  3. Teacher: 오늘의 출석부에서 출석 추가/삭제/기록 작성 확인.
  4. Assistant: teacher/branch 선택 후 슬롯 조회 + 세션/출석/기록 동작 확인.
  5. Student: 기본 슬롯 시간표에서 선택/변경 성공 및 에러 케이스 확인.
  6. Student: 이번 주 시간표에서 추가 참석/이동 모드 분기 및 제한(30분 전) 확인.
  7. 모든 역할에서 잠금 시간(10분 전) 이후 버튼 비활성화 확인.

---

## 8. Implementation Phases
### Phase 0 — 스캐폴딩/공통 준비
- 목표: 역할별 페이지 뼈대와 기본 레이아웃을 고정한다.
- 범위:
  - `/teacher/clinics`, `/assistant/clinics`, `/student/clinics` 라우트 생성
  - 상단 설명 카드/탭/아코디언 자리잡기
  - 이번 주 `dateRange` 유틸 준비
- 완료 기준: 각 페이지 진입 가능 + 기본 레이아웃 표시

### Phase 1 — 데이터 훅/타입 레이어
- 목표: API 호출과 상태 처리를 훅 단위로 분리해 UI 복잡도를 낮춘다.
- 범위:
  - OpenAPI 타입 alias 정리
  - `useClinicContexts`, `useClinicSlots`, `useClinicSessions`, `useStudentAttendances`, `useAttendanceMutations` 설계
  - 에러 처리 `getApiErrorMessage`/`getFetchError` 통합
- 완료 기준: 각 훅이 로딩/성공/에러 상태를 구분해 반환

### Phase 2 — 학생: 기본 슬롯 시간표
- 목표: 기본 슬롯 선택/변경 흐름을 시간표 기반으로 완성한다.
- 범위: 2-1 ~ 2-3 단계 순차 적용.
- 완료 기준: 기본 슬롯 설정/변경 성공/실패가 UI에 반영

#### Phase 2-1 — 컨텍스트/반 선택 UI
- 목표: teacher+branch 카드와 반 선택 흐름을 고정한다.
- 범위:
  - `StudentClinicContextCards` 구현
  - `useClinicContexts`로 컨텍스트 카드 렌더
  - 동일 teacher+branch에 Course가 여러 개면 반 드롭다운 노출
  - `selectedContextKey`, `selectedCourseId` 상태 확정
- 완료 기준: 컨텍스트/반 선택이 정상적으로 변경되고, 선택 상태가 유지됨

#### Phase 2-2 — 기본 슬롯 시간표 표시
- 목표: 슬롯 시간표를 렌더링하고 기본 슬롯을 강조한다.
- 범위:
  - `StudentClinicSlotTimetable` 구현
  - `useClinicSlots(courseId)` 연결
  - 기본 슬롯/미선택 슬롯 스타일 구분
- 완료 기준: 선택한 course 기준 슬롯 시간표가 표시되고 기본 슬롯이 강조됨

#### Phase 2-3 — 기본 슬롯 변경 액션
- 목표: 기본 슬롯 변경을 API와 연결한다.
- 범위:
  - `PATCH /api/v1/students/me/courses/{courseId}/clinic-slot` 연결
  - 첫 선택/변경 확인 모달 + 토스트 처리
  - 변경 후 슬롯 재조회/상태 갱신
- 완료 기준: 기본 슬롯 변경 성공/실패가 즉시 UI에 반영됨

### Phase 3 — 학생: 이번 주 세션 시간표
- 목표: 추가 참석/이동 흐름을 시간표 셀 클릭으로 연결한다.
- 범위: 3-1 ~ 3-3 단계 순차 적용.
- 완료 기준: 참석/비참석 셀 클릭 분기 + 추가/이동 성공/실패 처리

#### Phase 3-1 — 주간 시간표 데이터 결합
- 목표: 주간 세션 + 내 참석 목록을 한 시간표에서 볼 수 있게 한다.
- 범위:
  - `StudentClinicWeekTimetable` 기본 구조 구성
  - `useClinicContexts`, `useClinicSessions`, `useStudentAttendances` 연결
  - 주간 범위(dateRange) 선택/고정 상태 구성
  - 참석/비참석 세션 스타일 구분
- 완료 기준: 선택한 context 기준으로 주간 시간표가 표시됨

#### Phase 3-2 — 세션 상세 패널/추가 참석 흐름
- 목표: 시간표 셀 클릭 시 상세 패널을 열고 추가 참석을 연결한다.
- 범위:
  - `StudentClinicSessionDetailPanel` 구현
  - 비참석 세션 클릭 → 상세 패널에서 “추가 참석” 버튼 표시
  - `POST /api/v1/students/me/clinic-attendances` 연결
- 완료 기준: 비참석 세션에서 추가 참석이 성공/실패로 처리됨

#### Phase 3-3 — 변경 모드/이동 흐름
- 목표: 참석 중 세션 클릭 시 변경 모드로 전환한다.
- 범위:
  - 참석 중 세션 클릭 → 변경 모드 활성화
  - 이동 가능 세션 강조/비활성 처리
  - `PATCH /api/v1/students/me/clinic-attendances` 연결
  - 30분 제한 사전 비활성 + 서버 에러 메시지 처리
- 완료 기준: 이동 요청 성공/실패가 즉시 UI에 반영됨

### Phase 4 — Teacher/Assistant: 슬롯/세션
- 목표: Teacher/Assistant 슬롯/세션 관리 UI를 완성한다.
- 범위: 4-1 ~ 4-3 단계 순차 적용.
- 완료 기준: 슬롯/세션 조회 + CRUD/취소 흐름 정상 동작

#### Phase 4-1 — 컨텍스트 선택기
- 목표: Teacher/Assistant의 branch/teacher 선택 맥락을 고정한다.
- 범위:
  - `ClinicContextSelector` 기본 구조 구현
  - Teacher: `/api/v1/teachers/me/branches` 연결
  - Assistant: `/api/v1/assistants/me/courses`에서 teacher/branch 조합 추출
- 완료 기준: 선택된 teacher/branch가 다른 섹션에 전달됨

#### Phase 4-2 — 슬롯 패널
- 목표: 슬롯 목록과 CRUD 흐름을 완성한다.
- 범위:
  - `ClinicSlotPanel`, `ClinicSlotFormModal` 구현
  - Teacher: 슬롯 생성/수정/삭제 연결
  - Assistant: 조회 전용 모드 적용
- 완료 기준: 슬롯 목록 표시 + CRUD 동작 정상 처리

#### Phase 4-3 — 주차별 세션 패널
- 목표: 주간 세션 조회/긴급 생성/취소를 연결한다.
- 범위:
  - `ClinicSessionWeekPanel`, `ClinicEmergencySessionModal` 구현
  - `GET /api/v1/clinic-sessions` 연결
  - `POST /api/v1/clinic-sessions/emergency` + `PATCH /api/v1/clinic-sessions/{sessionId}/cancel` 연결
- 완료 기준: 세션 조회/생성/취소 동작 정상 처리

### Phase 5 — Teacher/Assistant: 오늘의 출석부/기록
- 목표: 출석부 → 기록 작성/수정/삭제 흐름을 완성한다.
- 범위: 5-1 ~ 5-3 단계 순차 적용.
- 완료 기준: 출석/기록 CRUD + 잠금 시간 제한 UI 반영

#### Phase 5-1 — 출석부 기본 흐름
- 목표: 세션 목록 선택 → 출석 명단 표시 흐름을 완성한다.
- 범위:
  - `ClinicAttendanceBoard` 기본 구조 구현
  - 오늘 세션 목록 조회 + 선택 UI
  - `GET /api/v1/clinic-attendances` 연결
- 완료 기준: 세션 선택 시 출석 명단이 표시됨

#### Phase 5-2 — 출석 예외 추가/삭제
- 목표: 출석 예외(추가/삭제) 플로우를 연결한다.
- 범위:
  - `ClinicAttendanceAddModal` 구현
  - `POST /api/v1/clinic-sessions/{sessionId}/attendances` 연결
  - `DELETE /api/v1/clinic-attendances/{attendanceId}` 연결
  - 잠금 시간(10분 전) 비활성화 처리
- 완료 기준: 출석 예외 추가/삭제가 성공/실패로 처리됨

#### Phase 5-3 — 클리닉 기록 CRUD
- 목표: 출석부에서 기록 작성/수정/삭제를 마무리한다.
- 범위:
  - `ClinicRecordFormModal` 구현
  - `POST/GET/PATCH/DELETE /api/v1/clinic-records` 연결
  - 저장 후 출석부 재조회
- 완료 기준: 기록 CRUD 동작과 화면 반영이 일치함

### 확인 필요 사항
- 신규 `GET /api/v1/students/me/clinic-contexts` 응답에 `teacherName`, `branchName`, `companyName`까지 포함하도록 백엔드 스펙 정리 필요.
