# Feature: 선생님 학생 관리 UI (신청 처리/학생 목록/상세/반 배치)

## 1. Problem Definition
- 기존 수업 신청 기반 플로우에서 선생님-학생 연결 및 반 배치/휴원·재원 처리를 한 화면에서 수행해야 한다.
- 선생님은 연결 요청을 빠르게 승인/거절하고, 연결된 학생 목록을 관리하며, 반 배치 및 휴원/재원 상태를 상세 모달에서 제어해야 한다.

## 2. User Flows & Use Cases

### 2.1 신청 처리 탭 (학생 → 선생님 연결 요청 관리)
1. `/teacher/students` 진입 → 신청 처리 탭 선택
2. 기본 상태: `PENDING` 요청만 노출
3. 요청 카드/행에서 학생 요약 정보 확인 후 단건 승인/거절
4. 여러 건 체크 → 일괄 승인/거절
5. 처리 후 목록 자동 새로고침

### 2.2 학생 목록 탭
1. `/teacher/students` 기본 탭(학생 목록)
2. 반 필터 선택(선택사항) + 학생 이름 검색
3. 학생 목록 로드 → 학생 클릭 시 상세 모달 열림

### 2.3 학생 상세 모달
1. 학생 기본정보 노출
2. 반 카드 리스트 확인(정렬: `startDate desc` → `createdAt desc`)
3. 카드 상단에서 휴원/재원 토글
   - 종료된 반은 토글 비활성화
   - 삭제된 반은 read-only
4. 카드 하단에서 수업 기록 수정 버튼

### 2.4 반 배치 탭
1. 반 배치 탭 진입
2. 반 목록(종료되지 않은 반) 선택
3. 배치 후보 학생 목록 로드
4. 체크박스로 학생 선택 → 일괄 배치
5. 완료 후 목록 갱신

## 3. Page & Layout Structure

### 3.1 공통 구조
- 경로: `/teacher/students`
- 상단 헤더 + 탭 구조 재사용
  - 탭: `학생 목록(기본)` / `신청 처리` / `반 배치`
- 기존 `StudentManagementView` 레이아웃 유지

### 3.2 신청 처리 탭
- 상단: 상태 필터(기본 PENDING), 검색(학생 이름)
- 중단: 요청 목록 테이블
- 하단: 일괄 처리 버튼 영역

### 3.3 학생 목록 탭
- 상단: 반 필터 + 검색 입력
- 중단: 학생 목록 테이블
- 상세: 학생 상세 모달

### 3.4 학생 상세 모달
- 상단: 학생 기본 정보 영역
- 중단: 반 카드 리스트
  - 각 카드에 상태 배지 + 휴원/재원 토글 + 기록 수정 버튼
- 하단: 닫기 버튼

### 3.5 반 배치 탭
- 상단: 반 선택 + 검색 입력
- 중단: 배치 후보 학생 목록 테이블 + 체크박스
- 하단: “일괄 배치” 버튼

## 4. Component Breakdown

### 4.1 StudentManagementView (기존 컴포넌트 수정)
- **왜 필요한지**: 탭 기반 학생 관리 흐름을 한 페이지에서 제공
- **어떻게 동작하는지**: 탭 상태 관리 후 각 탭 컴포넌트 렌더
- **어디에 붙는지**: `/teacher/students` 페이지

### 4.2 RequestsTab (기존 컴포넌트 수정)
- **왜 필요한지**: 연결 요청 일괄 처리
- **어떻게 동작하는지**: 요청 목록 로딩 → 체크 상태 관리 → 승인/거절 액션
- **어디에 붙는지**: `StudentManagementView`의 “신청 처리” 탭
- 사용 컴포넌트: `Card`, `Tabs`, `Table`, `Checkbox`, `Badge`, `Button`, `ConfirmDialog`, `InlineError`, `EmptyState`

### 4.3 StudentsTab (기존 컴포넌트 수정)
- **왜 필요한지**: 연결된 학생 목록과 상세 진입
- **어떻게 동작하는지**: 필터/검색 → 목록 렌더 → 학생 클릭 시 상세 모달 오픈
- **어디에 붙는지**: `StudentManagementView`의 “학생 목록” 탭
- 사용 컴포넌트: `Card`, `Select`, `Input`, `Table`, `Badge`, `Modal`, `Button`

### 4.4 StudentDetailModal (기존 컴포넌트 수정)
- **왜 필요한지**: 학생의 반 배치 및 휴원/재원 상태를 한 화면에서 관리
- **어떻게 동작하는지**: 상세 데이터 로드 → 카드 리스트 렌더 → 토글/기록 수정 액션 처리
- **어디에 붙는지**: `StudentsTab` 내부 모달
- 사용 컴포넌트: `Modal`, `Card`, `Badge`, `Button`, `InlineError`, `Skeleton`

