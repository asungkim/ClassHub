# Feature: Course Management UI

## 1. Problem Definition
- Teacher가 자신의 Course(반) 목록을 보고, 생성/수정/활성/비활성 전환을 수행할 수 있는 UI가 필요하다.
- 현재 백엔드 API만 존재하므로 프론트에서는 Course 대시보드 화면, 폼, 상태 필터 등을 구현해야 한다.
- **기존 조교/학생 목록 UI 패턴을 최대한 재활용**하여 일관된 사용자 경험을 제공한다.

## 2. Requirements

### 2.1 Functional Requirements

#### 목록 조회 & 필터링
- **경로**: `/dashboard/teacher/courses`
- **권한**: Teacher only (`useRoleGuard("TEACHER")` 사용)
- **필터**:
  - Tabs 컴포넌트로 전체/활성/비활성 전환
  - 조교 목록의 FilterChip 패턴 참고 (Tabs로 구현)
- **Empty State**:
  - "아직 생성된 반이 없습니다" 메시지
  - "첫 반 만들기" 버튼 (→ 생성 Modal 열기)
- **Loading**: Skeleton 컴포넌트 표시
- **Error**: ErrorState 컴포넌트 + "다시 시도" 버튼

#### Course 생성/수정
- **생성 트리거**:
  - Empty State의 "첫 반 만들기" 버튼
  - 목록 화면 우측 상단 "새 반 만들기" 버튼 (Button variant="default")
- **수정 트리거**:
  - Course 카드 내부의 "수정" 버튼 (Button variant="ghost")
- **UI 형태**: Modal (새로 구현 필요)
- **폼 필드**:
  1. **반 이름**: TextField
     - label: "반 이름"
     - placeholder: "예: 중등 수학 A반"
     - required, max 100자
  2. **학원/회사**: TextField
     - label: "소속 학원/회사"
     - placeholder: "예: ABC 학원"
     - required, max 100자
  3. **수업 요일/시간표 입력**:
     - 요일 선택 토글(UI 버튼)로 원하는 요일 활성화
     - 선택된 각 요일마다 TimeSelect 2개(시작/종료)를 제공해 서로 다른 시간 설정 지원
     - Validation: 요일 중복 불가, 각 요일에서 시작 < 종료
- **Validation 규칙**:
  - 반 이름: required, max 100자
  - 회사: required, max 100자
  - 요일: required, 최소 1개
  - 시작 시간: required, HH:mm 형식
  - 종료 시간: required, startTime보다 이후

#### 활성/비활성 토글
- **위치**: Course 카드 우측에 토글 버튼 (조교/학생 목록 패턴)
- **상태 표시**: Badge 컴포넌트 (활성: primary, 비활성: gray)
- **동작**:
  1. 토글 버튼 클릭
  2. 즉시 API 호출 (`PATCH /api/v1/courses/{id}/activate` or `/deactivate`)
  3. 성공 시: React Query invalidation으로 목록 즉시 갱신
  4. 실패 시: InlineError 또는 Toast로 에러 메시지 표시
  5. 로딩 중: 버튼 disabled + Spinner

#### Course 카드 표시 정보
- **카드 헤더**:
  - 반 이름 (font-semibold, text-lg)
  - 활성/비활성 Badge
- **카드 바디**:
  - 학원/회사명
  - 수업 요일 (예: "월, 수, 금")
  - 수업 시간 (예: "14:00 - 16:00")
  - 생성일 (작은 글씨, optional)
- **카드 액션**:
  - 수정 버튼 (Button variant="ghost")
  - 토글 버튼 (활성화/비활성화)

#### 반응형 레이아웃
- **>= 1280px**: 3단 그리드 (`grid-cols-3`)
- **768px ~ 1279px**: 2단 그리드 (`grid-cols-2`)
- **< 768px**: 1단 리스트 (`grid-cols-1`)

### 2.2 Non-functional Requirements

#### 재사용 컴포넌트 (이미 존재)
- ✅ Button (`@/components/ui/button`)
- ✅ Card (`@/components/ui/card`)
- ✅ TextField (`@/components/ui/text-field`)
- ✅ Select (`@/components/ui/select`)
- ✅ Tabs (TabsList, TabsTrigger, TabsContent) (`@/components/ui/tabs`)
- ✅ EmptyState (`@/components/shared/empty-state`)
- ✅ ErrorState (`@/components/ui/error-state`)
- ✅ LoadingSkeleton (`@/components/shared/loading-skeleton`)
- ✅ Badge (`@/components/ui/badge`)
- ✅ DashboardShell (`@/components/dashboard/dashboard-shell`)

#### 새로 구현할 컴포넌트 (사용자 승인 필요)
1. **Modal/Dialog** (`@/components/ui/modal`)
   - **용도**: Course 생성/수정 폼 컨테이너
   - **기능**:
     - Overlay + Portal
     - 닫기 버튼 (X)
     - ESC 키로 닫기
     - Focus trap
     - 접근성 (role="dialog", aria-labelledby, aria-modal)
   - **props**: `open`, `onClose`, `title`, `children`, `size`

