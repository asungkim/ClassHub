# Feature: Student Multi-Course Enrollment UI

## 1. Problem Definition

- Teacher 대시보드의 학생 등록/수정 화면은 현재 단일 `courseId`만 처리해 백엔드 다중 수강 구조(`courseIds`, `enrolledCourses`)와 맞지 않는다.
- 목록 페이지에서도 `StudentProfileSummary.courseNames[]`가 아닌 단일 `courseName` 필드를 사용해 잘못된 정보를 보여주고 있다.
- 프런트엔드 폼·훅·컴포넌트 전반을 새 OpenAPI 스키마에 맞게 개편하고, 사용자가 여러 반을 직관적으로 선택/수정할 수 있어야 한다.

## 2. Requirements

### Functional

1. **Student 생성 화면 (`/dashboard/students/new`)**
   - CoursePicker를 다중 선택 UI로 확장하거나 동일 스타일의 Multi Picker를 도입해 여러 반을 토글 선택할 수 있게 한다.
   - 최소 1개 이상 선택 시 폼 제출 가능, 선택 상태/갯수 표시 및 삭제(선택 해제) 경험을 제공한다.
   - `StudentProfileCreateRequest` 전송 시 `courseIds: string[]`로 변환한다.

2. **Student 수정 화면 (`/dashboard/students/[id]/edit`)**
   - 상세 API 응답(`StudentProfileResponse.enrolledCourses[]`)을 기반으로 초기 선택 상태를 세팅한다.
   - 수정 폼 제출 시 `courseIds?: string[]`을 포함해 추가/삭제된 반 목록이 서버와 동기화되도록 한다.
   - `enrolledCourses`가 비어 있을 때는 안내 문구를 표시하고 선택을 유도한다.

3. **학생 목록 페이지 (`/dashboard/students`)**
   - `StudentProfileSummary.courseNames[]`를 사용해 다중 반을 표시한다. Desktop 테이블에서는 최대 2개 + `외 n개` 요약, Mobile 카드에서는 배지 리스트 등을 활용한다.
   - 기존 정렬/필터/토글 기능은 유지한다.

4. **공용 훅 / 컴포넌트**
   - `useCreateStudentProfile`, `useUpdateStudentProfile`, `useStudentProfileDetail` 훅의 타입과 payload를 새로운 스키마에 맞게 업데이트한다.
   - `CoursePicker`를 다중 선택으로 개선하거나 `CourseMultiPicker` 같은 재사용 가능한 컴포넌트를 도입하여 선택 상태/검증/에러 UX를 일관성 있게 유지한다.

5. **검증/피드백**
   - 필수 입력 검증 시 `courseIds.length === 0`이면 오류 메시지를 보여준다.
   - 성공 시 기존 라우팅/토스트 흐름 유지, 실패 시 `getApiErrorMessage` 메시지를 갱신한다.

### Non-functional

- 기존 Tailwind/공통 UI 컴포넌트(TextField, Button, Select 등)를 재사용한다. Course 선택 UI도 가능하면 기존 구조 확장으로 해결한다.
- React Query 캐시 키(`["student-profile", id]`, `["student-profiles", filters]`)는 그대로 두되, 응답 파싱 로직을 새 필드에 맞춰 정비한다.
- 접근 제어(`useRoleGuard`)와 form 상태 관리는 현재 방식(`useState`)을 유지한다.
- 구현 후 `cd frontend && npm run build -- --webpack`으로 타입/빌드 검증을 수행해야 한다.

## 3. API Design (Draft)

| Flow | Method | URL | Request | Response / Notes |
| --- | --- | --- | --- | --- |
| 학생 생성 | POST | `/api/v1/student-profiles` | `StudentProfileCreateRequest` (`courseIds: string[]`, 기타 필드 동일) | `StudentProfileResponse` (`enrolledCourses[]` 포함) |
| 학생 수정 | PATCH | `/api/v1/student-profiles/{id}` | `StudentProfileUpdateRequest` (`courseIds?: string[]`) | `StudentProfileResponse` |
| 학생 상세 | GET | `/api/v1/student-profiles/{id}` | - | `StudentProfileResponse` (`enrolledCourses[]`) |
| 학생 목록 | GET | `/api/v1/student-profiles` | 기존 필터 + `courseNames[]`가 채워진 `StudentProfileSummary` | React Query 캐시 구조 유지 |

## 4. Domain Model (Draft)

- **UI 상태**
  - `selectedCourseIds: string[]`를 Create/Edit 폼 state로 관리한다.
  - CoursePicker는 `selectedCourseIds` 배열과 비교해 카드 활성화 상태를 렌더링하고, 클릭 시 토글한다.
  - 목록 페이지는 `courseNames: string[]`를 받아 `renderCourseNames(courseNames)` 헬퍼로 요약 문자열/배지를 생성한다.

- **데이터 매핑**
  - Detail 응답 `enrolledCourses` → `selectedCourseIds` (courseId only) / 별도 뱃지 표시에 사용할 수 있도록 courseName도 유지.
  - Summary 응답 `courseNames[]` → UI에 표시. 빈 배열이면 "-" 처리.

## 5. TDD Plan

1. **타입/빌드 검증**
   - 모든 수정 후 `cd frontend && npm run build -- --webpack` 실행, TypeScript 에러 0개 확인.
2. **수동 시나리오 테스트**
   - (1) 학생 등록: 2개 이상의 반 선택 → 등록 성공 후 목록에서 course 배지가 여러 개 노출되는지 확인.
   - (2) 학생 수정: 기존 학생 불러온 뒤 특정 반 추가/해제 → 저장 → 목록/상세에서 변경 사항 반영 확인.
   - (3) 검증 에러: course 미선택 상태로 제출 시 오류 메시지 확인.
3. **회귀 체크**
   - 학생 목록 필터/페이지 이동, 활성/비활성 토글이 기존과 동일하게 동작하는지 확인.

