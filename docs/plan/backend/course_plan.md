# Feature: Course 엔티티 및 CRUD

## 1. Problem Definition

- 선생님이 본인이 관리하는 수업(반)에 대한 정보를 저장하고 관리할 수 있는 Course 엔티티가 필요하다.
- Course는 반 이름, 소속 학원(회사), 수업 요일 및 시간, 활성 상태를 포함하며, 이후 SharedLesson, StudentProfile 등 다른 도메인과 연결된다.
- Teacher만 Course를 생성/수정/삭제할 수 있으며, Course 기반으로 학생을 배정하고 공통 진도를 작성한다.

## 2. Requirements

### Functional

- **Course 엔티티 속성**
  - `name`: 반 이름 (string, 필수, max 100자)
  - `company`: 소속 학원/회사명 (string, 필수, max 100자)
  - `teacherId`: 반을 생성한 선생님 (FK → Member.id, 필수)
  - `daysOfWeek`: 수업 요일 (Set<DayOfWeek>, 필수) - 하나의 반이 여러 요일에 수업할 수 있음
  - `startTime`: 수업 시작 시간 (LocalTime, 필수)
  - `endTime`: 수업 종료 시간 (LocalTime, 필수)
  - `active`: 활성 상태 (boolean, 기본값 true)
  - BaseEntity 상속 (id, createdAt, updatedAt)

- **CRUD 기능**
  1. **생성**: Teacher가 새로운 Course 생성
  2. **목록 조회**: Teacher가 본인이 생성한 Course 목록 조회 (활성/비활성 필터 가능)
  3. **상세 조회**: 특정 Course의 상세 정보 조회
  4. **수정**: Course 정보(이름, 회사, 요일, 시간) 수정
  5. **비활성화**: `isActive`를 false로 변경 (물리 삭제 X)
  6. **활성화**: 비활성화된 Course를 다시 `isActive`를 true로 변경

- **권한**
  - Teacher만 본인이 생성한 Course에 대해 CRUD 수행 가능
  - Controller: `@PreAuthorize("hasAuthority('TEACHER')")` + `principal null` 체크
  - Service: `validateTeacher(UUID teacherId)` - Member 존재 및 TEACHER 역할 검증
  - Service: `getCourseOwnedByTeacher(UUID courseId, UUID teacherId)` - 소유권 검증
  - Assistant, Student는 Course 조회만 가능 (추후 확장)

### Non-functional

- Course 엔티티는 `global.entity.BaseEntity` 상속
- Lombok `@Builder`, `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PROTECTED)` 적용
- 테이블명: `course` (단수형)
- teacherId에 대한 외래키 제약 및 인덱스 설정
- 서비스 계층 `@Transactional` 적용
- 입력 검증: `jakarta.validation` 애노테이션 사용

## 3. API Design (Draft)

### 3.1 생성

**POST** `/api/v1/courses`

**Request Body**
```json
{
  "name": "중등 수학 A반",
  "company": "ABC 학원",
  "daysOfWeek": ["MONDAY", "FRIDAY"],
  "startTime": "14:00",
  "endTime": "16:00"
}
```

**Response** (성공 시 201)
```json
{
  "code": 1001,
  "message": "생성 성공",
  "data": {
    "id": "uuid-string",
    "name": "중등 수학 A반",
    "company": "ABC 학원",
    "daysOfWeek": ["MONDAY", "FRIDAY"],
    "startTime": "14:00",
    "endTime": "16:00",
    "isActive": true,
    "teacherId": "teacher-uuid",
    "createdAt": "2025-12-09T10:00:00",
    "updatedAt": "2025-12-09T10:00:00"
  }
}
```

### 3.2 목록 조회

**GET** `/api/v1/courses?isActive=true`

**Response** (성공 시 200)
```json
{
  "code": 1000,
  "message": "SUCCESS",
  "data": [
    {
      "id": "uuid-string",
      "name": "중등 수학 A반",
      "company": "ABC 학원",
      "daysOfWeek": ["MONDAY", "FRIDAY"],
      "startTime": "14:00",
      "endTime": "16:00",
      "isActive": true,
      "teacherId": "teacher-uuid",
      "createdAt": "2025-12-09T10:00:00",
      "updatedAt": "2025-12-09T10:00:00"
    }
  ]
}
```

