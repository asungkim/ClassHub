# Feature: StudentProfile ↔ Course M:N Enrollment

## 1. Problem Definition
- 현재 `StudentProfile`이 `courseId` 단일 필드로만 Course를 참조해 “학생=1개 반” 구조에 묶여 있다.
- 학생이 여러 반에 속하거나, 반 간 이동 이력을 남기려면 Course ↔ StudentProfile 구조를 M:N 으로 확장해야 한다.
- 기존 API (학생 등록/수정, Course별 학생 조회 등)는 새 Enrollment 구조를 기반으로 재동작해야 하며, seed 데이터 및 테스트도 일관되게 업데이트되어야 한다.

## 2. Requirements

### Functional
1. **새 엔티티 `StudentCourseEnrollment`**
   - 필드: `studentProfileId (UUID)`, `courseId (UUID)`, `teacherId (Course.teacherId)`, `assignedAt (LocalDateTime)`, `assignedBy (UUID)`, `status (enum: ACTIVE, INACTIVE)`, `primary (boolean, 기본 true)` 등.
   - `StudentProfile` ↔ `Course` 관계를 이 엔티티를 통해서만 관리한다.
   - Course가 삭제되면 해당 Course의 Enrollment도 함께 삭제된다. 학생 프로필 삭제 시에도 연관 Enrollment를 비활성화.

2. **StudentProfile API 수정**
   - 생성/수정 DTO에서 `courseId` 필드는 “primaryCourseId”로 유지하되, 내부적으로 Enrollment를 생성/업데이트.
   - 이미 primary Enrollment가 존재하면 업데이트 시 `primary=true`를 새 Course에 지정하고 기존은 `primary=false` 처리.
   - 전화번호 중복 검증은 “Teacher + Course 조합 내 unique” 조건을 Enrollment 기준으로 변경.

3. **조회 API 업데이트**
   - `GET /api/v1/student-profiles?courseId=...` 는 Enrollment를 조인해 Course 필터링.
   - `GET /api/v1/courses/{courseId}/students` 는 Enrollment 테이블을 기준으로 학생 목록 반환 (기존 StudentProfileRepository 메서드 수정).

4. **도메인 서비스 영향**
   - StudentProfileService: EnrollmentRepository 주입, `assignPrimaryCourse` 로직 추가.
   - PersonalLessonService: StudentProfile → Course 소유 검증 시 Enrollment를 참조하거나 StudentProfile에 `teacherId` 필드를 계속 활용하되, Course 관계는 Enrollment로 확인.

5. **InitData & Seed**
   - `StudentProfileInitData` 생성 시 StudentCourseEnrollment를 함께 생성하여 과거 구조와 동일한 결과가 되도록 한다.
   - 기존 seed에서 “학생 1~30명”을 각 반에 배정하던 로직을 Enrollment 생성으로 대체.

### Non-functional
- Enrollment 테이블 인덱스: `(student_profile_id, course_id)` unique, `(course_id, primary)` 필터 인덱스.
- Migration/마이그레이션 시 기존 데이터에서 `courseId`를 Enrollment 레코드로 변환하는 데이터 스크립트를 준비 (이번 작업에서는 seed/test만 대상으로 함).
- Service/Repository 테스트는 모두 Enrollment 기준으로 갱신하고, N+1 이슈 없도록 fetch 전략 단순화(ID 기반 조회 유지).

## 3. API Design (Draft)
| Endpoint | 변경 사항 |
| --- | --- |
| `POST /api/v1/student-profiles` | body에 `courseId`를 계속 받지만, 응답 `StudentProfileResponse`에는 `primaryCourseId`, `enrolledCourseIds` 필드 추가 |
| `PATCH /api/v1/student-profiles/{id}` | courseId 변경 시 Enrollment 업데이트, 기존 Course는 `primary=false` |
| `GET /api/v1/student-profiles?courseId=` | Enrollment 기준으로 필터 (새 Repository 메서드 사용) |
| `GET /api/v1/courses/{courseId}/students` | Enrollment에서 ACTIVE + primary 기준으로 학생 반환 |
| (추가 고려) `GET /api/v1/student-profiles/{id}/courses` | 학생이 속한 Course 목록 조회 (필요시) |

## 4. Domain Model (Draft)
- **StudentProfile**
  - 기존 `courseId` 필드는 `primaryCourseId` (nullable) 로 변경하거나 제거.
  - `assignPrimaryCourse(UUID courseId)`는 EnrollmentService를 통해 처리.
- **StudentCourseEnrollment**
  - PK `UUID id`, FK `studentProfileId`, FK `courseId`, `UUID teacherId`, `Boolean primary`, `EnrollmentStatus status`, `LocalDateTime assignedAt`.
  - Service 계층에서 생성/비활성화/재할당 메서드 제공.
- **Repositories**
  - `StudentCourseEnrollmentRepository`:
    - `List<StudentCourseEnrollment> findByStudentProfileIdAndStatus(UUID profileId, EnrollmentStatus status)`
    - `Optional<StudentCourseEnrollment> findByStudentProfileIdAndPrimaryTrue(UUID profileId)`
    - `Page<StudentProfile> findAllByTeacherIdAndCourseId(UUID teacherId, UUID courseId, Pageable pageable)` (쿼리 dsl 또는 JPA @Query)
- **Services**
  - `StudentCourseEnrollmentService` (내부용): primary enforcement, 권한 체크.
  - `StudentProfileService`: EnrollmentService 호출, DTO ↔ 응답 매핑에서 enrolled course list 포함.

## 5. TDD Plan
1. **Repository Tests**
   - EnrollmentRepositoryTest: primary uniqueness, course filtering, 상태 변경 확인.
   - StudentProfileRepositoryTest: `findAllByTeacherIdAndCourseId`가 Enrollment join 기반으로 정상 동작.

2. **Service Tests**
   - StudentProfileServiceTest:
     - 생성 시 Enrollment 생성/primary 표시.
     - 코스 변경 시 기존 primary false, 새 Course primary true.
     - 코스별 검색/dup phone 검증 로직이 Enrollment 기반으로 작동.
   - StudentCourseEnrollmentServiceTest (신규):
     - `assignPrimaryCourse`/`removeCourse` 시나리오.

3. **Controller Tests**
   - StudentProfileControllerTest: 코스 변경, Course 필터로 GET 시 정상 동작.
   - CourseControllerTest (학생 목록 API): Enrollment 기반 데이터 확인.

4. **InitData Tests/Validation**
   - Seed 실행 후 Enrollment 수 체크 (teacher 별로 기대 값과 일치).
   - PersonalLesson/Invitation 테스트에서 Enrollment 데이터가 자동 생성되는지 확인.
