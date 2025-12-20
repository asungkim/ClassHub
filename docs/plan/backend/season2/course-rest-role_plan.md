# Feature: Course REST API for Admin/Assistant/Student

## 1. Problem Definition

- Teacher 전용 Course API만 존재해 다른 역할(Admin/Assistant/Student)이 필요한 데이터를 조회하거나 관리할 수 없다.
- 관리자(Admin)는 모든 반을 감시·정리해야 하며, 잘못된 Course를 완전히 삭제(hard delete)할 수 있어야 한다.
- 조교(Assistant)는 본인과 연결된 선생님들의 반 정보를 확인해 클리닉/수업 준비를 해야 한다.
- 학생(Student)은 공개된 Course를 키워드/지역/시간대 기준으로 검색해 상세 정보 없이도 수강 여부를 판단해야 한다.
- 요구 출처: `docs/requirement/v1.3.md`와 `docs/spec/v1.3.md`의 Course 관리 항목, `docs/design/final-entity-spec.md`의 Course/Branch/Company 구조.

## 2. Requirements

### Functional

1. **Admin**
   - 모든 Course 목록을 Teacher/Branch/Company/상태/검색어 기준으로 조회할 수 있다.
   - Course 데이터가 잘못됐을 때 hard delete API로 완전히 제거할 수 있다(Teacher soft delete와 구분).
2. **Assistant**
   - 본인 계정과 TeacherAssistantAssignment 로 연결된 선생님들의 Course를 필터링(teacherId, 상태)하여 조회할 수 있다.
   - 반환 데이터에는 Course 요약(이름, 기간, 스케줄, 회사/지점명)과 Teacher 식별 정보가 포함된다.
3. **Student**
   - 공개 Course만을 검색할 수 있다(활성 Course + 공개 지점).
   - 필터: `companyId`, `branchId`, `teacherId`, `keyword`(Course 명), `page/size`.
   - 응답 필수 필드: Course 이름, Teacher 이름, 회사명, 지점명, 스케줄 요약.
   - 상세 조회는 이번 범위에서 제외한다.

### Non-functional

- 모든 응답은 `RsData` 규약을 따른다.
- 인증: Admin → ADMIN ROLE, Assistant → ASSISTANT ROLE + 자기 Assignment 검증, Student → PUBLIC(로그인 불필요)이나 STUDENT ROLE.
- Pagination: `PageResponse` 포맷, 기본 size 10.
- Hard delete 시 Audit 로그 남기고 soft delete/활성상태와 구분.

## 3. API Design (Draft)

| Method | URL                                | 설명                      | 요청 파라미터/Body                                                                                                          | 응답                                                                 |
| ------ | ---------------------------------- | ------------------------- | --------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| GET    | `/api/v1/admin/courses`            | 전체 Course 페이징 조회   | query: `teacherId?`, `branchId?`, `companyId?`, `status? (ACTIVE/INACTIVE/ALL)`, `keyword?`, `page`, `size`                 | `RsData<PageResponse<CourseResponse>>`                               |
| DELETE | `/api/v1/admin/courses/{courseId}` | Course hard delete        | path: `courseId`                                                                                                            | `RsData<Void>`                                                       |
| GET    | `/api/v1/assistants/me/courses`    | 연결된 선생님 Course 목록 | query: `teacherId?`, `status?`, `page`, `size`                                                                              | `RsData<PageResponse<CourseResponseWithTeacher>>` (teacherName 포함) |
| GET    | `/api/v1/courses/public`           | 학생 공개 Course 검색     | query: `companyId?`, `branchId?`, `teacherId?`, `keyword?`, `page`, `size` | `RsData<PageResponse<PublicCourseResponse>>`                         |

## 4. Domain Model (Draft)

- **Course**: 기존 Teacher API와 동일. 추가적으로 Admin hard delete 시 `CourseRepository.deleteById` 사용, soft delete는 건드리지 않음.
- **TeacherAssistantAssignment**: Assistant API에서 teacherId 목록을 얻기 위해 사용. Query 시 다중 teacherId IN 조건.
- **Public Course 기준**
  - Course.active = true
  - Company/Branch 가 `VerifiedStatus.VERIFIED` 또는 `onlyVerified=false`일 때 조건 완화.
- **Response DTO**
  - `CourseResponseWithTeacher`: CourseResponse + `teacherId`, `teacherName`.
  - `PublicCourseResponse`: CourseResponse + `teacherName`, `companyName`, `branchName`, `scheduleSummary`.

## 5. TDD Plan

1. **AdminCourseControllerTest**
   - `GET /api/v1/admin/courses` 필터 조합별 응답 검증.
   - `DELETE /api/v1/admin/courses/{courseId}` 성공/존재하지 않는 ID/권한 없는 경우.
2. **AdminCourseServiceTest**
   - Repository mock으로 조건별 조회, hard delete 시 존재 확인 후 삭제.
3. **AssistantCourseControllerTest**
   - 연결된 teacherId만 노출되는지, teacherId 필터 동작 여부, status 필터.
4. **AssistantCourseServiceTest**
   - TeacherAssistantAssignmentRepository mock으로 권한 검증, CourseRepository custom query 검증.
5. **PublicCourseControllerTest**
   - 필터별 검색(company/branch/teacher/keyword) 및 빈 결과.
6. **PublicCourseServiceTest**
   - Specification/QueryDSL 로직 단위 테스트: 공개 Course 조건, company/branch/teacher 필터, keyword 검색.

## 6. 3단계 개발 순서
1. **1단계 – Admin 기능**
   - `AdminCourseService`/`AdminCourseController` 추가, 전체 Course 조회 + hard delete API 구현.
   - CourseRepository에 Admin 필터(teacherId/branchId/companyId/status/keyword) 쿼리 작성.
   - ROLE_ADMIN 보안 체크 및 TDD (Controller/Service) 완료.
2. **2단계 – Assistant 기능**
   - TeacherAssistantAssignment 기반 권한 검증을 포함한 `AssistantCourseService`/Controller 구현.
   - CourseResponseWithTeacher DTO 도입 및 teacherId/status 필터 동작 테스트.
3. **3단계 – Student 공개 검색**
   - Public Course 전용 Service/Controller 구현, 활성 Course + 공개 지점 조회 쿼리 작성.
   - company/branch/teacher/keyword 필터, 페이지네이션, 응답 DTO 검증 및 통합 테스트.
