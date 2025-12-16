# ClassHub 최종 엔티티 스펙

## 기본 데이터 (BaseEntity)

모든 엔티티는 BaseEntity 상속:
- id (UUID, PK)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)

---

## 1. 조직 구조

### COMPANY
- name (String, not null)
- description (Text, nullable)
- type (CompanyType, not null) // INDIVIDUAL, ACADEMY
- status (CompanyStatus, not null, default: UNVERIFIED) // UNVERIFIED, VERIFIED, REJECTED
- creatorMemberId (UUID, FK → Member, nullable) // 등록한 선생님
- rejectionReason (String, nullable) // 거부 사유
- isActive (Boolean, not null, default: true)

**ENUM:**
```java
enum CompanyType {
    INDIVIDUAL,  // 개인 학원
    ACADEMY      // 학원 체인
}

enum CompanyStatus {
    UNVERIFIED,  // 미검증 (사용자 등록)
    VERIFIED,    // 검증 완료 (관리자 승인)
    REJECTED     // 거부됨
}
```

**인덱스:**
- `idx_company_status` on (status)
- `idx_company_creator` on (creatorMemberId)

**비고:**
- 선생님이 반 생성 시 회사 없으면 UNVERIFIED 상태로 생성
- 관리자 승인 후 VERIFIED → 다른 선생님 검색 가능
- INDIVIDUAL 타입은 자동 VERIFIED 처리 가능

### BRANCH
- companyId (UUID, FK → Company, not null)
- name (String, not null)
- isActive (Boolean, not null, default: true)

**인덱스:**
- `idx_branch_company` on (companyId)

---

## 2. 회원 & 역할별 정보

### MEMBER
- email (String, unique, not null)
- password (String, not null)
- name (String, not null)
- phoneNumber (String, not null) // unique 제거
- roles (List<MemberRole>, not null) // ElementCollection
- isActive (Boolean, not null, default: true)

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

**JPA 매핑:**
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "member_roles",
    joinColumns = @JoinColumn(name = "member_id"))
