# Feature: Student Monthly Calendar API (Shared/Personal)

## 1. Problem Definition
- Teacher/Assistant need a single endpoint to inspect everything a student experienced in a month, but SharedLesson(반 공통)과 PersonalLesson(개인 진도)이 각기 다른 도메인에 흩어져 있어 조회 흐름이 복잡하다.
- 기존 SharedLesson/PersonalLesson API는 Course나 Student 단독 기준만 제공하므로, 학생 중심 UI의 월간 캘린더를 구성할 데이터가 한 API에서 내려오지 않는다.
- 클리닉 도메인은 아직 미구현이라 이번 범위에서는 공통/개별 진도만 다루되, 추후 ClinicRecord를 확장할 수 있는 response 스키마를 선행 설계해야 한다.

## 2. Requirements

### Functional

#### Endpoint
- **URL**: `GET /api/v1/students/{studentId}/calendar`
  - `{studentId}`: StudentProfile의 ID (UUID)
- **Query Parameters**:
  - `year` (필수, yyyy, 2000~2100 범위)
  - `month` (필수, 1~12)
- **응답**: `sharedLessons[]`, `personalLessons[]`, `clinicRecords[]` (현재는 빈 배열)

#### SharedLesson 수집
- 학생이 속한 **모든 Course의 공통 진도**를 조회한다 (Teacher/Assistant 권한 검증 후 접근 가능).
- 날짜 범위: 해당 월의 1일~말일까지.
- Course가 여러 개라면 `IN` 쿼리로 한 번에 조회한다.
- 각 항목에는 `id`, `courseId`, `courseName`, `date`, `title`, `content`, `writerId`, `writerRole`을 포함한다.

#### PersonalLesson 수집
- `studentId` 직접 필터로 월간 데이터를 조회하고 SharedLesson과 동일한 날짜 범위를 적용한다.
- 응답 항목에는 `id`, `date`, `focus` (선택), `content`, `writerId`, `writerRole` 정보를 포함한다.

#### 권한
- **Teacher**:
  - 본인이 관리하는 학생만 조회 가능
  - 검증: `studentCourseEnrollmentRepository.existsByTeacherIdAndStudentId(teacherId, studentId)`

- **Assistant**:
  - **본인의 Teacher가 관리하는 모든 학생** 조회 가능
  - 검증 로직:
    ```java
    // 1. Assistant의 Member.teacherId 확인
    Member assistant = memberRepository.findById(requesterId);
    if (assistant.getTeacherId() == null) throw ACCESS_DENIED;

    // 2. 해당 Teacher가 관리하는 학생인지 확인
    boolean hasAccess = studentCourseEnrollmentRepository
        .existsByTeacherIdAndStudentId(assistant.getTeacherId(), studentId);
    if (!hasAccess) throw ACCESS_DENIED;
    ```

- **Student**:
  - 1차 릴리스에서는 403 반환 (추후 본인 조회 확장 예정)

#### 에러 처리
- 학생을 찾을 수 없으면 `STUDENT_NOT_FOUND` (404)
- 권한 없으면 `ACCESS_DENIED` (403)
- year/month 유효성 검증 실패 시 `INVALID_INPUT` (400)

### Non-functional

#### 성능
- N+1 방지: `findAllByCourseIdInAndDateBetween`, `findAllByStudentIdAndDateBetween` 사용
- Service에서 `YearMonth.of(year, month)`으로 범위를 계산하고, 범위 계산 로직은 재사용 가능하도록 헬퍼 메서드로 분리한다.

#### 날짜 범위 계산
- `start = LocalDate.of(year, month, 1)` (해당 월 1일)
- `end = YearMonth.of(year, month).atEndOfMonth()` (해당 월 말일)
- Repository 쿼리: `WHERE date BETWEEN :start AND :end` (inclusive)
- 시간대: 서버 기준 LocalDate (Asia/Seoul, application.yml 설정)