### 3.3 상세 조회

**GET** `/api/v1/courses/{courseId}`

**Response** (성공 시 200)
```json
{
  "code": 1000,
  "message": "SUCCESS",
  "data": {
    "id": "uuid-string",
    "name": "중등 수학 A반",
    "company": "ABC 학원",
    "daysOfWeek": ["MONDAY", "FRIDAY"],
    "startTime": "14:00",
    "endTime": "16:00",
    "isActive": true,
    "teacherId": "teacher-uuid",
    "createdAt": "2025-12-09T10:00:00",
    "updatedAt": "2025-12-09T10:00:00"
  }
}
```

### 3.4 수정

**PATCH** `/api/v1/courses/{courseId}`

**Request Body** (일부 필드만 수정 가능, 모두 선택)
```json
{
  "name": "중등 수학 B반",
  "company": "XYZ 학원",
  "daysOfWeek": ["TUESDAY", "THURSDAY"],
  "startTime": "15:00",
  "endTime": "17:00"
}
```

**Response** (성공 시 200)
```json
{
  "code": 1000,
  "message": "SUCCESS",
  "data": {
    "id": "uuid-string",
    "name": "중등 수학 B반",
    "company": "XYZ 학원",
    "daysOfWeek": ["TUESDAY", "THURSDAY"],
    "startTime": "15:00",
    "endTime": "17:00",
    "isActive": true,
    "teacherId": "teacher-uuid",
    "createdAt": "2025-12-09T10:00:00",
    "updatedAt": "2025-12-09T11:00:00"
  }
}
```

### 3.5 비활성화

**PATCH** `/api/v1/courses/{courseId}/deactivate`

**Response** (성공 시 200)
```json
{
  "code": 1000,
  "message": "SUCCESS",
  "data": null
}
```

### 3.6 활성화

**PATCH** `/api/v1/courses/{courseId}/activate`

**Response** (성공 시 200)
```json
{
  "code": 1000,
  "message": "SUCCESS",
  "data": null
}
```

## 4. Domain Model (Draft)

### 4.1 Course 엔티티

```java
@Entity
@Table(name = "course", indexes = {
    @Index(name = "idx_course_teacher", columnList = "teacher_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Course extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String company;

    @Column(name = "teacher_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherId;

    @ElementCollection
    @CollectionTable(
        name = "course_days",
        joinColumns = @JoinColumn(name = "course_id")
    )
    @Column(name = "day_of_week", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public boolean isOwnedBy(UUID teacherId) {
        return teacherId != null && teacherId.equals(this.teacherId);
    }

    public void update(String name, String company, Set<DayOfWeek> daysOfWeek,
                       LocalTime startTime, LocalTime endTime) {
        if (name != null) this.name = name;
        if (company != null) this.company = company;
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) this.daysOfWeek = daysOfWeek;
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
```

### 4.2 DayOfWeek Enum

`java.time.DayOfWeek` 사용 (MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)

### 4.3 Repository

```java
public interface CourseRepository extends JpaRepository<Course, UUID> {
    Optional<Course> findByIdAndActiveTrue(UUID id);
    Optional<Course> findByIdAndTeacherId(UUID id, UUID teacherId);
    Optional<Course> findByNameIgnoreCaseAndTeacherId(String name, UUID teacherId);
    boolean existsByIdAndTeacherId(UUID id, UUID teacherId);
    List<Course> findByTeacherId(UUID teacherId);
    List<Course> findByTeacherIdAndActive(UUID teacherId, boolean active);
}
```

### 4.4 Service 계층

- `CourseService`: CRUD 비즈니스 로직 처리
  - `createCourse(UUID teacherId, CourseCreateRequest)` → CourseResponse
  - `getCoursesByTeacher(UUID teacherId, Boolean isActive)` → List<CourseResponse>
  - `getCourseById(UUID courseId, UUID teacherId)` → CourseResponse
  - `updateCourse(UUID courseId, UUID teacherId, CourseUpdateRequest)` → CourseResponse
  - `deactivateCourse(UUID courseId, UUID teacherId)` → void
  - `activateCourse(UUID courseId, UUID teacherId)` → void

- **권한 검증 헬퍼 메서드**:
  - `validateTeacher(UUID teacherId)` - Member 존재 및 TEACHER 역할 확인
  - `getCourseOwnedByTeacher(UUID courseId, UUID teacherId)` - Course 소유권 검증

