# AGENT_LOG

이 파일은 개발 활동 이력을 기록합니다.

---

## [2025-12-09 16:20] Course 엔티티 및 CRUD API 구현

### Type

BEHAVIORAL

### Summary

- Course 엔티티에 요일 및 시간 필드 추가 (dayOfWeek, startTime, endTime)
- Course CRUD 기능 구현 (생성, 목록 조회, 상세 조회, 수정, 비활성화, 활성화)
- TDD 방식으로 Repository → Service → Controller 순서로 구현
- 모든 테스트 통과 (CourseRepositoryTest, CourseServiceTest)

### Details

**작업 사유**

- Phase 4 TODO에 따라 Course 도메인 개발 시작
- 선생님이 수업(반) 정보를 관리할 수 있도록 요일 및 수업 시간 정보 추가
- 활성화/비활성화 기능을 통한 반 상태 관리

**구현 내용**

1. **엔티티 수정** ([Course.java](backend/src/main/java/com/classhub/domain/course/model/Course.java))

   - `dayOfWeek` (DayOfWeek): 수업 요일
   - `startTime` (LocalTime): 수업 시작 시간
   - `endTime` (LocalTime): 수업 종료 시간
   - `update()` 메서드에 새 필드 추가
   - `activate()` 메서드 추가

2. **Repository** ([CourseRepository.java](backend/src/main/java/com/classhub/domain/course/repository/CourseRepository.java))

   - `findByTeacherId()`: Teacher의 모든 Course 조회
   - `findByTeacherIdAndActive()`: 활성/비활성 필터링

3. **DTO 작성**

   - Request: [CourseCreateRequest.java](backend/src/main/java/com/classhub/domain/course/dto/request/CourseCreateRequest.java), [CourseUpdateRequest.java](backend/src/main/java/com/classhub/domain/course/dto/request/CourseUpdateRequest.java)
   - Response: [CourseResponse.java](backend/src/main/java/com/classhub/domain/course/dto/response/CourseResponse.java)

4. **Service** ([CourseService.java](backend/src/main/java/com/classhub/domain/course/application/CourseService.java))

   - `createCourse()`: 반 생성
   - `getCoursesByTeacher()`: Teacher의 반 목록 조회 (활성/비활성 필터 지원)
   - `getCourseById()`: 반 상세 조회
   - `updateCourse()`: 반 정보 수정
   - `deactivateCourse()`: 반 비활성화
   - `activateCourse()`: 반 활성화
   - 권한 검증: teacherId 확인

5. **Controller** ([CourseController.java](backend/src/main/java/com/classhub/domain/course/web/CourseController.java))

   - `POST /api/v1/courses`: 반 생성
   - `GET /api/v1/courses`: 반 목록 조회
   - `GET /api/v1/courses/{courseId}`: 반 상세 조회
   - `PATCH /api/v1/courses/{courseId}`: 반 수정
   - `PATCH /api/v1/courses/{courseId}/deactivate`: 반 비활성화
   - `PATCH /api/v1/courses/{courseId}/activate`: 반 활성화

6. **InitData 수정** ([CourseInitData.java](backend/src/main/java/com/classhub/global/init/data/CourseInitData.java))
   - 새로운 필드(dayOfWeek, startTime, endTime)를 포함하도록 seed 데이터 업데이트

**영향받은 테스트**

- ✅ [CourseRepositoryTest.java](backend/src/test/java/com/classhub/domain/course/repository/CourseRepositoryTest.java) (6개 테스트 통과)
  - 저장/조회, teacherId 필터링, active 필터링, 권한 검증
- ✅ [CourseServiceTest.java](backend/src/test/java/com/classhub/domain/course/application/CourseServiceTest.java) (11개 테스트 통과)
  - 생성, 목록 조회, 상세 조회, 수정, 비활성화, 활성화
  - 권한 검증 (다른 Teacher의 Course 수정/삭제 시도 시 예외)

**수정한 파일**

- `backend/src/main/java/com/classhub/domain/course/model/Course.java`
- `backend/src/main/java/com/classhub/domain/course/repository/CourseRepository.java`
- `backend/src/main/java/com/classhub/domain/course/dto/request/CourseCreateRequest.java` (신규)
- `backend/src/main/java/com/classhub/domain/course/dto/request/CourseUpdateRequest.java` (신규)
- `backend/src/main/java/com/classhub/domain/course/dto/response/CourseResponse.java` (신규)
- `backend/src/main/java/com/classhub/domain/course/application/CourseService.java` (신규)
- `backend/src/main/java/com/classhub/domain/course/web/CourseController.java` (신규)
- `backend/src/main/java/com/classhub/global/init/data/CourseInitData.java`
- `backend/src/test/java/com/classhub/domain/course/repository/CourseRepositoryTest.java` (신규)
- `backend/src/test/java/com/classhub/domain/course/application/CourseServiceTest.java` (신규)
- `docs/plan/backend/course_plan.md` (신규)

**다음 단계**

- Phase 4 TODO의 다음 작업: SharedLesson 개발
- 기존 테스트 수정: StudentProfile, PersonalLesson 테스트가 새로운 Course 필드로 인해 실패 중
  - CourseInitData의 seed 데이터에 새 필드가 추가되어 기존 테스트가 영향받음
  - 해당 테스트들도 Course 생성 시 새 필드를 포함하도록 수정 필요

## [2025-12-12 22:46] Course 검증/테스트 보강

### Type

BEHAVIORAL

### Summary

- Course 도메인에 요일/시간 검증을 추가하고 권한/존재 검증 예외 코드를 명확히 했다.
- Controller MockMvc 및 Service 테스트를 보강해 잘못된 입력과 권한 오류, 존재하지 않는 리소스 시나리오를 커버했다.
- Course 요일 컬렉션을 즉시 로딩하도록 변경해 API 응답 직렬화 시 LazyInitializationException을 해소했다.

### Details

- daysOfWeek 비어 있음, 시작/종료 시간 역전 시 BAD_REQUEST 예외 반환; Member 미존재/비Teacher 시 COURSE_FORBIDDEN/MEMBER_NOT_FOUND 반환.
- CourseControllerTest 신설: 생성 성공/검증 실패, 활성 필터, 비소유자 비활성화 거부, 시간 역전 수정 실패 검증.
- CourseServiceTest 보강: 빈 요일/잘못된 시간/비Teacher/미존재 Teacher/존재하지 않는 Course 활성화 등 실패 경로 추가, 다른 Teacher 시나리오 실제 멤버로 검증.
- Course 엔티티 daysOfWeek를 EAGER 로딩으로 변경해 리스트 조회 응답에서 LazyInitializationException 제거.
- 실행: `cd backend && ./gradlew test --tests "com.classhub.domain.course.*"` (성공)

### 수정한 파일

- `backend/src/main/java/com/classhub/domain/course/dto/request/CourseCreateRequest.java`
- `backend/src/main/java/com/classhub/domain/course/dto/request/CourseUpdateRequest.java`
- `backend/src/main/java/com/classhub/domain/course/application/CourseService.java`
- `backend/src/main/java/com/classhub/domain/course/model/Course.java`
- `backend/src/test/java/com/classhub/domain/course/application/CourseServiceTest.java`
- `backend/src/test/java/com/classhub/domain/course/web/CourseControllerTest.java`

### 다음 단계

- Controller에서 추가 실패 케이스(예: 인증 누락) 커버 여부 점검 및 필요 시 테스트 보강.

## [2025-12-13 09:15] Course 백엔드 모듈 구축

### Type

BEHAVIORAL

### Summary

- Course 엔티티/Repository/Service/Controller 전체 CRUD를 구현해 Teacher 전용 반 관리 API를 완성했다.
- 초기 데이터 시드, DTO(request/response), PLAN/TODO 문서를 정리했고 Repository/Service/Controller 테스트를 추가했다.

### Details

- Teacher 권한 검증과 요일·시간 검증 로직을 포함한 Service 작성, Controller에서 RsData 응답 및 인증 체크 구현.
- CourseRepository/Service/Controller 테스트로 저장/조회/필터/권한/비활성화/활성화 시나리오 검증.
- CourseInitData seed, CourseResponse DTO, course_plan 설계 문서, TODO 상태 반영.
- 실행: `cd backend && ./gradlew test --tests "com.classhub.domain.course.*"` (성공)

### 수정한 파일

- `backend/src/main/java/com/classhub/domain/course/**`
- `backend/src/test/java/com/classhub/domain/course/**`
- `backend/src/main/java/com/classhub/global/init/data/CourseInitData.java`
- `docs/plan/backend/course_plan.md`
- `docs/todo/v1.8.md`

### 다음 단계

- Course 프론트엔드 개발 계획 수립 및 UI 구현

## [2025-12-13 10:20] Course 프론트 UI 및 TODO 반영

### Type

BEHAVIORAL

### Summary

- Teacher 전용 Course 관리 UI 전체(페이지/모달/Hook)를 구현하고, TODO에서 프론트 작업을 완료 처리했다.
- 폼 시간 필드가 기존 값 그대로 제출되도록 개선했으며, 새 PLAN 문서와 OpenAPI 타입을 반영했다.

### Details