@Enumerated(EnumType.STRING)
@Column(name = "role")
private List<MemberRole> roles;
```

**인덱스:**
- `uk_member_email` unique on (email)
- `idx_member_roles_member` on member_roles(member_id)

**비고:**
- roles는 List<MemberRole>로 별도 테이블(member_roles) 관리
- 확장성 고려 (현재는 단일 역할만 사용, roles.get(0))
- phoneNumber는 unique 제거 (학부모 번호와 중복 가능)

### TEACHER_INFO
- memberId (UUID, FK → Member, unique, not null)
- subjects (String, nullable) // 담당 과목

**인덱스:**
- `uk_teacher_info_member` unique on (memberId)

**비고:**
- AssistantInfo는 생성하지 않음 (추가 정보 불필요)

### STUDENT_INFO
- memberId (UUID, FK → Member, unique, not null)
- schoolName (String, not null)
- grade (String, not null)
- birthDate (LocalDate, not null)
- parentPhone (String, not null)

**인덱스:**
- `uk_student_info_member` unique on (memberId)

---

## 3. 배정 관계 (M:N)

### TEACHER_BRANCH_ASSIGNMENT
- teacherMemberId (UUID, FK → Member, not null)
- branchId (UUID, FK → Branch, not null)
- role (BranchRole, not null) // 선생님의 역할
- isActive (Boolean, not null, default: true)

**ENUM:**
```java
enum BranchRole {
    OWNER,       // 소유자 (개인학원 운영자)
    MANAGER,     // 지점장 (체인학원)
    EMPLOYEE,    // 정규 직원
    FREELANCE    // 출강 강사
}
```

**인덱스:**
- `idx_tba_teacher` on (teacherMemberId)
- `idx_tba_branch` on (branchId)

**비고:**
- 반 생성 시 자동 생성
- INDIVIDUAL 타입 Company → role: OWNER
- 기존 ACADEMY 출강 → role: FREELANCE

### TEACHER_ASSISTANT_ASSIGNMENT
- teacherMemberId (UUID, FK → Member, not null)
- assistantMemberId (UUID, FK → Member, not null)
- isActive (Boolean, not null, default: true)

**인덱스:**
- `idx_taa_teacher` on (teacherMemberId)
- `idx_taa_assistant` on (assistantMemberId)

---

## 4. 수업 구조

### COURSE
- branchId (UUID, FK → Branch, not null)
- teacherMemberId (UUID, FK → Member, not null)
- name (String, not null)
- schedules (Set<CourseSchedule>, not null) // ElementCollection
- isActive (Boolean, not null, default: true)

**CourseSchedule (Embeddable):**
- dayOfWeek (DayOfWeek, not null)
- startTime (LocalTime, not null)
- endTime (LocalTime, not null)

**인덱스:**
- `idx_course_branch` on (branchId)
- `idx_course_teacher` on (teacherMemberId)

---

## 5. 학생 등록 프로세스

### STUDENT_ENROLLMENT_REQUEST
- studentMemberId (UUID, FK → Member, not null)
- courseId (UUID, FK → Course, not null)
- status (EnrollmentStatus, not null, default: PENDING) // PENDING, APPROVED, REJECTED
- message (Text, nullable)
- processedAt (LocalDateTime, nullable)

**인덱스:**
- `idx_enroll_req_student` on (studentMemberId)
- `idx_enroll_req_course` on (courseId)
- `idx_enroll_req_status` on (status)

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
- defaultClinicSlotId (UUID, FK → ClinicSlot, nullable)
- teacherNotes (Text, nullable)
- isActive (Boolean, not null, default: true)

**제약조건:**
- `uk_student_course_record` unique on (studentMemberId, courseId)

**인덱스:**
- `idx_scr_student` on (studentMemberId)
- `idx_scr_course` on (courseId)

**비고:**
- StudentRecord → StudentCourseRecord로 네이밍
- "선생님이 관리하는 반별 학생 기록" 의미

---

## 6. 진도 관리

### SHARED_LESSON
- course (ManyToOne → Course, not null, ON DELETE CASCADE)
- writerId (UUID, FK → Member, not null)
- date (LocalDate, not null)
- title (String, not null)
- content (Text, not null)

**JPA 매핑:**
```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "course_id", nullable = false)
@OnDelete(action = OnDeleteAction.CASCADE)
private Course course;
```

**인덱스:**
- `idx_shared_lesson_course` on (course_id)
- `idx_shared_lesson_date` on (date)

**Cascade:** Course 삭제 시 SharedLesson도 함께 삭제

### PERSONAL_LESSON
- studentCourseRecordId (UUID, FK → StudentCourseRecord, not null)
- writerId (UUID, FK → Member, not null)
- date (LocalDate, not null)
- title (String, not null)
- content (Text, not null)

**인덱스:**
- `idx_personal_lesson_record` on (studentCourseRecordId)
- `idx_personal_lesson_date` on (date)

**비고:**
- StudentCourseRecord를 통해 특정 반의 개별 진도로 관리

---

## 7. 클리닉 구조

### CLINIC_SLOT
- teacherMemberId (UUID, FK → Member, not null)
- branchId (UUID, FK → Branch, not null)
- dayOfWeek (DayOfWeek, not null) // MON, TUE, WED, THU, FRI, SAT, SUN
- startTime (LocalTime, not null)
- endTime (LocalTime, not null)
- capacity (Integer, not null)
- isActive (Boolean, not null, default: true)

**인덱스:**
- `idx_clinic_slot_teacher` on (teacherMemberId)
- `idx_clinic_slot_branch` on (branchId)

### CLINIC_SESSION
- slotId (UUID, FK → ClinicSlot, not null)
- date (LocalDate, not null)
- isCanceled (Boolean, not null, default: false)

**제약조건:**
- `uk_clinic_session_slot_date` unique on (slotId, date)

**인덱스:**
- `idx_clinic_session_slot` on (slotId)
- `idx_clinic_session_date` on (date)

### CLINIC_ATTENDANCE
- clinicSessionId (UUID, FK → ClinicSession, not null)
- studentCourseRecordId (UUID, FK → StudentCourseRecord, not null)

**제약조건:**
- `uk_clinic_attendance` unique on (clinicSessionId, studentCourseRecordId)

**인덱스:**
- `idx_clinic_attendance_session` on (clinicSessionId)
- `idx_clinic_attendance_student` on (studentCourseRecordId)

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

## 9. 초대 시스템

### INVITATION
- senderId (UUID, FK → Member, not null)
- branchId (UUID, FK → Branch, nullable)
- targetEmail (String, nullable)
- inviteeRole (InvitationRole, not null) // ASSISTANT만 (학생은 자유 가입)
- status (InvitationStatus, not null, default: PENDING) // PENDING, ACCEPTED, EXPIRED, REVOKED
- code (String, unique, not null)
- expiredAt (LocalDateTime, not null)
- useCount (Integer, not null, default: 0)
- maxUses (Integer, not null, default: -1) // -1: 무제한

**인덱스:**
- `uk_invitation_code` unique on (code)
- `idx_invitation_sender` on (senderId)
- `idx_invitation_status` on (status)

**비고:**
- 조교만 초대 코드로 가입 (ASSISTANT)
- 학생은 자유 가입 후 StudentEnrollmentRequest로 반 등록

---

## 엔티티 관계 요약

### 1:1 관계
- Member ↔ TeacherInfo (memberId unique)
- Member ↔ StudentInfo (memberId unique)
- ClinicAttendance ↔ ClinicRecord (clinicAttendanceId unique)

### 1:N 관계
- Company → Branch
- Branch → Course
- Member(TEACHER) → Course
- Course → SharedLesson (CASCADE)
- StudentCourseRecord → PersonalLesson
- Member(TEACHER) → ClinicSlot
- Branch → ClinicSlot
- ClinicSlot → ClinicSession
- ClinicSession → ClinicAttendance

### M:N 관계 (중간 테이블)
- Member(TEACHER) ↔ Branch via TeacherBranchAssignment
- Member(TEACHER) ↔ Member(ASSISTANT) via TeacherAssistantAssignment
- Member(STUDENT) ↔ Course via StudentCourseEnrollment

### 복합 관계
- StudentCourseRecord: (studentMemberId + courseId) UK
  - 학생 1명이 Course마다 별도 Record
  - PersonalLesson, ClinicAttendance는 이 Record 기준

---

## Cascade 전략

### Hard Delete (ON DELETE CASCADE)
- SharedLesson → Course
  - 반 삭제 시 공통 진도도 함께 삭제

### Soft Delete (isActive flag)
- Company (isActive)
- Branch (isActive)
- Member (isActive)
- Course (isActive)
- StudentCourseRecord (isActive)
- ClinicSlot (isActive)
- TeacherBranchAssignment (isActive)
- TeacherAssistantAssignment (isActive)

### 실제 DELETE 허용
- PersonalLesson (삭제 시 실제 DELETE)
- ClinicRecord (삭제 시 실제 DELETE)
- StudentEnrollmentRequest (삭제 시 실제 DELETE)
- ClinicAttendance (삭제 시 실제 DELETE)
- Feedback (삭제 시 실제 DELETE)

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
