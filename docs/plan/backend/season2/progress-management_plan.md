# Feature: Progress Management (CourseProgress & PersonalProgress)

## 1. Problem Definition
- Season2 요구(v1.3) 기준으로 CourseProgress(SharedLesson)와 PersonalProgress(학생 개별 진도)를 서버에 기록/조회/수정/삭제할 수 있는 API가 없다.
- Teacher가 수업 직후 반 전체 공통 진도와 학생별 개인 진도를 한 번에 저장하려면 CourseProgress와 PersonalProgress를 동일한 트랜잭션으로 처리할 수 있어야 한다.
- Assistant는 자신이 배정된 반/학생의 최신 진도를 조회만 해야 하지만, 현재로서는 권한을 검증하는 표준화된 서비스가 없다.
- Student Calendar(요구사항 2.3.3)에서 CourseProgress/PersonalProgress/ClinicRecord를 월 단위로 합쳐 보여줘야 하나, 이를 지원하는 Aggregation API가 없다.

## 2. Requirements
### Functional
- **생성**
  - Teacher 전용 단건 API: CourseProgress 생성 (`POST /courses/{courseId}/course-progress`).
  - Teacher 전용 배치 API: CourseProgress + N개의 PersonalProgress를 한 요청에 생성 (`POST /courses/{courseId}/course-progress/compose`), PersonalProgress는 StudentCourseRecord 기반으로 연결.
  - PersonalProgress 단건 생성 API도 Teacher만 사용 가능 (`POST /student-courses/{recordId}/personal-progress`).
- **조회**
  - CourseProgress 목록: Course 기준 최신순(cursor 기반) 조회, Teacher/Assistant만 접근.
  - PersonalProgress 목록: StudentCourseRecord 기준 최신순(cursor 기반) 조회, Teacher/Assistant만 접근.
  - Student Calendar: Teacher/Assistant가 특정 학생에 대해 year/month 요청 시 CourseProgress + PersonalProgress + ClinicRecord를 날짜별 정렬로 응답.
- **수정**
  - Teacher는 CourseProgress/PersonalProgress 모두 수정 가능.
  - Assistant는 어떤 Progress도 수정/삭제할 수 없다 (조회 전용).
- **삭제**
  - Teacher만 CourseProgress/PersonalProgress 삭제 가능 (물리 삭제, spec 4.7 준수).
- **권한 제약**
  - Teacher는 자신이 OWNER/FREELANCE로 Assignment된 Course에만 접근.
  - Assistant는 TeacherAssistantAssignment가 ACTIVE이며 Course와 관계있는 StudentCourseRecord에 대해서만 **조회** 권한을 갖는다.
  - Student와 Admin은 Progress/Calendar API에 접근 권한이 없다.

### Non-functional
- 모든 쓰기 API는 `@Valid` DTO 검증 + 존재 여부 검증 + 권한 검증 후에 실행한다.
- Cursor 기반 페이징(`cursorId`, `cursorCreatedAt`, `limit`)으로 최신 진도 탐색을 최적화한다. 정렬은 `(createdAt DESC, id DESC)`.
- Calendar API는 month 범위를 현재 날짜 기준 ±3개월로 제한하고, 1회 최대 500 이벤트만 반환한다.
- Service 계층은 `@Transactional`로 묶고, 배치 생성 API는 CourseProgress와 PersonalProgress를 한 트랜잭션에서 저장한다.
- Repository는 N+1을 피하기 위해 CourseProgress ↔ Course, PersonalProgress ↔ StudentCourseRecord ↔ Course fetch join 쿼리를 제공한다.
- 모든 엔드포인트는 `RsData` 응답 포맷 + `RsCode` 에러를 사용한다.

## 3. API Design (Draft)
### CourseProgress (SharedLesson)
- `POST /api/v1/courses/{courseId}/course-progress`
  - Body: `{ "date": "2025-01-05", "title": "3주차", "content": "..." }`
  - Response: `RsData<CourseProgressResponse>`
- `POST /api/v1/courses/{courseId}/course-progress/compose`
  - Body: `{ "courseProgress": {...}, "personalProgressList": [ { "studentCourseRecordId": "...", "title": "...", "content": "...", "date": "..." }, ... ] }`
  - 모든 PersonalProgress는 동일한 courseId인지 검증하며, 실패 시 전체 롤백.
- `GET /api/v1/courses/{courseId}/course-progress?cursorId=&cursorCreatedAt=&limit=20`
  - Response: `{ "items": [...], "nextCursor": { "id": "...", "createdAt": "..." } }`
- `PATCH /api/v1/course-progress/{progressId}`
  - Body: `{ "title": "...", "content": "...", "date": "..." }`
- `DELETE /api/v1/course-progress/{progressId}`
  - Response: 성공 메시지, 404/RsCode.COURSE_PROGRESS_NOT_FOUND if missing.

### PersonalProgress (PersonalLesson)
- `POST /api/v1/student-courses/{recordId}/personal-progress`
  - Body: `{ "date": "...", "title": "...", "content": "..." }`