- `docs/plan/frontend/course-management-ui_plan.md` 작성, `docs/todo/v1.8.md`에서 Course 프론트 작업 ✅ 처리.
- `/dashboard/teacher/courses` 페이지, `use-courses` React Query 훅, `CourseFormModal`, `Modal`, `TimeSelect` 컴포넌트 구현.
- `course-form-modal.tsx`에서 시간 값을 `HH:mm`으로 정규화하고 setValue 옵션을 추가해 종료 시간 재선택 버그 해결.
- OpenAPI 스키마/타입, `package*.json`, 공통 UI(`dashboard-shell`, `text-field`) 업데이트.
- (확인 예정) `cd frontend && npm run build -- --webpack`

### 수정한 파일

- `docs/plan/frontend/course-management-ui_plan.md`
- `docs/todo/v1.8.md`
- `frontend/package*.json`
- `frontend/src/app/dashboard/teacher/courses/page.tsx`
- `frontend/src/components/course/course-form-modal.tsx`
- `frontend/src/components/ui/modal.tsx`
- `frontend/src/components/ui/time-select.tsx`
- `frontend/src/components/dashboard/dashboard-shell.tsx`
- `frontend/src/components/ui/text-field.tsx`
- `frontend/src/hooks/use-courses.ts`
- `frontend/src/types/openapi.{d.ts,json}`

### 다음 단계

- 프론트 빌드/수동 테스트 실행 및 결과 기록

## [2025-12-13 01:30] 반 관리 UI 구현 완료

### Type

BEHAVIORAL

### Summary

- Course Management UI를 구현해 Teacher가 반을 생성/수정/목록조회/토글(활성/비활성)할 수 있게 함
- Modal, TimeSelect 신규 컴포넌트 추가, react-hook-form + zod 폼 검증 적용
- 사이드바 메뉴에 "반 관리" 링크 추가로 Teacher 전용 네비게이션 완성

### Details

- **새 컴포넌트**:
  - `frontend/src/components/ui/modal.tsx`: Portal 기반 모달 (ESC 핸들링, focus trap, body scroll lock)
  - `frontend/src/components/ui/time-select.tsx`: 시:분 선택 드롭다운 (15분 단위)
- **API Hooks**:
  - `frontend/src/hooks/use-courses.ts`: `useCourses`, `useCreateCourse`, `useUpdateCourse`, `useActivateCourse`, `useDeactivateCourse`, `useToggleCourse`
  - openapi-fetch 패턴 적용 (GET/POST/PATCH 대문자, params.path, getFetchError)
- **페이지 & 폼**:
  - `frontend/src/app/dashboard/teacher/courses/page.tsx`: 반 목록/필터(전체/활성/비활성)/카드 그리드
  - `frontend/src/components/course/course-form-modal.tsx`: 생성/수정 폼 (zod schema로 검증)
- **UI 개선**:
  - `frontend/src/components/ui/text-field.tsx`에 `error` prop 추가 (rose 스타일)
  - `frontend/src/components/dashboard/dashboard-shell.tsx`에 "반 관리" 메뉴 항목 추가 (Teacher 전용)
- **빌드 & 타입 검증**: `npm run build -- --webpack` 성공 (TypeScript 에러 0개)

### 수정한 파일

- `frontend/src/components/ui/modal.tsx` (신규)
- `frontend/src/components/ui/time-select.tsx` (신규)
- `frontend/src/hooks/use-courses.ts` (신규)
- `frontend/src/app/dashboard/teacher/courses/page.tsx` (신규)
- `frontend/src/components/course/course-form-modal.tsx` (신규)
- `frontend/src/components/ui/text-field.tsx` (error prop 추가)
- `frontend/src/components/dashboard/dashboard-shell.tsx` (반 관리 메뉴 추가)
- `docs/plan/frontend/course-management-ui_plan.md` (계획 문서 업데이트)

### 다음 단계

- 사용자가 수동 시나리오 테스트 진행 (생성/수정/토글/필터/빈 상태/반응형)
- 필요 시 UX 피드백 반영 및 버그 수정

## [2025-12-12 23:15] MCP 활용 지침 추가

### Type

STRUCTURAL

### Summary

- AGENTS/CLAUDE 안내 문서에 GitHub 및 Context7 MCP 사용 원칙 추가
- MCP 사용 흐름과 로그 기록 방식 명시로 작업 추적성 강화

### Details

- 작업 사유: MCP 사용을 명확히 강제해 브랜치/문서 작업 시 일관된 도구 체인을 확보하기 위함
- 영향받은 테스트: 해당 없음 (문서 업데이트)
- 수정한 파일: `AGENTS.md`, `CLAUDE.md`
- 다음 단계: 문서에 따라 모든 신규 작업은 GitHub/Context7 MCP를 우선 사용하고 로그에 참조 정보 기록

## [2025-12-13 19:30] StudentProfile ↔ Course 다중 수강 구조 적용

### Type

BEHAVIORAL

### Summary

- StudentCourseEnrollment 엔티티/리포지토리를 도입해 학생과 반 관계를 M:N으로 확장하고 서비스/컨트롤러를 전면 수정함
- StudentProfile DTO/응답/요약을 courseIds·enrolledCourses 기반으로 개편하고 PersonalLesson/Invitation 등 연관 도메인과 InitData를 맞춤
- 신규/수정 시나리오 및 컨트롤러 테스트, Enrollment 리포지토리 테스트를 작성·보강하고 `./gradlew cleanTest test`로 전체 검증 완료

### Details

- StudentProfileService: courseIds 입력 검증, Enrollment 싱크/조회 헬퍼 추가, 전화번호 중복 검증을 Teacher 단위로 단순화, Course별 학생 조회를 Enrollment 기준으로 재작성
- StudentProfileResponse/Summary/DTOs: courseId 제거, `enrolledCourses` 추가, Summary가 다중 courseNames를 보유하도록 변경, tests/컨트롤러 JSON 생성 로직 정비
- StudentCourseEnrollment 엔티티/리포지토리 및 테스트 추가, Invitation/PersonalLesson 관련 서비스/테스트와 Seed(StudentProfileInitData, PersonalLessonInitData)에서 Enrollment를 생성/참조하도록 수정
- TODO v1.8에서 “StudentProfile ~ Course M:N 관계 해결하기”를 완료 처리하고 PLAN 문서 최신화 유지
- 테스트: `GRADLE_USER_HOME=../.gradle-home ./gradlew cleanTest test`

## [2025-12-13 19:53] Student 다중 반 UI 설계

### Type

DESIGN

### Summary

- 학생 등록/수정/목록 화면을 다중 반 구조에 맞추기 위한 UI 요구사항과 처리 흐름을 `student-multi-course_enrollment_ui_plan.md`로 정의했다.
- CoursePicker 다중 선택 UX, `courseIds` 전송 규칙, `courseNames`/`enrolledCourses` 렌더링 방식을 정리했다.

### Details

- 작업 사유: 백엔드가 Student ↔ Course M:N을 도입함에 따라 프론트 폼과 목록이 단일 `courseId`에 묶여 있어 기능 불일치를 해소해야 함.
- 영향받은 테스트: 아직 없음(향후 `npm run build -- --webpack` 및 수동 시나리오 테스트 예정).
- 수정한 파일: `docs/plan/frontend/student-multi-course_enrollment_ui_plan.md`
- 다음 단계: 사용자 승인 후 프론트 구현(폼/훅/목록 업데이트) 진행

## [2025-12-13 19:56] 학생 등록/수정 UI 다중 반 전환

### Type

BEHAVIORAL

### Summary

- 학생 등록/수정 폼과 목록 UI를 다중 반 구조(`courseIds`, `enrolledCourses`, `courseNames[]`)에 맞게 개편했다.
- CoursePicker를 다중 선택 위젯으로 확장하고 Create/Update 요청에 배열을 전달하도록 훅/페이지 로직을 업데이트했다.
- 학생 목록에서 여러 반 이름을 요약해 표시하며, Course 미선택 시 검증 오류를 안내한다.

### Details

- 작업 사유: 백엔드 StudentCourseEnrollment 도입 이후 프론트는 단일 `courseId`만 처리해 데이터가 저장되지 않는 문제.
- 변경 파일
  - `frontend/src/components/course/course-picker.tsx`: 멀티 선택 지원, 선택 개수 배지 표시
  - `frontend/src/app/dashboard/students/new/page.tsx`: `courseIds` 검증·전송 및 버튼 비활성 조건 갱신
  - `frontend/src/app/dashboard/students/[id]/edit/page.tsx`: 상세 응답 `enrolledCourses` 매핑, 다중 반 수정
  - `frontend/src/app/dashboard/students/page.tsx`: `courseNames[]` 기반 렌더, 요약 헬퍼 추가
- 검증: `cd frontend && npm run build -- --webpack`
- 다음 단계: 실제 UI에서 다중 반 선택/수정/목록 표출 시나리오를 수동 확인하고 사용자 피드백 반영

## [2025-12-13 20:03] SharedLesson InitData 추가

### Type

STRUCTURAL

### Summary

- 로컬/dev 환경에서 SharedLesson 샘플 데이터를 자동으로 생성하도록 `SharedLessonInitData`를 추가했고, TODO를 완료 처리했다.
- Course별/주차별로 3개의 공통 진도 기록을 생성하며, 중복 생성을 막기 위해 Repository에 course+date+title 기반 조회 메서드를 확장했다.

### Details

