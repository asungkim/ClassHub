# Entity Refactor Plan (Season 2)

## 개요

`docs/design/final-entity-spec.md`와 `docs/design/full-erd.md` 스펙에 맞춰 백엔드 엔티티 구조를 전면 개편합니다.

**주요 변경 사항:**
- 멀티 Company/Branch 구조 지원
- Member와 역할별 Info 분리 (TeacherInfo, StudentInfo)
- StudentProfile → StudentCourseRecord 네이밍 변경 및 구조 변경
- 새로운 배정 관계 엔티티 추가 (TeacherBranchAssignment, TeacherAssistantAssignment)
- Feedback 엔티티 신규 추가

---

## Phase 1: 엔티티 + Repository 전체 구축

### 1.1 새로 생성할 엔티티 (11개)

| 엔티티 | 패키지 | 우선순위 | 비고 |
|--------|--------|----------|------|
| Company | `domain/company/model` | P1 | 조직 구조 루트 |
| Branch | `domain/branch/model` | P1 | Company 하위 지점 |
| TeacherInfo | `domain/member/model` | P1 | 선생님 추가 정보 |
| StudentInfo | `domain/member/model` | P1 | 학생 추가 정보 |
| TeacherBranchAssignment | `domain/assignment/model` | P1 | 선생님-지점 배정 |
| TeacherAssistantAssignment | `domain/assignment/model` | P1 | 선생님-조교 배정 |
| StudentEnrollmentRequest | `domain/enrollment/model` | P2 | 학생 등록 요청 |
| ClinicSession | `domain/clinic/clinicsession/model` | P2 | 클리닉 세션 |
| ClinicAttendance | `domain/clinic/clinicattendance/model` | P2 | 클리닉 참석 |
| ClinicRecord | `domain/clinic/clinicrecord/model` | P2 | 클리닉 기록 |
| Feedback | `domain/feedback/model` | P3 | 사용자 피드백 |

### 1.2 수정이 필요한 엔티티 (7개)

| 엔티티 | 현재 상태 | 변경 내용 |
|--------|----------|----------|
| **Member** | role: MemberRole (단일) | roles: List<MemberRole> (ElementCollection), phoneNumber unique 제거, teacherId 제거 |
| **Course** | company: String | branchId: UUID FK, teacherMemberId 네이밍 변경 |
| **StudentProfile** | 현재 구조 | → **StudentCourseRecord**로 완전 변경 (구조 대폭 변경) |
| **StudentCourseEnrollment** | studentProfileId | studentMemberId로 변경 (Member FK) |
| **ClinicSlot** | teacherId | teacherMemberId, branchId 추가 |
| **PersonalLesson** | studentProfile (ManyToOne) | studentCourseRecordId (UUID FK) |
| **SharedLesson** | 현재 OK | writerId 네이밍 확인 |
| **Invitation** | 현재 구조 | branchId 추가, inviteeRole은 ASSISTANT만 |

### 1.3 삭제할 엔티티

| 엔티티 | 사유 |
|--------|------|
| (없음) | 기존 엔티티는 수정/대체로 처리 |

---

## Phase 2: 상세 변경 명세

### 2.1 Company (신규)

```java
@Entity
@Table(name = "company", indexes = {
    @Index(name = "idx_company_status", columnList = "status"),
    @Index(name = "idx_company_creator", columnList = "creator_member_id")
})
public class Company extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType type;  // INDIVIDUAL, ACADEMY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status;  // UNVERIFIED, VERIFIED, REJECTED

    @Column(name = "creator_member_id")
    private UUID creatorMemberId;

    private String rejectionReason;

    @Column(nullable = false)
    private boolean isActive = true;
}
```

**Enum 추가:**
- `CompanyType`: INDIVIDUAL, ACADEMY
- `CompanyStatus`: UNVERIFIED, VERIFIED, REJECTED

### 2.2 Branch (신규)

```java
@Entity
@Table(name = "branch", indexes = {
    @Index(name = "idx_branch_company", columnList = "company_id")
})
public class Branch extends BaseEntity {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean isActive = true;
}
```