- `GET /api/v1/student-courses/{recordId}/personal-progress?cursorId=&cursorCreatedAt=&limit=20`
- `PATCH /api/v1/personal-progress/{personalProgressId}`
- `DELETE /api/v1/personal-progress/{personalProgressId}`

### Student Calendar
- `GET /api/v1/students/{studentId}/calendar?year=2025&month=3`
  - Query validates `1 <= month <= 12`.
  - Response:
    ```json
    {
      "studentId": "uuid",
      "year": 2025,
      "month": 3,
      "courseProgress": [ { "id": "...", "courseId": "...", "courseName": "...", "date": "2025-03-02", "title": "공통", "writerId": "...", "writerRole": "TEACHER" } ],
      "personalProgress": [ { "id": "...", "studentCourseRecordId": "...", "courseId": "...", "courseName": "...", "date": "2025-03-03", "title": "개별", "writerRole": "ASSISTANT" } ],
      "clinicEvents": [ { "clinicSessionId": "...", "clinicAttendanceId": "...", "courseId": "...", "slotId": "...", "date": "2025-03-04", "startTime": "18:00", "endTime": "19:00", "isCanceled": false, "recordSummary": { "title": "...", "writerRole": "TEACHER" } } ]
    }
    ```

## 4. Domain Model (Draft)
- `CourseProgress`
  - Fields: `Course course`, `UUID writerMemberId`, `LocalDate date`, `String title`, `String content`.
  - Constraints: Course must be ACTIVE(verified company/branch); writer must equal authenticated teacher.
  - Repository methods:
    - `findByCourseIdForCursor(UUID courseId, Cursor cursor)` uses `createdAt` and `id`.
    - `existsByIdAndCourseTeacher(UUID id, UUID teacherId)` for permission check.
- `PersonalProgress`
  - Fields: `StudentCourseRecord studentCourseRecord`, `UUID writerMemberId`, `LocalDate date`, `String title`, `String content`.
  - Repository methods similar to CourseProgress, plus `deleteByStudentCourseRecordId` helper for cascade cleanup.
- `ProgressPermissionValidator`
  - Validates teacher-course relationships (via `TeacherBranchAssignment`/`Course.teacherMemberId`).
  - Validates assistant assignments via `TeacherAssistantAssignment` + `StudentCourseRecord`.
  - Student/Admin access는 차단 대상이므로 관련 검증 로직을 명시적으로 포함한다.
- `StudentCalendarAggregator`
  - Repositories:
    - `CourseProgressRepository.findByStudentAndMonth(studentId, YearMonth)`.
    - `PersonalProgressRepository.findByStudentAndMonth(...)`.
    - `ClinicRecordRepository.findEventsByStudentAndMonth(...)` (joins ClinicSession + Attendance).
  - Returns `StudentCalendarResponse` DTO.

## 5. TDD Plan
1. **Permission Validator**
   - Unit tests verifying Teacher/Assistant/Student access combinations, including blocked cases (deleted assignment, mismatched course).
2. **CourseProgressService**
   - Red: Teacher가 권한 없는 Course에 작성 시 예외.
   - Green: CourseProgress 생성 성공 + repository save 검증.
   - Tests for compose API ensuring CourseProgress + PersonalProgress가 함께 저장되고, 잘못된 recordId가 있을 때 전체 롤백.
   - Update/Delete tests verifying only Teacher 접근.
3. **PersonalProgressService**
   - Creation tests verifying Teacher만 접근 가능하고 Assistant/Student/Admin은 예외가 발생한다.
   - Update/Delete permission tests (Teacher만 성공, 나머지는 차단).
   - Cursor 조회 tests hitting repository query ordering.
4. **StudentCalendarService**
  - Repository slice tests (DataJpaTest) covering month filtering & ordering.
  - Service tests ensuring Teacher/Assistant 권한만 허용되고, Student/Admin 요청 시 예외를 반환한다.
5. **Controller Tests**
   - MockMvc tests for each endpoint verifying HTTP status, validation 실패(400), 권한 실패(403), 성공 응답 구조.
   - Compose endpoint test verifying request/response JSON structure.
6. **Refactoring & Cleanup**
   - Extract mapper classes (`CourseProgressMapper`, `PersonalProgressMapper`, `StudentCalendarMapper`).
   - Ensure tests remain green after refactors.

## 6. Implementation Stages
1. **Stage 1 – Permission & Domain 준비**
   - `ProgressPermissionValidator`, Repository 커서 쿼리, DTO 스켈레톤을 먼저 구현하고 단위 테스트로 Teacher/Assistant/Student 차단 로직을 확정한다.
2. **Stage 2 – Course/Personal Progress CRUD**
   - CourseProgress 단건/배치 + PersonalProgress CRUD 서비스를 TDD로 완성하고, Controller/MockMvc 테스트까지 포함해 API를 안정화한다.
3. **Stage 3 – Student Calendar Aggregation**
   - Calendar Service/Controller와 month 필터 쿼리를 구현하고, 기존 Progress/Clinic 데이터를 통합하는 응답/권한 테스트를 마무리한다.