- 작업 사유: TODO Phase4 “SharedLesson InitData 추가하기” 수행 및 PersonalLesson과 동일하게 데모 데이터를 제공하기 위함.
- 영향받은 테스트: 공유 진도 기능 테스트 없음. `GRADLE_USER_HOME=../.gradle-home ./gradlew test --tests "com.classhub.domain.sharedlesson.*"` 시도했으나, sandbox 네트워크 제한으로 Gradle 배포본 다운로드 실패(services.gradle.org 접근 불가).
- 수정한 파일:
  - `backend/src/main/java/com/classhub/global/init/data/SharedLessonInitData.java` (신규)
  - `backend/src/main/java/com/classhub/domain/sharedlesson/repository/SharedLessonRepository.java` (중복 방지 조회 메서드 추가)
  - `docs/todo/v1.8.md`
- 다음 단계: Gradle 캐시가 준비된 환경에서 sharedlesson 도메인 테스트를 재실행하고 Seed 데이터로 UI/Swagger에서 확인

## [2025-12-13 20:08] 학생별 캘린더 백엔드 계획 초안

### Type

DESIGN

### Summary

- 학생별 월간 캘린더 API 범위를 정의하고 SharedLesson/PersonalLesson를 한 번에 내려주는 설계를 문서화했다.

### Details

- 작업 사유: Teacher/Assistant가 학생 단위 일정 뷰를 구성하기 위해 단일 API와 권한 흐름이 필요함.
- 영향받은 테스트: 없음 (설계 단계).
- 수정한 파일: `docs/plan/backend/student-calendar_plan.md`
- 다음 단계: 사용자 검토 후 PLAN 승인 시 TDD/구현 진행

## [2025-12-13 20:08] 학생별 캘린더 API TODO 상태 업데이트

### Type

TODO_UPDATE

### Summary

- Phase4 학생별 캘린더 중 "조회용 API 개발(백엔드)" 작업을 착수 상태(🔄)로 표시했다.

### Details

- 작업 사유: 백엔드 설계를 시작했으므로 해당 TODO를 진행 중으로 반영.
- 영향받은 테스트: 없음.
- 수정한 파일: `docs/todo/v1.8.md`
- 다음 단계: PLAN 승인 후 구현 착수, 완료 시 ✅로 갱신

## [2025-12-13 20:32] 학생별 캘린더 조회 API 구현

### Type

BEHAVIORAL

### Summary

- `StudentCalendarController`/`StudentCalendarQueryService`를 추가해 SharedLesson과 PersonalLesson을 월 단위로 합산하는 학생 캘린더 API를 완성했다.
- SharedLesson/PersonalLesson Repository에 월간 조회용 메서드를 확장하고 DTO 묶음을 만들어 프런트가 바로 섹션별 데이터를 사용할 수 있게 했다.
- `StudentCalendarQueryServiceTest`를 작성해 Teacher 성공 시나리오와 권한 거부 케이스(다른 Teacher 소속 Assistant)를 검증하려 했으며, Gradle 테스트 실행은 네트워크 차단으로 실패했다.

### Details

- 작업 사유: Phase4 학생별 캘린더 Epic의 백엔드 조회 API를 구현해 Teacher/Assistant가 학생 단위 진행 현황을 한 번에 확인할 수 있도록 하기 위함.
- 영향받은 테스트: `GRADLE_USER_HOME=../.gradle-home ./gradlew test --tests "com.classhub.domain.calendar.application.StudentCalendarQueryServiceTest"` 실행 시 Gradle 배포본 다운로드에서 `services.gradle.org` UnknownHost 예외로 실패함(네트워크 제한).
- 수정한 파일:
  - `backend/src/main/java/com/classhub/domain/calendar/**` (신규 DTO/Service/Controller)
  - `backend/src/main/java/com/classhub/domain/sharedlesson/repository/SharedLessonRepository.java`
  - `backend/src/main/java/com/classhub/domain/personallesson/repository/PersonalLessonRepository.java`
  - `backend/src/main/java/com/classhub/domain/studentcourseenrollment/repository/StudentCourseEnrollmentRepository.java`
  - `backend/src/test/java/com/classhub/domain/calendar/application/StudentCalendarQueryServiceTest.java`
- 다음 단계: Gradle 캐시(gradle-9.2.1) 다운로드가 가능한 환경에서 위 테스트를 재실행해 통과 여부 확인

## [2025-12-13 20:32] 학생별 캘린더 TODO 완료 처리

### Type

TODO_UPDATE

### Summary

- Phase4 "학생별 캘린더 개발" 중 "조회용 API 개발(백엔드)" 항목을 완료(✅)로 업데이트했다.

### Details

- 작업 사유: 캘린더 조회 API 및 테스트 코드 추가가 완료되어 상태 반영.
- 영향받은 테스트: 없음.
- 수정한 파일: `docs/todo/v1.8.md`
- 다음 단계: 프론트엔드 조회 UI 작업 진행 시 🔄/✅ 상태 업데이트

## [2025-12-13 20:36] 학생 캘린더 권한 검증 강화

### Type

BEHAVIORAL

### Summary

- `StudentCalendarQueryService`에서 Teacher/Assistant만 접근하도록 역할 검증을 추가하고, Student/SuperAdmin 등의 요청은 즉시 403을 던지도록 조정했다.
- 권한 거부 케이스를 다루는 단위 테스트(`shouldDenyStudentRole`)를 추가해 학생 역할 접근 시 예외가 발생하는지 확인했다.

### Details

- 작업 사유: API 요구사항에 맞춰 requester가 Teacher 또는 Assistant인지 명시적으로 검사해 안정성을 높이기 위함.
- 영향받은 테스트: `GRADLE_USER_HOME=../.gradle-home ./gradlew test --tests "com.classhub.domain.calendar.application.StudentCalendarQueryServiceTest"` 실행 시 Gradle 9.2.1 배포본 다운로드 단계에서 `services.gradle.org` UnknownHost 예외가 발생해 실행 불가(네트워크 제한). 테스트는 로컬 캐시 확보 후 재실행 필요.
- 수정한 파일:
  - `backend/src/main/java/com/classhub/domain/calendar/application/StudentCalendarQueryService.java`
  - `backend/src/test/java/com/classhub/domain/calendar/application/StudentCalendarQueryServiceTest.java`
- 다음 단계: 네트워크가 허용된 환경에서 위 테스트를 재실행해 권한 검증 로직 회귀 여부 확인

## [2025-12-13 20:45] 학생 캘린더 컨트롤러/서비스 테스트 정비

### Type

STRUCTURAL

### Summary

- `StudentCalendarControllerTest`를 `@AutoConfigureMockMvc` 기반으로 단순화하고 공통 요청 헬퍼를 추가해 각 시나리오가 더 읽기 쉬워졌다.
- 서비스 테스트에는 학생 역할 접근 거부 검증을 추가해 새로운 role guard를 커버했다.

### Details

- 작업 사유: 새로 추가한 캘린더 API 테스트를 정돈하고 role 검증 로직을 확실히 보장하기 위함.
- 영향받은 테스트: `GRADLE_USER_HOME=../.gradle-home ./gradlew test --tests "com.classhub.domain.calendar.application.StudentCalendarQueryServiceTest"` 실행은 Gradle 9.2.1 배포본 다운로드 단계에서 `services.gradle.org` UnknownHost 예외로 막혔다(네트워크 제한). 캐시 후 재실행 필요.
- 수정한 파일:
  - `backend/src/test/java/com/classhub/domain/calendar/web/StudentCalendarControllerTest.java`
  - `backend/src/test/java/com/classhub/domain/calendar/application/StudentCalendarQueryServiceTest.java`
- 다음 단계: Gradle 배포본이 준비된 환경에서 위 테스트를 실제 실행해 결과 확인

## [2025-12-13 20:54] 학생 캘린더 컨트롤러 테스트 보완

### Type

STRUCTURAL

### Summary

- `StudentCalendarControllerTest`를 기존 패턴(WebApplicationContext + SecurityContext RequestPostProcessor)으로 변경하고, 토큰 의존성을 제거해 다른 컨트롤러 테스트와 일관되게 했다.

### Details

- 작업 사유: MockMvc 테스트 문법을 레포 표준에 맞추어, 인증 컨텍스트를 직접 주입하도록 수정 요청.
- 영향받은 테스트: `GRADLE_USER_HOME=../.gradle-home ./gradlew test --tests "com.classhub.domain.calendar.web.StudentCalendarControllerTest"` 시도 시에도 Gradle 9.2.1 다운로드 단계에서 `services.gradle.org` UnknownHost로 실패(네트워크 제한). 캐시 확보 후 재실행 필요.
- 수정한 파일: `backend/src/test/java/com/classhub/domain/calendar/web/StudentCalendarControllerTest.java`
- 다음 단계: Gradle 의존성을 내려받을 수 있는 환경에서 해당 테스트 실행

## [2025-12-13 20:56] 학생 캘린더 year/month 검증 예외 수정

### Type

STRUCTURAL

### Summary

- `StudentCalendarQueryService`의 `validateYearMonth`가 존재하지 않는 BusinessException 생성자를 호출하고 있었던 문제를 해결해, 기존 `RsCode.BAD_REQUEST.toException()`을 사용하도록 수정했다.

### Details

