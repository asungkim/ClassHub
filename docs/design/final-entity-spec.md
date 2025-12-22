# ClassHub 최종 엔티티 스펙

## 기본 데이터 (BaseEntity)

모든 엔티티는 BaseEntity 상속:

- id (UUID, PK)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)
- deletedAt (LocalDateTime, nullable)

**Soft Delete 헬퍼 메서드:**

- `isDeleted()`: deletedAt이 null이 아니면 true 반환
- `delete()`: deletedAt을 현재 시각으로 설정
- `restore()`: deletedAt을 null로 설정하여 복구

**조회 조건:**

- 활성 데이터 조회: `WHERE deletedAt IS NULL`
- 삭제된 데이터 조회: `WHERE deletedAt IS NOT NULL`
- 특정 기간 삭제 데이터: `WHERE deletedAt BETWEEN :start AND :end`

---

## 1. 조직 구조

### COMPANY

- name (String, not null)
- description (Text, nullable)
- type (CompanyType, not null) // INDIVIDUAL, ACADEMY
- verifiedStatus (VerifiedStatus, not null, default: VERIFIED) // UNVERIFIED, VERIFIED
- creatorMemberId (UUID, FK → Member, nullable) // 등록한 선생님

**ENUM:**

```java
enum CompanyType {
    INDIVIDUAL,  // 개인 학원
    ACADEMY      // 학원 체인
}

enum VerifiedStatus {
    UNVERIFIED,  // 미검증 (사용자 등록)
    VERIFIED     // 검증 완료 (사전 등록, 관리자 승인)
}
```

**인덱스:**

- `idx_company_verified_status` on (verifiedStatus)
- `idx_company_creator` on (creatorMemberId)

**비고 (출강 등록 흐름):**

1. **개인 학원(INDIVIDUAL)**
   - Teacher가 Company + Branch를 직접 입력해 생성한다.
   - Company.verifiedStatus는 생성 즉시 VERIFIED로 설정해 별도 검증 과정 없이 바로 사용할 수 있다.
   - Branch는 한 개만 허용하며, 생성한 Teacher는 `TeacherBranchAssignment`의 OWNER로 기록된다.
2. **회사 학원(ACADEMY)**
   - 기본적으로 SuperAdmin이 사전에 VERIFIED 상태의 Company/Branch를 등록해 두고, Teacher는 목록에서 선택해 FREELANCE Assignment를 얻는다.
   - 목록에 없다면 Teacher가 직접 입력을 선택해 Company/Branch를 UNVERIFIED 상태로 생성할 수 있으며, 이 경우 생성한 Teacher만 사용할 수 있다.
   - SuperAdmin 검증이 완료되면 해당 Company/Branch의 verifiedStatus를 VERIFIED로 변경해 다른 Teacher도 검색 및 사용 가능하도록 한다.

### BRANCH

- companyId (UUID, FK → Company, not null)
- name (String, not null)
- creatorMemberId (UUID, FK → Member, nullable)
- verifiedStatus (VerifiedStatus, not null, default: VERIFIED)

**인덱스:**

- `idx_branch_company` on (companyId)
- `idx_branch_verified_status` on (verifiedStatus)
- `idx_branch_creator` on (creatorMemberId)

**비고:**

- INDIVIDUAL Company는 하나의 Branch만 허용하며 생성 즉시 VERIFIED 상태로 사용된다.
- ACADEMY Company와 연결된 Branch는 기존 VERIFIED 목록을 선택하거나, Teacher가 직접 추가 시 UNVERIFIED로 저장되어 SuperAdmin 검증 전까지 생성자만 사용할 수 있다.
- Branch를 생성한 Teacher는 `TeacherBranchAssignment` OWNER로 설정되고, 기존 Branch를 선택해 Course를 만드는 Teacher는 FREELANCE Assignment가 자동 생성된다.

---

## 2. 회원 & 역할별 정보

### MEMBER

