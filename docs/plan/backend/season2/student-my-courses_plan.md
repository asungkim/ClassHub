# Feature: Student My Courses (Assignment 기반)

## 1. Problem Definition
- 기존 학생 수업 목록은 StudentCourseEnrollment 기반이었지만 Enrollment 도메인을 제거했다.
- 학생이 자신의 반 목록을 확인할 수 있도록 StudentCourseAssignment 기반 API로 대체해야 한다.

## 2. Requirements
### Functional
- 학생 본인의 반 목록을 조회한다.
- 반 목록은 StudentCourseAssignment 기준으로 구성한다.
- 각 항목에 Course 요약 정보와 StudentCourseRecord id를 포함한다.
- 휴원(assignmentActive=false)도 목록에 포함한다.
- 폐강(soft delete)된 Course는 제외한다.

### Non-functional
- StudentCourseAssignment-StudentCourseRecord 매핑이 없는 경우를 고려한다.
- 응답은 `RsData` + `PageResponse` 형식으로 유지한다.

## 3. API Design (Draft)
- `GET /api/v1/students/me/courses`
  - Query: 없음 (휴원/재원 모두 반환)
  - Response: Page of
    - `assignmentId`, `assignedAt`, `assignmentActive`
    - `recordId`
    - `course` (CourseResponse)

## 4. Domain Model (Draft)
- StudentCourseAssignment: studentMemberId, courseId, assignedAt, deletedAt
- StudentCourseRecord: studentMemberId, courseId, recordId
- Course: deletedAt 기준으로 비활성 제외

## 5. TDD Plan
1. Repository/Query
   - 학생 기준 Assignment 조회 (status 필터 포함)
   - Course deletedAt 제외
2. Service
   - Assignment ↔ Course/Record 매핑
   - recordId 없는 케이스 처리
3. Controller
   - status 파라미터 파싱 및 권한 검증

## 6. Implementation Steps
1. StudentCourseAssignment 기반 조회 쿼리/DTO 정의
2. Service 응답 조합 및 Course/Record 매핑
3. Controller 엔드포인트 구현
4. 기존 프론트 `내 수업` 페이지 응답 스키마 변경