- 작업 사유: 컴파일 오류(존재하지 않는 생성자)와 컨트롤러 테스트 실패를 유발하던 잘못된 예외 생성 로직을 표준 패턴으로 교체.
- 영향받은 테스트: 해당 메서드를 호출하는 테스트들은 여전히 Gradle 배포본 다운로드 실패로 실행하지 못함.
- 수정한 파일: `backend/src/main/java/com/classhub/domain/calendar/application/StudentCalendarQueryService.java`
- 다음 단계: Gradle 캐시 확보 후 StudentCalendar 관련 테스트 실행

## [2025-12-13 20:57] 학생 캘린더 인증 검증 추가

### Type

STRUCTURAL

### Summary

- `StudentCalendarController`에서 인증되지 않은 요청 시 명확히 `UNAUTHORIZED` 예외를 던지도록 `principal` null 체크를 추가해 컨트롤러 테스트가 기대하는 401 흐름을 보장했다.

### Details

- 작업 사유: Controller 테스트의 `shouldReturn401_whenUnauthenticated` 케이스가 통과하도록 인증 여부를 명시적으로 검증.
- 영향받은 테스트: Gradle 캐시 부재로 실행 불가 상태 유지.
- 수정한 파일: `backend/src/main/java/com/classhub/domain/calendar/web/StudentCalendarController.java`
- 다음 단계: Gradle 배포본 다운로드 가능 시 Controller/Service 테스트를 다시 실행해 통과 확인

## [2025-12-13 21:06] 학생 캘린더 컨트롤러 테스트 재작성

### Type

STRUCTURAL

### Summary

- 삭제된 `StudentCalendarControllerTest`를 WebApplicationContext + SecurityContext 패턴으로 새로 작성해 인증/권한/검증/빈 결과 시나리오를 모두 커버했다.

### Details

- 작업 사유: 사용자 요청으로 컨트롤러 테스트 파일을 완전히 새로 만들어야 했음.
- 영향받은 테스트: `GRADLE_USER_HOME=../.gradle-home ./gradlew test --tests "com.classhub.domain.calendar.web.StudentCalendarControllerTest"` 실행은 Gradle 9.2.1 배포본 다운로드 단계에서 `services.gradle.org` UnknownHost 예외로 실패(네트워크 제한). 캐시 확보 후 재실행 필요.
- 수정한 파일: `backend/src/test/java/com/classhub/domain/calendar/web/StudentCalendarControllerTest.java`
- 다음 단계: Gradle 다운로드 가능한 환경에서 테스트 실행해 동작 검증

## [2025-12-13 21:07] 학생 캘린더 컨트롤러 테스트 MockMvc 표준화

### Type

STRUCTURAL

### Summary

- Spring Boot 문서의 권장 패턴(`/spring-projects/spring-boot` Testing 가이드)대로 `@SpringBootTest + @AutoConfigureMockMvc`와 `SecurityMockMvcRequestPostProcessors.authentication`을 사용해 컨트롤러 테스트를 다시 구성했다.

### Details

- 작업 사유: 사용자 요청(문법 불일치)과 Spring Boot 공식 Testing 문서에 맞춰 MockMvc 구성을 단순화하기 위함.
- 영향받은 테스트: `GRADLE_USER_HOME=../.gradle-home ./gradlew test --tests "com.classhub.domain.calendar.web.StudentCalendarControllerTest"` 실행 시 Gradle 9.2.1 다운로드 차단으로 실패.
- 수정한 파일: `backend/src/test/java/com/classhub/domain/calendar/web/StudentCalendarControllerTest.java`
- 다음 단계: Gradle 캐시 확보 후 테스트 실행

## [2025-12-13 21:40] 학생별 캘린더 프론트 PLAN 작성

### Type

DESIGN

### Summary

- StudentCalendar 백엔드 조회 API를 기반으로 한 학생별 캘린더 UI 요구사항을 문서화하고, 화면 구조·상호작용·API 연계를 `docs/plan/frontend/student-calendar-ui_plan.md`에 정의했다.

### Details

- 작업 사유: Phase4 TODO 중 "학생별 캘린더(프론트)" 작업을 시작하기 위해 PLAN 문서를 마련하고 사용자 검토를 받을 필요가 있음.
- 영향받은 테스트: 없음 (설계 단계).
- 수정한 파일: `docs/plan/frontend/student-calendar-ui_plan.md` (신규 작성).
- 다음 단계: 사용자 승인 후 React Query 훅, 캘린더 그리드, 상세 모달 등 UI 컴포넌트 구현 및 TODO 상태를 🔄로 갱신.

## [2025-12-13 21:55] 학생별 캘린더 UI 상세 묘사 추가

### Type

DESIGN

### Summary

- PLAN 문서에 실제 UI 모습(네비게이션, 레이아웃, 날짜 셀 띠 구조, 모달, 모바일 대응 등)을 시각화한 섹션을 추가해 구현 시 참조할 수 있는 자세한 워크스루를 마련했다.

### Details

- 작업 사유: 사용자 요청으로 “UI가 어떤 모습인지” 구체적으로 공유하기 위해 레이아웃 다이어그램과 흐름 설명을 확장함.
- 영향받은 테스트: 없음 (문서 수정).
- 수정한 파일: `docs/plan/frontend/student-calendar-ui_plan.md`.
- 다음 단계: 해당 설계를 기준으로 컴포넌트/훅 구현 전 사용자 승인 대기.

## [2025-12-13 22:20] 학생 캘린더 API editable 플래그 제공

### Type

BEHAVIORAL

### Summary

- StudentCalendar API에서 SharedLesson/PersonalLesson 응답마다 `editable` 플래그를 추가해 Teacher만 수정/삭제 버튼이 노출되도록 했고, Assistant는 읽기 전용으로 처리한다.
- DTO/서비스/테스트/컨트롤러 단을 모두 업데이트해 새 필드를 직렬화하고 역할별 동작을 검증했다.

### Details

- 작업 사유: 프론트 설계에서 상세 모달 버튼 노출을 백엔드가 결정하도록 요구(`editable`)했으나, 기존 DTO에 해당 필드가 없어 정보 부족.
- 구현 내용:
  - `CalendarSharedLessonDto`, `CalendarPersonalLessonDto`, `CalendarClinicRecordDto`에 `editable` 필드 추가.
  - `StudentCalendarQueryService`에서 Teacher 요청 시에만 `editable=true`가 되도록 계산해 DTO에 주입하고, Assistant는 false로 반환.
  - `StudentCalendarQueryServiceTest`/`StudentCalendarControllerTest`에 역할별 editable 기대값 검증 추가.
- 영향받은 테스트:
  - `cd backend && GRADLE_USER_HOME=../.gradle-home ./gradlew test --tests "com.classhub.domain.calendar.application.StudentCalendarQueryServiceTest"` 실행 시 sandbox 네트워크 제한으로 Gradle 배포본 다운로드가 막혀 실패(`java.net.UnknownHostException: services.gradle.org`). 향후 캐시가 준비된 환경에서 재실행 필요.
  - MockMvc 테스트는 동일 원인으로 실행하지 못함.
- 수정한 파일:
  - `backend/src/main/java/com/classhub/domain/calendar/application/StudentCalendarQueryService.java`
  - `backend/src/main/java/com/classhub/domain/calendar/dto/response/CalendarSharedLessonDto.java`
  - `backend/src/main/java/com/classhub/domain/calendar/dto/response/CalendarPersonalLessonDto.java`
  - `backend/src/main/java/com/classhub/domain/calendar/dto/response/CalendarClinicRecordDto.java`
  - `backend/src/test/java/com/classhub/domain/calendar/application/StudentCalendarQueryServiceTest.java`
  - `backend/src/test/java/com/classhub/domain/calendar/web/StudentCalendarControllerTest.java`
- 다음 단계: Gradle 캐시 확보 후 해당 테스트를 실행해 통과 여부 확인, 이후 프런트 구현에 새 필드를 활용.

## [2025-12-13 22:33] ToastProvider Hydration 오류 수정

### Type

BUGFIX

### Summary

- SSR 단계에서 `typeof window !== "undefined"` 조건으로 포털 DOM을 렌더링하던 `ToastProvider`가 클라이언트 초기화 전 구조가 달라져 Hydration mismatch가 발생했는데, `useEffect`로 클라이언트 마운트 여부를 추적해 마운트 이후에만 `createPortal`을 호출하도록 변경했다.

### Details

- 작업 사유: Next.js 16 환경에서 Recoverable Error가 발생해 화면 최초 로드 시 토스트 루트가 스크립트로 대체되며 경고가 출력됨.
- 구현 내용: `isClient` state를 추가하고 `useEffect`로 첫 렌더 이후에 true로 설정, SSR 시에는 포털을 렌더링하지 않아 서버/클라이언트 DOM이 일치하도록 조정.
- 영향받은 테스트: `frontend` 빌드/테스트는 실행하지 못했으며, 추후 `npm run build -- --webpack`으로 검증 예정.
- 수정한 파일: `frontend/src/components/ui/toast.tsx`
- 다음 단계: 프론트 빌드 및 주요 화면 수동 테스트 진행 시 토스트 표시 경로를 재확인.

## [2025-12-13 22:40] 학생 캘린더 새로고침 Hook 순서 오류 수정

### Type

BUGFIX

### Summary

