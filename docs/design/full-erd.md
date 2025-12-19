# ClassHub 전체 ERD

## 전체 엔티티 관계도

```mermaid
erDiagram
    %% ========================================
    %% 조직 구조
    %% ========================================
    COMPANY ||--o{ BRANCH : has

    %% ========================================
    %% 회원 & 역할별 정보 (1:1)
    %% ========================================
    MEMBER ||--o| STUDENT_INFO : has

    %% ========================================
    %% 배정 관계 (M:N)
    %% ========================================
    MEMBER }|--o{ TEACHER_BRANCH_ASSIGNMENT : teacher
    BRANCH }|--o{ TEACHER_BRANCH_ASSIGNMENT : branch

    MEMBER }|--o{ TEACHER_ASSISTANT_ASSIGNMENT : teacher
    MEMBER }|--o{ TEACHER_ASSISTANT_ASSIGNMENT : assistant

    %% ========================================
    %% 수업 구조
    %% ========================================
    BRANCH ||--o{ COURSE : has
    MEMBER ||--o{ COURSE : teaches

    %% ========================================
    %% 학생 등록 프로세스
    %% ========================================
    MEMBER ||--o{ STUDENT_ENROLLMENT_REQUEST : requests
    COURSE ||--o{ STUDENT_ENROLLMENT_REQUEST : receives

    MEMBER }|--o{ STUDENT_COURSE_ENROLLMENT : student
    COURSE }|--o{ STUDENT_COURSE_ENROLLMENT : course

    %% ========================================
    %% 학생 기록 (반별 추가 정보)
    %% ========================================
    MEMBER ||--o{ STUDENT_COURSE_RECORD : student
    MEMBER ||--o{ STUDENT_COURSE_RECORD : assistant
    COURSE ||--o{ STUDENT_COURSE_RECORD : course
    CLINIC_SLOT ||--o{ STUDENT_COURSE_RECORD : defaultSlot

    %% ========================================
    %% 진도 관리
    %% ========================================
    COURSE ||--o{ SHARED_LESSON : has
    MEMBER ||--o{ SHARED_LESSON : writes

    STUDENT_COURSE_RECORD ||--o{ PERSONAL_LESSON : owns
    MEMBER ||--o{ PERSONAL_LESSON : writes

    %% ========================================
    %% 클리닉 구조
    %% ========================================
    COURSE ||--o{ CLINIC_SLOT : has
    MEMBER ||--o{ CLINIC_SLOT : owns
    MEMBER ||--o{ CLINIC_SLOT : creates
    BRANCH ||--o{ CLINIC_SLOT : locates

    CLINIC_SLOT ||--o{ CLINIC_SESSION : generates
    MEMBER ||--o{ CLINIC_SESSION : creates

    CLINIC_SESSION ||--o{ CLINIC_ATTENDANCE : has
    STUDENT_COURSE_RECORD ||--o{ CLINIC_ATTENDANCE : attends

    CLINIC_ATTENDANCE ||--|| CLINIC_RECORD : hasRecord
    MEMBER ||--o{ CLINIC_RECORD : writes

    %% ========================================
    %% 피드백 시스템
    %% ========================================
    MEMBER ||--o{ FEEDBACK : writes

    %% ========================================
    %% 공지사항 시스템
    %% ========================================
    MEMBER ||--o{ NOTICE : writes
    NOTICE ||--o{ NOTICE_READ : hasReads
    MEMBER ||--o{ NOTICE_READ : reads

    %% ========================================
    %% 근무 일지
    %% ========================================
    MEMBER ||--o{ WORK_LOG : writes

    %% ========================================
    %% 엔티티 정의
    %% ========================================

    COMPANY {
        uuid id PK
        string name
        text description
        string type
        string verifiedStatus
        uuid creatorMemberId FK
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
        %% type = INDIVIDUAL,ACADEMY
        %% verifiedStatus = UNVERIFIED,VERIFIED
    }

    BRANCH {
        uuid id PK
        uuid companyId FK
        string name
        uuid creatorMemberId FK
        string verifiedStatus
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
        %% verifiedStatus = UNVERIFIED,VERIFIED
    }

    MEMBER {
        uuid id PK
        string email UK
        string password
        string name
        string phoneNumber
        string role
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
        %% role = TEACHER,ASSISTANT,STUDENT,ADMIN,SUPER_ADMIN
    }

    STUDENT_INFO {
        uuid id PK
        uuid memberId FK
        string schoolName
        string grade  %% StudentGrade enum (E1~E6,M1~M3,H1~H3,GAP_YEAR)
        date birthDate
        string parentPhone
        datetime createdAt
        datetime updatedAt
        %% UNIQUE(memberId)
    }

    TEACHER_BRANCH_ASSIGNMENT {
        uuid id PK
        uuid teacherMemberId FK
        uuid branchId FK
        string role
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
        %% role = OWNER,FREELANCE
    }

    TEACHER_ASSISTANT_ASSIGNMENT {
        uuid id PK
        uuid teacherMemberId FK
        uuid assistantMemberId FK
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
    }

    COURSE {
        uuid id PK
        uuid branchId FK
        uuid teacherMemberId FK
        string name
        string schedules
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
        %% schedules = ElementCollection(CourseSchedule)
    }

    STUDENT_ENROLLMENT_REQUEST {
        uuid id PK
        uuid studentMemberId FK
        uuid courseId FK
        string status
        text message
        uuid processedByMemberId FK
        datetime processedAt
        datetime createdAt
        datetime updatedAt
        %% status = PENDING,APPROVED,REJECTED
    }

    STUDENT_COURSE_ENROLLMENT {
        uuid id PK
        uuid studentMemberId FK
        uuid courseId FK
        datetime enrolledAt
        datetime createdAt
        datetime updatedAt
        %% UNIQUE(studentMemberId, courseId)
    }

    STUDENT_COURSE_RECORD {
        uuid id PK
        uuid studentMemberId FK
        uuid courseId FK
        uuid assistantMemberId FK
        uuid defaultClinicSlotId FK
        text teacherNotes
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
        %% UNIQUE(studentMemberId, courseId)
        %% assistantMemberId nullable, references ASSISTANT
    }

    SHARED_LESSON {
        uuid id PK
        uuid courseId FK
        uuid writerId FK
        date date
        string title
        text content
        datetime createdAt
        datetime updatedAt
    }

    PERSONAL_LESSON {
        uuid id PK
        uuid studentCourseRecordId FK
        uuid writerId FK
        date date
        string title
        text content
        datetime createdAt
        datetime updatedAt
    }

    CLINIC_SLOT {
        uuid id PK
        uuid courseId FK
        uuid teacherMemberId FK
        uuid creatorMemberId FK
        uuid branchId FK
        string dayOfWeek
        time startTime
        time endTime
        int defaultCapacity
        datetime createdAt
        datetime updatedAt
        datetime deletedAt
        %% dayOfWeek = MON,TUE,WED,THU,FRI,SAT,SUN
    }

    CLINIC_SESSION {
        uuid id PK
        uuid slotId FK %% nullable
        string sessionType
        uuid creatorMemberId FK %% nullable
        date date
        int capacity
        boolean isCanceled
        datetime createdAt
        datetime updatedAt
        %% sessionType = REGULAR,EMERGENCY
        %% CHECK constraint for session type validation
    }

    CLINIC_ATTENDANCE {
        uuid id PK
        uuid clinicSessionId FK
        uuid studentCourseRecordId FK
        datetime createdAt
        datetime updatedAt
        %% UNIQUE(clinicSessionId, studentCourseRecordId)
    }

    CLINIC_RECORD {
        uuid id PK
        uuid clinicAttendanceId FK
        uuid writerId FK
        string title
        text content
        string homeworkProgress
        datetime createdAt
        datetime updatedAt
        %% UNIQUE(clinicAttendanceId)
    }

    FEEDBACK {
        uuid id PK
        uuid memberId FK
        text content
        string status
        datetime createdAt
        datetime updatedAt
        %% status = SUBMITTED,RESOLVED
    }

    NOTICE {
        uuid id PK
        uuid teacherMemberId FK
        string title
        text content
        datetime createdAt
        datetime updatedAt
    }

    NOTICE_READ {
        uuid id PK
        uuid noticeId FK
        uuid assistantMemberId FK
        datetime readAt
        datetime createdAt
        datetime updatedAt
        %% UNIQUE(noticeId, assistantMemberId)
    }

    WORK_LOG {
        uuid id PK
        uuid assistantMemberId FK
        date date
        time startTime
        time endTime
        decimal hours
        text memo
        datetime createdAt
        datetime updatedAt
        %% UNIQUE(assistantMemberId, date)
    }
```

