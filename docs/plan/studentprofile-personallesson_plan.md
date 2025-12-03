# Feature: StudentProfile & PersonalLesson 관리

## 1. Problem Definition
- Requirement v1.2 기준으로 반(Course)에 속한 학생 정보를 Teacher가 직접 관리하고, 학생 개인별 진도 기록(PersonalLesson)을 남길 수 있는 기능이 없다.
- Invitation을 통해 가입한 Student와의 매핑, 학부모 연락처/학교/학년/클리닉 선호 슬롯 등 학습 운영에 필요한 정보를 영속화해야 한다.
- Course 단위 공통 진도(SharedLesson)와는 달리 학생 단위 히스토리를 관리할 저장소/권한/조회 API가 없어 선생님이 개인별 피드백을 누적할 수 없다.

## 2. Requirements

### Functional
1. **StudentProfile CRUD**
   - Teacher만 `POST /student-profiles`로 학생 프로필 생성 가능.
   - 필수 필드: `courseId`, `name`, `phoneNumber`(학생 연락처/식별자), `assistantId`(담당 조교), `parentPhone`, `schoolName`, `grade`, `age`.
   - 선택 필드: `memberId`(초대 기반 가입 완료 시 연결), `defaultClinicSlotId`(클리닉 기본 슬롯, 추후 ClinicSlot 완성 시 유효성 검사).
   - `GET /student-profiles`에서 courseId, name keyword, isActive(추후) 등 필터 제공.
   - `GET /student-profiles/{profileId}`로 상세 조회, `PATCH`로 정보 수정, `DELETE`(soft delete or deactivate flag)로 비활성화.
   - `GET /courses/{courseId}/students`는 Course 기준 학생 목록을 돌려준다.
2. **PersonalLesson CRUD**
   - Teacher가 `POST /personal-lessons`로 특정 StudentProfile에 개인 진도 작성 (date, title?, content, attachments? → 현재 스펙상 content만 필수, title optional).
   - `GET /personal-lessons`는 `studentProfileId` 필터 필수, 날짜 범위 optional, 최신순 정렬.
   - `GET /personal-lessons/{lessonId}`, `PATCH`, `DELETE` 모두 작성자(Teacher) 또는 해당 Course 책임자만 가능.
   - `GET /student-profiles/{profileId}/personal-lessons`로 학생별 기록만 빠르게 조회.
3. **권한**
   - Teacher는 자신이 담당하는 Course에 속한 StudentProfile만 CRUD 가능.
   - 추후 Assistant 권한이 필요하면 별도 TODO로 확장하되, 본 작업에서는 Teacher 전용으로 고정.
4. **연계**
   - Invitation으로 생성된 Student 계정(memberId)을 StudentProfile에 연결할 수 있어야 하고, 연결 여부는 nullable field로 관리.
   - PersonalLesson 작성 시 StudentProfile이 Course 및 Teacher와 일치하는지 검증한다.

### Non-functional
- 모든 엔티티는 `global.entity.BaseEntity` 상속 + UUID PK, Auditing 필드 사용.
- 요청 바디 검증: 전화번호/학년 포맷, content 길이, date가 미래 가능 여부 등 Validation annotation 활용.
- Repository 레벨에서 soft delete(예: `isActive`) 필터를 기본으로 적용하도록 Specification/Query 지원.
- API는 향후 Student/Assistant 권한 확장을 염두에 두고 Service 계층에서 권한 체크를 중앙화한다.
- 리스트 API는 페이지네이션(Pageable)과 정렬 지원으로 대규모 반에서도 사용 가능하게 한다.

## 3. API Design (Draft)