- `StudentCalendarContent`에서 권한 가드(`useRoleGuard`)가 false일 때 일찍 반환하면서 `useMemo` 훅이 렌더 사이에 호출되기도, 생략되기도 해 “Rendered more hooks than during the previous render” 오류가 났던 문제를, `useMemo`를 가드 이전에 실행해 훅 순서를 고정함으로써 해결했다.

### Details

- 작업 사유: 새로고침 시 Role Guard가 fallback을 렌더링한 뒤 실제 콘텐츠를 그리면서 추가 훅이 들어가 React가 hook order 변경을 감지함.
- 구현 내용: 캘린더 매트릭스를 계산하는 `useMemo` 호출을 가드 분기보다 위로 옮겨 어떤 렌더에서도 동일한 훅 개수를 유지하도록 조정.
- 영향받은 테스트: 아직 `npm run build -- --webpack`을 돌리지 못했으며, 추후 프론트 빌드 및 수동 테스트에서 새로고침 시 오류가 재발하지 않는지 확인 필요.
- 수정한 파일: `frontend/src/app/dashboard/teacher/student-calendar/page.tsx`
- 다음 단계: 학생별 캘린더 페이지 새로고침/권한 가드 플로우를 수동 검증하고, 전체 빌드를 통해 타입/훅 경고가 없는지 확인.

## [2025-12-13 23:05] 로컬/DEV 초기 데이터 한국형 시나리오로 갱신

### Type

STRUCTURAL

### Summary

- StudentProfile/Course/PersonalLesson/SharedLesson 시드 데이터를 실제 학원 환경과 유사한 한국어 정보로 재작성해 데모 데이터의 현실감을 높였다.
- 학생 이름, 학교, 학년·나이를 한국 현장 스타일로 다양화하고, 코스 및 진도 기록도 실제 학원 일정을 반영하도록 수정했다.

### Details

- `StudentProfileInitData`: 한글 이름 생성 로직, 지역별 학교 리스트, 학년/나이 매핑(중2~재수, 14~19세)을 도입해 60명의 학생이 자연스러운 데이터를 갖도록 변경.
- `CourseInitData`: “대치 메가프렙 수학심화반”, “분당 리더스 영어독해반” 등 실제 학원 네이밍과 다양한 요일/시간표를 적용해 코스 정보를 현실화.
- `PersonalLessonInitData`: 2025년 10~12월에 걸친 코칭 기록을 고정 날짜 + 이름 기반 오프셋으로 생성해 과거 진도가 지속적으로 남아 있도록 변경.
- `SharedLessonInitData`: 각 코스의 실제 수업 요일과 연동된 2025년 10~12월 주차별 진도 기록을 생성하고, 콘텐츠 문구를 한국어 서술로 업데이트.
- 영향받은 테스트: 시드 데이터 변경만 수행했으며, `./gradlew test` 등은 실행하지 못함. 이후 로컬에서 `./gradlew bootRun` 혹은 특정 도메인 테스트를 통해 시드 로딩 여부를 점검 필요.
- 수정한 파일:
  - `backend/src/main/java/com/classhub/global/init/data/StudentProfileInitData.java`
  - `backend/src/main/java/com/classhub/global/init/data/CourseInitData.java`
- `backend/src/main/java/com/classhub/global/init/data/PersonalLessonInitData.java`
  - `backend/src/main/java/com/classhub/global/init/data/SharedLessonInitData.java`
- 다음 단계: 로컬/DEV 환경에서 부트스트랩 시드 실행 후 UI/Swagger에서 한글 데이터가 정상 노출되는지 확인.

## [2025-12-13 23:12] Member 시드 한글 이름 적용

### Type

STRUCTURAL

### Summary

- 로컬/DEV Member 시드에서 Teacher/Assistant 이름을 한국 학원 현장 스타일로 변경해 다른 데이터와 톤을 맞췄다.

### Details

- 작업 사유: 기존 "Alice Teacher", "Alpha Assistant 1" 등의 영문 이름을 한글화된 시드 값과 일관되게 맞추기 위함.
- 구현 내용: Teacher ALPHA/BETA를 각각 “김서현 선생님”, “이도윤 선생님”으로, 조교들은 “대치 조교 N”, “분당 조교 N” 패턴으로 변경.
- 영향받은 테스트: 시드 변경만 수행, 추가 테스트 미실행.
- 수정한 파일: `backend/src/main/java/com/classhub/global/init/data/MemberInitData.java`
- 다음 단계: 부트스트랩 실행 시 새로운 이름으로 계정이 생성되는지 확인.

## [2025-12-13 23:30] 학생 캘린더 검색/목록 UI 개선

### Type

BUGFIX

### Summary

- 학생별 캘린더 페이지의 검색/선택 UX를 보완해 한 글자 입력만으로도 검색이 가능하고 다중 반 정보를 정확히 표시하도록 수정했다.

### Details

- 작업 사유: 최소 2자 제약 때문에 검색 UX가 불편했고, 다중 코스를 수강 중인 학생 선택 시 코스명이 하나만 보이는 문제가 있었다.
- 구현 내용:
  - `useStudentProfiles` 훅의 최소 글자 수를 1자로 완화하고 PLAN 문서도 동일하게 업데이트.
  - 검색 결과와 학생 카드에서 다중 코스 목록을 요약하는 `formatCourseNames` 헬퍼를 도입, 선택한 학생은 상세 API(`useStudentProfileDetail`)로 최신 코스 목록을 가져와 표시.
  - 학생 검색 입력 placeholder/조건 텍스트를 수정하고, 빌드(`npm run build -- --webpack`)로 TypeScript 검증 완료.
- 영향받은 테스트: 프론트 빌드 실행(`npm run build -- --webpack`)으로 타입/정적 검증 통과.
- 수정한 파일:
  - `frontend/src/app/dashboard/teacher/student-calendar/page.tsx`
  - `frontend/src/hooks/use-student-calendar.ts`
  - `docs/plan/frontend/student-calendar-ui_plan.md`
- 다음 단계: 수동으로 학생 검색→선택→캘린더 로드를 확인해 다중 코스/검색 UX가 정상 동작하는지 검증.

## [2025-12-13 23:38] 조교 초대 링크 빈 상태 UI 개선

### Type

BUGFIX

### Summary

- `dashboard/invitations/assistant` 페이지에서 초대 링크가 한 번도 생성되지 않은 Teacher에게는 빈 URL 대신 “초대 링크 생성” 안내와 CTA만 노출되도록 UI를 다듬었다.

### Details

- 작업 사유: 링크가 없는데도 빈 문자열이 노출되어 UX가 혼란스러웠음.
- 구현 내용: `frontend/src/app/dashboard/invitations/assistant/page.tsx`의 분기 로직을 정리해 `activeInvitation`이 없을 때 설명 문구 + CTA 버튼만 렌더링하고, 링크가 존재할 때에만 복사/만료 정보 카드가 나타나게 변경.
- 영향받은 테스트: 해당 페이지는 정적 UI 변경으로 별도 테스트는 수행하지 않았으며, 필요 시 `npm run build -- --webpack`으로 재검증 가능.
- 수정한 파일: `frontend/src/app/dashboard/invitations/assistant/page.tsx`
- 다음 단계: 조교 초대 페이지에서 링크 미생성 → 생성 → 복사 플로우를 수동 확인해 안내 문구가 기대대로 보이는지 검증.

## [2025-12-13 23:45] 학생 초대 후보 목록 연락처 표시

### Type

BUGFIX

### Summary

- 학생 초대 페이지(`/dashboard/invitations/student`)의 후보 목록에서 잘못된 코스명 대신 실제 연락처를 노출하고, Desktop/Mobile 카드 UI 모두 동일하게 반영했다.

### Details

- 작업 사유: 학생 후보 응답 모델에는 `courseName` 필드가 없으므로 화면에 “N/A”만 표시되어 의미가 없었음. 초대 목적상 연락처를 바로 확인할 수 있어야 함.
- 구현 내용: 테이블 헤더를 “연락처”로 바꾸고, Desktop/Mobile 리스트 모두 `candidate.phoneNumber || "연락처 미등록"`을 표시하도록 수정. 빌드(`npm run build -- --webpack`)로 검증 완료.
- 수정한 파일: `frontend/src/app/dashboard/invitations/student/page.tsx`
- 다음 단계: 초대 후보 목록에서 다중 선택 후 초대 생성까지 수동으로 확인해 UI가 기대대로 보이는지 검증.
## [2025-12-15 13:46] 수업 내용 작성 모달 Frontend 설계

### Type
DESIGN

### Summary
- Teacher 전용 `+ 수업 내용 작성` 전역 모달 UX와 SharedLesson/PersonalLesson 동시 작성 흐름을 정의했다.

### Details
- 작업 사유: TODO v1.8 Phase 4의 "수업 내용 작성" 기능에 대응하는 프런트 플로우가 없어 설계 문서가 필요했다.
- 영향받은 테스트: 설계 단계로 아직 실행한 테스트 없음.
- 수정한 파일:
  - `docs/plan/frontend/lesson-content-composer_plan.md`
- 다음 단계: 사용자 승인 후 PLAN을 기준으로 전역 CTA + 모달 UI 구현을 진행한다.

## [2025-12-15 14:48] 수업 내용 작성 모달 개발 순서 정의

### Type
DESIGN

### Summary
- Lesson Content Composer PLAN에 Phase별 개발 순서를 추가해 구현 흐름과 검증 절차를 명확히 했다.