### 2.3 Member (수정)

**현재:**
```java
private MemberRole role;  // 단일
@Column(unique = true)
private String phoneNumber;
private UUID teacherId;  // 조교용
```

**변경 후:**
```java
@ElementCollection(fetch = FetchType.EAGER)
@CollectionTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"))
@Enumerated(EnumType.STRING)
@Column(name = "role")
private List<MemberRole> roles;  // 복수형

@Column(nullable = false)
private String phoneNumber;  // unique 제거

// teacherId 삭제 -> TeacherAssistantAssignment로 이동
```

**영향 범위:**
- MemberService: role -> roles.get(0) 또는 roles.contains() 로직 변경
- AuthService: 회원가입 시 roles 처리
- SecurityConfig: roles 기반 권한 체크
- 모든 @PreAuthorize 어노테이션 검토

### 2.4 TeacherInfo (신규)

```java
@Entity
@Table(name = "teacher_info", indexes = {
    @Index(name = "uk_teacher_info_member", columnList = "member_id", unique = true)
})
public class TeacherInfo extends BaseEntity {
    @Column(name = "member_id", nullable = false, unique = true)
    private UUID memberId;

    private String subjects;  // 담당 과목
}
```

### 2.5 StudentInfo (신규)

```java
@Entity
@Table(name = "student_info", indexes = {
    @Index(name = "uk_student_info_member", columnList = "member_id", unique = true)
})
public class StudentInfo extends BaseEntity {
    @Column(name = "member_id", nullable = false, unique = true)
    private UUID memberId;

    @Column(nullable = false)
    private String schoolName;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private String parentPhone;
}
```

### 2.6 TeacherBranchAssignment (신규)

```java
@Entity
@Table(name = "teacher_branch_assignment", indexes = {
    @Index(name = "idx_tba_teacher", columnList = "teacher_member_id"),
    @Index(name = "idx_tba_branch", columnList = "branch_id")
})
public class TeacherBranchAssignment extends BaseEntity {
    @Column(name = "teacher_member_id", nullable = false)
    private UUID teacherMemberId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BranchRole role;  // OWNER, MANAGER, EMPLOYEE, FREELANCE

    @Column(nullable = false)
    private boolean isActive = true;
}
```

**Enum 추가:**
- `BranchRole`: OWNER, MANAGER, EMPLOYEE, FREELANCE

### 2.7 TeacherAssistantAssignment (신규)

```java
@Entity
@Table(name = "teacher_assistant_assignment", indexes = {
    @Index(name = "idx_taa_teacher", columnList = "teacher_member_id"),
    @Index(name = "idx_taa_assistant", columnList = "assistant_member_id")
})
public class TeacherAssistantAssignment extends BaseEntity {
    @Column(name = "teacher_member_id", nullable = false)
    private UUID teacherMemberId;

    @Column(name = "assistant_member_id", nullable = false)
    private UUID assistantMemberId;

    @Column(nullable = false)
    private boolean isActive = true;
}
```

### 2.8 Course (수정)

**현재:**
```java
private String company;
private UUID teacherId;
```

**변경 후:**
```java
@Column(name = "branch_id", nullable = false)
private UUID branchId;

@Column(name = "teacher_member_id", nullable = false)
private UUID teacherMemberId;

// company 필드 삭제 (Branch 통해 조회)
```

### 2.9 StudentProfile -> StudentCourseRecord (대폭 변경)

**현재 StudentProfile:**
```java
private UUID teacherId;
private UUID assistantId;
private UUID memberId;
private String name;
private String phoneNumber;
private String parentPhone;
private String schoolName;
private String grade;
private int age;
private UUID defaultClinicSlotId;
private boolean active;
```

