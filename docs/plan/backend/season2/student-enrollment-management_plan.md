# Feature: Student Enrollment API Suite

## 1. Problem Definition
- 선생님·조교·학생·어드민이 Enrollment 흐름을 각자 목적에 맞게 사용할 수 있는 API가 비어 있다.
- 현재 StudentEnrollmentRequest/StudentCourseEnrollment/StudentCourseRecord 엔티티만 존재하고, 이를 소비할 컨트롤러/서비스/TDD 계획이 없다.
- 학생은 반 등록과 내역 확인/취소를, 선생님과 조교는 요청 처리와 학생 데이터 조회/수정을, 어드민은 전체 요청 감사 조회를 수행해야 한다.

## 2. Requirements
### Functional
1. **Teacher & Assistant (StudentEnrollmentRequest)**
   - 목록 조회: `GET /api/v1/student-enrollment-requests?courseId&status&studentName`
     - Teacher: 본인 Course 기준. Assistant: 연결된 Teacher Course만.
   - 상세 조회: `GET /api/v1/student-enrollment-requests/{requestId}`
   - 승낙: `PATCH /api/v1/student-enrollment-requests/{requestId}/approve`
     - 승인 시 StudentCourseEnrollment + StudentCourseRecord 생성.
   - 거절: `PATCH /api/v1/student-enrollment-requests/{requestId}/reject`
2. **Teacher & Assistant (StudentData)**
   - 목록 조회: `GET /api/v1/student-courses?courseId&status&keyword`
     - 기본 정보만 제공: 이름, 연락처, 학교/학년, 나이, 듣는 Course 명 등 요약 데이터.
   - 상세 조회: `GET /api/v1/student-courses/{recordId}`
     - Teacher 전용으로 모든 상세 정보(Member + StudentInfo + StudentCourseRecord) 제공.
   - 수정: `PATCH /api/v1/student-courses/{recordId}` (Teacher 전용, Record 필드 업데이트).
3. **Student (Course & EnrollmentRequest)**
   - 수업 목록: `GET /api/v1/students/me/courses` (승인된 Enrollment만, 검색/필터 optional).
   - 등록 요청 생성: `POST /api/v1/student-enrollment-requests`
   - 신청 내역 조회: `GET /api/v1/student-enrollment-requests/me?status` (기본 PENDING, 다중 상태 허용).
   - 요청 취소: `PATCH /api/v1/student-enrollment-requests/{requestId}/cancel` (PENDING 소유자만).
4. **Admin**
   - 전체 요청 조회: `GET /api/v1/admin/student-enrollment-requests?teacherId&courseId&status&studentName` (읽기 전용).

### Non-functional
- Role 기반 접근 제어 (Teacher/Assistant assignment 검증).
- 승인 트랜잭션에서 Enrollment/Record 생성 + Request 상태 변경을 원자적으로 처리.
- 요청 상태 Enum: PENDING, APPROVED, REJECTED, CANCELED.
- 중복 신청 방지: (student, course) + PENDING unique.
- 응답은 `RsData` 포맷, 상태 전이 오류 시 적절한 `RsCode` 사용.

## 3. API Design (Draft)
| Method | URI | Role | Notes |
| --- | --- | --- | --- |
| POST | /api/v1/student-enrollment-requests | STUDENT | 신청 생성 |
| GET | /api/v1/student-enrollment-requests/me | STUDENT | 상태 필터, 기본 PENDING |
| PATCH | /api/v1/student-enrollment-requests/{id}/cancel | STUDENT | PENDING 취소 |
| GET | /api/v1/students/me/courses | STUDENT | 승인된 수업 목록 |
| GET | /api/v1/student-enrollment-requests | TEACHER/ASSISTANT | Course/상태/이름 필터 |
| GET | /api/v1/student-enrollment-requests/{id} | TEACHER/ASSISTANT | 권한 내 상세 |
| PATCH | /api/v1/student-enrollment-requests/{id}/approve | TEACHER/ASSISTANT | Enrollment/Record 생성 |
| PATCH | /api/v1/student-enrollment-requests/{id}/reject | TEACHER/ASSISTANT | 상태=REJECTED |
| GET | /api/v1/student-courses | TEACHER/ASSISTANT | 학생 목록 (Member+Info+Record) |
| GET | /api/v1/student-courses/{recordId} | TEACHER/ASSISTANT | 상세 |
| PATCH | /api/v1/student-courses/{recordId} | TEACHER | Record 수정 |
| GET | /api/v1/admin/student-enrollment-requests | SUPER_ADMIN | 감사용 전체 조회 |

## 4. Domain Model (Focus)
- **StudentEnrollmentRequest**: id, courseId, studentMemberId, status, message, processedByMemberId, processedAt, rejectionReason, createdAt, updatedAt.
- **StudentCourseEnrollment**: studentMemberId, courseId, enrolledAt (unique pair).
- **StudentCourseRecord**: recordId, studentCourseEnrollmentId, teacherNotes, assistantMemberId, defaultClinicSlotId, deletedAt.
- Relationships: Assistant 권한 체크는 TeacherAssistantAssignment + Course.teacherMemberId 기준.

## 5. TDD Plan
1. **StudentEnrollmentRequestService (Student)**
   - shouldCreateRequest_whenValid
   - shouldPreventDuplicatePendingRequest
   - shouldListOwnRequests_filteredByStatus
   - shouldCancelPendingRequest_whenOwner
2. **EnrollmentApprovalService (Teacher/Assistant)**
   - shouldApproveRequest_andCreateEnrollmentAndRecord
   - shouldRejectRequest_andSetProcessedFields
   - shouldThrow_whenStatusIsNotPending
   - shouldVerifyAssistantPermission
3. **StudentCourseQueryService**
   - shouldListStudentsForTeacher_withFilters
   - shouldReturnStudentDetail_withInfoAndRecord
4. **StudentCourseUpdateService**
   - shouldUpdateStudentCourseRecordFields
5. **AdminEnrollmentRequestQueryService**
   - shouldListAllRequests_withFilters
6. **Controller Tests**
   - Student REST: create/list/cancel + forbidden cases.
   - Teacher/Assistant REST: list/detail/approve/reject + auth rules.
   - StudentCourse REST: list/detail/update.
   - Admin REST: list-only guard.

## 6. 구현 단계 계획 (3단계)
1. **1단계 – Student 기능**
   - 학생 등록 요청 생성/조회/취소 API (`POST /student-enrollment-requests`, `GET /student-enrollment-requests/me`, `PATCH /{id}/cancel`)
   - 승인된 수업 조회 API (`GET /students/me/courses`)
   - Student 서비스/컨트롤러 TDD 완료 후 빌드 검증
2. **2단계 – Teacher & Assistant 처리 및 StudentData**
   - 요청 목록/상세/승낙/거절 API (`GET /student-enrollment-requests`, `GET /{id}`, `PATCH /{id}/approve|reject`)
   - 학생 목록/상세/수정 API (`GET /student-courses`, `GET /student-courses/{recordId}`, `PATCH /student-courses/{recordId}`)
   - Assistant 권한, Teacher 전용 상세/수정 로직 검증
3. **3단계 – Admin 조회 및 마무리**
   - Admin 전체 요청 조회 (`GET /admin/student-enrollment-requests`)
   - 통합 테스트/빌드, 문서/AGENT_LOG 업데이트, 향후 개선(배치 처리 등) 기록