| Method | URL | Request | Response | Notes |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/student-profiles` | `{ courseId, name, phoneNumber, assistantId, parentPhone, schoolName, grade, age, memberId?, defaultClinicSlotId? }` | `StudentProfileResponse` | Teacher 전용, courseId/assistant 소유 검증 |
| GET | `/api/v1/student-profiles` | `courseId?`, `name?`, `page` | `Page<StudentProfileSummary>` | Teacher 소유 Course만 반환 |
| GET | `/api/v1/student-profiles/{profileId}` | - | `StudentProfileResponse` | |
| PATCH | `/api/v1/student-profiles/{profileId}` | 변경 필드 subset | `StudentProfileResponse` | |
| DELETE | `/api/v1/student-profiles/{profileId}` | - | `RsData<Void>` | 기본은 soft delete (isActive=false) |
| GET | `/api/v1/courses/{courseId}/students` | - | `List<StudentProfileSummary>` | 리스트 뷰 최적화 |
| POST | `/api/v1/personal-lessons` | `{ studentProfileId, date, content, attachmentUrls? }` | `PersonalLessonResponse` | Teacher 전용 |
| GET | `/api/v1/personal-lessons` | `studentProfileId`, `from?`, `to?`, `page` | `Page<PersonalLessonSummary>` | studentProfileId 필수 |
| GET | `/api/v1/student-profiles/{profileId}/personal-lessons` | `from?`, `to?`, `page` | 동일 | Nested endpoint shortcut |
| GET/PATCH/DELETE | `/api/v1/personal-lessons/{lessonId}` | - / subset / - | 상세 응답 | 작성자 또는 해당 Course Owner만 |

`StudentProfileResponse` 예시: `{ id, courseId, name, phoneNumber, assistantId, parentPhone, schoolName, grade, age, memberId, defaultClinicSlotId, createdAt, updatedAt }`  
`PersonalLessonResponse`: `{ id, studentProfileId, writerId, writerName, courseId, date, content, attachments, createdAt }`

## 4. Domain Model (Draft)

- **StudentProfile**
  - Fields: `UUID id`, `Course course`, `Member member`(nullable, role=STUDENT, 초대 완료 후 연결), `String name`, `String phoneNumber`, `Member assistant`(role=ASSISTANT), `String parentPhone`, `String schoolName`, `String grade`, `Integer age`, `ClinicSlot defaultClinicSlot`(nullable), `boolean active`.
  - Validations: parentPhone format, grade enumerations(예: ELEMENTARY_5 등) or string; phoneNumber uniqueness per course; assistant가 해당 Teacher의 조교인지 검사; defaultClinicSlot는 같은 Teacher의 Slot만 연결 허용.
  - Repository: `StudentProfileRepository` with `findAllByCourseIdAndActive`, `findByIdAndCourseTeacherId`, `existsByCourseIdAndPhoneNumber`.
- **PersonalLesson**
  - Fields: `UUID id`, `StudentProfile ownerProfile`, `Course course`(derivable from profile), `Member writer`, `LocalDate date`, `String content`, `List<LessonAttachment>`(Optional, future).
  - Repository: `PersonalLessonRepository` with `findAllByOwnerProfileIdAndDateBetween`, `findByIdAndCourseTeacherId`.
- **Services**
  - `StudentProfileService`
    - `createStudentProfile(Teacher teacher, StudentProfileCreateRequest req)`
    - `updateStudentProfile(Teacher teacher, UUID profileId, StudentProfileUpdateRequest req)`
    - `deactivateStudentProfile(Teacher teacher, UUID profileId)`
    - `getProfiles(Teacher teacher, StudentProfileSearchCondition cond, Pageable pageable)`
  - `PersonalLessonService`
    - `createPersonalLesson(Teacher teacher, PersonalLessonCreateRequest req)`
    - `updatePersonalLesson(...)`, `deletePersonalLesson(...)`, `getPersonalLessons(...)`.
  - 권한 검증을 별도 helper(`TeacherOwnershipValidator`)로 추출해 Course ↔ Teacher 일치 여부 재사용.
- **DTO/Mapper**
  - Request DTO에 Bean Validation.
  - Response DTO `StudentProfileSummary` (id, name, parent info, courseName) & `PersonalLessonSummary` (id, date, writerName, preview).

## 5. TDD Plan
1. **StudentProfileServiceTest**
   - Teacher가 자신의 Course로 생성 성공.
   - 다른 Teacher의 CourseId를 넘기면 `RsCode.ACCESS_DENIED`.
   - 필수 필드 검증 실패 시 ValidationException(연락처 중복, 조교 권한 불일치 등).
   - update/deactivate 흐름 검증.
2. **StudentProfileControllerTest**
   - Authenticated Teacher 요청만 허용, 401/403 케이스.
   - Pageable 목록/필터 동작.
3. **PersonalLessonServiceTest**
   - StudentProfile가 Teacher Course에 속할 때만 생성 허용.
   - 날짜 범위 필터 및 최신순 정렬 검증.
   - 수정/삭제 시 본인 작성 또는 Course Owner만 허용.
4. **PersonalLessonControllerTest**
   - Request validation, 권한 체크, 필터 query parameter.
5. **Integration / Repository**
   - JPA mapping(Aggregation) 확인, soft delete 필터 동작, StudentProfile 삭제 후 PersonalLesson orphan 처리(기본: on delete restrict).  
   - 추후 E2E에서 Auth → StudentProfile → PersonalLesson 플로우를 검증할 Playwright 시나리오에 대비해 Seed 데이터를 주입하는 Fixture 구성.
