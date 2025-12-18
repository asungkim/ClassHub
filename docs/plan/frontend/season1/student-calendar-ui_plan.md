# Feature: Student Calendar UI

## 1. Problem Definition

- Teacher/Assistant는 학생별 월간 학습 데이터를 한 화면에서 조회하고 관리해야 하지만, 현재 UI에는 해당 기능이 없어 백엔드 조회 API (`GET /api/v1/students/{id}/calendar`)를 활용할 수 없다.
- 기존 학생 목록/상세 페이지는 선형 데이터만 제공하므로 SharedLesson·PersonalLesson·ClinicRecord의 월간 맥락을 직관적으로 파악하기 어렵다.
- 학생 검색 → 월간 캘린더 → 날짜별 상세 모달이라는 흐름을 제공해 운영자가 빠르게 상황을 파악하고 후속 수정/삭제 액션을 실행할 수 있는 UI가 필요하다.

## 2. Requirements

### Functional

- 대시보드 사이드바에 "학생별 캘린더" 메뉴를 추가하고 Teacher/Assistant 역할에만 노출한다.
- 상단에 학생 검색 입력과 자동완성 리스트를 배치한다.
  - `GET /api/v1/student-profiles?name=`(기존 API)를 활용해 이름 키워드 기반으로 5~10건의 후보를 보여주고, 선택 시 학생 정보 카드와 상태(담당 코스, 연락처 등 요약)를 고정 표기한다.
  - 학생이 선택되기 전에는 캘린더 영역을 흐리게 처리하고 "학생을 선택해주세요" 빈 상태 안내를 노출한다.
