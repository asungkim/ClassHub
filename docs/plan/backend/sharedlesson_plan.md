# Feature: SharedLesson (Course 공통 진도) 백엔드

## 1. Problem Definition

- Course별 공동 진도를 관리할 저장소가 없어 Teacher가 수업별 진행 현황을 일관되게 기록할 수 없다.
- 현재 PersonalLesson은 학생 개별 기록만 제공하므로, Course 전체에 공통 공지·커리큘럼을 남길 수 있는 구조가 필요하다.
- Course ↔ SharedLesson 간 1:N 종속 관계를 명확히 해 SharedLesson CRUD + 권한 제어를 제공해야 한다.

## 2. Requirements

### Functional

- **엔티티**
  - `SharedLesson`는 `BaseEntity` 상속 + `UUID id`.
  - 필드: `course (Course, @ManyToOne)`, `writerId (UUID, Teacher만 허용)`, `date (LocalDate)`, `title (String, 100자)`, `content (Text)`.
- Course 삭제 시 SharedLesson도 함께 삭제되며(물리 삭제), Course 비활성화일 경우 SharedLesson은 그대로 유지되고 조회 가능.
- `date` 값이 요청에 없으면(null) 기본적으로 현재 날짜(LocalDate.now())를 사용한다. 프론트엔드에서는 DatePicker에 오늘 날짜를 기본값으로 표시하되 사용자가 수정 가능.
- **권한**
  - Teacher(소유자)만 CRUD 가능. Assistant는 권한 없음.
  - Controller 레벨 `@PreAuthorize("hasAuthority('TEACHER')")`, Service에서 Course 소유권 검증.
- **API/동작**
  1. 생성 `POST /api/v1/shared-lessons`: Course ID, 날짜, 제목, 내용을 받아 SharedLesson 저장.
  2. 목록 `GET /api/v1/shared-lessons?courseId&from&to`: 특정 Course 기준, 날짜 범위 필터 + `PageResponse`.
  3. 상세 `GET /api/v1/shared-lessons/{id}`: Course ownership 검증 후 반환.
  4. 수정 `PATCH /api/v1/shared-lessons/{id}`: 제목/내용/날짜 수정, writerId는 변경 불가.
  5. 삭제 `DELETE /api/v1/shared-lessons/{id}`: 실제 삭제 (물리 삭제). 프론트엔드에서 삭제 확인 모달 제공.
- **Validation**
  - `title` 1~100자, `content` 최소 1자/최대 4000자, `date`는 오늘 이후 허용 (과거 기록도 가능하도록 제한 X).
  - CourseId는 필수이며 Teacher가 소유해야 한다.

### Non-functional

- JPA는 `@ManyToOne(fetch = LAZY, optional = false)`로 Course와 연결. Course 삭제 시 DB FK constraint로 cascade 삭제 처리 (단방향 관계 유지).
- Repository는 PersonalLessonRepository 패턴 참고: 날짜 필터 유무에 따라 `Between` 메서드와 전체 조회 메서드 분리.
- API 응답은 `RsData` + DTO(`SharedLessonResponse`, `SharedLessonSummary`).
- 목록은 기본 정렬 `date desc, createdAt desc`.

## 3. API Design (Draft)

| Method | URL                           | Request                                             | Response                                    | Notes                 |
| ------ | ----------------------------- | --------------------------------------------------- | ------------------------------------------- | --------------------- |
| POST   | `/api/v1/shared-lessons`      | body `SharedLessonCreateRequest`                    | `RsData<SharedLessonResponse>` (201)        | Teacher만             |
| GET    | `/api/v1/shared-lessons`      | query: `courseId`(필수), `from?`, `to?`, `pageable` | `RsData<PageResponse<SharedLessonSummary>>` | Teacher만             |
| GET    | `/api/v1/shared-lessons/{id}` | path id                                             | `RsData<SharedLessonResponse>`              | Course ownership 검증 |
| PATCH  | `/api/v1/shared-lessons/{id}` | body `SharedLessonUpdateRequest`                    | `RsData<SharedLessonResponse>`              | 부분 업데이트         |
| DELETE | `/api/v1/shared-lessons/{id}` | -                                                   | `RsData<Void>`                              | 실제 삭제             |

### Request/Response 예시

- `SharedLessonCreateRequest`: `{ "courseId": "UUID", "date": "2025-12-15", "title": "3주차 공통 진도", "content": "교재 52~60쪽" }`
- `SharedLessonSummary`: `{ id, courseId (Course.id에서 추출), date, title, createdAt }`
- `SharedLessonResponse`: Summary + `content`, `writerId`, `updatedAt`.

## 4. Domain Model (Draft)

- **SharedLesson**
  - `UUID id`
  - `Course course` (`@ManyToOne(fetch = LAZY, optional = false)`)
  - `UUID writerId` (작성자, Teacher만 허용)
  - `LocalDate date` (Service에서 null이면 LocalDate.now() 적용)
  - `String title`
  - `String content`
  - `BaseEntity` → `createdAt`, `updatedAt`