- email (String, unique, not null)
- password (String, not null)
- name (String, not null)
- phoneNumber (String, not null) // unique 제거
- role (MemberRole, not null) // 단일 역할

**ENUM:**

```java
enum MemberRole {
    TEACHER,
    ASSISTANT,
    STUDENT,
    ADMIN,
    SUPER_ADMIN
}
```

**인덱스:**

- `uk_member_email` unique on (email)

**비고:**

- role은 단일 값으로 관리 (한 계정당 하나의 역할)

### STUDENT_INFO

- memberId (UUID, FK → Member, unique, not null)
- schoolName (String, not null) // 입력값을 Trim + 중복 공백 제거 후 저장
- grade (StudentGrade, not null)
- birthDate (LocalDate, not null)
- parentPhone (String, not null)

**ENUM: StudentGrade**

```java
public enum StudentGrade {
    ELEMENTARY_1, ELEMENTARY_2, ELEMENTARY_3, ELEMENTARY_4, ELEMENTARY_5, ELEMENTARY_6,
    MIDDLE_1, MIDDLE_2, MIDDLE_3,
    HIGH_1, HIGH_2, HIGH_3,
    GAP_YEAR   // 재수/삼수 등 N수 상태
}
```

**비고:**

- grade는 위 열거형 중 하나만 허용하며, 프런트엔드는 드롭다운으로 선택하도록 강제한다.
- schoolName은 기본적으로 자유 입력 String이지만, 저장 시 `SchoolNameFormatter`로 양쪽 공백/중복 공백을 제거하고, 프런트에서는 학교 검색/자동완성(예: "오마중", "경문고")을 제공해 표기를 유도한다.

**인덱스:**

- `uk_student_info_member` unique on (memberId)

---

## 3. 배정 관계 (M:N)

### TEACHER_BRANCH_ASSIGNMENT

- teacherMemberId (UUID, FK → Member, not null)
- branchId (UUID, FK → Branch, not null)
- role (BranchRole, not null) // 선생님의 역할

**ENUM:**

```java
enum BranchRole {
    OWNER,       // 소유자 (자기 학원 운영자)
    FREELANCE    // 출강 강사 (다른 학원에 출강)
}
```

**인덱스:**

- `idx_tba_teacher` on (teacherMemberId)
- `idx_tba_branch` on (branchId)

**비고:**

- 출강 나가는 학원 등록시 생성
- 출강 학원 등록 시 기존 회사 명단에 없는 경우, Branch role은 OWNER로 생성.
- 출강 학원 등록 시 기존 회사 명단에 있는 경우, Branch role은 FREELANCE로 생성.

### TEACHER_ASSISTANT_ASSIGNMENT

- teacherMemberId (UUID, FK → Member, not null)
- assistantMemberId (UUID, FK → Member, not null)

**제약조건:**

- `uk_teacher_assistant_assignment` unique on (teacherMemberId, assistantMemberId)

**인덱스:**

- `idx_taa_teacher` on (teacherMemberId)
- `idx_taa_assistant` on (assistantMemberId)

**비고:**

- Teacher가 이미 가입한 Assistant를 이메일로 검색 후 직접 배정
- Soft delete(deletedAt)로 활성/비활성 관리: `isActive = (deletedAt IS NULL)`
- 비활성 시 조교의 접근 권한 즉시 차단
- M:N 관계: 한 Assistant는 여러 Teacher와 연결 가능, 한 Teacher도 여러 Assistant 배정 가능

---

## 4. 수업 구조

### COURSE

- branchId (UUID, FK → Branch, not null)
- teacherMemberId (UUID, FK → Member, not null)
- name (String, not null)
- schedules (Set<CourseSchedule>, not null) // ElementCollection

**CourseSchedule (Embeddable):**

- dayOfWeek (DayOfWeek, not null)
- startTime (LocalTime, not null)
- endTime (LocalTime, not null)

**인덱스:**

- `idx_course_branch` on (branchId)
- `idx_course_teacher` on (teacherMemberId)

