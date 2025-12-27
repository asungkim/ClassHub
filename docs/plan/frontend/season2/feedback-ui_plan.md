# Feature: Feedback UI (전역 작성 + 관리자)

## 1. Problem Definition
- 선생님/조교/학생이 **모든 화면**에서 즉시 피드백을 제출할 수 있는 진입점이 필요하다.
- 관리자는 피드백을 목록으로 보고 해결 상태를 변경해야 한다.
- 해결된 피드백은 작성자가 **대시보드(메인 페이지)**에서 즉시 확인할 수 있어야 한다.
- 현재 프런트에는 피드백 제출/조회/해결 UI가 없어 사용자와 관리자가 모두 상태를 알 수 없다.

## 2. User Flows & Use Cases

### 공통 작성자(Teacher/Assistant/Student)
1. 어떤 화면에서든 오른쪽 아래의 **동그란 `?` 버튼**을 클릭한다.
2. 메모장 형태의 피드백 패널(모달)이 열리고 내용을 입력한다.
   - 비어 있으면 제출 불가(버튼 비활성 + 에러 텍스트 표시).
   - 2000자 초과 시 경고 표시(프런트에서 제한).
3. 제출 성공 시 토스트로 안내되고 패널이 닫힌다.
4. 대시보드에서 본인 피드백 목록이 `미해결 → 해결됨`으로 업데이트된다.

### SuperAdmin
1. 사이드바에서 `피드백 관리` 메뉴 진입 (`/admin/feedback`).
2. 상태 필터(ALL/SUBMITTED/RESOLVED)를 선택해 목록 조회.
3. SUBMITTED 행의 `해결 처리` 버튼을 누르면 확인 모달이 열린다.
4. 확인 시 해결 처리 API 호출 → 상태와 해결 시각/해결자 반영.

### 대시보드 알림(작성자)
1. `/teacher`, `/assistant`, `/student` 대시보드 진입 시 본인 피드백이 있으면 리스트 요약을 노출한다.
2. 각 피드백은 `미해결` 또는 `해결됨` 상태 배지로 표시된다.
3. 관리자 해결 처리 후 재조회 시 상태가 `해결됨`으로 바뀐다.

## 3. Page & Layout Structure

### 3.1 전역 피드백 런처(모든 화면)
- 위치: 화면 오른쪽 아래 고정 원형 버튼(`?` 아이콘), 모든 페이지에 노출.
- 동작: 클릭 시 메모장 형태의 패널(Modal) 열림.
- 패널 구성:
  - 제목/설명(“건의사항을 남겨주세요”)
  - 텍스트 영역(멀티라인), 글자 수 표시
  - 제출 버튼(로딩/비활성 상태)
- Mobile: 하단 여백 확보(사이드바가 없는 화면에서도 FAB 겹침 최소화).

### 3.2 관리자 피드백 페이지 (`/admin/feedback`)
- 상단: 타이틀/설명.
- 필터: 상태 탭(ALL/SUBMITTED/RESOLVED).
- 목록: 테이블(상태 배지, 작성자, 이메일, 전화번호, 내용 요약, 작성일, 해결일, 액션).
- 행 액션: `해결 처리` 버튼 + `ConfirmDialog`.
- Mobile: 테이블은 가로 스크롤 허용.
- 관리자 사이드바에 `피드백 관리` 메뉴를 추가해 접근 경로를 제공한다.

### 3.3 작성자 대시보드 피드백 요약
- Teacher/Assistant/Student 대시보드에 “내 피드백” 카드(또는 섹션) 추가.
- 최근 N건(예: 3건)만 노출하고 상태 배지를 표시한다.
- 상태는 `미해결`/`해결됨`으로 표기하며, 해결 시 자동 갱신된다.

## 4. Component Breakdown

### 공통 UI 재사용 (신규 컴포넌트 추가 없이 기존 컴포넌트 사용)
- **Card / Button / Badge / Tabs / Table / Skeleton / InlineError / EmptyState / ConfirmDialog / Modal**를 그대로 활용.
- 텍스트 입력은 **기존 TextField + native `<textarea>` 스타일**로 구성해 새 UI 컴포넌트 추가를 피한다.

### 전역 피드백 런처(앱 공통 UI 블록)
- **왜 필요한지**: 모든 화면에서 즉시 피드백을 남길 수 있는 단일 진입점이 필요하다.
- **어떻게 동작하는지**: 우측 하단 `?` 버튼 클릭 → Modal(메모장) 열림 → 제출 시 토스트/닫힘.
- **어디에 붙는지**: `AppChrome`(전역 레이아웃) 내부에 고정 버튼과 Modal을 배치해 모든 페이지에 노출.

