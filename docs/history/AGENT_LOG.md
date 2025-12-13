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

<<<<<<< HEAD
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

=======
>>>>>>> origin/main
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