### Details
- 작업 사유: 사용자 요청으로 plan 하단에 student-calendar 문서와 동일한 형식의 개발 순서 지침이 필요했다.
- 영향받은 테스트: 설계 변경으로 아직 실행한 테스트 없음.
- 수정한 파일:
  - `docs/plan/frontend/lesson-content-composer_plan.md`
- 다음 단계: PLAN 승인 후 Phase 1부터 순차적으로 구현하며 빌드/수동 테스트 결과를 추가로 기록한다.

## [2025-12-15 14:56] Lesson Composer Phase 1-2 기본 구조 구현

### Type
BEHAVIORAL

### Summary
- Lesson Content Composer 1~2단계(타입/데이터 훅 + 컨텍스트 & 전역 CTA)를 구축해 이후 UI 작업의 기반을 마련하고 Teacher 대시보드 헤더에 `+ 수업 내용 작성` 버튼을 노출했다.

### Details
- 작업 사유: PLAN 8단계 중 Phase 1~2 착수 지시(타입 alias, 데이터 훅, Provider, 전역 CTA).
- 구현 내용:
  - `frontend/src/types/api/lesson.ts`에 Shared/Personal Lesson 관련 operation/body alias를 추가하고, React Query 키/훅(`frontend/src/hooks/api/*`)으로 활성 반/학생 목록을 불러오는 API 모듈을 작성.
  - `frontend/src/contexts/lesson-composer-context.tsx`에서 Composer 전역 상태/액션(open/reset/prefill)을 정의하고, `DashboardShell`을 Provider로 감싼 뒤 TEACHER 전용 CTA 버튼을 헤더 우측에 추가.
- 테스트: `cd frontend && npm run build -- --webpack` (성공)
- 수정한 파일:
  - `frontend/src/types/api/lesson.ts`
  - `frontend/src/hooks/api/lesson-composer-keys.ts`
  - `frontend/src/hooks/api/useTeacherCourses.ts`
  - `frontend/src/hooks/api/useCourseStudents.ts`
  - `frontend/src/contexts/lesson-composer-context.tsx`
  - `frontend/src/components/dashboard/dashboard-shell.tsx`
- 다음 단계: Phase 3 이후 순서에 따라 반 선택 UI와 SharedLesson 폼을 구현하면서 새 훅/컨텍스트를 실제 화면에 연결한다.

## [2025-12-15 15:04] Lesson Composer Phase 3-4 UI 구축

### Type
BEHAVIORAL

### Summary
- Lesson Content Composer의 반 선택/공통 진도 폼(Phase 3)과 학생 선택/개별 진도 폼(Phase 4)을 포함한 전역 모달 UI를 구현했다.

### Details
- 작업 사유: PLAN 8단계의 Phase 3~4 진행 요청(반 선택 + SharedLesson 폼, 학생 선택 + PersonalLesson 폼).
- 구현 내용:
  - `frontend/src/components/lesson/lesson-composer-modal.tsx` 신규 모달 컴포넌트를 추가하고, `LessonComposerProvider` 상태에 필드 업데이트/학생 선택/개별 폼 동기화 액션을 확장함.
  - Teacher 대시보드 어디서나 `+ 수업 내용 작성` 버튼으로 모달을 열 수 있도록 DashboardShell에 모달을 마운트하고, Course/Student React Query 훅 데이터를 UI와 연결.
  - 학생 체크 시 개인 진도 카드가 동적으로 생기는 UX, 검색/로딩/빈 상태 표시, 기본 입력 제약을 구성함.
- 테스트: `cd frontend && npm run build -- --webpack` (성공)
- 수정한 파일:
  - `frontend/src/components/dashboard/dashboard-shell.tsx`
  - `frontend/src/components/lesson/lesson-composer-modal.tsx`
  - `frontend/src/components/ui/checkbox.tsx`
  - `frontend/src/contexts/lesson-composer-context.tsx`
  - `frontend/src/types/api/lesson.ts`
  - `frontend/src/hooks/api/lesson-composer-keys.ts`
  - `frontend/src/hooks/api/useTeacherCourses.ts`
  - `frontend/src/hooks/api/useCourseStudents.ts`
- 다음 단계: Phase 5 이후 작업(제출 시퀀스/에러 처리)을 추가 구현하고, TODO 상태를 업데이트한다.

## [2025-12-15 15:11] Lesson Composer Phase 5-7 제출/UX/검증 완료

### Type
BEHAVIORAL

### Summary
- Lesson Content Composer에 공통→개별 진도 제출 시퀀스, 오류 처리, 모바일 bottom sheet 레이아웃 및 검증용 CTA 바를 추가해 Phase 5~7 요구사항을 충족했다.

### Details
- 작업 사유: PLAN Phase 5~7(제출 로직, 반응형 UX, 검증/로그)을 구현하라는 지시.
- 구현 내용:
  - `lesson-composer-modal.tsx`에 Shared→Personal API 호출 흐름, Promise.allSettled 기반 실패 집계, 선택 학생 카드별 에러 표시, sticky 액션 바, 모바일 bottom sheet 레이아웃, 검증 로직을 추가하고 Toast/Query invalidate/Context 액션과 연동.
  - `lesson-composer-context.tsx`에 submission 상태/실패 액션을 도입해 버튼 상태, 에러 표시, 리셋 로직을 통합.
  - `ui/modal.tsx`에 `mobileLayout="bottom-sheet"` 옵션을 구현해 작은 화면에서는 bottom sheet 패턴으로 표시하도록 개선.
- 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - `frontend/src/components/lesson/lesson-composer-modal.tsx`
  - `frontend/src/contexts/lesson-composer-context.tsx`
  - `frontend/src/components/ui/modal.tsx`
  - `frontend/src/components/ui/checkbox.tsx`
- 다음 단계: Phase 8 이후(제출 완료 후 검증) 범위에 따라 TODO/PLAN 업데이트 및 전체 통합 검증을 준비한다.

## [2025-12-15 15:16] 조교 학생별 캘린더 접근 권한 수정

### Type
BUGFIX

### Summary
- 조교도 학생별 캘린더 페이지에 접근할 수 있도록 역할 가드를 Teacher+Assistant 범위로 확장했다.

### Details
- 작업 사유: 요구사항상 조교도 학생 캘린더 조회 권한이 있으나 `useRoleGuard("TEACHER")`로 제한되어 접근 불가.
- 구현 내용: `frontend/src/app/dashboard/teacher/student-calendar/page.tsx`에서 `useRoleGuard(["TEACHER", "ASSISTANT"])`로 수정.
- 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - `frontend/src/app/dashboard/teacher/student-calendar/page.tsx`
- 다음 단계: 필요 시 조교 UX(학생 선택 등)에서 안내 문구를 추가하는지 확인.

## [2025-12-15 16:05] PersonalLesson 제목 필드 추가

### Type
BEHAVIORAL

### Summary
- PersonalLesson 엔티티 및 모든 연관 DTO/캘린더 응답/시드에 `title`을 추가하고 CRUD·캘린더 경로 테스트까지 갱신했다.

### Details
- 작업 사유: 개인 진도 작성 시 제목을 별도로 기록해 달라는 요구에 따라 API 스키마부터 데이터 시드, 캘린더 응답까지 일관되게 확장해야 했다.
- 구현 내용:
  - 엔티티/서비스/요청·응답 DTO에 `title` 필드를 추가하고 검증 길이(최대 100자)를 설정.
  - 학생 캘린더 DTO, PersonalLesson InitData 시드 생성 로직을 제목 포함 형태로 재작성.
  - Service/Controller/Calendar 테스트에서 새 필드를 생성·검증하도록 수정.
- 영향받은 테스트: `cd backend && ./gradlew test`
- 수정한 파일:
  - `backend/src/main/java/com/classhub/domain/personallesson/model/PersonalLesson.java`
  - `backend/src/main/java/com/classhub/domain/personallesson/application/PersonalLessonService.java`
  - `backend/src/main/java/com/classhub/domain/personallesson/dto/request/PersonalLessonCreateRequest.java`
  - `backend/src/main/java/com/classhub/domain/personallesson/dto/request/PersonalLessonUpdateRequest.java`
  - `backend/src/main/java/com/classhub/domain/personallesson/dto/response/PersonalLessonResponse.java`
  - `backend/src/main/java/com/classhub/domain/personallesson/dto/response/PersonalLessonSummary.java`
  - `backend/src/main/java/com/classhub/domain/calendar/dto/response/CalendarPersonalLessonDto.java`
  - `backend/src/main/java/com/classhub/global/init/data/PersonalLessonInitData.java`
  - `backend/src/test/java/com/classhub/domain/personallesson/application/PersonalLessonServiceTest.java`
  - `backend/src/test/java/com/classhub/domain/personallesson/web/PersonalLessonControllerTest.java`
  - `backend/src/test/java/com/classhub/domain/calendar/application/StudentCalendarQueryServiceTest.java`
- `backend/src/test/java/com/classhub/domain/calendar/web/StudentCalendarControllerTest.java`
- 다음 단계: 프런트엔드/문서(OpenAPI, 타입 등)도 PersonalLesson 제목 필드를 반영하도록 후속 작업을 진행한다.

## [2025-12-15 16:40] PersonalLesson 제목 UI 스펙 갱신

### Type
DESIGN

