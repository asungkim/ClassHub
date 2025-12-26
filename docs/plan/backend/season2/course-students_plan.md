# Feature: Course 학생 목록 조회 (진도 작성용)

## 1. Problem Definition
- 통합 수업 작성 모달에서 선택한 반에 속한 재원 학생(활성 배정)을 불러오는 API가 없다.
- 기존 `/api/v1/student-courses` 목록 API가 제거되어 프론트에서 학생/recordId를 조회할 수 없다.

## 2. Requirements
### Functional
- 반(courseId)에 배정된 학생을 **재원/휴원 모두 조회**한다.
- Teacher 권한만 조회 가능하며, 본인 소유 반만 접근 가능해야 한다.
- 응답에는 개인 진도 작성에 필요한 `recordId`와 학생 요약 정보, **재원 여부(assignmentActive)**가 포함되어야 한다.
- 페이지네이션을 지원한다.

### Non-functional
- N+1 조회를 피하고, recordId 매핑이 누락되지 않도록 한다.
- 기존 진도 작성 API와 연동에 문제가 없도록 응답 스키마를 안정적으로 유지한다.

## 3. API Design (Draft)
- `GET /api/v1/courses/{courseId}/students`
  - Query: `page`, `size`
  - Response: `RsData<PageResponse<CourseStudentResponse>>`

`CourseStudentResponse` (draft)
- `recordId` (UUID)
- `assignmentActive` (boolean)
- `student` (`StudentSummaryResponse`)

## 4. Domain Model (Draft)
- `StudentCourseAssignment` (active/inactive) → 수강 상태 포함 목록의 기준
- `StudentCourseRecord` (deletedAt null) → 개인 진도 생성용 recordId 제공
- `Member`, `StudentInfo` → 학생 요약 정보 구성

## 5. TDD Plan
1. Service 테스트: 특정 courseId에 대해 active/inactive assignment 모두 포함하고 recordId+학생 요약+assignmentActive가 반환되는지 검증한다.
2. Controller 테스트: Teacher 권한으로 호출 시 PageResponse 형식이 내려오는지 확인한다.
3. (필요 시) Repository 쿼리 테스트: courseId + active assignment 조회가 정확한지 확인한다.