**변경 후 StudentCourseRecord:**
```java
@Entity
@Table(name = "student_course_record",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_student_course_record",
        columnNames = {"student_member_id", "course_id"}
    ),
    indexes = {
        @Index(name = "idx_scr_student", columnList = "student_member_id"),
        @Index(name = "idx_scr_course", columnList = "course_id")
    }
)
public class StudentCourseRecord extends BaseEntity {
    @Column(name = "student_member_id", nullable = false)
    private UUID studentMemberId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "default_clinic_slot_id")
    private UUID defaultClinicSlotId;

    @Column(columnDefinition = "TEXT")
    private String teacherNotes;

    @Column(nullable = false)
    private boolean isActive = true;
}
```

**주요 변경점:**
- 학생 개인정보(name, phoneNumber, schoolName 등) -> StudentInfo로 이동
- teacherId, assistantId 삭제 (Course, Assignment를 통해 조회)
- `(studentMemberId, courseId)` 복합 유니크 제약

### 2.10 StudentCourseEnrollment (수정)

**현재:**
```java
private UUID studentProfileId;
private UUID courseId;
private UUID teacherId;
```

**변경 후:**
```java
@Column(name = "student_member_id", nullable = false)
private UUID studentMemberId;

@Column(name = "course_id", nullable = false)
private UUID courseId;

@Column(nullable = false)
private LocalDateTime enrolledAt;

// teacherId 삭제 (Course를 통해 조회)
```

### 2.11 StudentEnrollmentRequest (신규)

```java
@Entity
@Table(name = "student_enrollment_request", indexes = {
    @Index(name = "idx_enroll_req_student", columnList = "student_member_id"),
    @Index(name = "idx_enroll_req_course", columnList = "course_id"),
    @Index(name = "idx_enroll_req_status", columnList = "status")
})
public class StudentEnrollmentRequest extends BaseEntity {
    @Column(name = "student_member_id", nullable = false)
    private UUID studentMemberId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime processedAt;
}
```

**Enum 추가:**
- `EnrollmentStatus`: PENDING, APPROVED, REJECTED

### 2.12 ClinicSlot (수정)

**현재:**
```java
private UUID teacherId;
```

**변경 후:**
```java
@Column(name = "teacher_member_id", nullable = false)
private UUID teacherMemberId;

@Column(name = "branch_id", nullable = false)
private UUID branchId;
```

### 2.13 ClinicSession (신규)

```java
@Entity
@Table(name = "clinic_session",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_clinic_session_slot_date",
        columnNames = {"slot_id", "date"}
    ),
    indexes = {
        @Index(name = "idx_clinic_session_slot", columnList = "slot_id"),
        @Index(name = "idx_clinic_session_date", columnList = "date")
    }
)
public class ClinicSession extends BaseEntity {
    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean isCanceled = false;
}
```

### 2.14 ClinicAttendance (신규)

```java
@Entity
@Table(name = "clinic_attendance",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_clinic_attendance",
        columnNames = {"clinic_session_id", "student_course_record_id"}
    ),
    indexes = {
        @Index(name = "idx_clinic_attendance_session", columnList = "clinic_session_id"),
        @Index(name = "idx_clinic_attendance_student", columnList = "student_course_record_id")
    }
)
public class ClinicAttendance extends BaseEntity {
    @Column(name = "clinic_session_id", nullable = false)
    private UUID clinicSessionId;

    @Column(name = "student_course_record_id", nullable = false)
    private UUID studentCourseRecordId;
}
```

### 2.15 ClinicRecord (신규)

```java
@Entity
@Table(name = "clinic_record",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_clinic_record_attendance",
        columnNames = {"clinic_attendance_id"}
    ),
    indexes = {
        @Index(name = "idx_clinic_record_writer", columnList = "writer_id")
    }
)
public class ClinicRecord extends BaseEntity {
    @Column(name = "clinic_attendance_id", nullable = false, unique = true)
    private UUID clinicAttendanceId;

    @Column(name = "writer_id", nullable = false)
    private UUID writerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String homeworkProgress;
}
```

### 2.16 PersonalLesson (수정)

**현재:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "student_profile_id", nullable = false)
private StudentProfile studentProfile;