- 범례 영역을 학생 선택 바로 아래에 두고 Shared(P#0A63FF)/Personal(#00A86B)/Clinic(#F2B705) 색상을 표시한다. 모바일에서는 Accordion 또는 Collapsible로 토글한다.
- 기본 캘린더는 월간 뷰(7열)이며, `year`/`month` 상태와 locale 요일 헤더를 갖는다. 주 시작 요일은 월요일.
- 각 날짜 셀은 숫자(일) + 최대 3개의 색상 띠(bar)로 구성하고, 동일 유형 이벤트가 여러 개면 띠 수로 표현한다.
  - 셀 공간이 부족하면 `+N` 오버플로 인디케이터를 오른쪽 하단에 표시한다.
  - Hover/Focus 시 outline을 표시해 접근성을 높인다.
- 날짜를 클릭하면 상세 모달을 띄운다.
  - 모달은 `Modal` 공통 컴포넌트를 확장한다.
  - SharedLesson/PersonalLesson 섹션을 순서대로 렌더링하고, 각 항목 옆에 [수정][삭제] 버튼을 배치한다.
  - 버튼 노출은 백엔드 응답의 `editable` 값을 그대로 따른다 (`editable: true`일 때만 버튼 표시).
  - ClinicRecord는 조회만 가능하며 수정/삭제 버튼을 표시하지 않는다 (추후 API 추가 시 확장 가능하도록 구조 유지).
- 월 이동 컨트롤(◀ YYYY년 MM월 ▶)을 제공한다.
  - 초기 값은 현재 날짜 기준 `year/month`.
  - 좌/우 클릭 시 선택된 학생을 유지한 채 `useStudentCalendar` 훅이 새로운 데이터를 재요청한다.
- Loading/Empty/Error 상태를 정의한다.
  - 학생 선택 후 첫 조회 시 Skeleton 캘린더.
  - 결과 없을 경우 "기록 없음" Empty State.
  - 실패 시 Retry 버튼과 원인 메시지.
- ClinicRecord는 현재 읽기 전용으로만 노출하고, 추후 API 추가 시 수정/삭제 기능을 쉽게 확장할 수 있도록 구조를 유지한다.
- SharedLesson/PersonalLesson 수정/삭제는 기존 API를 활용한다:
  - SharedLesson 수정: `PATCH /api/v1/shared-lessons/{id}` (권한: TEACHER만)
  - SharedLesson 삭제: `DELETE /api/v1/shared-lessons/{id}` (권한: TEACHER만)
  - PersonalLesson 수정: `PATCH /api/v1/personal-lessons/{id}` (권한: 인증된 사용자, 소유권 확인)
  - PersonalLesson 삭제: `DELETE /api/v1/personal-lessons/{id}` (권한: 인증된 사용자, 소유권 확인)

### Non-functional

- 모든 UI는 `frontend/src/components/ui` 공통 컴포넌트를 재사용하거나 확장한다 (Button, TextField, Modal, Badge, Card 등).
- Type-safe 데이터 처리를 위해 `frontend/src/types/openapi.d.ts`의 `paths["/students/{studentId}/calendar"].get`를 alias로 정의하고 React Query + `apiClient`를 사용한다.
- 학생 검색 입력에는 디바운스(300ms)와 로딩 인디케이터를 적용해 API 과호출을 막는다.
- Year/Month 상태는 URL query (`?studentId=...&year=2025&month=2`)로 동기화해 새로고침/공유가 가능하도록 한다.
  - `studentId` 없이 진입 시 현재 연/월을 표시하되 캘린더는 비활성 상태 유지.
  - 학생 선택 시 URL에 `studentId` 추가 및 캘린더 활성화.
- 모바일 레이아웃에서 Month View는 가로 스크롤 없이 노출되도록 typography/spacing을 조정하고, 상세 모달은 bottom sheet 패턴을 채택한다.
  - Bottom sheet는 화면 하단에서 슬라이드업 애니메이션으로 나타나고, 배경 dimmed overlay 제공.
  - Desktop에서는 중앙 모달, Mobile에서는 `@media (max-width: 768px)` 기준으로 bottom sheet로 전환.
- 모든 인터랙션마다 포커스 스타일과 키보드 네비게이션을 제공한다.
- 상태 관리는 최소화: 전역 스토어 대신 페이지 컴포넌트 + React Query 캐시로 해결한다.

## 3. API Design (Draft)

### 학생 캘린더 조회

- **Endpoint**: `GET /api/v1/students/{studentId}/calendar`
- **Query**: `year` (int), `month` (1-12)
- **Auth**: TEACHER/ASSISTANT (Access Token)
- **Response**: `StudentCalendarResponse`
  - `schemaVersion`: number (현재 1)
  - `studentId`, `year`, `month`
  - `sharedLessons[]`: `CalendarSharedLessonDto`
    - `id`, `courseId`, `courseName`, `date`, `title`, `content`, `writerId`, `writerRole`, `editable`
  - `personalLessons[]`: `CalendarPersonalLessonDto`
    - `id`, `date`, `title`, `content`, `writerId`, `writerRole`, `editable`
  - `clinicRecords[]`: `CalendarClinicRecordDto`
    - `id`, `clinicSlotId`, `date`, `note`, `writerId`, `writerRole` (editable 필드 없음, 조회 전용)
- **Frontend 매핑**: 응답을 `Map<date, { shared: CalendarSharedLessonDto[]; personal: ... }>`로 정규화해 날짜 셀 렌더링과 모달 데이터를 공유한다.

### 학생 검색

- **Endpoint**: `GET /api/v1/student-profiles`
- **Query**: `name` (string, 필수), `courseId` (UUID, optional), `active` (boolean, optional), pagination
- **Auth**: TEACHER/ASSISTANT
- **Response**: `RsData<PageResponse<StudentProfileSummary>>`
- **StudentProfileSummary 필드**:
  - `id` (UUID), `name` (string), `courseNames` (string[]), `grade` (string), `phoneNumber` (string)
  - `assistantId` (UUID), `assistantName` (string), `memberId` (UUID), `parentPhone` (string)
  - `age` (number), `active` (boolean)
- **사용 방식**: `name` 파라미터로 최소 2자 이상일 때 호출, 결과를 SelectList로 표기. 학생 카드에는 `name`, `courseNames.join(', ')`, `phoneNumber`, `active` 표시.

## 4. Domain Model (Draft)

- **StudentCalendarPageState**
  - `selectedStudent?: StudentProfileSummary`
  - `monthCursor: { year: number; month: number }`
  - `viewMode: 'grid'` (확장 대비)
  - `filters: { typeFilter?: LessonType }` (미사용시 null)
- **CalendarDayAggregate**
  - `date: string (YYYY-MM-DD)`
  - `sharedLessons: CalendarSharedLessonDto[]`
  - `personalLessons: CalendarPersonalLessonDto[]`
  - `clinicRecords: CalendarClinicRecordDto[]`
  - `overflowCount`: number (총 이벤트 - 렌더 가능한 슬롯)
- **LegendItem**
  - `{ type: 'shared' | 'personal' | 'clinic'; label: string; colorToken: string }`
- **ModalState**
  - `{ open: boolean; date?: string; sharedLessons: ...; personalLessons: ...; clinicRecords: ... }`
- **Derived helpers**
  - `getBarsForDay(dayAggregate) -> Array<{ color: string; count: number }>`
  - `formatMonthLabel(year, month) -> string`
  - `buildCalendarMatrix(year, month) -> CalendarDayAggregate[][]` (include leading/trailing days for alignment, disabled state)

## 5. TDD Plan

1. **Type contracts**
   - 생성: `types/api/student-calendar.ts`에서 OpenAPI 타입 alias (`type StudentCalendarOperation = paths["/students/{studentId}/calendar"].get`)를 정의하고, `npm run build -- --webpack`으로 타입 검증.
2. **Data hook**
   - `useStudentCalendar(studentId, { year, month })` React Query 훅을 작성하고 fetch mock을 이용한 단위 테스트가 없다면 Story/Preview에서 JSON fixture로 검증. 최소한 hook-only component를 만들어 `npm run build -- --webpack`으로 타입/공통 오류를 잡는다.
3. **Student picker + empty state**
   - 학생이 선택되지 않을 때 캘린더가 Disabled Overlay와 안내 문구를 보여주는지 QA (수동 테스트: `/dashboard/teacher/student-calendar`).
4. **Calendar grid rendering**
   - Fixture 데이터를 주입한 Story/Playground 컴포넌트로 날짜 셀에 색상 띠가 올바르게 표시되는지 수동 확인.
   - 키보드 포커스 이동이 가능한지 Cypress/Playwright가 없으므로 devtools로 Tab 순서를 확인.
5. **Modal interactions**
   - 날짜 클릭 → 상세 모달 오픈, `editable: true`일 때만 [수정][삭제] 버튼 노출 확인.
   - ClinicRecord는 버튼 없이 조회만 가능한지 확인.
   - 수정 버튼 클릭 시 해당 API (`PATCH /api/v1/shared-lessons/{id}` 또는 `/personal-lessons/{id}`) 호출 후 성공/실패 처리 수동 시험.
   - 삭제 버튼 클릭 시 확인 다이얼로그 → `DELETE` API 호출 → 성공 시 캘린더 재조회 확인.
6. **Month navigation**
   - Next/Prev 클릭 시 동일 학생으로 재요청, Loading Skeleton → 데이터 반영 과정을 수동 검증.
7. **Responsive**
   - Chrome DevTools Device Mode에서 모바일 레이아웃, Bottom Sheet 모달, Legend accordion 동작을 확인.
8. **Regression checklist**
   - 최종적으로 `npm run build -- --webpack` 실행 결과와 수동 시나리오(검색→조회→모달→월 이동)를 `docs/history/AGENT_LOG.md`에 기록한다.

## 6. UI Walkthrough (Structure & Visuals)

### 6.1 Navigation Entry

- 대시보드 사이드바의 Teacher/Assistant 섹션에 `학생별 캘린더` 메뉴 항목을 추가한다.
- 선택 시 `/dashboard/teacher/student-calendar`(권한에 따라 assistant 경로 재사용) 페이지가 렌더링되고, 기존 DashboardShell 레이아웃을 그대로 활용한다.
- 첫 로드는 현재 날짜 기준 연/월 정보를 상단 상태로 보여주되 학생이 선택되지 않았으므로 아래 캘린더는 비활성 상태다.

### 6.2 High-level Layout

```
┌───────────────────────────────┐
│ 학생 검색 영역                │
│ 학생 카드 / 빈 상태          │
├───────────────────────────────┤
│ 범례 (Shared/Personal/Clinic) │
├───────────────────────────────┤
│ ◀ 2025년 2월 ▶               │ ← 월 이동 컨트롤
├───────────────────────────────┤
│ 월간 캘린더 Grid (7열)       │
└───────────────────────────────┘
```

- 상단과 캘린더 사이에 얇은 divider를 두어 시선을 분리한다.
- PC 해상도에서는 최대 폭 1200px, 내부 그리드는 동일 높이 셀(약 140px)로 맞춘다.

### 6.3 Student Search & Selection

- `TextField` + Search icon 버튼 조합으로 학생 이름 입력을 받는다.
- 입력 1글자 이상에서 React Query로 `student-profiles` API를 호출하고 결과를 dropdown list(예: `Command`/`Popover`)로 노출한다.
- 학생을 선택하면 검색 영역 아래에 `StudentCard`와 유사한 요약 박스가 붙는다.
  - 내용: 이름, 담당 코스 뱃지, 연락처, 활성/비활성 상태.
  - 오른쪽에 “변경” 버튼으로 다른 학생을 선택할 수 있다.
- 학생이 없을 때는 `EmptyState`(아이콘 + “학생을 선택해주세요”)와 함께 캘린더 영역에 반투명 overlay를 씌운다.

### 6.4 Legend Block

- 학생 카드 아래에 pill 형태의 legend를 가로로 배치한다.
  - 🟦 **SharedLesson** – 공통 진도
  - 🟩 **PersonalLesson** – 개인 진도
  - 🟨 **ClinicRecord** – 클리닉 기록
- 모바일에서는 `Accordion` 컴포넌트로 감싸 눌러야 펼쳐지게 하며, 기본은 접힌 상태로 두어 공간을 절약한다.

### 6.5 Month Controls

- 중앙 정렬된 텍스트 `2025년 2월` 사이에 `IconButton`(chevron-left/right)을 두고 클릭 시 월 이동.
- 버튼 hover 시 테두리, disabled 상태(예: 연도 범위 제한 시)는 회색 처리.
- 월이 바뀌면 캘린더 영역에 skeleton shimmering 효과를 보여주고, 데이터 수신 후 fade-in.

### 6.6 Calendar Grid & Day Cell Composition

- 날짜 헤더는 월요일 시작: `Mon Tue Wed Thu Fri Sat Sun`.
- 각 셀 구성:

```
┌───────────────┐
│ 15            │ ← 날짜
│ ██████        │ ← SharedLesson bars (blue)
│ ██████        │ ← PersonalLesson bars (green)
│ ██████        │ ← ClinicRecord bars (yellow)
│ +2            │ ← overflow indicator (optional)
└───────────────┘
```

- 동일 유형 이벤트가 여러 개면 bar 개수를 늘린다. 렌더 가능한 최대 3개, 이후는 `+N`으로 표시.
- 주차를 채우기 위해 전월/다음 달 날짜 셀도 표시하되 텍스트와 배경을 연한 회색으로 처리하고 클릭을 막는다.

### 6.7 Day Detail Modal

- 셀 클릭 시 `Modal`이 열리고 header에 `YYYY년 MM월 DD일`을 표시한다.
- 본문은 세 섹션(Shared/Personal/Clinic)으로 나뉘며, 각 섹션의 항목 리스트는 카드 스타일:
  - SharedLesson: `수학 A반 / 미적분 1단원   [수정][삭제]` (editable: true일 때만 버튼 표시)
  - PersonalLesson: `제목 (예: 오답 노트 보완)` + 본문 내용 + `[수정][삭제]` 버튼 (editable: true일 때만)
  - ClinicRecord: `18:00~19:00 문제 풀이 (작성자: 조교 A)` (버튼 없음, 조회 전용)
- `editable: false`일 경우 버튼을 숨기고 항목만 표시한다.
- 모바일에서는 bottom sheet 형태(전체 폭, 라운드 상단)에 동일 내용이 세로 스크롤로 배치된다.

### 6.8 Loading / Empty / Error States

- Loading: 캘린더 셀을 회색 skeleton 박스로 렌더링, legend와 학생 카드도 skeleton 버전 제공.
- Empty: “해당 월에는 기록이 없습니다” 텍스트 + 서브 설명, 추가 CTA는 없음.
- Error: `Alert` 컴포넌트로 메시지와 “다시 시도” 버튼 제공, 클릭 시 React Query refetch.

### 6.9 Mobile Considerations

- 레이아웃을 단일 컬럼으로 스택하고, 캘린더는 가로 스크롤 없이 보이도록 셀 높이를 100px 안팎으로 줄인다.
- 모달은 bottom sheet, legend는 접힘 상태, 학생 정보 카드는 카드 형태 유지하되 텍스트 줄바꿈 허용.
- 날짜 셀 탭 영역을 padding으로 넓히고 focus-visible outline을 명확히 표시한다.

### 6.10 UX Flow Recap

1. 메뉴 진입 → 학생 검색 입력 노출.
2. 자동완성으로 학생 선택 → 카드 고정, 캘린더 활성화.
3. 현재 월 데이터 자동 로드 → 각 날짜별 색 띠로 활동 확인.
4. 특정 날 클릭 → 상세 모달에서 editable 항목의 수정/삭제 액션 수행, ClinicRecord는 조회만 가능.
5. 월 이동, 다른 학생 선택 등을 반복하며 기록 탐색.

### 6.11 One-line Summary

> 학생을 선택하면 월간 캘린더에서 Shared/Personal/Clinic 활동을 색상 띠로 한눈에 파악하고, 날짜 클릭 시 모달에서 즉시 조치하는 운영자 중심 뷰.

## 7. 개발 순서

### Phase 1: 타입 정의 및 기본 인프라

**참고**: 섹션 2 (Non-functional), 섹션 3 (API Design)

1. **React Query 훅 작성**

   - `frontend/src/hooks/api/useStudentCalendar.ts` 생성
   - `useStudentCalendar(studentId, { year, month })` 구현
   - `useStudentProfiles(name)` 학생 검색 훅 구현 (디바운스 300ms 포함)
   - 참고: 섹션 2.41 (디바운스 요구사항)
   - 참고: 기존 `frontend/src/lib/api-client.ts`의 `apiClient` 사용 패턴

2. **빌드 검증**

- `cd frontend && npm run build -- --webpack` 실행
- 타입 에러 0개 확인
- 참고: `CLAUDE.md`의 "프론트엔드 테스트 & 검증 프로세스"

### Phase 2: 페이지 라우팅 및 레이아웃

**참고**: 섹션 6.1 (Navigation Entry), 섹션 6.2 (High-level Layout)

3. **페이지 파일 생성**

   - `frontend/src/app/dashboard/teacher/student-calendar/page.tsx` 생성
   - DashboardShell 레이아웃 적용
   - 참고: 기존 `frontend/src/app/dashboard/teacher/*/page.tsx` 패턴
   - 빈 페이지 렌더링 확인 (`npm run dev` 후 수동 테스트)

4. **사이드바 메뉴 추가**
   - Teacher/Assistant 섹션에 "학생별 캘린더" 메뉴 항목 추가
   - 참고: 기존 사이드바 컴포넌트 (예: `frontend/src/components/layout/DashboardNav.tsx`)
   - 권한별 노출 확인 (TEACHER/ASSISTANT만)
   - 참고: 섹션 2.10 (Functional Requirements - 메뉴 노출)
   - 메뉴 클릭 시 페이지 이동 확인

### Phase 3: 학생 검색 및 선택

**참고**: 섹션 2.11-13 (Functional - 학생 검색), 섹션 6.3 (Student Search & Selection)

5. **학생 검색 UI**

   - `TextField` + 자동완성 드롭다운 구현
   - 참고: `frontend/src/components/ui/TextField.tsx`
   - 참고: Radix UI `Command` 또는 `Popover` 컴포넌트 활용
   - 1글자 이상 입력 시 API 호출 확인 (섹션 3.2 참고)
   - 로딩 인디케이터 표시

6. **학생 카드 컴포넌트**

   - `StudentCard` 또는 유사 컴포넌트 작성
   - 참고: `frontend/src/components/ui/Card.tsx` 재사용
   - `name`, `courseNames.join(', ')`, `phoneNumber`, `active` 표시
   - 참고: 섹션 3.2 (StudentProfileSummary 필드)
   - "변경" 버튼으로 재검색 가능

7. **빈 상태 처리**

   - 학생 미선택 시 "학생을 선택해주세요" EmptyState
   - 참고: 기존 EmptyState 컴포넌트 패턴 (예: `frontend/src/components/ui/EmptyState.tsx`)
   - 캘린더 영역 반투명 overlay 적용 (섹션 6.3 참고)

8. **URL 동기화**
   - `studentId` query parameter 추가/삭제
   - 참고: Next.js `useSearchParams`, `useRouter` 훅
   - 참고: 섹션 2.42-44 (URL query 동기화 요구사항)
   - 새로고침 시 상태 복원 확인

### Phase 4: 캘린더 그리드 렌더링

**참고**: 섹션 2.14-18 (Functional - 캘린더 뷰), 섹션 4 (Domain Model), 섹션 6.4-6.6 (UI 구조)

1. **범례(Legend) 컴포넌트**

   - Shared/Personal/Clinic 색상 표시
   - 참고: 섹션 2.14 (색상 값 - #0A63FF, #00A86B, #F2B705)
   - 참고: 섹션 6.4 (Legend Block 구조)
   - 참고: `frontend/src/components/ui/Badge.tsx` 재사용
   - 모바일에서 Accordion으로 접힘 처리 (Radix UI Accordion)

2. **월 이동 컨트롤**

   - 중앙 정렬 `◀ YYYY년 MM월 ▶`
   - 참고: 섹션 6.5 (Month Controls)
   - 참고: `frontend/src/components/ui/Button.tsx` (IconButton 패턴)
   - `year`/`month` state 관리 (섹션 4 - StudentCalendarPageState 참고)
   - URL query (`?year=...&month=...`) 동기화 (섹션 2.42-44 참고)

3. **캘린더 그리드 레이아웃**

   - 7열 그리드 (월요일 시작)
   - 참고: 섹션 2.15 (주 시작 요일 월요일)
   - 참고: 섹션 6.6 (Calendar Grid 구조)
   - 요일 헤더 렌더링 (`Mon Tue Wed Thu Fri Sat Sun`)
   - 전월/다음달 날짜 셀 비활성 처리 (연한 회색, 클릭 불가)

4. **날짜 셀 렌더링**

   - 날짜 숫자 표시
   - 색상 띠(bar) 렌더링 로직 구현
   - 참고: 섹션 2.16-18 (셀 구성, 오버플로, 접근성)
   - 참고: 섹션 6.6 (Day Cell Composition 다이어그램)
   - `+N` 오버플로 인디케이터 (최대 3개 띠)
   - Hover/Focus 스타일 (outline, 섹션 2.18, 2.49 참고)

5. **데이터 매핑 헬퍼**
   - `buildCalendarMatrix(year, month, data)` 구현
   - `getBarsForDay(dayAggregate)` 구현
   - 참고: 섹션 3.1 (Frontend 매핑 전략)
   - 참고: 섹션 4 (CalendarDayAggregate, Derived helpers)
   - 날짜별 데이터 정규화 (`Map<date, { shared, personal, clinic }>`)

### Phase 5: 상태별 UI 처리

**참고**: 섹션 2.27-30 (Functional - Loading/Empty/Error), 섹션 6.8 (UI States)

1. **Loading Skeleton**

   - 학생 카드 skeleton
   - 캘린더 그리드 skeleton
   - 범례 skeleton
   - 참고: 섹션 6.8 (Loading 상태 설명)
   - 참고: 기존 Skeleton 컴포넌트 패턴

2. **Empty State**

   - "해당 월에는 기록이 없습니다" 메시지
   - 데이터 없을 때 빈 캘린더 표시
   - 참고: 섹션 2.29, 6.8 (Empty State 요구사항)

3. **Error State**
   - Alert 컴포넌트로 에러 메시지
   - "다시 시도" 버튼 + refetch 연결
   - 참고: 섹션 2.30, 6.8 (Error State 요구사항)
   - 참고: `frontend/src/components/ui/Alert.tsx`

### Phase 6: 날짜 상세 모달

**참고**: 섹션 2.19-23 (Functional - 모달), 섹션 6.7 (Day Detail Modal)

1. **모달 기본 구조**

   - `Modal` 공통 컴포넌트 확장
   - 참고: `frontend/src/components/ui/Modal.tsx`
   - 참고: 섹션 2.20, 6.7 (모달 구조)
   - 날짜 클릭 시 모달 열기/닫기
   - Header에 `YYYY년 MM월 DD일` 표시

2. **모달 본문 렌더링**

   - SharedLesson 섹션
   - PersonalLesson 섹션
   - ClinicRecord 섹션
   - 각 항목 카드 스타일 적용
   - 참고: 섹션 6.7 (본문 섹션 구조)
   - 참고: `frontend/src/components/ui/Card.tsx` 재사용

3. **수정/삭제 버튼 조건부 렌더링**
   - `editable: true`일 때만 [수정][삭제] 버튼 표시
   - ClinicRecord는 버튼 없이 조회만
   - `editable: false`일 때 버튼 숨김
   - 참고: 섹션 2.22-23, 6.7 (버튼 조건부 렌더링)

### Phase 7: 수정/삭제 액션

**참고**: 섹션 2.32-36 (Functional - API 활용), 섹션 5.5 (TDD - Modal interactions)

1. **삭제 기능**

   - 삭제 버튼 클릭 시 확인 다이얼로그
   - `DELETE /api/v1/shared-lessons/{id}` 호출
   - `DELETE /api/v1/personal-lessons/{id}` 호출
   - 참고: 섹션 2.33-34, 2.35-36 (API 엔드포인트)
   - 성공 시 캘린더 재조회 (React Query invalidate)
   - 참고: 섹션 5.5 (삭제 확인 다이얼로그 → API 호출 → 재조회 플로우)

2. **수정 기능**

   - 수정 버튼 클릭 시 모달 내 편집 폼 또는 인라인 수정
   - `PATCH /api/v1/shared-lessons/{id}` 호출
   - `PATCH /api/v1/personal-lessons/{id}` 호출
   - 참고: 섹션 2.33, 2.35 (API 엔드포인트)
   - 성공 시 캘린더 재조회
   - 참고: 섹션 5.5 (수정 API 호출 후 성공/실패 처리)

3. **에러 처리**
   - API 실패 시 Toast 또는 Alert로 에러 메시지
   - 403 권한 에러 처리
   - 네트워크 에러 처리
   - 참고: `frontend/src/lib/api-error.ts` (getApiErrorMessage, getFetchError)

### Phase 8: 모바일 반응형

**참고**: 섹션 2.45-49 (Non-functional - 모바일), 섹션 6.9 (Mobile Considerations)

1. **모바일 레이아웃 조정**

   - 단일 컬럼 스택 레이아웃
   - 캘린더 셀 높이 100px로 축소
   - Typography/spacing 조정
   - 참고: 섹션 6.9 (모바일 레이아웃 상세)

2. **Bottom Sheet 모달**

   - `@media (max-width: 768px)` 기준 분기
   - 화면 하단 고정 + 슬라이드업 애니메이션
   - Dimmed overlay 적용
   - 참고: 섹션 2.46-48 (Bottom sheet 요구사항)
   - 참고: 섹션 6.9 (모바일 모달 = bottom sheet)

3. **범례 Accordion**

   - 모바일에서 접힘 상태 기본값
   - 탭으로 펼치기/접기
   - 참고: 섹션 2.14, 6.4 (모바일 Accordion 요구사항)

4. **포커스 스타일**
   - 날짜 셀 focus-visible outline
   - 키보드 네비게이션 확인 (Tab 순서)
   - 참고: 섹션 2.49 (키보드 네비게이션 요구사항)
   - 참고: 섹션 6.9 (focus-visible outline 명확히 표시)

### Phase 9: 최종 검증 및 문서화

**참고**: 섹션 5 (TDD Plan), 섹션 6.10 (UX Flow Recap)

1. **통합 테스트 시나리오**

   - 학생 검색 → 선택 → 캘린더 로드
   - 월 이동 → 데이터 재조회
   - 날짜 클릭 → 모달 열기 → 수정/삭제
   - 에러 상태 → Retry
   - 모바일 레이아웃 확인
   - 참고: 섹션 5 전체 (각 TDD Plan 항목)
   - 참고: 섹션 6.10 (UX Flow 1-5단계)

2. **빌드 최종 검증**

   - `npm run build -- --webpack` 실행
   - 타입 에러 0개, 빌드 성공 확인
   - 참고: 섹션 5.8 (Regression checklist)
   - 참고: `CLAUDE.md`의 "프론트엔드 테스트 & 검증 프로세스"

3. **문서화**
   - `docs/history/AGENT_LOG.md`에 구현 내용 기록
   - 테스트 시나리오 결과 기록
   - 알려진 제약사항 또는 추후 개선사항 메모
   - 참고: 섹션 5.8 (AGENT_LOG 기록 항목)

---

### 단계별 체크포인트

각 Phase 완료 시 다음을 확인:

- [ ] 해당 Phase의 모든 항목 구현 완료
- [ ] `npm run build -- --webpack` 빌드 성공
- [ ] 수동 테스트로 기능 동작 확인
- [ ] 다음 Phase로 진행 전 사용자에게 진행 상황 보고