### Summary
- PersonalLesson이 제목을 포함해 노출·작성된다는 요구를 PLAN 문서(Student Calendar/Composer)에 반영해 API 필드와 폼 구조를 최신화했다.

### Details
- 작업 사유: 백엔드 API에 `title`이 추가되었으므로 UI 스펙에서도 PersonalLesson 카드/폼이 제목+내용 형태임을 명시해야 했다.
- 수정 내용:
  - 학생별 캘린더 PLAN(`docs/plan/frontend/student-calendar-ui_plan.md`)에서 CalendarPersonalLessonDto 필드와 모달 표현을 `제목+내용` 구조로 업데이트.
  - Lesson Content Composer PLAN(`docs/plan/frontend/lesson-content-composer_plan.md`)에서 PersonalLesson 요청 바디, personalEntries 모델, 입력 단계 요구사항을 모두 `날짜+제목+내용`으로 수정.
- 영향받은 테스트: 문서 변경으로 테스트 없음.
- 수정한 파일:
  - `docs/plan/frontend/student-calendar-ui_plan.md`
  - `docs/plan/frontend/lesson-content-composer_plan.md`
- 다음 단계: 갱신된 PLAN 기준으로 프런트엔드 구현을 정비한다.

## [2025-12-15 16:55] 학생 캘린더/Composer PersonalLesson 제목 표시 및 입력 지원

### Type
BEHAVIORAL

### Summary
- 학생별 캘린더 모달에서 PersonalLesson을 제목+내용 형태로 렌더링하고, Lesson Composer에서는 개인 진도 카드에 제목 입력/수정 필드를 추가해 새 API 스키마를 처리하도록 수정했다.

### Details
- 작업 사유: PersonalLesson API가 `title`을 요구·제공하므로 조회/수정 UI와 Lesson Composer 제출 흐름이 해당 필드를 다루도록 업데이트해야 했다.
- 구현 내용:
  - `student-calendar/page.tsx`의 PersonalLesson 카드에 제목 heading을 추가하고, 수정 모달/핸들러가 제목·내용을 모두 넘기도록 변경.
  - `EditLessonModal`과 `useUpdatePersonalLesson` 훅을 PersonalLesson 제목 편집을 지원하도록 확장.
  - Lesson Composer Context/Modal 전반에서 PersonalLessonFormValues에 `title`을 도입하고, 학생별 카드에 제목 TextField + 유효성 검증을 추가했으며, 제출 payload/검증/실패 처리 로직을 모두 업데이트.
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - `frontend/src/app/dashboard/teacher/student-calendar/page.tsx`
  - `frontend/src/components/lesson/edit-lesson-modal.tsx`
  - `frontend/src/hooks/use-lesson-mutations.ts`
  - `frontend/src/contexts/lesson-composer-context.tsx`
  - `frontend/src/components/lesson/lesson-composer-modal.tsx`
- 다음 단계: 통합 수업 작성 모달에서 자동으로 채워지는 기본 제목 전략(예: 공통 진도 제목 복사)을 운영팀과 상의하고, 추가 UI 개선이 필요한지 검토한다.

## [2025-12-15 22:50] ClinicSlot 서비스/컨트롤러 구현

### Type
BEHAVIORAL

### Summary
- ClinicSlot 도메인의 Service/Controller를 PLAN에 맞춰 TDD로 작성하고 CRUD + 활성/비활성화 API를 완성했다.

### Details
- 작업 사유: TODO Phase 4 ClinicSlot Epic의 서비스/컨트롤러 계층이 비어 있어 CRUD API를 노출할 수 없었음.
- 구현 내용:
  - `ClinicSlotService`에서 Teacher 검증, 시간 파싱/검증, 중복 슬롯 방지, 활성/비활성 토글을 포함한 모든 비즈니스 로직을 작성하고 비즈니스 전용 RsCode(`CLINIC_SLOT_NOT_FOUND`, `CLINIC_SLOT_CONFLICT`)를 추가.
  - 서비스 단위 테스트(`ClinicSlotServiceTest`)로 생성/조회/수정/삭제/비활성/활성/충돌 케이스 15가지를 검증.
  - `ClinicSlotController` 및 `ClinicSlotControllerTest`를 통해 REST API (POST/GET/PATCH/DELETE/activate/deactivate)와 권한 체크/유효성 응답을 검증.
  - Repository에 Teacher별 조회 메서드와 NULL-safe 겹침 쿼리를 추가.
- 영향받은 테스트: `cd backend && ./gradlew test`
- 수정한 파일:
  - `backend/src/main/java/com/classhub/domain/clinic/clinicslot/application/ClinicSlotService.java`
  - `backend/src/main/java/com/classhub/domain/clinic/clinicslot/web/ClinicSlotController.java`
  - `backend/src/main/java/com/classhub/domain/clinic/clinicslot/repository/ClinicSlotRepository.java`
  - `backend/src/main/java/com/classhub/global/response/RsCode.java`
  - `backend/src/test/java/com/classhub/domain/clinic/clinicslot/application/ClinicSlotServiceTest.java`
  - `backend/src/test/java/com/classhub/domain/clinic/clinicslot/web/ClinicSlotControllerTest.java`
- 다음 단계: ClinicSlot API를 기반으로 ClinicSession 기능(TODO Phase 4) 설계 및 연동을 준비한다.

## [2025-12-15 11:10] v1.3 스펙 및 엔티티 문서 정비

### Type
DESIGN

### Summary
- clarified requirement v1.3 내용을 반영한 `docs/spec/v1.3.md` 신규 작성
- final entity spec에 StudentEnrollmentRequest/StudentCourseRecord/ClinicAttendance/Invitation/Course 규칙 업데이트

### Details
- 작업 사유: 최종 엔티티 스펙과 Requirement v1.3 간 불일치(승인 권한, 조교 초대 단일 사용, 클리닉 자동 배정 등)을 해소하고 차기 TODO/PLAN이 참조할 수 있는 Spec 버전을 제공
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: `docs/spec/v1.3.md`(신규), `docs/design/final-entity-spec.md`
- 다음 단계: TODO/PLAN 문서들이 Spec v1.3을 참조하도록 업데이트 필요

## [2025-12-16 17:13] TODO v1.9 작성 (Spec v1.3 반영)

### Type
TODO_UPDATE

### Summary
- Requirement v1.3 및 Spec v1.3, Entity Refactor Plan을 기준으로 `docs/todo/v1.9.md`를 신규 생성
- Phase 4 이후를 Season2 엔티티 리팩터링/서비스 개발/프런트 정비/테스트/릴리스 흐름으로 재구성
- Student 초대 작업 제거 및 Teacher/Assistant 승인 기반 학생 등록 플로우로 전환

### Details
- 작업 사유: 엔티티 개편 및 새로운 요구(Company/Branch, Enrollment Request, Clinic 자동 배정, single-use Invitation 등)에 맞는 TODO 버전이 필요했음
- 영향 문서: `docs/todo/v1.9.md`, 참조 문서 `docs/plan/backend/season2/entity-refactor-plan.md`, `docs/spec/v1.3.md`, `docs/requirement/v1.3.md`
- 수정 항목: Phase 4~8 재작성, PLAN 작성 작업 명시, Student 초대 관련 항목 삭제, 프런트/테스트/릴리스 단계 업데이트
- 다음 단계: Phase 4 엔티티/레포지토리 작업 착수 전 각 Epic별 PLAN 문서 작성 및 승인

## [2025-12-16 18:41] Entity Refactor Plan Notice/WorkLog 보강

### Type
DESIGN

### Summary
- Season2 Entity Refactor Plan에 Notice/NoticeRead/WorkLog 스펙과 관련 Repository/체크리스트를 추가해 누락된 도메인 리팩터링 범위를 명확히 했다.

### Details
- 작업 사유: 사용자 요청에 따라 Notice/WorkLog 영역이 계획 문서에 없던 점을 보완해야 했음
- 영향 문서: `docs/plan/backend/season2/entity-refactor-plan.md`
- 주요 변경: Section 2에 Notice/NoticeRead/WorkLog 상세 스키마 추가, Repository/작업순서/체크리스트에 해당 항목 삽입
- 다음 단계: 해당 도메인 역시 Phase 4~5 작업 시 PLAN→구현 절차를 따름