private UUID teacherId;
```

**변경 후:**
```java
@Column(name = "student_course_record_id", nullable = false)
private UUID studentCourseRecordId;

@Column(name = "writer_id", nullable = false)
private UUID writerId;

// teacherId 삭제
```

### 2.17 Invitation (수정)

**현재:**
```java
private UUID studentProfileId;
private InviteeRole inviteeRole;  // ASSISTANT, STUDENT
```

**변경 후:**
```java
@Column(name = "branch_id")
private UUID branchId;  // 추가

// studentProfileId 삭제 (학생은 자유 가입)
// inviteeRole은 ASSISTANT만 허용 (Enum 값 제한)
```

### 2.18 Feedback (신규)

```java
@Entity
@Table(name = "feedback", indexes = {
    @Index(name = "idx_feedback_member", columnList = "member_id"),
    @Index(name = "idx_feedback_status", columnList = "status")
})
public class Feedback extends BaseEntity {
    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackStatus status = FeedbackStatus.SUBMITTED;
}
```

**Enum 추가:**
- `FeedbackStatus`: SUBMITTED, RESOLVED

---

## Phase 3: Repository 생성/수정

### 3.1 신규 Repository (12개)

| Repository | 메서드 (예상) |
|------------|-------------|
| CompanyRepository | findByStatus, findByCreatorMemberId, findByType |
| BranchRepository | findByCompanyId, findByCompanyIdAndIsActive |
| TeacherInfoRepository | findByMemberId |
| StudentInfoRepository | findByMemberId |
| TeacherBranchAssignmentRepository | findByTeacherMemberId, findByBranchId, findByTeacherMemberIdAndIsActive |
| TeacherAssistantAssignmentRepository | findByTeacherMemberId, findByAssistantMemberId |
| StudentEnrollmentRequestRepository | findByStudentMemberId, findByCourseId, findByStatus |
| StudentCourseRecordRepository | findByStudentMemberId, findByCourseId, findByStudentMemberIdAndCourseId |
| ClinicSessionRepository | findBySlotId, findByDate, findBySlotIdAndDate |
| ClinicAttendanceRepository | findByClinicSessionId, findByStudentCourseRecordId |
| ClinicRecordRepository | findByClinicAttendanceId, findByWriterId |
| FeedbackRepository | findByMemberId, findByStatus |

### 3.2 수정 Repository

| Repository | 변경 내용 |
|------------|----------|
| MemberRepository | role -> roles 관련 쿼리 수정, teacherId 관련 제거 |
| CourseRepository | teacherId -> teacherMemberId, company -> branchId |
| StudentProfileRepository | -> **StudentCourseRecordRepository**로 대체 |
| StudentCourseEnrollmentRepository | studentProfileId -> studentMemberId |
| ClinicSlotRepository | teacherId -> teacherMemberId, branchId 추가 |
| PersonalLessonRepository | studentProfile -> studentCourseRecordId |
| InvitationRepository | studentProfileId 제거, branchId 추가 |

---

## Phase 4: Enum 정리

### 4.1 신규 Enum (5개)

| Enum | 값 | 위치 |
|------|---|------|
| CompanyType | INDIVIDUAL, ACADEMY | `domain/company/model/` |
| CompanyStatus | UNVERIFIED, VERIFIED, REJECTED | `domain/company/model/` |
| BranchRole | OWNER, MANAGER, EMPLOYEE, FREELANCE | `domain/assignment/model/` |
| EnrollmentStatus | PENDING, APPROVED, REJECTED | `domain/enrollment/model/` |
| FeedbackStatus | SUBMITTED, RESOLVED | `domain/feedback/model/` |

### 4.2 수정 Enum

| Enum | 변경 내용 |
|------|----------|
| MemberRole | 변경 없음 (TEACHER, ASSISTANT, STUDENT, ADMIN, SUPER_ADMIN) |
| InviteeRole | STUDENT 제거, ASSISTANT만 유지 (또는 Enum 삭제) |

---

## Phase 5: 패키지 구조 변경

### 5.1 신규 패키지

```
domain/
├── company/
│   ├── model/
│   │   ├── Company.java
│   │   ├── CompanyType.java
│   │   └── CompanyStatus.java
│   └── repository/
│       └── CompanyRepository.java
├── branch/
│   ├── model/
│   │   └── Branch.java
│   └── repository/
│       └── BranchRepository.java
├── assignment/
│   ├── model/
│   │   ├── TeacherBranchAssignment.java
│   │   ├── TeacherAssistantAssignment.java
│   │   └── BranchRole.java
│   └── repository/
│       ├── TeacherBranchAssignmentRepository.java
│       └── TeacherAssistantAssignmentRepository.java
├── enrollment/
│   ├── model/
│   │   ├── StudentEnrollmentRequest.java
│   │   └── EnrollmentStatus.java
│   └── repository/
│       └── StudentEnrollmentRequestRepository.java
├── feedback/
│   ├── model/
│   │   ├── Feedback.java
│   │   └── FeedbackStatus.java
│   └── repository/
│       └── FeedbackRepository.java
```

### 5.2 이동/이름 변경

| 현재 | 변경 후 |
|------|--------|
| `domain/studentprofile/` | `domain/studentcourserecord/` |
| `StudentProfile.java` | `StudentCourseRecord.java` |
| `StudentProfileRepository.java` | `StudentCourseRecordRepository.java` |

### 5.3 member 패키지 구조

```
domain/member/
├── model/
│   ├── Member.java
│   ├── MemberRole.java
│   ├── TeacherInfo.java
│   └── StudentInfo.java
└── repository/
    ├── MemberRepository.java
    ├── TeacherInfoRepository.java
    └── StudentInfoRepository.java