2. **TimeSelect** (`@/components/ui/time-select`)
   - **용도**: HH:mm 형식 시간 입력
   - **구현**: Select 2개 조합 (시, 분)
   - **props**: `label`, `value` (string "HH:mm"), `onChange`, `error`, `required`

#### 타입 & API
- **타입 기반**: `frontend/src/types/openapi.d.ts`
  ```typescript
  type CourseResponse = components["schemas"]["CourseResponse"];
  type CourseCreateRequest = components["schemas"]["CourseCreateRequest"];
  type CourseUpdateRequest = components["schemas"]["CourseUpdateRequest"];
  ```
- **API 클라이언트**: `frontend/src/lib/api.ts` 사용
- **에러 처리**: `getApiErrorMessage` 헬퍼 사용

#### 상태 관리 (React Query Hooks)
- `useCourses(isActive?: boolean)` - 목록 조회
- `useCreateCourse()` - 생성 mutation
- `useUpdateCourse()` - 수정 mutation
- `useActivateCourse()` - 활성화 mutation
- `useDeactivateCourse()` - 비활성화 mutation

#### 접근성
- 모든 폼 필드에 label 필수
- Checkbox 그룹에 fieldset + legend
- Modal focus trap
- 키보드 네비게이션 (Tab, ESC)

## 3. API 스펙 (백엔드 기준)

### 3.1 목록 조회
**GET** `/api/v1/courses?isActive={boolean}`

**Query Parameters**:
- `isActive` (optional): true/false/없음

**Response** (200):
```json
{
  "code": 1000,
  "message": "SUCCESS",
  "data": [
    {
      "id": "uuid",
      "name": "중등 수학 A반",
      "company": "ABC 학원",
      "schedules": [
        { "dayOfWeek": "MONDAY", "startTime": "14:00", "endTime": "16:00" },
        { "dayOfWeek": "FRIDAY", "startTime": "16:00", "endTime": "18:00" }
      ],
      "isActive": true,
      "teacherId": "teacher-uuid",
      "createdAt": "2025-12-09T10:00:00",
      "updatedAt": "2025-12-09T10:00:00"
    }
  ]
}
```

### 3.2 생성
**POST** `/api/v1/courses`

**Request Body**:
```json
{
  "name": "중등 수학 A반",
  "company": "ABC 학원",
  "schedules": [
    { "dayOfWeek": "MONDAY", "startTime": "14:00", "endTime": "16:00" },
    { "dayOfWeek": "FRIDAY", "startTime": "16:00", "endTime": "18:00" }
  ]
}
```

**Response** (201):
```json
{
  "code": 1001,
  "message": "생성 성공",
  "data": {
    "id": "uuid",
    "name": "중등 수학 A반",
    "company": "ABC 학원",
      "schedules": [
        { "dayOfWeek": "MONDAY", "startTime": "14:00", "endTime": "16:00" },
        { "dayOfWeek": "FRIDAY", "startTime": "16:00", "endTime": "18:00" }
      ],
    "isActive": true,
    "teacherId": "teacher-uuid",
    "createdAt": "2025-12-09T10:00:00",
    "updatedAt": "2025-12-09T10:00:00"
  }
}
```

### 3.3 수정
**PATCH** `/api/v1/courses/{courseId}`

**Request Body** (모든 필드 optional):
```json
{
  "name": "중등 수학 B반",
  "company": "XYZ 학원",
  "schedules": [
    { "dayOfWeek": "TUESDAY", "startTime": "15:00", "endTime": "17:00" },
    { "dayOfWeek": "THURSDAY", "startTime": "18:00", "endTime": "20:00" }
  ]
}
```

**Response** (200): CourseResponse 동일

### 3.4 비활성화
**PATCH** `/api/v1/courses/{courseId}/deactivate`

**Response** (200):
```json
{
  "code": 1000,
  "message": "SUCCESS",
  "data": null
}
```

### 3.5 활성화
**PATCH** `/api/v1/courses/{courseId}/activate`

**Response** (200):
```json
{
  "code": 1000,
  "message": "SUCCESS",
  "data": null
}
```

## 4. Component 구조

```
CoursesPage (/dashboard/teacher/courses)
├─ DashboardShell (title, subtitle)
├─ FilterBar
│  ├─ Tabs (전체/활성/비활성)
│  └─ Button ("새 반 만들기")
├─ CourseList (conditional)
│  ├─ LoadingSkeleton (loading 시)
│  ├─ ErrorState (error 시)
│  ├─ EmptyState (빈 목록 시)
│  └─ CourseGrid
│     └─ CourseCard[]
│        ├─ 카드 헤더 (이름 + Badge)
│        ├─ 카드 바디 (회사, 요일, 시간)
│        └─ 카드 액션 (수정, 토글)
└─ CourseFormModal (create/edit mode)
   ├─ Modal 래퍼
   └─ Form (react-hook-form + zod)
      ├─ TextField (이름)
      ├─ TextField (회사)
      ├─ CheckboxGroup (요일)
      ├─ TimeSelect (시작 시간)
      ├─ TimeSelect (종료 시간)
      └─ Button (저장/취소)
```