#### 응답 정렬
- **섹션별 분리**: `sharedLessons`, `personalLessons`, `clinicRecords` 각각 배열
- **각 섹션 내 정렬**:
  - sharedLessons: `date ASC, courseName ASC`
  - personalLessons: `date ASC, createdAt ASC`
  - clinicRecords: `date ASC` (추후)

#### 색상 구분 전략
- **프론트엔드에서 타입별로 고정 색상 적용**
- 백엔드 응답에 색상 필드 불필요
- 프론트 구현 예시:
  - SharedLesson: 파란색 (#3B82F6)
  - PersonalLesson: 초록색 (#10B981)
  - ClinicRecord: 노란색 (#F59E0B)

#### 확장성
- `StudentCalendarResponse`는 `schemaVersion: 1` 필드를 포함하여 향후 ClinicRecord 추가 시 DTO 컬렉션에 append만 하면 되도록 설계

## 3. API Design

### Endpoint
| Method | URL | Request | Response | Notes |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/students/{studentId}/calendar` | Path `studentId`, Query `year`, `month` | `RsData<StudentCalendarResponse>` | Teacher/Assistant only |

### Request Example
```http
GET /api/v1/students/{studentId}/calendar?year=2025&month=2
Authorization: Bearer <AccessToken>
```

**Parameters**:
- `studentId`: Path variable (UUID)
- `year`: Query param, 2000~2100 범위 정수
- `month`: Query param, 1~12 (1자리일 경우 0패딩 없이 허용)

### Response Example
```json
{
  "code": 1000,
  "message": "성공",
  "data": {
    "schemaVersion": 1,
    "studentId": "uuid",  // StudentProfile.id (학생 프로필 ID)
    "year": 2025,
    "month": 2,
    "sharedLessons": [
      {
        "id": "uuid",
        "courseId": "uuid",
        "courseName": "중3 심화반",
        "date": "2025-02-03",
        "title": "5단원 진도",
        "content": "교재 52~60쪽",
        "writerId": "uuid",
        "writerRole": "TEACHER"
      }
    ],
    "personalLessons": [
      {
        "id": "uuid",
        "date": "2025-02-05",
        "focus": "Functions",
        "content": "개인 클리닉",
        "writerId": "uuid",
        "writerRole": "TEACHER"
      }
    ],
    "clinicRecords": []
  }
}
```

### 학생 검색 UI 구현 가이드
- 학생 선택 드롭다운/자동완성은 **기존 StudentProfile API 재사용**
- Teacher: `GET /api/v1/student-profiles?teacherId={본인ID}`
- Assistant: `GET /api/v1/student-profiles?teacherId={본인의teacherId}`
- 응답에서 `id` (StudentProfile ID), `name` 추출해서 드롭다운 구성
- 선택 시 → `GET /api/v1/students/{studentProfileId}/calendar` 호출
- **중요**: URL path와 response의 `studentId`는 실제로는 `StudentProfile.id`를 의미함

## 4. Domain Model

### 패키지 구조
```
domain/
  calendar/
    web/
      StudentCalendarController.java
    application/
      StudentCalendarQueryService.java
    dto/
      response/
        StudentCalendarResponse.java
        CalendarSharedLessonDto.java
        CalendarPersonalLessonDto.java
```

**신규 패키지 생성 이유**:
- 캘린더는 여러 도메인(SharedLesson, PersonalLesson, ClinicRecord)을 조합하는 독립적인 기능
- 조회 전용 서비스로 CRUD와 책임 분리
- 추후 Teacher 캘린더, Course 캘린더 등 확장 가능

**의존성**:
- `StudentCourseEnrollmentRepository` (domain.studentprofile)
- `SharedLessonRepository` (domain.sharedlesson)
- `PersonalLessonRepository` (domain.personallesson)
- `MemberRepository` (domain.member) - 권한 검증용

### StudentCalendarQueryService (신규)
- **위치**: `domain.calendar.application.StudentCalendarQueryService`
- **메서드**: `getMonthlyCalendar(UUID requesterId, Role requesterRole, UUID studentId, int year, int month)`
- **책임**:
  1. 권한 검증 (Teacher/Assistant)
  2. 학생의 Course 목록 조회
  3. SharedLesson + PersonalLesson batch 조회
  4. DTO 매핑 및 정렬

### Authorization Helpers
- **Teacher 검증**:
  ```java
  studentCourseEnrollmentRepository
      .existsByTeacherIdAndStudentId(teacherId, studentId)
  ```

- **Assistant 검증**:
  ```java
  Member assistant = memberRepository.findById(requesterId);
  if (assistant.getTeacherId() == null) throw ACCESS_DENIED;

  boolean hasAccess = studentCourseEnrollmentRepository
      .existsByTeacherIdAndStudentId(assistant.getTeacherId(), studentId);
  if (!hasAccess) throw ACCESS_DENIED;
  ```

- **Student 검증** (추후 확장):
  ```java
  if (requesterRole == STUDENT && !requesterId.equals(studentId)) {
    throw ACCESS_DENIED;
  }
  ```

### Repository 메서드

#### StudentCourseEnrollmentRepository
- 기존: `existsByTeacherIdAndStudentId(UUID teacherId, UUID studentId)`
- 추가: `List<UUID> findCourseIdsByStudentId(UUID studentId)`

#### SharedLessonRepository
- 추가: `List<SharedLesson> findAllByCourseIdInAndDateBetween(List<UUID> courseIds, LocalDate startDate, LocalDate endDate)`

#### PersonalLessonRepository
- 추가: `List<PersonalLesson> findAllByStudentIdAndDateBetween(UUID studentId, LocalDate startDate, LocalDate endDate)`

### DTO (신규)
- `StudentCalendarResponse`
  - `schemaVersion` (Integer)
  - `studentId` (UUID)
  - `year` (Integer)
  - `month` (Integer)
  - `sharedLessons` (List<CalendarSharedLessonDto>)
  - `personalLessons` (List<CalendarPersonalLessonDto>)
  - `clinicRecords` (List<?>, 현재 빈 배열)

- `CalendarSharedLessonDto`
  - `id`, `courseId`, `courseName`, `date`, `title`, `content`, `writerId`, `writerRole`

- `CalendarPersonalLessonDto`
  - `id`, `date`, `focus`, `content`, `writerId`, `writerRole`

## 5. TDD Plan

### 1. StudentCalendarQueryServiceTest
- `shouldReturnSharedAndPersonalLessons_whenTeacherOwnsStudent()`
- `shouldReturnEmptyLists_whenNoLessonsInMonth()`
- `shouldThrowWhenStudentNotFound()`
- `shouldThrowWhenTeacherHasNoAccess()`
- `shouldAllowAssistant_whenBelongsToSameTeacher()`
- `shouldDenyAssistant_whenDifferentTeacher()`
- `shouldHandleLeapYearFebruary_when2024()`
- `shouldHandleMultipleCourses_whenStudentEnrolledInThree()`

### 2. Repository Tests
- `SharedLessonRepositoryTest.shouldFindByCourseIdsAndDateBetween()`
- `PersonalLessonRepositoryTest.shouldFindByStudentAndDateBetween()`
- `StudentCourseEnrollmentRepositoryTest.shouldReturnCourseIdsByStudent()`

### 3. StudentCalendarControllerTest
- `shouldReturn401_whenUnauthenticated()`
- `shouldReturn403_whenStudentRoleRequests()`
- `shouldReturn403_whenTeacherHasNoAccess()`
- `shouldReturn403_whenAssistantOfDifferentTeacher()`
- `shouldReturn400_whenInvalidMonth()` (0, 13)
- `shouldReturn400_whenYearOutOfRange()` (1999, 2101)
- `shouldReturnCalendar_whenTeacherRequestsValidData()`
- `shouldReturnCalendar_whenAssistantOfSameTeacher()`
- `shouldReturnEmptyCalendar_whenNoData()`

### 4. Future Extension Test
- `shouldReturnEmptyClinicRecords_inCurrentVersion()` (추후 확장 시 회귀 방지)
