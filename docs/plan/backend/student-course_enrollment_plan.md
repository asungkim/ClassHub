# Feature: StudentProfile ↔ Course M:N Enrollment

## 1. Problem Definition

- 현재 `StudentProfile`이 `courseId` 단일 필드로만 Course를 참조해 "학생=1개 반" 구조에 묶여 있다.
- **학생 프로필 생성/수정 시 여러 개의 반을 선택**할 수 있도록 Course ↔ StudentProfile 구조를 M:N으로 확장해야 한다.
- 기존 StudentProfile API는 `courseId` 하나만 받던 것을 `courseIds` 배열로 받아 여러 Course에 동시 등록할 수 있어야 한다.

## 2. Requirements

### Functional

1. **새 엔티티 `StudentCourseEnrollment`**

   - 필드: `studentProfileId (UUID)`, `courseId (UUID)`, `teacherId (UUID)` + BaseEntity 공통 필드(`createdAt`, `updatedAt`).
   - JPA 연관 없이 **UUID만 저장**하는 기존 패턴 유지.
   - `StudentProfile` ↔ `Course` 관계는 이 엔티티를 통해서만 관리하며, 다수 수강 허용.
   - Course/StudentProfile 삭제 시에는 Service 레이어에서 연관 Enrollment를 함께 삭제한다 (DB FK 사용 안 함).

2. **StudentProfile API 수정**

   - **생성**: `POST /api/v1/student-profiles`

     - Request DTO: `courseIds (List<UUID>)` - **여러 개 반 선택 가능**
     - 각 courseId마다 Enrollment 레코드 생성

   - **수정**: `PATCH /api/v1/student-profiles/{id}`

     - Request DTO: `courseIds (List<UUID>)` - *nullable*
     - `courseIds == null` → 수강 정보 변경 없음
     - `courseIds != null` → 기존 Enrollment와 비교:
       - 새 Course → Enrollment 생성
       - 빠진 Course → Enrollment 삭제

   - **조회**: `GET /api/v1/student-profiles/{id}`
     - Response DTO: `enrolledCourses (List<EnrolledCourseInfo>)` 포함
     - EnrolledCourseInfo: `{ courseId, courseName, enrolledAt }`

3. **권한 & 검증**

   - Teacher만 StudentProfile 생성/수정 가능
   - courseIds에 포함된 모든 Course는 해당 Teacher가 소유해야 함
   - assistantId는 해당 Teacher에 속한 Assistant여야 함 (Course와는 독립적)
   - 전화번호 중복 검증: 동일 Teacher 내에서 phoneNumber 중복 방지 (Course 무관)

4. **도메인 서비스 영향**

   - StudentProfileService:
     - StudentCourseEnrollmentRepository 주입
     - 생성 시: courseIds 순회하며 Enrollment 레코드 생성
     - 수정 시: 기존 Enrollment 조회 → 추가/삭제 처리
   - PersonalLessonService:
     - StudentProfile의 teacherId 필드 유지 (비정규화)
     - Course 관계는 Enrollment로 확인 (선택 사항)

5. **InitData & Seed**
   - `StudentProfileInitData` 생성 시 StudentCourseEnrollment를 함께 생성
   - 기존 seed에서 "학생이 courseId 하나"였던 것을 Enrollment 레코드로 변환

### Non-functional

- Enrollment 테이블 인덱스:
  - `(student_profile_id, course_id)` unique constraint (중복 수강 방지)
  - `(student_profile_id)` index (학생의 수강 목록 조회)
  - `(course_id)` index (반별 학생 조회)
  - `(teacher_id)` index (Teacher별 관리)
- DB FK 대신 Service 레벨 검증/삭제로 일관성 유지.
- Repository는 UUID 기반 조회(PersonalLesson 패턴 일관성), N+1 방지.

## 3. API Design (Draft)

### 3.1 StudentProfile API 변경

**기존 Request DTO**:

```java
StudentProfileCreateRequest {
    UUID courseId;  // 단일 Course
    UUID assistantId;
    String name;
    String phoneNumber;
    // ...
}
```

**새로운 Request DTO**:

```java
StudentProfileCreateRequest {
    List<UUID> courseIds;  // 여러 Course 선택 가능
    UUID assistantId;
    String name;
    String phoneNumber;
    // ...
}
```

**Response DTO 변경**:

```java
StudentProfileResponse {
    UUID id;
    String name;
    String phoneNumber;
    List<EnrolledCourseInfo> enrolledCourses;  // 수강 중인 반 목록
    // ...
}

EnrolledCourseInfo {
    UUID courseId;
    String courseName;
    LocalDateTime enrolledAt;
}
```