### 4.5 Controller

- `CourseController`: REST API 엔드포인트 제공
  - `POST /api/v1/courses`
  - `GET /api/v1/courses`
  - `GET /api/v1/courses/{courseId}`
  - `PATCH /api/v1/courses/{courseId}`
  - `PATCH /api/v1/courses/{courseId}/deactivate`
  - `PATCH /api/v1/courses/{courseId}/activate`

## 5. TDD Plan

### 5.1 Repository 테스트

1. **Course 저장 및 조회 테스트**
   - Course 엔티티를 저장하고 ID로 조회되는지 검증
   - BaseEntity 필드(id, createdAt, updatedAt)가 자동으로 설정되는지 확인

2. **teacherId로 목록 조회 테스트**
   - `findByTeacherId()`로 특정 Teacher의 모든 Course 조회
   - `findByTeacherIdAndIsActive()`로 활성/비활성 필터링 검증

### 5.2 Service 테스트

1. **Course 생성 테스트**
   - `shouldCreateCourse_whenValidRequest()`
   - 필수 필드 누락 시 예외 발생 검증 (`shouldThrowException_whenNameIsNull()`)

2. **Course 목록 조회 테스트**
   - `shouldReturnCourses_whenTeacherHasCourses()`
   - `shouldReturnEmptyList_whenTeacherHasNoCourses()`
   - `shouldFilterByIsActive_whenFilterProvided()`

3. **Course 상세 조회 테스트**
   - `shouldReturnCourse_whenCourseExists()`
   - `shouldThrowException_whenCourseNotFound()`

4. **Course 수정 테스트**
   - `shouldUpdateCourse_whenValidRequest()`
   - `shouldThrowException_whenNotOwner()` (권한 검증)
   - `shouldThrowException_whenCourseNotFound()`

5. **Course 비활성화 테스트**
   - `shouldDeactivateCourse_whenValidRequest()`
   - `shouldThrowException_whenNotOwner()` (권한 검증)
   - `shouldThrowException_whenCourseNotFound()`

6. **Course 활성화 테스트**
   - `shouldActivateCourse_whenValidRequest()`
   - `shouldThrowException_whenNotOwner()` (권한 검증)
   - `shouldThrowException_whenCourseNotFound()`

### 5.3 Controller 테스트 (MockMvc)

1. **POST /api/v1/courses**
   - `shouldCreateCourse_whenValidRequest()` → 201
   - `shouldReturnBadRequest_whenInvalidRequest()` → 400

2. **GET /api/v1/courses**
   - `shouldReturnCourseList_whenTeacherLoggedIn()` → 200
   - `shouldReturnEmptyList_whenNoCourses()` → 200

3. **GET /api/v1/courses/{courseId}**
   - `shouldReturnCourse_whenCourseExists()` → 200
   - `shouldReturnNotFound_whenCourseNotExists()` → 404

4. **PATCH /api/v1/courses/{courseId}**
   - `shouldUpdateCourse_whenValidRequest()` → 200
   - `shouldReturnForbidden_whenNotOwner()` → 403

5. **PATCH /api/v1/courses/{courseId}/deactivate**
   - `shouldDeactivateCourse_whenValidRequest()` → 200
   - `shouldReturnForbidden_whenNotOwner()` → 403

6. **PATCH /api/v1/courses/{courseId}/activate**
   - `shouldActivateCourse_whenValidRequest()` → 200
   - `shouldReturnForbidden_whenNotOwner()` → 403

### 5.4 통합 테스트

1. **Course 전체 플로우 테스트**
   - Teacher 로그인 → Course 생성 → 목록 조회 → 상세 조회 → 수정 → 비활성화 → 목록에서 제외 확인 → 활성화 → 다시 목록에 포함 확인

2. **권한 검증 통합 테스트**
   - Teacher A가 생성한 Course를 Teacher B가 수정/삭제 시도 → 403 Forbidden

---

## 구현 순서

1. **엔티티 및 Repository** 구현 및 테스트
2. **Service 계층** 구현 및 단위 테스트
3. **Controller** 구현 및 MockMvc 테스트
4. **통합 테스트** 작성 및 실행
5. **문서화** (Swagger 스펙 추가)