### 새 훅(로직 전용)
- **왜 필요한지**: 대시보드/관리자 목록/전역 제출 로직이 분산되면 상태 동기화가 어려워진다.
- **어떻게 동작하는지**: `status`, `page`, `size`를 입력받아 `api.GET` 호출 → 로딩/에러/데이터 상태를 반환하고 `refresh()`를 제공한다.
- **어디에 붙는지**: `/admin/feedback`, 대시보드 요약 섹션에서 공통 사용.

예: `useFeedbackList`, `useFeedbackSummary` (UI 컴포넌트가 아닌 로직 훅)

## 5. State & Data Flow

### API 계약 (OpenAPI 타입 사용 필수)
- `POST /api/v1/feedback` → `components["schemas"]["FeedbackCreateRequest"]`
- `GET /api/v1/feedback/me` → `components["schemas"]["PageResponseFeedbackResponse"]`
- `GET /api/v1/feedback` → `components["schemas"]["PageResponseFeedbackResponse"]`
- `PATCH /api/v1/feedback/{id}/resolve` → `components["schemas"]["FeedbackResponse"]`

> 현재 `frontend/src/types/openapi.d.ts`에 Feedback 스키마가 없다면 `openapi.json` 갱신 후 `npm run generate:types`로 타입을 추가한다.

### 상태 분리
- **전역 런처(메모장 패널)**
  - `isOpen`, `formContent`, `formError`, `isSubmitting`
- **관리자 페이지**
  - `statusFilter`, `page`, `size`
  - `items`, `totalElements`, `isLoading`, `listError`
  - `resolveTarget`, `isResolving`, `resolveError`
- **대시보드 요약 섹션**
  - `items`, `isLoading`, `error`

### 데이터 흐름
1. 전역 런처에서 제출 성공 시 토스트 표시 → 패널 닫기.
2. 제출 성공 시 `feedback:created` 커스텀 이벤트를 발행하고 대시보드 요약 훅에서 수신해 재조회한다.
3. 관리자 해결 처리 성공 시 현재 목록 재조회.
4. 대시보드 요약은 `GET /feedback/me?page=0&size=N`으로 최근 N건을 가져와 상태를 표시한다.

### 에러 처리
- `getFetchError`, `getApiErrorMessage`로 에러 메시지 추출.
- 목록 실패: `InlineError` 또는 `EmptyState`에 에러 안내.
- 제출 실패: 폼 하단 에러 메시지.

## 6. Interaction & UX Details
- **입력 검증**: 공백 제외 1자 이상일 때만 제출 가능, 2000자 초과 시 버튼 비활성 + 경고.
- **로딩 상태**: 제출 버튼 로딩 상태, 목록은 Skeleton 렌더.
- **상태 표시**: `Badge`로 SUBMITTED/RESOLVED 색상 구분.
- **전역 버튼**: 오른쪽 하단 고정 원형 버튼(`?`)은 hover 시 강조, 모바일에서는 안전 영역을 고려해 여백 확보.
- **모달**: 관리자 해결 처리 시 ConfirmDialog, 키보드 포커스/ESC 지원(기존 Modal 기능 활용).
- **접근성**: 버튼/탭은 키보드로 포커스 이동 가능, 텍스트 영역은 `aria-label` 제공.

## 7. Test & Verification Plan

### 타입/빌드 검증
- `cd frontend && npm run build -- --webpack`

### 수동 QA 체크리스트
- 작성자(Teacher/Assistant/Student)
  - 어떤 화면에서든 `?` 버튼 표시/클릭 가능
  - 피드백 작성 성공/실패
  - 대시보드 요약 리스트에서 미해결/해결됨 상태 표시 확인
- 관리자(SuperAdmin)
  - 목록 필터 전환
  - 해결 처리 확인 모달/상태 변경
  - 작성자 이메일/전화번호 표시 확인

---

### 계획 요약 (한국어)
- 모든 화면 우측 하단의 `?` 버튼으로 피드백을 제출하고, 관리자용 피드백 관리 페이지(목록 + 해결 처리)와 작성자 대시보드 요약 리스트를 추가한다.
- 기존 UI 컴포넌트를 재사용하고, 로직은 공통 훅으로 분리해 중복을 줄인다.
- OpenAPI 타입을 기반으로 데이터 계약을 유지하며, 누락된 타입은 생성 작업 후 반영한다.