### 3.2 API Endpoints

| Method | URL                                   | Request                  | Response                      | 변경 사항                              |
| ------ | ------------------------------------- | ------------------------ | ----------------------------- | -------------------------------------- |
| POST   | `/api/v1/student-profiles`            | `courseIds: List<UUID>`  | `StudentProfileResponse`      | courseId → courseIds 배열              |
| PATCH  | `/api/v1/student-profiles/{id}`       | `courseIds?: List<UUID>` | `StudentProfileResponse`      | null=변경 없음, []=전부 해제, 값 있으면 diff 적용 |
| GET    | `/api/v1/student-profiles/{id}`       | -                        | `StudentProfileResponse`      | enrolledCourses 포함                   |
| GET    | `/api/v1/student-profiles?courseId=`  | query                    | `Page<StudentProfileSummary>` | Enrollment 기준 필터링                 |
| GET    | `/api/v1/courses/{courseId}/students` | -                        | `Page<StudentProfileSummary>` | Enrollment 조회                        |

## 4. Domain Model (Draft)

### 4.1 엔티티

**StudentProfile** (courseId 필드 제거):

- `UUID id`
- ~~`UUID courseId`~~ - **제거** (Enrollment로 대체)
- `UUID teacherId`
- `UUID assistantId`
- `String name`, `String phoneNumber`, ...
- Course 관계는 StudentCourseEnrollment 테이블에서만 관리

**StudentCourseEnrollment** (신규):

```java
@Entity
@Table(name = "student_course_enrollment",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_enrollment_student_course",
        columnNames = {"student_profile_id", "course_id"}
    ),
    indexes = {
        @Index(name = "idx_enrollment_student", columnList = "student_profile_id"),
        @Index(name = "idx_enrollment_course", columnList = "course_id"),
        @Index(name = "idx_enrollment_teacher", columnList = "teacher_id")
    }
)
public class StudentCourseEnrollment extends BaseEntity {

    @Column(name = "student_profile_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentProfileId;

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "teacher_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherId;

}
```

### 4.2 Repository

**StudentCourseEnrollmentRepository**:

```java
List<StudentCourseEnrollment> findAllByStudentProfileId(UUID studentProfileId);
List<StudentCourseEnrollment> findAllByCourseId(UUID courseId);
Optional<StudentCourseEnrollment> findByStudentProfileIdAndCourseId(UUID studentProfileId, UUID courseId);
boolean existsByStudentProfileIdAndCourseId(UUID studentProfileId, UUID courseId);
void deleteByStudentProfileIdAndCourseId(UUID studentProfileId, UUID courseId);
```

### 4.3 Service 로직

**StudentProfileService 변경**:

1. **생성 시**:

   ```java
   // courseIds 순회하며 각각 Enrollment 생성
   for (UUID courseId : request.courseIds()) {
       Course course = getCourseOwnedByTeacher(courseId, teacherId);
       StudentCourseEnrollment enrollment = StudentCourseEnrollment.builder()
           .studentProfileId(savedProfile.getId())
           .courseId(courseId)
           .teacherId(teacherId)
           .assistantId(request.assistantId())
           .build();
       enrollmentRepository.save(enrollment);
   }
   ```

2. **수정 시**:

   ```java
   if (request.courseIds() != null) {
       Set<UUID> existingCourseIds = enrollmentRepository
           .findAllByStudentProfileId(profileId)
           .stream().map(StudentCourseEnrollment::getCourseId)
           .collect(Collectors.toSet());

       Set<UUID> newCourseIds = new HashSet<>(request.courseIds());

       // 추가할 Course
       for (UUID courseId : newCourseIds) {
           if (!existingCourseIds.contains(courseId)) {
               // Enrollment 생성
           }
       }

       // 제거할 Course
       for (UUID courseId : existingCourseIds) {
           if (!newCourseIds.contains(courseId)) {
               enrollmentRepository.deleteByStudentProfileIdAndCourseId(profileId, courseId);
           }
       }
   }
   ```

3. **조회 시**:

   ```java
   List<StudentCourseEnrollment> enrollments =
       enrollmentRepository.findAllByStudentProfileId(profileId);

   List<UUID> courseIds = enrollments.stream()
       .map(StudentCourseEnrollment::getCourseId)
       .toList();

   Map<UUID, Course> courseMap = courseRepository.findAllById(courseIds)
       .stream().collect(Collectors.toMap(Course::getId, c -> c));

   List<EnrolledCourseInfo> enrolledCourses = enrollments.stream()
       .map(e -> new EnrolledCourseInfo(
           e.getCourseId(),
           courseMap.get(e.getCourseId()).getName(),
           e.getCreatedAt()
       ))
       .toList();
   ```