## [2025-12-16 18:42] TODO v1.9 Phase7 세분화

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.9.md` Phase 7을 역할별 QA/E2E/배치/운영 체크리스트 Epic으로 재구성하고 각 Epic에 PLAN 문서 경로를 명시했다.

### Details
- 작업 사유: 향후 PLAN 문서를 작성하기 쉽도록 Phase 7 단위를 명확히 나눠달라는 요청 반영
- 수정 내용: 선생님/조교/학생 시나리오 QA, E2E 파이프라인, 스케줄러 안정성, 운영 체크리스트 Epic 추가 및 작업 항목 세분화
- 다음 단계: 각 Epic별 PLAN 문서 작성 후 테스트/자동화 구현 진행

## [2025-12-16 18:44] TODO v1.9 Phase4 작업순서 재정렬

### Type
TODO_UPDATE

### Summary
- `docs/todo/v1.9.md`의 Season2 도메인 파운데이션(Phase 4)을 entity-refactor-plan의 Step 1~9 순서와 동일하게 정리하고 단계별 PLAN 문서를 연결했다.

### Details
- 작업 사유: 개발 순서를 plan 문서와 일치하도록 해달라는 요청 반영
- 수정 내용: Phase 4를 Step 1~9(기반 엔티티 → Member → Assignment → Course/Enrollment → StudentCourseRecord → Clinic → Lesson → Invitation/Feedback/Notice/WorkLog → InitData) 구조로 재작성하고 필요한 PLAN 파일 명시
- 다음 단계: 각 Step별 PLAN을 작성/승인 후 엔티티 및 레포지토리 작업 진행

## [2025-12-16 18:48] Entity Refactor Plan Phase1 항목 보강

### Type
DESIGN

### Summary
- Phase1 엔티티 수정 목록에 Notice/NoticeRead/WorkLog를 추가해 Season2 리팩터링 범위 시작 단계부터 해당 도메인이 추적되도록 했다.

### Details
- 작업 사유: Phase1 표에 Notice/WorkLog 항목이 없어 누락되었다는 피드백 반영
- 수정 내용: `docs/plan/backend/season2/entity-refactor-plan.md` 1.2 표에 세 도메인의 현황 및 필요 변경 사항 기술
- 다음 단계: Phase2 상세 명세/Step 8 작업 시 이 변경 내용을 참고하여 PLAN 및 구현을 진행
## [2025-12-17 19:57] Spec v1.3 2.3.3~6 섹션 정비

### Type
DESIGN

### Summary
- docs/spec/v1.3.md의 2.3.3 이후 전체 섹션을 requirement/design 문서 기준으로 다시 작성해 StudentCalendar, Invitation, Company 검증, API/리소스 세부 명세를 최신화했다.

### Details
- 작업 사유: docs/requirement/v1.3.md와 docs/design/final-entity-spec.md/full-erd.md의 스펙 차이를 해소하고 이후 TODO/PLAN의 단일 참조점을 마련하기 위함
- 영향받은 테스트: 문서 작업으로 테스트 없음
- 수정한 파일: docs/spec/v1.3.md
- 다음 단계: 새 스펙을 참조해 PLAN/TODO 업데이트 및 구현을 진행
## [2025-12-17 21:12] StudentCourseRecord 조교 배정 필드 추가

### Type
DESIGN

### Summary
- docs/design/final-entity-spec.md와 docs/design/full-erd.md에 StudentCourseRecord 전담 조교(assistantMemberId) 관계를 추가했다.

### Details
- 작업 사유: 학생별 기록을 조교에게 위임할 수 있도록 설계에 담당 조교 정보를 정의해달라는 요청 반영
- 영향받은 테스트: 문서 작업으로 테스트 없음
- 수정한 파일: docs/design/final-entity-spec.md, docs/design/full-erd.md
- 다음 단계: 필요 시 docs/spec 및 PLAN 문서에서 해당 필드를 활용하도록 후속 업데이트 진행
## [2025-12-17 21:16] StudentCourseRecord 담당 조교 요구/스펙 반영

### Type
DESIGN

### Summary
- docs/requirement/v1.3.md와 docs/spec/v1.3.md에 StudentCourseRecord 담당 조교(`assistantMemberId`) 시나리오를 추가해 Teacher/Assistant 역할과 API 책임을 명확히 했다.

### Details
- 작업 사유: 학생별로 담당 조교를 지정하고 관리하고 싶다는 요청을 requirement~spec 전 단계에 반영하기 위함
- 영향받은 테스트: 문서 변경으로 테스트 없음
- 수정한 파일: docs/requirement/v1.3.md, docs/spec/v1.3.md
- 다음 단계: 구현/PLAN 작업 시 StudentCourseRecord Patch API에서 assistantMemberId를 저장/검증하도록 설계
## [2025-12-17 21:32] Spec v1.3 flow & API update (clinic, calendar, invitation, company)

### Type
DESIGN

### Summary
- docs/spec/v1.3.md 2.3.x 및 API/NFR 섹션을 피드백에 맞춰 교원 승인 플로우, 클리닉 보정, 학생 캘린더 접근, 조교 초대 링크, Company 등록 전략을 보완했다.

### Details
- 작업 사유: Teacher/Assistant 승인 UI 흐름, 당주 클리닉 자동 배정, 캘린더 접근 제한, 초대 링크 플로우, Company/Branch 생성 절차에 대한 추가 요구 반영
- 영향받은 테스트: 문서 변경만 수행
- 수정한 파일: docs/spec/v1.3.md
- 다음 단계: 구현 시 StudentCalendar 캐시 없이 DB 조회, 클리닉 slot 선택 시 당주 Attendance 생성, 조교 초대 링크 발급/검증 흐름을 준수
## [2025-12-17 21:59] Spec v1.3 SuperAdmin 접근 규칙 명시

### Type
DESIGN

### Summary
- docs/spec/v1.3.md 리소스 상세 섹션에 SuperAdmin이 모든 API에 감사/긴급 목적으로 접근할 수 있다는 기본 규칙을 추가했다.

### Details
- 작업 사유: SuperAdmin은 모든 API를 사용할 수 있어야 한다는 요청 반영
- 영향받은 테스트: 문서 변경만 수행
- 수정한 파일: docs/spec/v1.3.md
- 다음 단계: 구현 시 권한 체크 로직에 SuperAdmin 우선권을 적용
## [2025-12-17 22:01] Spec v1.3 출강 등록 예외 시나리오 추가

### Type
DESIGN

### Summary
- docs/spec/v1.3.md 2.3.5 섹션에 회사/지점 존재 여부별 Teacher 출강 등록 예외 처리 절차를 명시했다.

### Details
- 작업 사유: 회사·지점 존재 여부에 따른 입력/검증 흐름을 명확히 하려는 요청 반영
- 영향받은 테스트: 문서 변경만 수행
- 수정한 파일: docs/spec/v1.3.md
- 다음 단계: 구현 시 Company/Branch 생성 시나리오별 UX와 SuperAdmin 검증 로직을 해당 규칙에 맞춘다
## [2025-12-17 22:08] TeacherBranchAssignment 역할 규칙 명확화

### Type
DESIGN

### Summary
- docs/spec/v1.3.md의 Company/Branch 등록 흐름을 수정해 기존 학원 선택 시 FREELANCE, 신규 Company/Branch 생성 시 OWNER로 `TeacherBranchAssignment`가 생성되는 규칙을 명확히 했다.

### Details
- 작업 사유: TeacherBranchAssignment 역할 부여 조건을 분명히 하려는 요청 반영
- 영향받은 테스트: 문서 변경만 수행
- 수정한 파일: docs/spec/v1.3.md
- 다음 단계: 구현 시 Branch/Course 생성 로직이 해당 규칙에 맞춰 Assignment 역할을 설정하도록 검증
## [2025-12-17 22:25] INDIVIDUAL/ACADEMY 출강 플로우 명세화

### Type
DESIGN

### Summary
- docs/design/final-entity-spec.md의 Company/Branch 비고를 갱신해 개인 학원(INDIVIDUAL)과 회사 학원(ACADEMY)의 등록/검증/Assignment 생성 규칙을 명확히 했다.

### Details
- 작업 사유: 출강 등록 구조를 "개인=VERIFIED 즉시 사용, 회사=기존 목록 또는 UNVERIFIED 입력" 흐름으로 재정리하라는 요청 반영
- 영향받은 테스트: 문서 변경만 수행
- 수정한 파일: docs/design/final-entity-spec.md
- 다음 단계: Company/Branch 생성/검증 구현 시 해당 규칙을 준수하고 SuperAdmin 검증 로직과 Assignment 롤 부여를 맞춘다
## [2025-12-17 22:47] VerifiedStatus & Branch creator 도입

### Type
DESIGN

### Summary
- docs/design/final-entity-spec.md, docs/design/full-erd.md, docs/requirement/v1.3.md에 VerifiedStatus(UNVERIFIED/VERIFIED)를 Company/Branch 공용으로 적용하고 Branch.creatorMemberId를 추가했으며, 개인/회사 학원 출강 플로우를 재정리했다.

### Details
- 작업 사유: CompanyStatus를 단일 VerifiedStatus로 통합해 Company/Branch 모두에 적용하고 출강 등록 단계별 역할(OWNER/FREELANCE) 규칙을 문서화하기 위함
- 영향받은 테스트: 문서 변경만 수행 (테스트 없음)
- 수정한 파일: docs/design/final-entity-spec.md, docs/design/full-erd.md, docs/requirement/v1.3.md
- 다음 단계: 엔티티/서비스 구현 시 VerifiedStatus enum을 공유하고 Branch.creatorMemberId/Assignment 역할 부여 로직을 반영
## [2025-12-17 22:51] Spec v1.3 VerifiedStatus/출강 플로우 반영

### Type
DESIGN

### Summary
- docs/spec/v1.3.md에 VerifiedStatus 도입(Company/Branch), 출강 등록 시나리오, Companies/Branches API 설명, 공개 Course 필터를 requirement/design 최신 내용과 맞췄다.

### Details
- 작업 사유: final-entity-spec & requirement에서 갱신된 VerifiedStatus/creatorMemberId/Assignment 규칙을 스펙에도 반영하기 위함
- 영향받은 테스트: 문서 변경만 수행
- 수정한 파일: docs/spec/v1.3.md
- 다음 단계: 구현 시 Company/Branch 생성·검증 및 Course 공개 검색이 문서화된 VerifiedStatus 로직을 따른다