**비고:**

- 공개 Course 검색 조건: Course.deletedAt IS NULL AND Branch.deletedAt IS NULL (Company status와 무관)

---

## 5. 학생 등록 프로세스

### STUDENT_ENROLLMENT_REQUEST

- studentMemberId (UUID, FK → Member, not null)
- courseId (UUID, FK → Course, not null)
- status (EnrollmentStatus, not null, default: PENDING) // PENDING, APPROVED, REJECTED
- message (Text, nullable)
- processedByMemberId (UUID, FK → Member, nullable)
- processedAt (LocalDateTime, nullable)

**인덱스:**

- `idx_enroll_req_student` on (studentMemberId)
- `idx_enroll_req_course` on (courseId)
- `idx_enroll_req_status` on (status)

**비고:**

- Course 담당 TEACHER 또는 연결된 ASSISTANT가 승인/거절 가능
- 승인 시 StudentCourseEnrollment & StudentCourseRecord 자동 생성, 해당 요청의 processedByMemberId/processedAt 기록

### STUDENT_COURSE_ENROLLMENT

- studentMemberId (UUID, FK → Member, not null)
- courseId (UUID, FK → Course, not null)
- enrolledAt (LocalDateTime, not null)

**제약조건:**

- `uk_student_course_enrollment` unique on (studentMemberId, courseId)

**인덱스:**

- `idx_sce_student` on (studentMemberId)
- `idx_sce_course` on (courseId)

### STUDENT_COURSE_RECORD

- studentMemberId (UUID, FK → Member, not null)
- courseId (UUID, FK → Course, not null)
- assistantMemberId (UUID, FK → Member, nullable) // 담당 조교 (선택)
- defaultClinicSlotId (UUID, FK → ClinicSlot, nullable)
- teacherNotes (Text, nullable)

**제약조건:**

- `uk_student_course_record` unique on (studentMemberId, courseId)

**인덱스:**

- `idx_scr_student` on (studentMemberId)
- `idx_scr_course` on (courseId)
- `idx_scr_assistant` on (assistantMemberId)

**비고:**

- "선생님이 관리하는 반별 학생 기록" 의미
- defaultClinicSlotId가 설정되어 있으면 해당 Slot 기반 ClinicSession 생성 시 자동으로 ClinicAttendance가 만들어짐
- defaultClinicSlotId 저장/변경 시 slot.teacherMemberId/branchId가 course와 동일한지 검증한다.
- 같은 학생은 동일 Slot을 여러 Course에 중복 선택할 수 없다.
- 같은 학생의 defaultClinicSlotId는 요일/시간이 겹치면 설정할 수 없다.
- assistantMemberId는 TeacherAssistantAssignment로 연결된 ASSISTANT만 지정할 수 있으며, Course 담당 Teacher가 관리 범위를 위임한 조교를 명시한다. Nullable이라 조교 미배정 상태도 허용한다.

---

## 6. 진도 관리

### COURSE_PROGRESS

- course (ManyToOne → Course, not null, ON DELETE CASCADE)
- writerId (UUID, FK → Member, not null)
- date (LocalDate, not null)
- title (String, not null)
- content (Text, not null)