## 5. 구현 단계 (작은 단위로 분할)

### Phase 1: 기반 컴포넌트 구현
1. **Modal 컴포넌트 작성**
   - `frontend/src/components/ui/modal.tsx` 생성
   - Portal, Overlay, Focus trap, 접근성
   - 검증: Storybook 또는 수동 테스트

2. **TimeSelect 컴포넌트 작성**
   - `frontend/src/components/ui/time-select.tsx` 생성
   - Select 2개 조합 (시, 분)
   - HH:mm 포맷 변환 로직
   - 검증: 수동 테스트

### Phase 2: API Hooks 작성
3. **useCourses Hook**
   - `frontend/src/hooks/use-courses.ts` 생성
   - React Query 기반 GET /api/v1/courses
   - isActive 필터 지원
   - 검증: 개발 서버에서 API 호출 확인

4. **Mutation Hooks**
   - useCreateCourse, useUpdateCourse
   - useActivateCourse, useDeactivateCourse
   - 검증: 개발 서버에서 API 호출 확인

### Phase 3: Course 목록 페이지
5. **CoursesPage 기본 구조**
   - `/dashboard/teacher/courses/page.tsx` 생성
   - DashboardShell, FilterBar, 빈 상태
   - 검증: 경로 접근, Teacher 권한 체크

6. **목록 조회 & 필터**
   - useCourses Hook 연결
   - Tabs 필터 동작
   - Loading/Error/Empty 상태 처리
   - 검증: 각 상태별 UI 확인

7. **CourseCard 컴포넌트**
   - 카드 레이아웃 (Card 컴포넌트 사용)
   - 정보 표시 (이름, 회사, 요일, 시간)
   - Badge (활성/비활성)
   - 검증: 반응형 그리드 확인

### Phase 4: 생성/수정 Modal
8. **CourseFormModal 구현**
   - react-hook-form + zod 스키마
   - 모든 필드 구현
   - Validation 로직
   - 검증: 폼 제출, 에러 메시지

9. **생성/수정 연동**
   - "새 반 만들기" 버튼 → Modal 열기
   - "수정" 버튼 → Modal 열기 (기존 데이터 로드)
   - Mutation 성공 시 목록 갱신
   - 검증: 전체 플로우 테스트

### Phase 5: 토글 기능
10. **활성/비활성 토글**
    - CourseCard에 토글 버튼 추가
    - useActivateCourse/useDeactivateCourse 연결
    - 로딩 중 버튼 disabled
    - 성공 시 목록 갱신
    - 검증: 토글 동작, 에러 처리

### Phase 6: 최종 검증 & 기록
11. **타입 검증**
    - `cd frontend && npm run build -- --webpack`
    - 컴파일 에러 0개 확인

12. **수동 시나리오 테스트**
    - Teacher 로그인
    - 목록 조회 (전체/활성/비활성)
    - 반 생성 (성공/실패)
    - 반 수정 (성공/실패)
    - 토글 (활성화/비활성화)
    - Empty State
    - 반응형 (Desktop/Tablet/Mobile)

13. **AGENT_LOG 기록**
    - 구현 내용, 테스트 결과, 사용한 컴포넌트 기록

## 6. 검증 체크리스트

### 타입 검증 (필수)
- [ ] `npm run build -- --webpack` 통과
- [ ] TypeScript 에러 0개

### 기능 테스트 (필수)
- [ ] Teacher 권한으로 접근 가능
- [ ] 목록 조회 (전체/활성/비활성 필터)
- [ ] 반 생성 (성공 시 목록에 추가)
- [ ] 반 수정 (성공 시 목록 갱신)
- [ ] 활성/비활성 토글 (성공 시 즉시 반영)
- [ ] Empty State 표시
- [ ] Loading Skeleton 표시
- [ ] Error State + 재시도

### UI/UX 검증 (필수)
- [ ] 반응형 (Desktop/Tablet/Mobile)
- [ ] Modal 열기/닫기 (ESC, X 버튼)
- [ ] 폼 Validation (에러 메시지)
- [ ] 키보드 네비게이션

### 엣지 케이스 (권장)
- [ ] API 에러 처리
- [ ] 빈 목록 (Empty State)
- [ ] 중복 제출 방지
- [ ] 로딩 중 버튼 disabled

### 문서화 (필수)
- [ ] AGENT_LOG에 구현 기록
- [ ] 테스트 경로 및 결과 기록