- **Repository** (정규화 유지, course 엔티티 참조)
  - `Page<SharedLesson> findAllByCourse_IdAndWriterIdAndDateBetween(UUID courseId, UUID writerId, LocalDate start, LocalDate end, Pageable pageable)`
  - `Page<SharedLesson> findAllByCourse_IdAndWriterId(UUID courseId, UUID writerId, Pageable pageable)`
  - `Optional<SharedLesson> findByIdAndWriterId(UUID id, UUID writerId)`
- **DTO**
  - `SharedLessonCreateRequest(UUID courseId, LocalDate date, String title, String content)` - date nullable
  - `SharedLessonUpdateRequest(LocalDate date, String title, String content)` - 모두 nullable, 부분 업데이트
  - `SharedLessonResponse`, `SharedLessonSummary`
- **Service 흐름**
  - 생성: Course 소유권 검증 → date null이면 LocalDate.now() → SharedLesson 저장
  - 목록: Course 소유권 검증 → 날짜 필터 유무에 따라 repository 메서드 분기 → DTO 변환
  - 상세/수정/삭제: `findByIdAndWriterId`로 권한 검증 및 조회

## 5. TDD Plan

### 5.1 RepositoryTest (SharedLessonRepositoryTest)

1. `shouldFindAllByCourseAndWriter_whenNoDateFilter()`
   - 특정 Course + Writer로 전체 조회, 정렬 확인 (date desc, createdAt desc)
2. `shouldFindAllWithDateBetween_whenDateFilterProvided()`
   - 날짜 범위 필터링 정상 동작 확인
3. `shouldFindByIdAndWriterId_whenExists()`
   - ID + writerId로 단건 조회 성공
4. `shouldReturnEmpty_whenWriterMismatch()`
   - 다른 Teacher의 SharedLesson 조회 시 empty 반환

### 5.2 ServiceTest (SharedLessonServiceTest)

**생성 (createLesson)**

1. `shouldCreateSharedLesson_whenValidRequest()`
   - 정상 생성, date가 제공된 경우
2. `shouldCreateWithTodayDate_whenDateIsNull()`
   - date null이면 LocalDate.now() 적용 확인
3. `shouldFailToCreate_whenCourseNotOwnedByTeacher()`
   - 타 Teacher의 Course → COURSE_NOT_FOUND 예외
4. `shouldFailToCreate_whenCourseNotFound()`
   - 존재하지 않는 courseId → COURSE_NOT_FOUND 예외

**목록 조회 (getLessons)** 5. `shouldGetLessons_withDateFilter()`

- from/to 파라미터로 필터링 확인

6. `shouldGetLessons_withoutDateFilter()`
   - from/to null이면 전체 조회
7. `shouldFailToGetLessons_whenCourseNotOwned()`
   - 타 Teacher Course → 예외

**상세 조회 (getLesson)** 8. `shouldGetLesson_whenOwnerRequests()`

- 정상 조회

9. `shouldFailToGetLesson_whenNotOwner()`
   - SHARED_LESSON_NOT_FOUND 예외

**수정 (updateLesson)** 10. `shouldUpdateLesson_whenOwnerRequests()` - title, content, date 수정 확인 11. `shouldFailToUpdate_whenNotOwner()` - SHARED_LESSON_NOT_FOUND 예외

**삭제 (deleteLesson)** 12. `shouldDeleteLesson_whenOwnerRequests()` - 물리 삭제 확인 13. `shouldFailToDelete_whenNotOwner()` - SHARED_LESSON_NOT_FOUND 예외

### 5.3 ControllerTest (SharedLessonControllerTest)

**인증 & 권한**

1. `shouldReturn401_whenNotAuthenticated()` - 인증 없이 요청 시
2. `shouldReturn403_whenNotTeacher()` - STUDENT/ASSISTANT 역할 요청 시

**Validation** 3. `shouldReturn400_whenTitleTooLong()` - title 101자 이상 4. `shouldReturn400_whenTitleEmpty()` - title 빈 문자열 5. `shouldReturn400_whenContentEmpty()` - content 빈 문자열 6. `shouldReturn400_whenCourseIdNull()` - courseId 누락

**기능** 7. `shouldCreateSharedLesson_whenValidRequest()` - 201 Created 8. `shouldGetPagedLessons_withDateFilter()` - 쿼리 파라미터 검증 9. `shouldUpdateLesson_whenValidRequest()` - 200 OK 10. `shouldDeleteLesson_whenValidRequest()` - 200 OK

### 5.4 RsCode 추가

- `SHARED_LESSON_NOT_FOUND` (404, "공통 진도를 찾을 수 없습니다")
- 기존 `COURSE_NOT_FOUND`, `UNAUTHORIZED` 재사용