**JPA 매핑:**

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
@OnDelete(action = OnDeleteAction.CASCADE)
private Course course;
```

**인덱스:**

- `idx_course_progress_course` on (course_id)
- `idx_course_progress_date` on (date)

**Cascade:** Course 삭제 시 CourseProgress도 함께 삭제

### PERSONAL_PROGRESS

- studentCourseRecordId (UUID, FK → StudentCourseRecord, not null)
- writerId (UUID, FK → Member, not null)
- date (LocalDate, not null)
- title (String, not null)
- content (Text, not null)

**인덱스:**

- `idx_personal_progress_record` on (studentCourseRecordId)
- `idx_personal_progress_date` on (date)

**비고:**

- StudentCourseRecord를 통해 특정 반의 개별 진도로 관리

---

## 7. 클리닉 구조

### CLINIC_SLOT

- teacherMemberId (UUID, FK → Member, not null)
- creatorMemberId (UUID, FK → Member, not null)
- branchId (UUID, FK → Branch, not null)
- dayOfWeek (DayOfWeek, not null) // MON, TUE, WED, THU, FRI, SAT, SUN
- startTime (LocalTime, not null)
- endTime (LocalTime, not null)
- defaultCapacity (Integer, not null)

**인덱스:**

- `idx_clinic_slot_teacher` on (teacherMemberId)
- `idx_clinic_slot_creator` on (creatorMemberId)
- `idx_clinic_slot_branch` on (branchId)

**비고:**

- ClinicSlot은 teacherMemberId + branchId 기준 시간표이며, 동일 teacher/branch의 Course 학생들이 조회/선택한다.
- Slot 목록은 동일 teacher/branch의 Course에서 동일하게 보인다.
- teacherMemberId: 해당 클리닉을 담당하는 선생님 (조회/통계 기준)
- creatorMemberId: 실제로 이 Slot을 생성한 사람 (Teacher 또는 Assistant 가능)
- defaultCapacity: Session 생성 시 기본 정원으로 사용됨 (Session별 개별 조정 가능, 정원은 슬롯 기준 합산)

### CLINIC_SESSION

- slotId (UUID, FK → ClinicSlot, nullable)
- sessionType (SessionType, not null)
- creatorMemberId (UUID, FK → Member, nullable)
- date (LocalDate, not null)
- startTime (LocalTime, not null)
- endTime (LocalTime, not null)
- capacity (Integer, not null)
- isCanceled (Boolean, not null, default: false)

**ENUM:**

```java
enum SessionType {
    REGULAR,     // Slot 기반 자동 생성
    EMERGENCY    // 선생님/조교 긴급 생성
}
```

**제약조건:**

- CHECK: `(sessionType = 'REGULAR' AND slotId IS NOT NULL AND creatorMemberId IS NULL) OR (sessionType = 'EMERGENCY' AND slotId IS NULL AND creatorMemberId IS NOT NULL)`

**인덱스:**

- `idx_clinic_session_slot` on (slotId)
- `idx_clinic_session_date` on (date)
- `idx_clinic_session_type` on (sessionType)
- `idx_clinic_session_creator` on (creatorMemberId)

**비고:**

**세션 타입별 필드 규칙:**

1. **REGULAR (정규 세션 - 스케줄러 자동 생성)**

   - slotId: NOT NULL (어떤 Slot에서 생성되었는지)
   - creatorMemberId: NULL (시스템 자동 생성)
   - startTime/endTime: Slot 시간 상속
   - capacity: Slot.defaultCapacity 상속 (생성 후 Teacher가 개별 조정 가능)

2. **EMERGENCY (긴급 세션 - Teacher/Assistant 수동 생성)**
   - slotId: NULL (Slot 없이 독립적으로 생성)
   - creatorMemberId: NOT NULL (생성한 Teacher 또는 Assistant)
   - startTime/endTime: 생성 시 직접 설정
   - capacity: 생성 시 직접 설정

**유즈케이스:**

- 정규 세션: 매주 화요일 18:00 클리닉 → ClinicSlot 기반 자동 생성
- 긴급 세션: 시험 전 특별 보강, 대체 클리닉 → Teacher가 직접 생성
- 정원 조정: 정규 세션이지만 이번 주만 10명 → 15명으로 증가

### CLINIC_ATTENDANCE

- clinicSessionId (UUID, FK → ClinicSession, not null)
- studentCourseRecordId (UUID, FK → StudentCourseRecord, not null)

**제약조건:**

- `uk_clinic_attendance` unique on (clinicSessionId, studentCourseRecordId)

**인덱스:**

- `idx_clinic_attendance_session` on (clinicSessionId)
- `idx_clinic_attendance_student` on (studentCourseRecordId)

**비고:**

- 학생은 클리닉 시작 전까지 동일 주(월~일) 내에서 Attendance를 다른 Session으로 이동 가능 (횟수 제한 없음)

### CLINIC_RECORD

- clinicAttendanceId (UUID, FK → ClinicAttendance, unique, not null)
- writerId (UUID, FK → Member, not null)
- title (String, not null)
- content (Text, not null)
- homeworkProgress (String, nullable)

**제약조건:**

- `uk_clinic_record_attendance` unique on (clinicAttendanceId)

**인덱스:**

- `idx_clinic_record_writer` on (writerId)

**비고:**

- ClinicAttendance와 1:1 관계
- Attendance는 "참석 여부", Record는 "작성한 기록"

---

## 8. 피드백 시스템

### FEEDBACK

- memberId (UUID, FK → Member, not null)
- content (Text, not null)
- status (FeedbackStatus, not null, default: SUBMITTED) // SUBMITTED, RESOLVED

**ENUM:**

```java
enum FeedbackStatus {
    SUBMITTED,   // 제출됨
    RESOLVED     // 확인/처리됨
}
```

**인덱스:**

- `idx_feedback_member` on (memberId)
- `idx_feedback_status` on (status)

**비고:**

- 로그인한 사용자만 작성 가능
- 관리자가 피드백 확인 후 RESOLVED 처리
- 실제 DELETE 허용 (관리자가 삭제 가능)

---

## 9. 공지사항 시스템

### NOTICE

- teacherMemberId (UUID, FK → Member, not null)
- title (String, not null)
- content (Text, not null)

**인덱스:**

- `idx_notice_teacher` on (teacherMemberId)
- `idx_notice_created` on (createdAt)

**비고:**

- 선생님이 조교에게 전달하는 공지사항
- 실제 DELETE 허용

### NOTICE_READ

- noticeId (UUID, FK → Notice, not null)
- assistantMemberId (UUID, FK → Member, not null)
- readAt (LocalDateTime, not null)

**제약조건:**

- `uk_notice_read` unique on (noticeId, assistantMemberId)

**인덱스:**

- `idx_notice_read_notice` on (noticeId)
- `idx_notice_read_assistant` on (assistantMemberId)

**비고:**

- 조교가 공지사항을 읽었는지 체크
- 실제 DELETE 허용

---

## 10. 근무 일지

### WORK_LOG

- assistantMemberId (UUID, FK → Member, not null)
- date (LocalDate, not null)
- startTime (LocalTime, not null)
- endTime (LocalTime, not null)
- hours (BigDecimal, not null) // 근무 시간 (계산된 값)
- memo (Text, nullable)

**제약조건:**

- `uk_work_log_date` unique on (assistantMemberId, date)

**인덱스:**

- `idx_work_log_assistant` on (assistantMemberId)
- `idx_work_log_date` on (date)

**비고:**

- 조교가 작성하는 근무 일지
- hours는 endTime - startTime으로 자동 계산
- 실제 DELETE 허용

---

## 엔티티 관계 요약

### 1:1 관계

- Member ↔ StudentInfo (memberId unique)
- ClinicAttendance ↔ ClinicRecord (clinicAttendanceId unique)

### 1:N 관계

- Company → Branch
- Branch → Course
- Member(TEACHER) → Course
- Member(ASSISTANT) → StudentCourseRecord (담당 조교 지정)
- Course → CourseProgress (CASCADE)
- StudentCourseRecord → PersonalProgress
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

### 논리 관계

- Course ↔ ClinicSlot: teacherMemberId + branchId가 동일할 때 연결

### 복합 관계

- StudentCourseRecord: (studentMemberId + courseId) UK
  - 학생 1명이 Course마다 별도 Record
  - PersonalProgress, ClinicAttendance는 이 Record 기준

---

## Cascade 전략

### Hard Delete (ON DELETE CASCADE)

- CourseProgress → Course
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

- PersonalProgress (삭제 시 실제 DELETE)
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