```

---

## Phase 6: Init Data 수정

### 6.1 수정이 필요한 InitData

| InitData | 변경 내용 |
|----------|----------|
| MemberInitData | roles 배열로 변경, TeacherInfo/StudentInfo 생성 추가 |
| CourseInitData | branchId 사용, Company/Branch 먼저 생성 필요 |
| StudentProfileInitData | -> StudentCourseRecordInitData로 변경 |
| InvitationInitData | branchId 추가, STUDENT 초대 제거 |

### 6.2 신규 InitData

| InitData | 순서 | 설명 |
|----------|------|------|
| CompanyInitData | 1 | Company 생성 |
| BranchInitData | 2 | Branch 생성 |
| TeacherBranchAssignmentInitData | 4 | 선생님-지점 배정 |
| TeacherAssistantAssignmentInitData | 5 | 선생님-조교 배정 |
| StudentCourseRecordInitData | 7 | 학생 코스 기록 |

### 6.3 InitData 실행 순서

```
1. CompanyInitData
2. BranchInitData
3. MemberInitData (Teacher, Assistant, Student + Info 생성)
4. TeacherBranchAssignmentInitData
5. TeacherAssistantAssignmentInitData
6. CourseInitData
7. StudentCourseEnrollmentInitData
8. StudentCourseRecordInitData
9. ClinicSlotInitData
10. SharedLessonInitData
11. PersonalLessonInitData
12. InvitationInitData
```

---

## Phase 7: 작업 순서 (권장)

### Step 1: 기반 엔티티 (의존성 없음)
1. CompanyType, CompanyStatus Enum 생성
2. Company 엔티티 + Repository 생성
3. Branch 엔티티 + Repository 생성
4. BranchRole Enum 생성

### Step 2: Member 관련
5. Member 수정 (roles ElementCollection)
6. TeacherInfo 엔티티 + Repository 생성
7. StudentInfo 엔티티 + Repository 생성

### Step 3: 배정 관계
8. TeacherBranchAssignment 엔티티 + Repository 생성
9. TeacherAssistantAssignment 엔티티 + Repository 생성

### Step 4: Course 관련
10. Course 수정 (branchId, teacherMemberId)
11. EnrollmentStatus Enum 생성
12. StudentEnrollmentRequest 엔티티 + Repository 생성
13. StudentCourseEnrollment 수정

### Step 5: StudentCourseRecord
14. StudentProfile -> StudentCourseRecord 변환
15. StudentCourseRecordRepository 생성

### Step 6: Clinic 관련
16. ClinicSlot 수정 (teacherMemberId, branchId)
17. ClinicSession 엔티티 + Repository 생성
18. ClinicAttendance 엔티티 + Repository 생성
19. ClinicRecord 엔티티 + Repository 생성

### Step 7: Lesson 관련
20. PersonalLesson 수정 (studentCourseRecordId)
21. SharedLesson 확인 (변경 최소화)

### Step 8: 기타
22. Invitation 수정 (branchId, ASSISTANT only)
23. FeedbackStatus Enum 생성
24. Feedback 엔티티 + Repository 생성

### Step 9: Init Data
25. 모든 InitData 수정/생성

---

## 체크리스트

### 엔티티 생성
- [ ] Company
- [ ] Branch
- [ ] TeacherInfo
- [ ] StudentInfo
- [ ] TeacherBranchAssignment
- [ ] TeacherAssistantAssignment
- [ ] StudentEnrollmentRequest
- [ ] StudentCourseRecord (from StudentProfile)
- [ ] ClinicSession
- [ ] ClinicAttendance
- [ ] ClinicRecord
- [ ] Feedback

### Enum 생성
- [ ] CompanyType
- [ ] CompanyStatus
- [ ] BranchRole
- [ ] EnrollmentStatus
- [ ] FeedbackStatus

### 엔티티 수정
- [ ] Member (roles ElementCollection)
- [ ] Course (branchId, teacherMemberId)
- [ ] StudentCourseEnrollment (studentMemberId)
- [ ] ClinicSlot (teacherMemberId, branchId)
- [ ] PersonalLesson (studentCourseRecordId)
- [ ] Invitation (branchId, ASSISTANT only)

### Repository 생성
- [ ] CompanyRepository
- [ ] BranchRepository
- [ ] TeacherInfoRepository
- [ ] StudentInfoRepository
- [ ] TeacherBranchAssignmentRepository
- [ ] TeacherAssistantAssignmentRepository
- [ ] StudentEnrollmentRequestRepository
- [ ] StudentCourseRecordRepository
- [ ] ClinicSessionRepository
- [ ] ClinicAttendanceRepository
- [ ] ClinicRecordRepository
- [ ] FeedbackRepository

### Init Data
- [ ] CompanyInitData
- [ ] BranchInitData
- [ ] MemberInitData 수정
- [ ] TeacherBranchAssignmentInitData
- [ ] TeacherAssistantAssignmentInitData
- [ ] CourseInitData 수정
- [ ] StudentCourseEnrollmentInitData 수정
- [ ] StudentCourseRecordInitData
- [ ] ClinicSlotInitData 수정
- [ ] SharedLessonInitData 수정
- [ ] PersonalLessonInitData 수정
- [ ] InvitationInitData 수정

---

## 예상 작업량

| 항목 | 개수 | 예상 복잡도 |
|------|------|-----------|
| 신규 엔티티 | 11개 | 중 |
| 수정 엔티티 | 6개 | 높음 |
| 신규 Enum | 5개 | 낮음 |
| 신규 Repository | 12개 | 낮음 |
| 수정 Repository | 7개 | 중 |
| Init Data | 12개 | 중 |

---

## 주의사항

1. **Member.roles 변경**: Security 설정, @PreAuthorize 전부 검토 필요
2. **StudentProfile -> StudentCourseRecord**: 가장 큰 변경, PersonalLesson/ClinicAttendance 영향
3. **Invitation ASSISTANT only**: 학생 가입 플로우 완전히 변경 (자유 가입 + EnrollmentRequest)
4. **FK 네이밍 통일**: `{role}MemberId` 형태로 통일 (teacherMemberId, studentMemberId, assistantMemberId)
5. **테이블명**: snake_case로 통일 (student_course_record, teacher_branch_assignment 등)
