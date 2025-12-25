# Feature: 학생 요청 처리 + 학생 목록/상세 + 반 배치 API (Teacher/Assistant)

## 1. Problem Definition
- 학생-선생님 연결 요청을 선생님/조교가 처리할 수 있어야 한다.
- 연결된 학생 목록과 학생 상세(반 배치/휴원·재원/기록 요약)를 한 번에 조회할 수 있어야 한다.
- 반 배치를 위해 “배치 가능한 반”과 “배치 후보 학생”을 조회하고, 단건 배치를 생성할 수 있어야 한다.

## 2. Requirements

### Functional
- 선생님 수신 요청 목록 조회
  - 상태(PENDING/APPROVED/REJECTED) 필터와 학생 keyword 검색을 지원한다.
  - 요청 목록은 로그인한 선생님에게 들어온 요청만 반환한다.
- 요청 승인/거절 처리
  - 승인 시 TeacherStudentAssignment 생성, 요청 상태/처리자/처리일 갱신.
  - 거절 시 상태/처리자/처리일 갱신.
  - 이미 처리된 요청은 중복 처리 불가(정책: 409 반환).
- 학생 목록 조회
  - TeacherStudentAssignment 기반으로 연결된 학생 목록을 반환한다.
  - courseId 필터가 있으면 해당 반에 배치된 학생만 반환한다.
- 학생 상세 조회
  - Member + StudentInfo + Course/Assignment/Record 요약을 반환한다.
  - Course는 startDate desc, 동률은 createdAt desc 정렬.
- 휴원/재원 처리
  - StudentCourseAssignment를 활성/비활성으로 전환한다.
  - endDate < today인 반은 전환 불가(409).
- 반 배치 API
  - 배치 가능한 반 목록: 삭제되지 않았고 endDate >= today인 반만 반환.
  - 배치 후보 학생: TeacherStudentAssignment 존재 + 해당 반 미배정 학생만 반환.
  - 학생을 반에 배치하면 StudentCourseAssignment + StudentCourseRecord를 생성.

### Non-functional
- Teacher/Assistant 권한 범위를 엄격히 검증한다.
  - Teacher: 본인 소유 반/요청/학생만 처리
  - Assistant: 배정된 Teacher 범위 내에서만 처리
- 응답은 RsData/RsCode 규칙을 따른다.
- 마이그레이션은 고려하지 않는다(개발 단계).

## 3. API Design (Draft)

### 3.1 TeacherStudentRequest (Teacher Inbox)
- GET /teacher-student-requests?status&keyword&page&size
  - status: PENDING | APPROVED | REJECTED (default: PENDING)
  - keyword: 학생 이름/학교/전화번호 부분 검색

- PATCH /teacher-student-requests/{id}/approve
  - 응답: TeacherStudentRequestResponse

- PATCH /teacher-student-requests/{id}/reject
  - 응답: TeacherStudentRequestResponse

### 3.2 Teacher Students (목록/상세)
- GET /teacher-students?courseId&keyword&page&size
  - courseId 없음: 연결된 전체 학생
  - keyword: 학생 이름 검색

- GET /teacher-students/{studentId}
  - 응답: Member + StudentInfo + courses[]
  - courses[] 항목: courseId, name, startDate, endDate, active, assignmentId, assignmentActive, recordId

### 3.3 StudentCourseAssignment (휴원/재원)
- PATCH /student-course-assignments/{assignmentId}/activate
- PATCH /student-course-assignments/{assignmentId}/deactivate

### 3.4 Course Assignment 지원
- GET /courses/assignable?branchId&keyword&page&size
  - 조건: deletedAt is null && endDate >= today
  - 본인 소유 반만

- GET /courses/{courseId}/assignment-candidates?keyword&page&size
  - 조건: TeacherStudentAssignment 존재 + 해당 반 미배정

- POST /student-course-assignments
  - body: studentId, courseId
  - 처리: StudentCourseAssignment + StudentCourseRecord 생성

## 4. Domain Model (Draft)

- StudentTeacherRequest
  - status, message, processedByMemberId, processedAt
  - 요청 처리 시 상태 전이 검증
- TeacherStudentAssignment
  - teacherMemberId, studentMemberId, deletedAt
  - 유니크: (teacherMemberId, studentMemberId)
- StudentCourseAssignment
  - courseId, studentMemberId, active
  - 유니크: (courseId, studentMemberId)
- StudentCourseRecord
  - courseId, studentMemberId
  - 배치 생성 시 함께 생성

### Repository (Draft)
- StudentTeacherRequestRepository
  - findByTeacherMemberIdAndStatusIn(...)
  - keyword 검색용 join 쿼리(학생 이름/학교/전화)
  - findByIdForTeacher(...) (권한 검증용)
- TeacherStudentAssignmentRepository
  - findByTeacherMemberId(...) with paging
  - existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull
- StudentCourseAssignmentRepository
  - existsByCourseIdAndStudentMemberId
  - findByCourseIdAndStudentMemberIdIn (후보 제외/배치 검증)
- CourseRepository
  - findAssignableByTeacherMemberIdAndBranchIdAndKeyword (...)

### Service (Draft)
- TeacherStudentRequestService (Teacher/Assistant)
  - 목록 조회, 승인, 거절
- TeacherStudentService
  - 학생 목록/상세 조회
- CourseAssignmentService
  - 배치 가능한 반/후보 학생 조회, 단건 배치 생성
- StudentCourseAssignmentService
  - activate/deactivate 처리

## 5. TDD Plan

1. Repository 테스트 (DataJpaTest)
- StudentTeacherRequest: teacher 기준 필터/검색
- TeacherStudentAssignment: 목록/exists 검증
- StudentCourseAssignment: 중복 방지/조회
- Course: assignable 필터 조건 검증

2. TeacherStudentRequest 서비스 테스트
- PENDING 목록 조회
- 승인 시 Assignment 생성 및 상태 전이
- 거절 시 상태 전이
- 기처리 요청 처리 시 실패(409)

3. Teacher 학생 목록/상세 서비스 테스트
- TeacherStudentAssignment 기준 목록 반환
- courseId 필터 적용
- 상세 응답 구성(정렬/필드)

4. Course 배치 서비스 테스트
- assignable 반 목록 필터링
- 배치 후보 학생 필터링(미배정)
- 배치 생성 시 Assignment + Record 생성
- 중복 배치 시 실패

5. 휴원/재원 서비스 테스트
- activate/deactivate 성공
- 종료된 반은 실패(409)

6. Controller 테스트
- 권한별 접근 제어
- 성공/실패 응답 스키마

## 6. 개발 순서

0. 필요한 엔티티/레포지토리 생성 및 쿼리 추가 → Repository 테스트
1. TeacherStudentRequest 처리(목록/승인/거절) → Service/Controller 테스트
2. 반 배치 처리(배치 가능한 반/후보 학생/배치 생성) → Service/Controller 테스트
3. 학생 목록/상세 + 휴원/재원 처리 → Service/Controller 테스트