## 주요 관계 요약

### 1:1 관계

- Member ↔ StudentInfo (memberId unique)
- ClinicAttendance ↔ ClinicRecord (clinicAttendanceId unique)

### 1:N 관계

- Company → Branch
- Branch → Course
- Member(TEACHER) → Course
- Member(ASSISTANT) → StudentCourseRecord
- Course → SharedLesson (CASCADE)
- Course → ClinicSlot
- StudentCourseRecord → PersonalLesson
- Member(TEACHER) → ClinicSlot (teacherMemberId)
- Member → ClinicSlot (creatorMemberId)
- Branch → ClinicSlot
- ClinicSlot → ClinicSession (REGULAR 타입)
- Member → ClinicSession (EMERGENCY 타입, creatorMemberId)
- ClinicSession → ClinicAttendance
- Member(TEACHER) → Notice
- Notice → NoticeRead
- Member(ASSISTANT) → WorkLog

### M:N 관계 (중간 테이블)

- Member(TEACHER) ↔ Branch via TeacherBranchAssignment
- Member(TEACHER) ↔ Member(ASSISTANT) via TeacherAssistantAssignment
- Member(STUDENT) ↔ Course via StudentCourseEnrollment

### 복합 관계

- StudentCourseRecord: (studentMemberId + courseId) UK
  - 학생 1명이 Course마다 별도 Record
  - PersonalLesson, ClinicAttendance는 이 Record 기준