### 4.5 BatchAssignmentTab (새 탭 컴포넌트, 페이지 레벨)
- **왜 필요한지**: 다수 학생을 한 번에 반 배치
- **어떻게 동작하는지**: 반 선택 → 후보 로딩 → 체크 → 배치 실행
- **어디에 붙는지**: `StudentManagementView`의 “반 배치” 탭
- 사용 컴포넌트: `Card`, `Select`, `Input`, `Table`, `Checkbox`, `Button`, `InlineError`, `EmptyState`

> 공통 UI 컴포넌트 조합으로 구현 가능하며, 신규 공통 컴포넌트 추가는 계획하지 않는다.

## 5. State & Data Flow

### 5.1 공통 상태
- 탭 상태: `students | requests | batch`
- 페이지네이션: `page`, `size`, `totalElements`
- 로딩/에러: `loading`, `error`
- 검색 입력: `keywordInput` + 적용값 `appliedKeyword`

### 5.2 신청 처리 탭
- 요청 목록: `TeacherStudentRequest[]`
- 체크 상태: `selectedRequestIds: Set<string>`
- 승인/거절 액션 상태: `actionLoading`
- API
  - `GET /api/v1/teacher-student-requests?status=&keyword=&page=&size=`
  - `PATCH /api/v1/teacher-student-requests/{requestId}`
  - `PATCH /api/v1/teacher-student-requests/batch`
- 타입: `paths`/`components` 기반 타입 alias 사용 (openapi.d.ts)

### 5.3 학생 목록 탭
- 목록: `StudentSummaryResponse[]`
- 반 필터: `courseId`
- API
  - `GET /api/v1/teacher-students?courseId=&keyword=&page=&size=`
  - `GET /api/v1/teacher-students/{studentId}`

### 5.4 학생 상세 모달
- 상세 데이터: `{ student, courses[] }`
- 토글 상태: `assignmentActive` 기준
- API
  - `PATCH /api/v1/student-course-assignments/{assignmentId}/activate`
  - `PATCH /api/v1/student-course-assignments/{assignmentId}/deactivate`
  - `GET /api/v1/student-courses/{recordId}`
  - `PATCH /api/v1/student-courses/{recordId}`

### 5.5 반 배치 탭
- 반 목록: `CourseResponse[]`
- 후보 학생: `StudentSummaryResponse[]`
- 체크 상태: `selectedStudentIds: Set<string>`
- API
  - `GET /api/v1/courses/assignable?branchId=&keyword=&page=&size=`
  - `GET /api/v1/courses/{courseId}/assignment-candidates?keyword=&page=&size=`
  - `POST /api/v1/student-course-assignments`

> 초기 구현은 단건 API를 체크된 학생 수만큼 순차 호출한다.

## 6. Interaction & UX Details
- 검색 입력은 입력 상태와 적용 상태를 분리하여 커서 이동 문제를 방지한다.
- PENDING 이외 요청은 체크박스 비활성화 및 배지로 상태 표시
- 토글 비활성 조건
  - `course.endDate < today` → 토글 비활성 + 안내 텍스트
  - `course.active=false` → 카드 read-only 처리
- 빈 상태 메시지
  - 신청 처리: “대기 중인 요청이 없습니다.”
  - 학생 목록: “연결된 학생이 없습니다.”
  - 반 배치: “배치 가능한 학생이 없습니다.”
- 에러 처리
  - `getApiErrorMessage` 사용하여 토스트/인라인 에러 노출

## 7. Test & Verification Plan
- 타입 검증: `npm run build -- --webpack`
- 수동 QA
  - `/teacher/students` 접속 → 탭 전환
  - 신청 처리: PENDING 목록 조회 → 단건/일괄 승인/거절
  - 학생 목록: 반 필터/검색 → 상세 모달 → 토글 동작
  - 반 배치: 반 선택 → 후보 목록 → 일괄 배치
- 빈 상태/에러 상태 확인 (목록 0건, API 실패)
- 반응형 확인 (모바일/데스크탑)

## 8. 구현 단계 (3단계)
1. **신청 처리 탭 리워크**
   - 요청 목록/필터/검색 UX 반영
   - 단건 승인/거절, 체크박스 + 일괄 처리
2. **학생 목록 + 상세 모달 연동**
   - `/api/v1/teacher-students` 목록 연결
   - 상세 모달 데이터 매핑 + 휴원/재원 토글 + 기록 수정 연결
3. **반 배치 탭 추가**
   - 반 목록/후보 학생 로딩 UI 구성
   - 체크된 학생에 대해 단건 배치 API 순차 호출