## 5. TDD Plan

### 5.1 RepositoryTest (StudentCourseEnrollmentRepositoryTest)

1. `shouldFindAllByStudentProfileId_whenEnrollmentsExist()`
   - 특정 학생의 모든 수강 목록 조회
2. `shouldFindAllByCourseId_whenEnrollmentsExist()`
   - 특정 반의 모든 수강생 조회
3. `shouldFindByStudentProfileIdAndCourseId_whenExists()`
   - StudentProfile + Course 조합으로 단건 조회
4. `shouldReturnTrue_whenEnrollmentExists()`
   - existsByStudentProfileIdAndCourseId 중복 확인
5. `shouldThrowException_whenDuplicateEnrollment()`
   - unique constraint 위반 시 예외 발생
6. `shouldDeleteEnrollment_whenExists()`
   - deleteByStudentProfileIdAndCourseId 정상 동작

### 5.2 ServiceTest (StudentProfileServiceTest 수정)

**생성 (createProfile)**

1. `shouldCreateProfile_withMultipleCourses()`
   - courseIds 배열로 여러 Course에 등록
   - 각 courseId마다 Enrollment 생성 확인
2. `shouldCreateProfile_withSingleCourse()`
   - courseIds에 하나만 있을 때도 정상 동작
3. `shouldFailToCreate_whenCourseNotOwned()`
   - courseIds 중 Teacher 소유가 아닌 Course 포함 시 예외
4. `shouldFailToCreate_whenDuplicateCourse()`
   - courseIds에 중복된 courseId 포함 시 예외

**수정 (updateProfile)** 5. `shouldAddNewCourse_whenCourseIdsUpdated()`

- courseIds에 새 Course 추가 → Enrollment 생성

6. `shouldRemoveCourse_whenCourseIdsUpdated()`
   - courseIds에서 기존 Course 제거 → Enrollment 삭제
7. `shouldKeepExistingCourses_whenNoChange()`
   - 변경 없는 Course는 유지
8. `shouldNotUpdateCourses_whenCourseIdsIsNull()`
   - courseIds가 null이면 Enrollment 변경 없음

**조회 (getProfile)** 9. `shouldReturnEnrolledCourses_whenMultipleEnrollments()`

- Response DTO에 enrolledCourses 포함 확인

10. `shouldReturnEmptyList_whenNoEnrollments()`
    - Enrollment 없을 때 빈 리스트 반환

### 5.3 ControllerTest (StudentProfileControllerTest 수정)

**Validation**

1. `shouldReturn400_whenCourseIdsEmpty()`
   - courseIds 빈 배열 시 400 에러
2. `shouldReturn400_whenCourseIdsNull()`
   - courseIds null 시 400 에러

**기능** 3. `shouldCreateProfile_withMultipleCourses_201()`

- courseIds 배열로 생성 성공

4. `shouldUpdateProfile_withCourseIdsChange_200()`
   - courseIds 변경 성공
5. `shouldGetProfile_withEnrolledCourses_200()`
   - enrolledCourses 포함된 응답 확인

### 5.4 구현 & 기존 코드 수정

1. **StudentProfile 엔티티 수정**:
   - `courseId` 필드 제거
   - 관련 getter/setter 제거

2. **StudentCourseEnrollment 엔티티 생성**:
   - JPA가 자동으로 테이블 생성 (ddl-auto 설정)
   - 또는 수동으로 DB에 테이블 생성 후 코드 작성

3. **StudentProfileService 수정**:
   - `courseId` 기반 로직 → `courseIds` 배열로 변경
   - Enrollment 생성/수정/삭제 로직 추가

4. **StudentProfileRepository 수정**:
   - `findByCourseId` 등 메서드 제거 또는 Enrollment 조인으로 변경

5. **DTO 수정**:
   - Request: `courseId` → `courseIds` (List)
   - Response: `enrolledCourses` 추가

6. **InitData 수정**:
   - StudentProfile 생성 시 Enrollment도 함께 생성
   - 기존 `courseId` 설정 제거

7. **모든 테스트 수정**:
   - `courseId` 참조 제거
   - Enrollment 검증 추가

8. **검증**:
   - 애플리케이션 실행 후 테이블 생성 확인
   - Seed 데이터 정상 생성 확인
   - API 테스트로 기능 검증