## 인덱스 전략

### 주요 인덱스

- `member.email` (UK)
- `student_info.member_id` (UK)
- `course.branch_id` (조회)
- `course.teacher_member_id` (조회)
- `student_course_record.(student_member_id, course_id)` (UK)
- `student_course_enrollment.(student_member_id, course_id)` (UK)
- `clinic_slot.creator_member_id` (조회)
- `clinic_session.session_type` (조회)
- `clinic_session.creator_member_id` (조회)
- `clinic_attendance.(clinic_session_id, student_course_record_id)` (UK)
- `clinic_record.clinic_attendance_id` (UK)
- `notice_read.(notice_id, assistant_member_id)` (UK)
- `work_log.(assistant_member_id, date)` (UK)
- `teacher_assistant_assignment.(teacher_member_id, assistant_member_id)` (UK)

### 추가 인덱스

- `company.status` on (status)
- `company.creator_member_id` on (creatorMemberId)
- `branch.company_id` on (companyId)
- `teacher_branch_assignment.teacher_member_id` on (teacherMemberId)
- `teacher_branch_assignment.branch_id` on (branchId)
- `teacher_assistant_assignment.teacher_member_id` on (teacherMemberId)
- `teacher_assistant_assignment.assistant_member_id` on (assistantMemberId)
- `student_enrollment_request.student_member_id` on (studentMemberId)
- `student_enrollment_request.course_id` on (courseId)
- `student_enrollment_request.status` on (status)
- `student_course_enrollment.student_member_id` on (studentMemberId)
- `student_course_enrollment.course_id` on (courseId)
- `student_course_record.student_member_id` on (studentMemberId)
- `student_course_record.course_id` on (courseId)
- `shared_lesson.course_id` on (course_id)
- `shared_lesson.date` on (date)
- `personal_lesson.student_course_record_id` on (studentCourseRecordId)
- `personal_lesson.date` on (date)
- `clinic_slot.course_id` on (courseId)
- `clinic_slot.teacher_member_id` on (teacherMemberId)
- `clinic_slot.creator_member_id` on (creatorMemberId)
- `clinic_slot.branch_id` on (branchId)
- `clinic_session.slot_id` on (slotId)
- `clinic_session.date` on (date)
- `clinic_session.session_type` on (sessionType)
- `clinic_session.creator_member_id` on (creatorMemberId)
- `clinic_attendance.clinic_session_id` on (clinicSessionId)
- `clinic_attendance.student_course_record_id` on (studentCourseRecordId)
- `clinic_record.writer_id` on (writerId)
- `feedback.member_id` on (memberId)
- `feedback.status` on (status)
- `notice.teacher_member_id` on (teacherMemberId)
- `notice.created_at` on (createdAt)
- `notice_read.notice_id` on (noticeId)
- `notice_read.assistant_member_id` on (assistantMemberId)
- `work_log.assistant_member_id` on (assistantMemberId)
- `work_log.date` on (date)

## Cascade 전략

### Hard Delete (ON DELETE CASCADE)

- SharedLesson → Course
  - 반 삭제 시 공통 진도도 함께 삭제

### Soft Delete (deletedAt timestamp)

- Company (deletedAt)
- Branch (deletedAt)
- Member (deletedAt)
- Course (deletedAt)
- StudentCourseRecord (deletedAt)
- ClinicSlot (deletedAt)
- TeacherBranchAssignment (deletedAt)
- TeacherAssistantAssignment (deletedAt)

**장점:**

- 삭제 시점 추적 가능 (언제 삭제되었는지)
- 복구 가능 (deletedAt = NULL)
- 삭제 패턴 분석 가능 (월별, 주별 삭제 통계)
- 감사 로그 및 규정 준수

**조회:**

- 활성 데이터: `WHERE deletedAt IS NULL`
- 삭제된 데이터: `WHERE deletedAt IS NOT NULL`
- 특정 기간 삭제: `WHERE deletedAt BETWEEN :start AND :end`

### 실제 DELETE 허용

- PersonalLesson (삭제 시 실제 DELETE)
- ClinicRecord (삭제 시 실제 DELETE)
- StudentEnrollmentRequest (삭제 시 실제 DELETE)
- ClinicAttendance (삭제 시 실제 DELETE)
- Feedback (삭제 시 실제 DELETE)
- Notice (삭제 시 실제 DELETE)
- NoticeRead (삭제 시 실제 DELETE)
- WorkLog (삭제 시 실제 DELETE)

### 취소 플래그

- ClinicSession (isCanceled) - 삭제보다 취소 표시

---

## 네이밍 규칙

### 일관성 유지

- {role}MemberId 형태 통일:

  - teacherMemberId ✅
  - assistantMemberId ✅
  - studentMemberId ✅

- 엔티티 참조는 명확히:
  - UUID FK: `{entity}Id`
  - ManyToOne: `{entity}` (소문자 시작)

### 테이블명

- Snake case 사용
- 엔티티명을 snake_case로 변환
  - StudentCourseRecord → student_course_record
  - TeacherBranchAssignment → teacher_branch_assignment
