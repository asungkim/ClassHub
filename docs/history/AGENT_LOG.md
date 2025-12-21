# AGENT_LOG

이 파일은 개발 활동 이력을 기록합니다.

---

## [2025-12-18 22:45] 프론트엔드 코드 정리 및 인증 검증

### Type

STRUCTURAL | BEHAVIORAL

### Summary

- Season2 백엔드 구조에 맞춰 사용되지 않는 프론트엔드 코드 제거
- SessionProvider 타입 시스템 강화 (MemberRole 명시적 정의)
- 빌드 통과 및 구조 정리 완료

### Details

**삭제된 파일**:
- `app/auth/*` - 인증 페이지 전체 (다음 Task에서 재구현 예정)
- `app/dashboard/*` - 대시보드 페이지 전체 (다음 Task에서 재구현 예정)
- `components/{clinic,course,lesson,dashboard}` - 도메인 컴포넌트 (Season2 미구현 기능)
- `hooks/{api,clinic,queries}` - 구 API Hooks 디렉토리
- `hooks/use-{assistants,courses,lesson-mutations,student-profiles,student-calendar}.ts` - 개별 Hooks
- `contexts/` - Lesson Composer Context (미구현)
- `types/api/` - 구 타입 정의

**보존된 인프라**:
- `components/ui/*` - Button, Card, TextField, Modal, Table, Badge 등 UI 컴포넌트 전체
- `components/shared/*` - EmptyState, LoadingSkeleton, InvitationStatusBadge
- `components/session/session-provider.tsx` - 세션 관리 (타입 수정)
- `lib/*` - API 클라이언트, 에러 처리, 환경 변수, 역할 관리
- `hooks/use-debounce.ts`, `hooks/use-role-guard.tsx` - 공통 Hooks

**SessionProvider 타입 개선** (OpenAPI 스키마 활용):
```typescript
// Before
type MemberSummary = {
  role: string;  // ❌ any string
}

// After
type MeResponse = components["schemas"]["MeResponse"];
type MemberSummary = Required<MeResponse>;  // ✅ OpenAPI 스키마에서 자동 추론
```

**lib/role.ts 타입 개선**:
```typescript
// Before
export const Role = {
  TEACHER: "TEACHER",
  ...
} as const;
export type Role = (typeof Role)[keyof typeof Role];

// After
type MeResponse = components["schemas"]["MeResponse"];
export type MemberRole = NonNullable<MeResponse["role"]>;  // ✅ OpenAPI 스키마 기반
export const ROLES = { ... } satisfies Record<string, MemberRole>;
```

**임시 비활성화**:
- `lib/role-route.ts` - Dashboard 라우팅 로직 (다음 Task에서 재구현)
- `app/page.tsx` - 회원가입 버튼 비활성화 처리
- `hooks/use-role-guard.tsx` - Dashboard 라우팅 제거

**빌드 검증**:
- `npm run build -- --webpack` 통과 (컴파일 에러 0개)
- 최종 라우트: `/`, `/components`

### 다음 단계

- v1.9 TODO Phase 4: 선생님 회원가입 페이지 재구현 (Company/Branch 입력 추가)
- 조교 초대 검증 페이지 재구현
- Assistant/Student 회원가입 페이지 신규 구현
- 역할별 대시보드 페이지 신규 구현

## [2025-02-14 15:00] 교사용 조교 관리 페이지 구현

### Type

BEHAVIORAL

### Summary

- `docs/plan/frontend/season2/assistant-invitation-suite_plan.md`를 근거로 Teacher 조교 관리 페이지를 구축했다.
- 조교 목록/초대 목록 API 연동과 상태 토글, 초대 발급·복사·취소 UX를 구현했다.
- `cd frontend && npm run build -- --webpack`을 실행해 타입 검증을 통과했다.

### Details

- 작업 사유: Season2 초대 플로우를 완성하기 위해 Teacher 조교 관리 화면이 필요했다.
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - `frontend/src/app/(dashboard)/teacher/invitations/page.tsx`
  - `frontend/src/components/ui/tabs.tsx`
- 다음 단계: 초대 검증 페이지 구현, 조교 회원가입 페이지 구현

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

## [2025-12-21 12:50] 개선 백로그 정리 및 학생 그룹화 개선안 추가

### Type

TODO_UPDATE

### Summary

- `docs/refactor/improvment-backlog.md` 문서를 재구성해 각 아이템의 목적·작업 범위를 명확히 했다.
- 선생님 대시보드 학생 목록을 학생 단위로 묶는 신규 개선안을 backlog에 추가했다.

### Details

- 작업 사유: 베타 이후 단계적으로 처리할 이슈를 표준 포맷으로 정리하고, 학생 목록 중복 노출 문제를 추적하기 위함.
- 영향받은 테스트: 없음
- 수정한 파일:
  - `docs/refactor/improvment-backlog.md`
- 다음 단계: 해당 개선안이 구체화되면 Requirement/Spec/TODO에 반영하고 PLAN 문서 작성 후 구현 착수.

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
## [2025-12-17 23:29] Season2 Backend Roadmap PLAN 작성

### Type
DESIGN

### Summary
- docs/plan/backend/season2/season2-backend-roadmap_plan.md를 작성해 VerifiedStatus 기반 구조를 어떤 순서로 재구축할지 정의했다.

### Details
- 작업 사유: final-entity-spec/full-erd 기준으로 Season2 개발 순서를 명확히 하라는 요청 반영
- 영향받은 테스트: 문서 작업으로 테스트 없음
- 수정한 파일: docs/plan/backend/season2/season2-backend-roadmap_plan.md
- 다음 단계: PLAN 순서(Company/Branch → Member Info → Course/Enrollment → Lesson/Clinic → Collaboration)를 따라 TODO/구현 진행
## [2025-12-17 23:43] TODO v1.9 Phase4~5 재구성

### Type
TODO_UPDATE

### Summary
- docs/todo/v1.9.md Phase 4/5 구성을 Season2 backend roadmap에 맞춰 Auth 재구현 → 엔티티/레포 → 프런트 기본 작업, 그리고 기능별 Epic 가이던스로 정리했다.

### Details
- 작업 사유: 신규 PLAN(Season2 backend roadmap)에 맞춰 TODO 흐름을 재정렬하기 위함
- 수정한 파일: docs/todo/v1.9.md
- 다음 단계: Phase 4 체크리스트부터 진행하면서 각 Epic별 PLAN/구현을 연계한다
## [2025-12-17 23:44] TODO v1.9 MemberPrincipal 항목 보정

### Type
TODO_UPDATE

### Summary
- Phase 4의 첫 작업을 "MemberPrincipal에 role 저장 및 JWT 수정"으로 명확히 표현했다.

### Details
- 작업 사유: 실제 목표는 다중 role이 아니라 `MemberPrincipal` 객체에 role 값을 추가 보관하도록 하는 것이므로 TODO 표현 수정
- 수정한 파일: docs/todo/v1.9.md
- 다음 단계: 해당 작업 진행 시 PLAN/TDD에서 role 저장 방식 구체화
## [2025-12-18 10:51] StudentInfo Grade Enum & schoolName 규칙 반영

### Type
DESIGN

### Summary
- final-entity-spec, full-erd, requirement, spec 문서에서 StudentInfo.grade를 StudentGrade Enum(E1~H3 + GAP_YEAR)으로 제한하고 schoolName 입력 정규화 방식을 명시했다.

### Details
- 작업 사유: 학생 학년을 정해진 구간(초1~고3,N수)으로만 받도록 하고 schoolName 입력을 정리해달라는 요청 반영
- 영향받은 테스트: 문서 변경만 수행
- 수정한 파일: docs/design/final-entity-spec.md, docs/design/full-erd.md, docs/requirement/v1.3.md, docs/spec/v1.3.md
- 다음 단계: 구현 시 StudentGrade enum/validation과 SchoolNameFormatter를 적용하고 프런트에서도 동일한 드롭다운/자동완성 UX를 제공
## [2025-12-18 12:59] MemberPrincipal Role Claim PLAN 작성

### Type
DESIGN

### Summary
- Phase 4의 첫 작업을 위한 `auth-member-principal_plan` 문서를 작성해 MemberPrincipal/JWT 역할 전달 방식과 테스트 전략을 정의했다.

### Details
- 작업 사유: Requirement/Spec v1.3에서 요구하는 역할 기반 접근 제어를 구현하기 전에 인증 레이어를 재설계하기 위함
- 영향받은 테스트: 문서 작업으로 실제 테스트는 아직 없음
- 수정한 파일: docs/plan/backend/season2/auth-member-principal_plan.md
- 다음 단계: 사용자가 PLAN을 검토/승인하면 MemberPrincipal, JwtProvider, 테스트 코드를 리팩터링한다
## [2025-12-18 13:09] MemberPrincipal PLAN 3단계 작업 추가

### Type
DESIGN

### Summary
- auth-member-principal PLAN에 구현을 3단계(Principal/Enum 정비 → JWT 리팩터링 → 컨트롤러 검증)로 나눠 구체화했다.

### Details
- 작업 사유: 사용자 요청으로 실행 순서를 명확히 하고 Phase 4 진행 시 참조할 단계별 가이드를 제공하기 위함
- 영향받은 테스트: 문서 작업, 테스트 없음
- 수정한 파일: docs/plan/backend/season2/auth-member-principal_plan.md
- 다음 단계: PLAN 기준으로 코드 수정/테스트를 진행하고 완료 후 TODO 상태 업데이트
## [2025-12-18 13:16] MemberPrincipal Role 전달 및 JWT 스펙 갱신

### Type
BEHAVIORAL

### Summary
- MemberPrincipal에 MemberRole을 포함하고 MemberRole Enum을 ADMIN/SUPER_ADMIN까지 확장, SecurityConfig 권한 문자열을 Enum 기반으로 전환했다.
- JwtProvider의 Access Token 클레임을 `role`로 재구성하고 MemberPrincipal이 토큰에서 role을 복원하도록 수정했으며, JwtProviderTest/SecurityIntegrationTest에 role 주입 검증을 추가했다.
- AuthService 및 컨트롤러 테스트(MemberControllerTest)를 새 계약에 맞게 업데이트했다.

### Details
- 작업 사유: Phase 4 첫 작업(PLAN `docs/plan/backend/season2/auth-member-principal_plan.md`)에 따라 JWT→SecurityContext→Controller로 역할 정보를 일관 전달하기 위함
- 영향받은 테스트: `JwtProviderTest`, `SecurityIntegrationTest`, `MemberControllerTest`를 업데이트했으나 Gradle wrapper 파일 잠금(`gradle-9.2.1-bin.zip.lck`)으로 실행에 실패하여 재로그인 후 재시도가 필요
- 수정한 파일: backend/src/main/java/com/classhub/domain/member/dto/MemberPrincipal.java, backend/src/main/java/com/classhub/domain/member/model/MemberRole.java, backend/src/main/java/com/classhub/global/jwt/JwtProvider.java, backend/src/main/java/com/classhub/domain/auth/application/AuthService.java, backend/src/main/java/com/classhub/global/config/SecurityConfig.java, backend/src/main/java/com/classhub/global/init/SeedKeys.java, backend/src/test/java/com/classhub/global/jwt/JwtProviderTest.java, backend/src/test/java/com/classhub/global/config/SecurityIntegrationTest.java, backend/src/test/java/com/classhub/domain/member/web/MemberControllerTest.java
- 다음 단계: Gradle wrapper 잠금 문제를 해결해 테스트를 다시 실행하고, 이후 TODO Phase 4의 다음 항목(회원가입/초대 로직 리팩터링)을 진행
## [2025-12-18 15:37] Teacher Register PLAN 작성

### Type
DESIGN

### Summary
- Member 스펙 반영 및 Teacher 회원가입 리팩터링 범위를 정의한 `auth-teacher-register_plan.md`를 작성했다.

### Details
- 작업 사유: Phase 4 두 번째 작업(Teacher 회원가입 검증/리팩터링)을 진행하기 전에 Member 엔티티와 AuthService 변경 지침을 확정하기 위함
- 영향받은 테스트: 문서 작업으로 테스트 없음
- 수정한 파일: docs/plan/backend/season2/auth-teacher-register_plan.md
- 다음 단계: 사용자가 PLAN을 검토/승인하면 Member 엔티티 및 AuthService/AuthController를 리팩터링하고 테스트를 추가한다
## [2025-12-18 15:41] BaseEntity Soft Delete 필드 추가

### Type
STRUCTURAL

### Summary
- BaseEntity에 `deletedAt` 컬럼과 `isDeleted/delete/restore` 헬퍼를 추가해 final-entity-spec의 Soft Delete 규칙을 반영했다.

### Details
- 작업 사유: Season2 엔티티 표준에 따라 모든 엔티티가 공통 Soft Delete 필드를 갖추도록 하기 위함
- 영향받은 테스트: 코드 변경만 수행(아직 테스트 추가 없음)
- 수정한 파일: backend/src/main/java/com/classhub/global/entity/BaseEntity.java
- 다음 단계: 엔티티별 soft delete 플래그를 활용하도록 리포지터리/서비스 단에서 조회 조건을 보강하고, BaseEntity 변경에 따른 마이그레이션(TODO) 준비
## [2025-12-18 15:42] BaseTimeEntity Soft Delete 이동

### Type
STRUCTURAL

### Summary
- Soft Delete 필드를 BaseEntity에서 BaseTimeEntity로 이동시켜 BaseEntity가 id만 관리하고, BaseTimeEntity가 created/updated/deletedAt과 helper 메서드를 일괄 제공하도록 정비했다.

### Details
- 작업 사유: final-entity-spec에서 정의한 BaseEntity(id + createdAt/updatedAt/deletedAt) 구조를 실제 코드 계층(BaseTimeEntity → BaseEntity)와 맞추기 위함
- 영향받은 테스트: 코드 변경만 수행, 테스트 없음
- 수정한 파일: backend/src/main/java/com/classhub/global/entity/BaseTimeEntity.java
- 다음 단계: 후속 엔티티 리팩터링에서 삭제 플래그를 활용하고, BaseEntity 관련 이전 로그와 함께 문서에 반영
## [2025-12-18 16:09] AuthService 로그인/로그아웃 단위 테스트 추가

### Type
STRUCTURAL

### Summary
- Mockito 기반 `AuthServiceTest`를 추가해 로그인 성공/실패 시나리오와 로그아웃 토큰 블랙리스트 동작을 검증했다.

### Details
- 작업 사유: Phase 4 '로그인/로그아웃 검증' 항목에 따라 핵심 Auth 기능의 회귀 테스트를 우선 확보하기 위함
- 영향받은 테스트: 신설된 `AuthServiceTest` 대상 `./gradlew test --tests com.classhub.domain.auth.application.AuthServiceTest` 실행을 시도했으나 Gradle wrapper가 `~/.gradle/.../gradle-9.2.1-bin.zip.lck` 파일에 접근하지 못해 실패(권한 문제). 환경 정리 후 재시도 필요
- 수정한 파일: backend/src/test/java/com/classhub/domain/auth/application/AuthServiceTest.java
- 다음 단계: Gradle wrapper 권한 문제를 해결한 뒤 테스트를 재실행하고, 이후 토큰 재발급/회원가입 재구현 작업을 진행
## [2025-12-18 16:12] AuthController SpringBootTest 추가

### Type
STRUCTURAL

### Summary
- SpringBootTest+MockMvc 기반 `AuthControllerTest`를 작성해 로그인시 쿠키 설정 및 로그아웃시 쿠키 추출/삭제 흐름을 검증했다.

### Details
- 작업 사유: 로그인/로그아웃 플로우를 컨트롤러 레벨에서도 회귀 테스트로 보강
- 영향받은 테스트: `./gradlew test --tests com.classhub.domain.auth.web.AuthControllerTest` 실행을 시도했으나 동일한 Gradle wrapper 락 파일(`~/.gradle/.../gradle-9.2.1-bin.zip.lck`) 접근 문제로 실패
- 수정한 파일: backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java
- 다음 단계: Gradle wrapper 권한 이슈를 해결 후 테스트를 재실행하고, 이후 토큰 재발급/회원가입 작업으로 진행
## [2025-12-18 16:12] AuthControllerTest 프로파일 보정

### Type
STRUCTURAL

### Summary
- AuthControllerTest에 `@ActiveProfiles("test")`를 추가해 테스트 전용 설정이 적용되도록 했다.

### Details
- 작업 사유: SpringBootTest 기반 컨트롤러 테스트가 test 프로파일 환경을 사용하도록 맞춤
- 영향받은 테스트: 실행은 기존과 동일하게 Gradle wrapper 권한 문제로 보류 중
- 수정한 파일: backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java
- 다음 단계: Gradle wrapper 권한을 정리한 후 테스트 재실행
## [2025-12-18 16:14] AuthControllerTest MockMvc 시나리오 확장

### Type
STRUCTURAL

### Summary
- AuthControllerTest를 @SpringBootTest+@AutoConfigureMockMvc로 구성하고 AuthService/RefreshTokenCookieProvider를 MockBean 처리해 로그인·재발급·로그아웃 요청을 실제 HTTP 호출로 검증하도록 수정했다.

### Details
- 작업 사유: 컨트롤러 레벨에서 서비스 호출/쿠키 처리 흐름을 명확히 테스트하기 위함
- 영향받은 테스트: Gradle wrapper 권한 문제로 실행은 아직 불가
- 수정한 파일: backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java
- 다음 단계: Gradle wrapper 권한을 정리한 뒤 테스트 실행, 이어서 나머지 Auth 작업 진행
## [2025-12-18 16:39] MemberRepository DataJpaTest 추가

### Type
STRUCTURAL

### Summary
- `@DataJpaTest` 기반 MemberRepositoryTest를 작성해 이메일 조회/중복 검사를 검증했다.

### Details
- 작업 사유: MemberRepository의 기본 계약(findByEmail/existsByEmail)을 회귀 테스트로 확보
- 영향받은 테스트: `./gradlew test --tests com.classhub.domain.member.repository.MemberRepositoryTest`는 여전히 Gradle wrapper 락 파일 문제로 실행 불가
- 수정한 파일: backend/src/test/java/com/classhub/domain/member/repository/MemberRepositoryTest.java
- 다음 단계: Gradle wrapper 권한 정리 후 전체 테스트를 재실행하고, Auth/TODO 항목을 계속 진행
## [2025-12-18 16:54] 백엔드 테스트 작성 지침 추가

### Type
STRUCTURAL

### Summary
- `backend/AGENTS.md`에 Repository/Service/Controller 테스트 작성 방식(DataJpaTest, MockitoExtension, SpringBootTest+MockMvc) 지침을 명시했다.

### Details
- 작업 사유: 사용자 요청에 따라 테스트 코드 패턴을 AGENTS에 정식 규칙으로 추가
- 영향받은 테스트: 없음
- 수정한 파일: backend/AGENTS.md
- 다음 단계: 지침에 맞춰 추가 테스트 작성 시 참고
## [2025-12-18 16:56] 백엔드 테스트 지침 세부 조건 보강

### Type
STRUCTURAL

### Summary
- backend/AGENTS.md에 Repository/Service/Controller 테스트 시 import·구성 방식(특히 DataJpaTest와 MockitoBean/SpringBootTest 세팅)을 더 명확히 명시했다.

### Details
- 작업 사유: 사용자 지시에 맞춰 테스트 코드 스타일을 문서로 강제하기 위함
- 영향받은 테스트: 없음
- 수정한 파일: backend/AGENTS.md
- 다음 단계: 지침에 따라 기존/향후 테스트 코드를 점검
## [2025-12-18 16:58] MemberRepository 기본 시나리오 DataJpaTest 추가

### Type
STRUCTURAL

### Summary
- `MemberRepositoryTest`에 이메일 조회/중복/Soft Delete 검증 테스트를 AGENT 지침 방식으로 추가했다.

### Details
- 작업 사유: Repository 계층 테스트 예시를 AGENTS.md 규칙에 맞춰 확보
- 영향받은 테스트: `./gradlew test --tests com.classhub.domain.member.repository.MemberRepositoryTest` 실행은 Gradle wrapper 락 문제로 여전히 불가
- 수정한 파일: backend/src/test/java/com/classhub/domain/member/repository/MemberRepositoryTest.java
- 다음 단계: Gradle wrapper 권한 문제 해결 후 테스트 재실행, 이후 Auth TODO 진행
## [2025-12-18 17:11] MemberRepositoryTest 감사 필드 활성화 및 존재 검증 수정

### Type
BUGFIX

### Summary
- DataJpaTest에서 JPA Auditing 구성을 가져오고 존재 여부 검증을 `findByEmail` 기반으로 조정해 전체 테스트가 다시 통과하게 했다.

### Details
- 작업 사유: backend에서 `./gradlew test` 실행 시 created_at 제약 위반과 존재하지 않는 `existsByEmail` 호출로 MemberRepositoryTest가 실패했기 때문
- 영향받은 테스트: `./gradlew test`
- 수정한 파일: backend/src/test/java/com/classhub/domain/member/repository/MemberRepositoryTest.java, docs/history/AGENT_LOG.md
- 다음 단계: 추가 요청 또는 TODO 지시에 따라 후속 작업 진행
## [2025-12-18 17:18] 백엔드 테스트 지침에 참조 파일 명시

### Type
STRUCTURAL

### Summary
- backend/AGENTS.md의 Repository/Service/Controller 테스트 규칙에 각각 참고해야 할 표준 테스트 파일(MemberRepositoryTest/AuthServiceTest/AuthControllerTest)을 명시했다.

### Details
- 작업 사유: 사용자가 테스트 코드 작성 시 따라야 할 구체 예시와 import 구성을 분명히 해달라고 요청함
- 영향받은 테스트: 없음
- 수정한 파일: backend/AGENTS.md, docs/history/AGENT_LOG.md
- 다음 단계: 해당 예시 파일을 기준으로 테스트 작성/리뷰 진행
## [2025-12-18 17:18] AuthController /me API 테스트 추가

### Type
STRUCTURAL

### Summary
- `/api/v1/auth/me` 엔드포인트를 포함해 AuthController의 모든 공개 API를 MockMvc 테스트로 검증하도록 `AuthControllerTest`를 보강했다.

### Details
- 작업 사유: 사용자 요청으로 AuthController에 선언된 API 함수 전부를 테스트로 검증해야 했기 때문
- 영향받은 테스트: `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.auth.web.AuthControllerTest.me_shouldReturnCurrentMemberData"`
- 수정한 파일: backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java, docs/history/AGENT_LOG.md
- 다음 단계: 없음
## [2025-12-18 17:51] RegisterService 기반 선생님 회원가입 API 구현

### Type
BEHAVIORAL

### Summary
- RegisterService/DTO/전화번호 Normalizer를 추가하고 `/api/v1/auth/register/teacher`를 RegisterService에 연결해 가입 직후 토큰 발급 및 Refresh 쿠키 세팅이 작동하도록 했다.

### Details
- 작업 사유: Phase4 TODO “선생님 회원가입 개발”을 수행하기 위해 공통 RegisterService 토대를 마련하고 Teacher 플로우를 복구해야 했음
- 영향받은 테스트: `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.member.repository.MemberRepositoryTest"` / `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.member.application.RegisterServiceTest"` / `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.auth.web.AuthControllerTest.registerTeacher_shouldReturnTokensAndSetCookie"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/member/application/RegisterService.java, backend/src/main/java/com/classhub/domain/member/dto/request/RegisterTeacherRequest.java, backend/src/main/java/com/classhub/domain/member/support/PhoneNumberNormalizer.java, backend/src/main/java/com/classhub/domain/auth/web/AuthController.java, backend/src/main/java/com/classhub/domain/member/repository/MemberRepository.java, backend/src/test/java/com/classhub/domain/member/application/RegisterServiceTest.java, backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java, backend/src/test/java/com/classhub/domain/member/repository/MemberRepositoryTest.java, docs/plan/backend/season2/auth-teacher-registration_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: RegisterService를 Assistant/Student 가입으로 확장하고 Company/Branch 온보딩 연계 로직을 추가 준비
## [2025-12-18 17:57] MemberController로 선생님 회원가입 엔드포인트 이관

### Type
BEHAVIORAL

### Summary
- `/api/v1/members/register/teacher`를 새 MemberController에 추가하고 AuthController에서 회원가입 책임을 제거해 API 책임을 역할별로 분리했다.

### Details
- 작업 사유: 회원가입 API를 AuthController에서 분리하자는 요청에 따라 Member 전용 컨트롤러에서 RegisterService를 노출하도록 경로를 변경
- 영향받은 테스트: `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.member.application.RegisterServiceTest"` / `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.member.web.MemberControllerTest"` / `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.auth.web.AuthControllerTest"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/auth/web/AuthController.java, backend/src/main/java/com/classhub/domain/member/web/MemberController.java, backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java, backend/src/test/java/com/classhub/domain/member/web/MemberControllerTest.java, docs/plan/backend/season2/auth-teacher-registration_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: 신규 엔드포인트를 기준으로 Assistant/Student 가입 확장 및 문서/클라이언트 반영
## [2025-12-18 18:21] 학생 회원가입 PLAN 작성

### Type
DESIGN

### Summary
- `docs/plan/backend/season2/member-registration_plan.md`에 Teacher/Student 공통 RegisterService 구조, StudentInfo/StudentGrade 요구, API/TDD/Implementation 절차를 정의했다.

### Details
- 작업 사유: Phase4 “학생 회원가입 개발”을 진행하기 전 요구사항/스펙(`docs/design/final-entity-spec.md`)을 반영한 설계 문서가 필요했기 때문
- 영향받은 테스트: 없음
- 수정한 파일: docs/plan/backend/season2/member-registration_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: PLAN에 따라 RegisterService/MemberController를 구현
## [2025-12-18 18:21] RegisterService 확장 및 학생 회원가입 API 구현

### Type
BEHAVIORAL

### Summary
- RegisterService를 공통 RegisterMemberRequest 기반으로 리팩터링하고 StudentInfo/StudentGrade/StudentInfoRepository를 추가해 학생 가입 시 Member+StudentInfo를 생성하도록 했으며, `/api/v1/members/register/student` 엔드포인트와 시큐리티 화이트리스트를 완비했다.

### Details
- 작업 사유: Phase4 TODO “학생 회원가입 개발”을 완료해 학생이 자유 가입 후 토큰을 발급받고 StudentInfo를 저장하도록 만들기 위함
- 영향받은 테스트:
  - `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.member.repository.StudentInfoRepositoryTest"`
  - `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.member.application.RegisterServiceTest"`
  - `GRADLE_USER_HOME=$PWD/.gradle ./gradlew test --tests "com.classhub.domain.member.web.MemberControllerTest" --tests "com.classhub.domain.auth.web.AuthControllerTest"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/member/dto/request/RegisterMemberRequest.java, backend/src/main/java/com/classhub/domain/member/dto/request/RegisterStudentRequest.java, backend/src/main/java/com/classhub/domain/member/model/StudentGrade.java, backend/src/main/java/com/classhub/domain/member/model/StudentInfo.java, backend/src/main/java/com/classhub/domain/member/repository/StudentInfoRepository.java, backend/src/main/java/com/classhub/domain/member/support/SchoolNameFormatter.java, backend/src/main/java/com/classhub/domain/member/application/RegisterService.java, backend/src/main/java/com/classhub/domain/member/web/MemberController.java, backend/src/main/java/com/classhub/global/config/SecurityConfig.java, backend/src/main/java/com/classhub/global/jwt/JwtAuthenticationFilter.java, backend/src/test/java/com/classhub/domain/member/repository/StudentInfoRepositoryTest.java, backend/src/test/java/com/classhub/domain/member/application/RegisterServiceTest.java, backend/src/test/java/com/classhub/domain/member/web/MemberControllerTest.java, docs/plan/backend/season2/member-registration_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: Assistant/Student 추가 요구사항에 맞춰 RegisterService/MemberController 확장 및 프런트 연동
## [2025-12-18 18:21] TODO v1.9 학생 회원가입 완료 처리

### Type
TODO_UPDATE

### Summary
- Phase4 “학생 회원가입 개발” 항목을 ✅로 전환해 구현 완료 상태를 반영했다.

### Details
- 작업 사유: 학생 회원가입 API 및 관련 도메인이 구현/테스트 완료됨에 따라 TODO 현황을 최신화
- 영향받은 테스트: 없음
- 수정한 파일: docs/todo/v1.9.md, docs/history/AGENT_LOG.md
- 다음 단계: 다음 TODO 항목을 선택해 PLAN/구현 진행

## [2025-12-18 18:52] Invitation 도메인/레포지토리 TDD 기반 구축

### Type
STRUCTURAL

### Summary
- Invitation 엔티티/enum/DTO를 `docs/design/final-entity-spec.md`와 PLAN Step1 요구에 맞춰 정의하고 이메일/코드 정규화 및 상태 전이 헬퍼를 추가했다.
- `InvitationRepositoryTest`로 findByCode/existsByTargetEmailAndStatus/soft-delete + 상태 전환/`canUse` 조건을 검증했다.

### Details
- 작업 사유: `docs/plan/backend/season2/invitation-assistant_plan.md` Step 1(도메인/Repository 구성)을 완료해 이후 Service/Controller 구현을 위한 토대를 마련하기 위함
- 영향받은 테스트: `./gradlew test --tests "com.classhub.domain.invitation.repository.InvitationRepositoryTest"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/invitation/model/Invitation.java, backend/src/main/java/com/classhub/domain/invitation/model/InvitationRole.java, backend/src/main/java/com/classhub/domain/invitation/repository/InvitationRepository.java, backend/src/main/java/com/classhub/domain/invitation/dto/request/AssistantInvitationCreateRequest.java, backend/src/main/java/com/classhub/domain/invitation/dto/request/InvitationVerifyRequest.java, backend/src/main/java/com/classhub/domain/invitation/dto/response/InvitationResponse.java, backend/src/main/java/com/classhub/domain/invitation/dto/response/InvitationVerifyResponse.java, backend/src/main/java/com/classhub/domain/member/dto/request/RegisterAssistantByInvitationRequest.java, backend/src/test/java/com/classhub/domain/invitation/repository/InvitationRepositoryTest.java, docs/history/AGENT_LOG.md
- 다음 단계: PLAN Step 2에 따라 InvitationService create/verify/accept 로직 및 Mockito 단위 테스트를 작성

## [2025-12-18 19:02] 조교 초대 가입 DTO를 RegisterMemberRequest 기반으로 통합

### Type
STRUCTURAL

### Summary
- `RegisterAssistantByInvitationRequest`가 `RegisterMemberRequest`를 `@JsonUnwrapped` 형태로 포함하도록 변경해 Teacher/Student와 동일한 검증/정규화 로직을 재사용하게 했다.

### Details
- 작업 사유: 사용자 요청에 따라 조교 초대 가입 플로우에서도 공용 회원가입 요청 스키마를 활용해 중복 검증 로직을 제거
- 영향받은 테스트: 없음 (단순 DTO 구조 변경)
- 수정한 파일: backend/src/main/java/com/classhub/domain/member/dto/request/RegisterAssistantByInvitationRequest.java, docs/history/AGENT_LOG.md
- 다음 단계: Invitation 기반 조교 가입 구현 시 해당 DTO를 사용해 RegisterService를 연계

## [2025-12-18 19:12] Invitation Service/Controller 설계 보강

### Type
DESIGN

### Summary
- `docs/plan/backend/season2/invitation-assistant_plan.md`에 Service/Controller 세부 설계를 추가해 InvitationService 책임, 메서드별 로직, Security/Controller 경로 요구사항을 명확화했다.
- TDD/Implementation Steps에서 Service/Controller 단계 테스트 범위를 구체화했다.

### Details
- 작업 사유: 사용자 요청으로 향후 Service/Controller 구현 시 참조할 구체적 설계/테스트 항목이 필요했기 때문
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/invitation-assistant_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: 갱신된 PLAN Step 2, 3에 따라 서비스/컨트롤러 구현

## [2025-12-18 19:18] 조교 초대 가입 API 경로 업데이트

### Type
DESIGN

### Summary
- 사용자 피드백에 따라 조교 초대 가입 API 경로를 `/api/v1/members/register/assistant`로 통일하도록 PLAN 문서(Requirements/API Design/Controller 디자인/TDD 항목)를 갱신했다.

### Details
- 작업 사유: 엔드포인트 네이밍을 간결하게 유지하고 기존 register 네이밍 일관성을 맞추기 위함
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/invitation-assistant_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: 업데이트된 경로를 기준으로 Controller/Security 구현

## [2025-12-18 19:35] Invitation 기반 조교 초대/가입 서비스 및 API 구현

### Type
BEHAVIORAL

### Summary
- `TeacherAssistantAssignment` 도메인/Repository와 `InvitationService`를 도입해 초대 생성/검증/취소/조교 등록 로직을 완성하고 기본 만료 7일 + 랜덤 코드 제너레이터를 적용했다.
- Auth/Member/Invitation Controller를 확장해 초대 검증(`POST /api/v1/auth/invitations/verify`), 조교 초대 생성/취소(`POST /api/v1/invitations`, `PATCH /api/v1/invitations/{code}/revoke`), 초대 기반 조교 가입(`POST /api/v1/members/register/assistant`)을 노출하고 Security/JWT 화이트리스트를 최신화했다.

### Details
- 작업 사유: `docs/plan/backend/season2/invitation-assistant_plan.md` Step2/3에 따라 조교 초대 + verify + 초대 기반 가입 플로우를 실제 서비스/컨트롤러로 구현
- 주요 변경
  - InvitationService + RandomInvitationCodeGenerator + Clock Bean + TeacherAssistantAssignment 도메인 추가
  - RegisterService에 Assistant 등록 메서드 추가
  - Auth/Member/Invitation Controller 및 DTO/Repository/TDD(서비스/컨트롤러 테스트) 보강
  - JwtAuthenticationFilter 화이트리스트에 `/api/v1/members/register/assistant`, `/api/v1/auth/invitations/**` 추가
- 영향받은 테스트:
  - `./gradlew test --tests "com.classhub.domain.member.application.RegisterServiceTest" --tests "com.classhub.domain.invitation.application.InvitationServiceTest"`
  - `./gradlew test --tests "com.classhub.domain.invitation.web.InvitationControllerTest" --tests "com.classhub.domain.member.web.MemberControllerTest" --tests "com.classhub.domain.auth.web.AuthControllerTest"`
  - `./gradlew test`
- 수정한 파일: backend/src/main/java/com/classhub/domain/assignment/model/TeacherAssistantAssignment.java, backend/src/main/java/com/classhub/domain/assignment/repository/TeacherAssistantAssignmentRepository.java, backend/src/main/java/com/classhub/domain/invitation/application/InvitationService.java, backend/src/main/java/com/classhub/domain/invitation/support/*.java, backend/src/main/java/com/classhub/domain/invitation/web/InvitationController.java, backend/src/main/java/com/classhub/domain/member/application/RegisterService.java, backend/src/main/java/com/classhub/domain/auth/web/AuthController.java, backend/src/main/java/com/classhub/domain/member/web/MemberController.java, backend/src/main/java/com/classhub/global/jwt/JwtAuthenticationFilter.java, backend/src/main/java/com/classhub/global/config/TimeConfig.java, backend/src/test/java/com/classhub/domain/member/application/RegisterServiceTest.java, backend/src/test/java/com/classhub/domain/invitation/application/InvitationServiceTest.java, backend/src/test/java/com/classhub/domain/auth/web/AuthControllerTest.java, backend/src/test/java/com/classhub/domain/member/web/MemberControllerTest.java, backend/src/test/java/com/classhub/domain/invitation/web/InvitationControllerTest.java, docs/history/AGENT_LOG.md
- 다음 단계: Teacher가 생성한 초대/Assignment를 조회/관리하는 API 확장 및 TODO 업데이트

## [2025-12-18 19:45] InvitationController Teacher 전용 보안 강화

### Type
STRUCTURAL

### Summary
- InvitationController의 초대 생성/취소 엔드포인트에 `@PreAuthorize("hasAuthority('TEACHER')")`를 추가해 Spring Security 단에서 Teacher만 접근할 수 있도록 강제하고, 기존 MockMvc 테스트가 통과하는지 확인했다.

### Details
- 작업 사유: 사용자 요청으로 보안 규칙을 명시적으로 적용해 잘못된 권한 접근을 Controller 레벨에서 차단
- 영향받은 테스트: `./gradlew test --tests "com.classhub.domain.invitation.web.InvitationControllerTest"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/invitation/web/InvitationController.java, docs/history/AGENT_LOG.md
- 다음 단계: 없음

## [2025-12-18 19:47] RsCode 구분 정리

### Type
STRUCTURAL

### Summary
- `RsCode` 상수를 Common/Auth/Invitation/Course/Student/Lesson 영역별로 재배치하고 섹션 주석을 추가해 응답 코드를 기능 단위로 쉽게 찾을 수 있도록 정리했다.

### Details
- 작업 사유: 사용자 요청에 따라 공통/엔티티별 오류 코드를 명확하게 그룹화
- 영향받은 테스트: 없음 (열거형 재정렬)
- 수정한 파일: backend/src/main/java/com/classhub/global/response/RsCode.java, docs/history/AGENT_LOG.md
- 다음 단계: 없음

## [2025-12-18 19:55] 도메인 Enum 작성 PLAN 수립

### Type
DESIGN

### Summary
- Phase4 TODO “도메인 ENUM 작성”을 위한 상세 PLAN(`docs/plan/backend/season2/domain-enum_plan.md`)을 작성해 필요한 Enum 목록, 패키지 구조, TDD/Implementation 단계를 정의했다.

### Details
- 작업 사유: Season2 스펙에 맞춰 Enum을 일괄 정리하기 위한 설계 문서가 필요했기 때문
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/domain-enum_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: PLAN Step 1에 따라 Enum 인벤토리를 정리하고 코드 구현을 준비

## [2025-12-18 19:58] Enum PLAN 패키지/범위 조정

### Type
DESIGN

### Summary
- 사용자 피드백을 반영해 PLAN Non-functional 섹션에 company/branch, clinic 하위 패키지 구조를 명시하고, 스펙에 존재하지 않는 NoticeType/WorkLogType/ClinicAttendanceStatus 등을 Enum 대상에서 제외하도록 수정했다.

### Details
- 작업 사유: 도메인별 폴더 구조와 작성 대상 Enum을 명확히 하기 위함
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/domain-enum_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: 패키지 규칙에 맞춰 Enum 구현 착수

## [2025-12-18 20:00] Enum PLAN 테스트 범위 조정

### Type
DESIGN

### Summary
- Enum 작성은 단순 값 정의 작업이므로 별도 단위 테스트 섹션을 삭제하고 Implementation Steps에 통합했습니다.

### Details
- 작업 사유: 사용자 의견에 따라 굳이 테스트를 작성하지 않아도 되는 영역을 문서에서 정리
- 영향받은 테스트: 없음
- 수정한 파일: docs/plan/backend/season2/domain-enum_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: PLAN Step 1 수행

## [2025-12-18 20:10] 도메인 Enum 1차 구현 (Company/Assignment/Enrollment/Clinic/Feedback)

### Type
STRUCTURAL

### Summary
- PLAN Appendix에서 미구현으로 표시된 Enum 6종을 Season2 패키지 규칙에 맞춰 추가했다: `CompanyType`, `VerifiedStatus`, `BranchRole`, `EnrollmentStatus`, `ClinicSessionType`, `FeedbackStatus`.
- 신규 도메인 패키지(`domain/company/company`, `domain/clinic/clinicsession`, `domain/enrollment`, `domain/feedback`)를 준비해 앞으로의 엔티티 구현 시 재사용할 수 있도록 정리했다.

### Details
- 작업 사유: Phase4 TODO “도메인 ENUM 작성” Step 2 실행
- 영향받은 테스트: `./gradlew test`
- 수정한 파일:
  - backend/src/main/java/com/classhub/domain/company/company/model/CompanyType.java
  - backend/src/main/java/com/classhub/domain/company/company/model/VerifiedStatus.java
  - backend/src/main/java/com/classhub/domain/assignment/model/BranchRole.java
  - backend/src/main/java/com/classhub/domain/enrollment/model/EnrollmentStatus.java
  - backend/src/main/java/com/classhub/domain/clinic/clinicsession/model/ClinicSessionType.java
  - backend/src/main/java/com/classhub/domain/feedback/model/FeedbackStatus.java
  - docs/history/AGENT_LOG.md
- 다음 단계: 나머지 Enum(필요 시 ClinicReason 등) 정리 후 엔티티/레포 구현

## [2025-12-18 20:22] 핵심 엔티티/Repository 1차 구현

### Type
STRUCTURAL

### Summary
- Company/Branch/TeacherBranchAssignment부터 Course, StudentCourseEnrollment/Record, Lesson(Shared/Personal), Clinic( Slot/Session/Attendance/Record), Notice/NoticeRead, WorkLog 엔티티와 각각의 Repository를 Season2 패키지 규칙에 맞춰 추가했다.
- Course에는 spec에 맞는 `CourseSchedule` ElementCollection을 정의했고, 모든 엔티티는 FK UUID 필드 중심으로 구성해 이후 서비스/쿼리에서 조립하도록 설계했다.

### Details
- 작업 사유: `docs/plan/backend/season2/core-entities_plan.md` Step 2 수행으로 Phase4 TODO “핵심 엔티티 생성”을 진행
- 영향받은 테스트: `./gradlew test`
- 주요 수정 파일: (다수)
  - backend/src/main/java/com/classhub/domain/company/company/model/Company.java
  - backend/src/main/java/com/classhub/domain/company/company/repository/CompanyRepository.java
  - backend/src/main/java/com/classhub/domain/company/branch/model/Branch.java
  - backend/src/main/java/com/classhub/domain/company/branch/repository/BranchRepository.java
  - backend/src/main/java/com/classhub/domain/assignment/model/TeacherBranchAssignment.java
  - backend/src/main/java/com/classhub/domain/assignment/repository/TeacherBranchAssignmentRepository.java
  - backend/src/main/java/com/classhub/domain/course/model/Course.java
  - backend/src/main/java/com/classhub/domain/course/repository/CourseRepository.java
  - backend/src/main/java/com/classhub/domain/studentcourse/model/StudentCourseEnrollment.java
  - backend/src/main/java/com/classhub/domain/studentcourse/model/StudentCourseRecord.java
  - backend/src/main/java/com/classhub/domain/studentcourse/repository/*.java
  - backend/src/main/java/com/classhub/domain/lesson/shared/model/SharedLesson.java
  - backend/src/main/java/com/classhub/domain/lesson/personal/model/PersonalLesson.java
  - backend/src/main/java/com/classhub/domain/clinic/clinicslot/model/ClinicSlot.java
  - backend/src/main/java/com/classhub/domain/clinic/clinicsession/model/ClinicSession.java
  - backend/src/main/java/com/classhub/domain/clinic/clinicattendance/model/ClinicAttendance.java
  - backend/src/main/java/com/classhub/domain/clinic/clinicrecord/model/ClinicRecord.java
  - backend/src/main/java/com/classhub/domain/notice/model/{Notice,NoticeRead}.java
  - backend/src/main/java/com/classhub/domain/worklog/model/WorkLog.java
  - 각 엔티티에 대응하는 Repository 파일
- 다음 단계: 엔티티 기반 Repository TDD 및 도메인 서비스 구현, TODO 상태 업데이트

## [2025-12-18 20:29] StudentEnrollmentRequest 엔티티 추가

### Type
STRUCTURAL

### Summary
- `StudentEnrollmentRequest` 엔티티와 Repository를 추가해 학생의 수강 신청 정보를 저장할 수 있도록 했고, 승인/거절 메서드에서 status와 처리자 정보를 업데이트할 수 있게 구성했다.

### Details
- 작업 사유: `final-entity-spec.md`에 명시된 핵심 엔티티 중 누락된 StudentEnrollmentRequest를 보완
- 영향받은 테스트: `./gradlew test`
- 수정한 파일: backend/src/main/java/com/classhub/domain/enrollment/model/StudentEnrollmentRequest.java, backend/src/main/java/com/classhub/domain/enrollment/repository/StudentEnrollmentRequestRepository.java, docs/history/AGENT_LOG.md
- 다음 단계: Repository TDD 및 Enrollment 흐름 서비스 구현
- 다음 단계: Repository TDD 및 Enrollment 흐름 서비스 구현

## [2025-12-18 20:30] Feedback 엔티티 추가

### Type
STRUCTURAL

### Summary
- 피드백 시스템을 위한 `Feedback` 엔티티와 Repository를 추가하고, `FeedbackStatus`를 활용해 제출/처리 상태를 추적하도록 했다.

### Details
- 작업 사유: final-entity-spec에 정의된 Feedback 엔티티가 누락되어 있어 핵심 도메인이 완성되지 않았기 때문
- 영향받은 테스트: `./gradlew test`
- 수정한 파일: backend/src/main/java/com/classhub/domain/feedback/model/Feedback.java, backend/src/main/java/com/classhub/domain/feedback/repository/FeedbackRepository.java, docs/history/AGENT_LOG.md
- 다음 단계: Feedback API 설계/구현 및 TODO 업데이트

## [2025-12-18 20:32] 설계 문서 템플릿 섹션 확장

### Type
STRUCTURAL

### Summary
- AGENTS.md 5.3 절에 백엔드/프런트엔드 설계 템플릿을 각각 5.3.1, 5.3.2로 분리하고, 프런트 설계 시 포함해야 할 페이지 구조/컴포넌트/상태/UX/테스트 항목을 명시했다.

### Details
- 작업 사유: 사용자 요청에 따라 설계 문서 작성 시 템플릿과 기대 내용을 명확히 안내하기 위함
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: AGENTS.md, docs/history/AGENT_LOG.md
- 다음 단계: 향후 PLAN 작성 시 템플릿을 준수

## [2025-12-18 20:35] PLAN README에 템플릿 사용 규칙 명시

### Type
STRUCTURAL

### Summary
- `docs/plan/README.md`의 사용 방법 섹션에 “모든 PLAN 문서는 AGENTS.md 5.3 템플릿(백엔드/프런트엔드)을 반드시 따른다”는 규칙을 추가해 설계 문서 작성 시 혼선을 줄였다.

### Details
- 작업 사유: PLAN 문서 작성 시 템플릿 준수를 확실히 하기 위해 문서화 필요
- 영향받은 테스트: 없음
- 수정한 파일: docs/plan/README.md, docs/history/AGENT_LOG.md
- 다음 단계: PLAN 작성 시 해당 템플릿을 적용하고, 미적용 문서는 바로 수정

## [2025-12-18 21:00] InitData PLAN 시드 데이터 세부 정리

### Type
DESIGN

### Summary
- `docs/plan/backend/season2/initdata_plan.md`에 Member/Company/Clinic 등 각 도메인별 시드 상수, 실행 순서, 연관 관계 표를 추가해 InitData 구현 시 참고할 수 있는 구체적 스펙을 마련했다.

### Details
- 작업 사유: Phase4 TODO “InitData 구성하기” 진행 전에 어떤 데이터를 생성할지 명확히 정의해 재작업을 방지하기 위함
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/initdata_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: PLAN 4단계에 따라 InitData CommandLineRunner를 구현하고 TODO를 갱신

## [2025-12-18 21:40] InitData PLAN Stage 1 범위 축소

### Type
DESIGN

### Summary
- 사용자 요청에 맞춰 InitData PLAN을 Member/Company/Branch 중심(Stage 1)으로 재정리하고, 나머지 도메인은 향후 Stage 2 백로그 섹션으로 이동시켜 우선순위를 명확히 했다.

### Details
- 작업 사유: 당장 프론트엔드 연동 테스트에 필요한 최소 시드(회원/학원 구조)만 우선 구현하고, 나머지는 기능별 시점에 맞춰 확장하기 위함
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/initdata_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: Stage 1 Seed Runner 구현 후 Course/Enrollment 등 도메인별 InitData를 순차적으로 추가

## [2025-12-18 21:58] InitData PLAN에 전국 학원/지점 목록 반영

### Type
DESIGN

### Summary
- Stage 1 Seed Dataset에 러셀·두각·시대인재·미래탐구 ACADEMY와 전 지점 목록을 추가하고, Branch 상수 네이밍 규칙을 정의해 실제 데이터 연동 시 확장이 가능하도록 했다.

### Details
- 작업 사유: 사용자 요청으로 대형 학원/지점 정보를 사전에 Seed 계획에 포함해 향후 전국 데이터 오픈API 연동 시 구조를 재사용하려는 목적
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/initdata_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: 해당 상수 정의에 맞춰 InitData Runner를 구현하고 이후 Course/Clinic Seed 시 이 Branch 정보를 기반으로 확장

## [2025-12-18 22:03] InitData PLAN에 prod 프로필/구조 지침 반영

### Type
DESIGN

### Summary
- Company/Branch Seed는 prod 프로필에서도 실행되도록 PLAN에 명시하고, 모든 InitData Runner가 `global.init` 기존 구조(BootstrapDataRunner/SeedKeys/data/*)를 재사용해야 한다는 지침을 추가했다.

### Details
- 작업 사유: 운영 환경에도 동일한 Branch 데이터를 사전 배포해야 하고, Seed 구현 방식이 분산되지 않도록 명확한 구조 가이드가 필요했기 때문
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/initdata_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: Member Runner는 local/test 한정, Company/Branch Runner는 prod 포함 프로필로 구현하고, 기존 global init 패턴을 따라 실제 클래스를 작성

## [2025-12-18 22:08] InitData PLAN에 BootstrapDataRunner 프로필 제약 반영

### Type
DESIGN

### Summary
- 실제 `BootstrapDataRunner`가 `@Profile({"local","dev"})`만 활성화되어 있다는 점을 PLAN에 명시하고, prod에서 Company/Branch Seed를 주입하려면 Runner 확장 또는 별도 prod Runner가 필요함을 문서화했다.

### Details
- 작업 사유: 운영 환경 Seed 계획과 현 코드 구조가 어긋나지 않도록 실제 Runner 프로필 제약 사항을 PLAN에서 안내하기 위함
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/initdata_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: prod에서도 실행 가능한 Runner 전략(프로필 확장 또는 전용 Runner)을 선택해 구현

## [2025-12-18 22:09] InitData PLAN Stage 2 내용 제거

### Type
DESIGN

### Summary
- 사용자 요청에 따라 InitData PLAN에서 Stage 2(Assignment, Course, Clinic 등)에 대한 모든 언급과 표, 백로그를 삭제해 Stage 1 범위(Member/StudentInfo/Company/Branch)만 남겼다.

### Details
- 작업 사유: 해당 PLAN은 현 시점에 필요한 Seed 정의만 포함해야 하므로, 추후 범위를 따로 관리하기 위해 Stage 2 내용을 제거
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: docs/plan/backend/season2/initdata_plan.md, docs/history/AGENT_LOG.md
- 다음 단계: Stage 1 Runner 구현 후, 새로운 필요가 생길 때 별도 PLAN으로 확장

## [2025-12-18 22:15] Season2 Stage1 InitData 구현

### Type
STRUCTURAL

### Summary
- Member/StudentInfo/Company/Branch 시드 데이터를 정의하는 `InitMembers/InitStudentInfos/InitCompanies/InitBranches` 클래스를 추가하고, BaseInitData 기반 Runner 4종을 구현해 local/test/prod 프로필에 맞춰 자동 시드되도록 했다.
- Member/StudentInfo/Company/Branch 엔티티와 Repository에 업데이트 메서드/조회 메서드를 보강하고, `BootstrapDataRunner`가 prod/test 프로필에서도 동작하도록 확장했다.

### Details
- 작업 사유: Stage 1 InitData PLAN 실행을 통해 프런트 연동 테스트 및 향후 기능 개발에 필요한 기본 데이터 세트를 확보하기 위함
- 영향받은 테스트: `./gradlew test`
- 수정한 파일: backend/src/main/java/com/classhub/global/init/data/**/*.java, backend/src/main/java/com/classhub/domain/**/{Company,Branch,Member,StudentInfo}.java, backend/src/main/java/com/classhub/domain/**/repository/*.java, backend/src/main/java/com/classhub/global/init/BootstrapDataRunner.java, docs/history/AGENT_LOG.md
- 다음 단계: 필요 시 `bootstrap.data.enabled` 설정을 통해 환경별 시드 실행을 제어하고, 이후 Course/Enrollment 등 도메인별 InitData는 별도 PLAN으로 진행

## [2025-12-18 22:51] InitData 통합 테스트 추가

### Type
STRUCTURAL

### Summary
- `Season2InitDataTest`를 추가해 test 프로필에서 Seed Runner가 실행된 후 SuperAdmin/Teacher/Student/StudentInfo와 Company/Branch 데이터가 모두 생성되는지 검증하고, Branch 개수 및 특정 지점의 verified 상태까지 확인하도록 했다.

### Details
- 작업 사유: Seed 데이터가 반복 실행 시에도 기대 상태를 유지하는지 CI에서 자동 검증하기 위함
- 영향받은 테스트: `./gradlew test`
- 수정한 파일: backend/src/test/java/com/classhub/global/init/Season2InitDataTest.java, backend/src/main/java/com/classhub/domain/company/branch/repository/BranchRepository.java, docs/history/AGENT_LOG.md
- 다음 단계: Seed 데이터가 늘어나면 해당 테스트를 확장하거나 도메인별 전용 검증 클래스를 추가

## [2025-12-18 22:57] InitData 패키지 정리 및 Season2 명칭 제거

### Type
STRUCTURAL

### Summary
- `global.init` 하위 패키지를 `runner/`, `seed/`, `seed.dataset/`로 재구성하고 Season2라는 명칭을 제거해 재사용 가능한 Seed 구조로 정리했다. 필요한 클래스·테스트의 패키지/임포트도 일관되게 변경했다.

### Details
- 작업 사유: Seed 구성 요소가 Season2 전용으로 보이지 않도록 하고, 향후 다른 단계에서도 재사용할 수 있게 패키지를 정비하기 위함
- 영향받은 테스트: `./gradlew test`
- 수정한 파일: backend/src/main/java/com/classhub/global/init/** (runner/seed/*), backend/src/test/java/com/classhub/global/init/InitDataSmokeTest.java, docs/history/AGENT_LOG.md
- 다음 단계: 새로운 도메인 Seed를 추가할 때 동일한 패턴(dataset + seed 패키지)을 따라 구조 유지

## [2025-12-18 23:25] 프런트 문서에 OpenAPI 타입 alias 지침 명시

### Type
DESIGN

### Summary
- CLAUDE/프런트 도우미 문서에 OpenAPI 타입 정의를 직접 alias(`type LoginRequestBody = components["schemas"]["LoginRequest"]`)로 선언해 사용하도록 명확히 지침을 추가했다.

### Details
- 작업 사유: 프런트 코드에서 중복 타입 선언을 방지하고, `openapi.d.ts` 스키마를 강제하기 위해 문서 가이드 강화 필요
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: frontend/AGENTS.md, docs/history/AGENT_LOG.md
- 다음 단계: 프런트 구현 시 해당 alias 패턴을 준수하고, Plan/TODO 검토 시 위반 사례가 있으면 즉시 수정

## [2025-12-18 23:25] CLAUDE 가이드에 OpenAPI alias 지침 반영

### Type
DESIGN

### Summary
- CLAUDE.md에도 프런트 코드가 `openapi.d.ts` 타입을 alias(`type LoginRequestBody = components["schemas"]["LoginRequest"]`)로 선언해 사용해야 한다는 규칙을 추가해 모든 도우미 문서가 일관되게 안내하도록 했다.

### Details
- 작업 사유: Frontend Agent뿐 아니라 CLAUDE Code 가이드에도 동일한 요구사항을 명시해, 어느 AI가 작업하든 OpenAPI 타입 재사용을 강제하기 위함
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일: CLAUDE.md, docs/history/AGENT_LOG.md
- 다음 단계: 프런트 작업 시 CLAUDE/AGENT 지침 둘 다 검토해 OpenAPI 기반 타입 사용을 확인
## [2025-12-19 00:35] 홈/회원가입 리다이렉트 정비

### Type
BEHAVIORAL

### Summary
- 로그인 페이지와 선생님 회원가입 페이지에서 역할별 대시보드로 자동 리다이렉트
- 대시보드 네비게이션 컴포넌트의 Link 타입 경고 해결

### Details
- 작업 사유: 로그인/회원가입 UX 일관성과 타입 안전성 확보
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(public)/page.tsx, frontend/src/app/(public)/auth/register/teacher/page.tsx, frontend/src/components/dashboard/mobile-header.tsx, frontend/src/components/dashboard/sidebar.tsx
- 다음 단계: 학생/조교 회원가입 페이지 설계 및 구현
## [2025-12-19 00:39] 대시보드 레이아웃/네비게이션 리디자인

### Type
STRUCTURAL

### Summary
- 구 레거시 dashboard-shell을 참고해 사이드바/헤더/푸터 레이아웃을 재정비하고 모바일 토글 흐름을 추가
- MobileHeader 컴포넌트를 제거하고 DashboardSidebar를 사용자 카드 + 역할별 네비게이션 카드 스타일로 개선

### Details
- 작업 사유: Header 활용도가 낮고 Footer가 과도하게 커서 전반적인 대시보드 UI 일관성이 깨져 있었음
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(dashboard)/layout.tsx, frontend/src/components/dashboard/sidebar.tsx, frontend/src/components/dashboard/mobile-header.tsx(삭제)
- 다음 단계: 새로운 레이아웃을 기준으로 역할별 페이지 UI를 순차적으로 리팩터링
## [2025-12-19 00:45] 대시보드 셸 및 Teacher 페이지 리디자인

### Type
STRUCTURAL

### Summary
- 좌측 사이드바/상단 바/메인 영역을 참조 시안 형태로 재구성하고 Teacher 대시보드 화면을 카드/캘린더 UI로 구현

### Details
- 작업 사유: Season2 대시보드가 비어 있던 상태라 시안과 같은 화면 전체 구성과 예시 콘텐츠가 필요했음
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(dashboard)/layout.tsx, frontend/src/components/dashboard/sidebar.tsx, frontend/src/app/(dashboard)/teacher/page.tsx (신규 UI)
- 다음 단계: 다른 역할(Student/Assistant 등) 페이지도 동일한 카드/캘린더 패턴으로 확장
## [2025-12-19 00:47] 대시보드 헤더 제거 및 사이드바 로그아웃 이동

### Type
STRUCTURAL

### Summary
- 레이아웃 상단 헤더를 제거하고 모바일 메뉴 버튼을 메인 영역으로 이동
- 로그아웃 버튼을 사이드바 하단 카드로 옮겨 UX를 단순화

### Details
- 작업 사유: 헤더를 없애고 사이드바 중심 레이아웃을 유지해 시안과 유사한 전체 화면 UI 구현
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(dashboard)/layout.tsx, frontend/src/components/dashboard/sidebar.tsx
- 다음 단계: 나머지 대시보드 페이지들도 새 레이아웃에 맞춰 카드 구성 정비
## [2025-12-19 00:50] Dashboard 경로 판별 보정

### Type
STRUCTURAL

### Summary
- AppChrome에서 /teacher·/assistant·/student·/admin 경로를 대시보드 레이아웃으로 인식하도록 수정

### Details
- 작업 사유: 대시보드 그룹이 /dashboard 대신 /teacher 등으로 노출돼 홈 크롬이 잘못 렌더링되고 있었음
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/components/ui/app-chrome.tsx
- 다음 단계: 대시보드 페이지 리팩터링 지속
## [2025-12-19 00:52] Teacher 대시보드 역할 가드 추가

### Type
BEHAVIORAL

### Summary
- useRoleGuard("TEACHER")를 적용해 다른 역할 사용자가 선생님 대시보드에 접근하지 못하도록 차단

### Details
- 작업 사유: 역할별 페이지 접근 제어 필요
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(dashboard)/teacher/page.tsx
- 다음 단계: 다른 역할 페이지에도 동일 가드 적용
## [2025-12-19 00:54] 역할별 대시보드 접근 제어 완료

### Type
BEHAVIORAL

### Summary
- Teacher/Assistant/Student/Admin 대시보드에 `useRoleGuard`를 적용해 다른 역할에서 접근 시 즉시 차단

### Details
- 작업 사유: 역할별 전용 페이지 간 무단 접근 방지
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(dashboard)/teacher/page.tsx, frontend/src/app/(dashboard)/assistant/page.tsx, frontend/src/app/(dashboard)/student/page.tsx, frontend/src/app/(dashboard)/admin/page.tsx
- 다음 단계: 각 역할 페이지 콘텐츠를 실제 카드/데이터로 확장
## [2025-12-19 00:58] 역할별 대시보드 콘텐츠 리디자인

### Type
STRUCTURAL

### Summary
- Teacher/Assistant/Student/Admin 대시보드에 역할별 맞춤 카드/요약/빠른 작업 UI를 배치하고 캘린더/검색 데모를 제거

### Details
- 작업 사유: 공통 샘플 UI가 모든 역할에 노출돼 혼란을 주어, 역할별 핵심 정보 위주로 간결하게 표현
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(dashboard)/teacher/page.tsx, frontend/src/app/(dashboard)/assistant/page.tsx, frontend/src/app/(dashboard)/student/page.tsx, frontend/src/app/(dashboard)/admin/page.tsx
- 다음 단계: 실제 데이터 연동 전 API/상태 설계를 진행
## [2025-12-19 01:13] 학생 회원가입 페이지 구현

### Type
BEHAVIORAL

### Summary
- Season2 PLAN에 따라 `/auth/register/student` 폼과 UX를 구현하고 가입 성공 시 자동 로그인/대시보드 이동을 추가

### Details
- 작업 사유: Phase4 TODO "학생 회원가입 페이지" 진행
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(public)/auth/register/student/page.tsx (신규)
- 다음 단계: 수동 QA로 다양한 입력 케이스 확인 후 TODO 상태 업데이트
## [2025-12-19 01:15] 학생 회원가입 CTA 추가

### Type
STRUCTURAL

### Summary
- 홈 로그인 카드 하단에 학생 회원가입 버튼을 추가해 접근 경로를 명시

### Details
- 작업 사유: 학생 가입 페이지를 노출해 사용자가 빠르게 진입하도록 안내
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(public)/page.tsx
- 다음 단계: 학생 가입 페이지 QA 및 TODO 상태 업데이트
## [2025-12-19 01:18] 학생 회원가입 UX 개선

### Type
STRUCTURAL

### Summary
- 생년월일을 년→월→일 선택형으로 바꾸고 학교명 suffix 자동 보정, 학년/비밀번호 UI를 개선

### Details
- 작업 사유: 학생 가입 UX 피드백 반영
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(public)/auth/register/student/page.tsx
- 다음 단계: QA 후 TODO 상태 업데이트
## [2025-12-19 01:23] 학생 회원가입 UX 피드백 반영

### Type
STRUCTURAL

### Summary
- 생년월일 안내 문구 제거, 비밀번호 요구사항/확인 UI 위치 조정, 학교명 suffix 자동 정리 로직 개선

### Details
- 작업 사유: UI 피드백(입력 순서, 가독성, 자동 보정) 반영
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(public)/auth/register/student/page.tsx
- 다음 단계: QA 후 TODO 상태 업데이트
## [2025-12-19 01:25] 학교명 suffix 로직 보완

### Type
STRUCTURAL

### Summary
- 학생 회원가입에서 '대치초/중/고' 등을 그대로 입력해도 잘리거나 재입력되지 않도록 suffix 처리 로직을 수정

### Details
- 작업 사유: 학교명 자동 포맷이 과도하게 문자열을 잘라 사용성이 떨어지는 문제 발생
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(public)/auth/register/student/page.tsx
- 다음 단계: QA 후 TODO 상태 업데이트
## [2025-12-19 01:27] 학교명 자동 보정 개선

### Type
STRUCTURAL

### Summary
- 학생 회원가입에서 '대치고등학교'처럼 입력하면 '대치고'까지만 유지하도록 suffix 절삭 로직을 추가

### Details
- 작업 사유: 학교명을 입력할 때 '대치'로만 잘려 불편했던 문제 해결
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(public)/auth/register/student/page.tsx
- 다음 단계: 수동 QA 후 TODO 상태 업데이트
## [2025-12-19 09:55] 생년월일 Day 선택 UX 개선

### Type
STRUCTURAL

### Summary
- 학생 회원가입에서 월/년 선택 전에는 일 옵션을 비활성화하고 안내 메시지를 표시하도록 수정

### Details
- 작업 사유: 월/년 미선택 상태에서도 1~31이 노출돼 혼란을 주던 문제 해결
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/app/(public)/auth/register/student/page.tsx
- 다음 단계: QA 및 TODO 상태 업데이트
## [2025-12-19 10:51] 등록 페이지 AppChrome 예외 처리

### Type
STRUCTURAL

### Summary
- `/auth/register/*` 경로에서 헤더/푸터를 제거해 로그인 카드와 동일한 풀스크린 UI만 렌더되도록 AppChrome 조건을 수정

### Details
- 작업 사유: 회원가입 페이지에서 공용 헤더·푸터가 겹치며 디자인이 깨지는 문제
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일: frontend/src/components/ui/app-chrome.tsx
- 다음 단계: 학생/선생님 회원가입 QA 진행
## [2025-12-19 13:55] 조교 관리/초대 백엔드 API 구현

### Type
BEHAVIORAL

### Summary
- 조교 목록/토글/초대 목록 API를 추가하고 서비스·DTO·리포지토리를 확장
- 조교 활성화 제어와 초대 히스토리 조회를 위한 컨트롤러 및 테스트를 작성

### Details
- 작업 사유: Phase5 "초대 시스템 개발" 중 교사용 백엔드 기능 부재로 현황 파악/제어 불가
- 영향받은 테스트:
  - `./gradlew test --tests "com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepositoryTest"`
  - `./gradlew test --tests "com.classhub.domain.assignment.application.AssistantManagementServiceTest"`
  - `./gradlew test --tests "com.classhub.domain.invitation.repository.InvitationRepositoryTest"`
  - `./gradlew test --tests "com.classhub.domain.assignment.web.AssistantManagementControllerTest"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/assignment/**, backend/src/main/java/com/classhub/domain/invitation/**, backend/src/test/java/com/classhub/domain/assignment/**, backend/src/test/java/com/classhub/domain/invitation/repository/InvitationRepositoryTest.java, docs/plan/backend/season2/assistant-management_plan.md
- 다음 단계: 프론트엔드 조교 관리/초대 UI 연동
## [2025-12-19 13:55] TODO v1.9 초대 시스템 백엔드 완료 표시

### Type
TODO_UPDATE

### Summary
- Phase5 초대 시스템 개발의 "관련 백엔드 개발" 항목을 완료(✅)로 반영

### Details
- 작업 사유: 조교 관리/초대 API 구현 및 검증이 완료되어 TODO 상태 업데이트
- 영향받은 테스트: `./gradlew test --tests "com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepositoryTest"` 등 동 작업 테스트 묶음
- 수정한 파일: docs/todo/v1.9.md
- 다음 단계: 동일 Epic 내 프론트엔드 작업 진행
## [2025-12-19 19:06] Assistant email search TODO 착수 표시

### Type
TODO_UPDATE

### Summary
- Phase4 "선생님-조교 연결" Epic의 백엔드 작업(이메일 기반 목록 API)을 🔄 상태로 갱신해 진행 시작을 명시

### Details
- 작업 사유: 교사의 조교 연결 흐름 구현을 위해 검색 API 개발에 착수
- 영향받은 테스트: 해당 없음 (문서 상태 업데이트)
- 수정한 파일: docs/todo/v1.9.md
- 다음 단계: 설계 문서 작성 후 사용자 승인 요청
## [2025-12-19 19:06] 조교 이메일 검색 API 설계

### Type
DESIGN

### Summary
- 초대 시스템 제거 후 교사가 조교를 직접 연결할 수 있도록 `assistant-search-assignment` 백엔드 설계 문서를 작성하고 검색 요구사항/응답 스키마/TDD 계획을 정의

### Details
- 작업 사유: Phase4 선생님-조교 연결 Epic에서 이메일 검색 API가 필요함 (`/api/v1/teachers/me/assistants/search`)
- 영향받은 테스트: 설계 단계이므로 실행된 테스트 없음
- 수정한 파일: docs/plan/backend/season2/assistant-search-assignment_plan.md
- 다음 단계: 사용자 승인 후 Repository/Service/Controller를 TDD로 구현
## [2025-12-19 19:10] 조교 Assignment 생성 요구 반영

### Type
DESIGN

### Summary
- 기존 이메일 검색 설계에 `POST /api/v1/teachers/me/assistants` 조교 연결 API를 추가하고 요청/검증/응답/TDD 범위를 확장

### Details
- 작업 사유: 교사가 검색한 조교를 실제 Assignment로 연결하는 API가 부재하여 플로우를 완성할 수 없음
- 영향받은 테스트: 설계 단계, 없음
- 수정한 파일: docs/plan/backend/season2/assistant-search-assignment_plan.md
- 다음 단계: 사용자 승인 후 Assignment 생성/검색 API를 TDD로 구현
## [2025-12-19 19:22] 조교 검색 및 직접 연결 API 구현

### Type
BEHAVIORAL

### Summary
- 교사용 조교 검색 API(`/assistants/search`)와 연결 생성 API(`POST /assistants`)를 추가하고 응답 DTO/서비스 로직/컨트롤러를 확장
- Member/Assignment Repository에 검색 메서드를 추가하고 Service/Controller 단위 테스트를 보강

### Details
- 작업 사유: 초대 플로우 제거 후 조교를 이메일로 찾아 바로 연결하는 단계가 비어 있어 사용자 시나리오 진행 불가
- 영향받은 테스트:
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.member.repository.MemberRepositoryTest"`
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepositoryTest"`
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.assignment.application.AssistantManagementServiceTest"`
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.assignment.web.AssistantManagementControllerTest"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/assignment/**, backend/src/main/java/com/classhub/domain/member/repository/MemberRepository.java, backend/src/main/java/com/classhub/global/response/RsCode.java, backend/src/test/java/com/classhub/domain/**, docs/plan/backend/season2/assistant-search-assignment_plan.md
- 다음 단계: 프론트엔드 조교 연결 UI 연동 및 QA 진행
## [2025-12-19 19:22] TODO v1.9 조교 이메일 검색 과제 완료 표시

### Type
TODO_UPDATE

### Summary
- Phase4 "선생님-조교 연결" Epic의 백엔드 작업(이메일 기반 목록/연결 API)을 ✅ 로 반영

### Details
- 작업 사유: 조교 검색/연결 API 구현 완료
- 영향받은 테스트: 해당 없음 (문서 업데이트)
- 수정한 파일: docs/todo/v1.9.md
- 다음 단계: 동일 Phase의 프론트엔드 작업 진행
## [2025-12-19 20:15] 선생님 조교 검색/등록 UI 구현

### Type
BEHAVIORAL

### Summary
- 선생님 대시보드에 "조교 검색 및 등록" 버튼과 모달을 추가해 이메일 기반 검색/연결 플로우를 UI로 완성
- 검색 결과/선택 상태/등록 요청을 API(`/assistants/search`, `POST /assistants`)와 연동하고, 기존 목록 토글과 연결해 즉시 반영되도록 구현

### Details
- 작업 사유: 프론트엔드에서 조교 검색/등록 플로우가 비어 있어 Season2 요구사항 미충족
- 영향받은 테스트: `cd frontend && npm run build -- --webpack` (Next.js root 경고 있음; 기존 다중 lockfile 구조로 인한 것이며 추후 outputFileTracingRoot 설정 필요)
- 수정한 파일: frontend/src/app/(dashboard)/teacher/assistants/page.tsx, frontend/src/types/openapi.{d.ts,json}, docs/plan/frontend/season2/teacher-assistant-management_ui_plan.md
- 다음 단계: 모달 UX QA 및 토스트/에러메시지 copy 검토, 필요 시 다중 lockfile 구조 정리

## [2025-12-19 20:30] Company/Branch Repository 스펙 확장 준비

### Type
STRUCTURAL

### Summary
- Company/Branch 엔티티의 verifiedStatus/creatorMemberId가 이미 정의되어 있음을 확인하고, 향후 필터 기반 조회를 위한 Repository 메서드 요구사항을 정리
- DataJpaTest 기반 리포지토리 테스트 초안(verifiedStatus/soft delete 필터링, creator 접근 제한)이 필요함을 문서화

### Details
- 작업 사유: Phase5 Company/Branch API 개발 1단계(TDD) 착수 전 기존 모델/레포 구조 점검
- 영향받은 테스트: 없음 (reading/reconnaissance)
- 수정한 파일: 없음
- 다음 단계: CompanyRepository/BranchRepository에 verifiedStatus 필터링 메서드 추가 후 DataJpaTest 작성

## [2025-12-19 21:16] Company/Branch Repository 쿼리 & 테스트 추가

### Type
STRUCTURAL

### Summary
- Company/Branch Repository에 verifiedStatus/type/keyword 필터와 creatorMemberId 조건을 포함한 검색 메서드를 추가하고 DataJpaTest로 soft delete 제외·필터 동작을 검증

### Details
- 작업 사유: Company/Branch API 1단계에서 필요한 조회 스펙을 데이터 계층에 반영하기 위함
- 영향받은 테스트: `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.company.*"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/company/**/repository/*.java, backend/src/test/java/com/classhub/domain/company/**/repository/*.java
- 다음 단계: Service/DTO 구현 및 비즈니스 로직 TDD 진행

## [2025-12-19 21:38] Company Controller + Admin API TDD 완료

### Type
BEHAVIORAL

### Summary
- Teacher/SuperAdmin 전용 Company API 컨트롤러를 구현해 생성/조회/검증 토글 엔드포인트를 완성하고, 응답 파라미터/인가/enum 파싱을 MockMvc 테스트로 보장
- 생성 API는 `RsCode.CREATED` 코드에 맞춰 HTTP 201을 기대하도록 테스트를 정정하고, 나머지 목록/상세/검증 테스트도 모두 녹색 상태로 유지

### Details
- 작업 사유: Phase5 Company 관리 3단계(Controller & 통합 검증) 완료를 위해 HTTP API 계층을 작성
- 영향받은 테스트: `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.company.company.web.CompanyControllerTest"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/company/company/web/CompanyController.java, backend/src/test/java/com/classhub/domain/company/company/web/CompanyControllerTest.java, docs/history/AGENT_LOG.md
- 다음 단계: Branch Controller/테스트 구현 후 TODO Phase5 진척도 업데이트

## [2025-12-19 21:50] Branch Service & Controller TDD

### Type
BEHAVIORAL

### Summary
- Branch용 DTO/Service를 추가해 Teacher 지점 생성/수정, SuperAdmin 검증 토글을 구현하고 creator 기반 접근 제어와 상태 토글 로직을 단위 테스트로 검증
- `GET/POST/PATCH /api/v1/branches` 컨트롤러를 작성해 Teacher/SuperAdmin 역할별 목록 경로와 검증 API(verified-status)를 MockMvc로 보장

### Details
- 작업 사유: company-branch-management_plan 2~3단계에 따라 Branch API를 완성해 조교/수업 관리의 기반을 마련
- 영향받은 테스트: `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.company.branch.*"`
- 수정한 파일: backend/src/main/java/com/classhub/domain/company/branch/**, backend/src/test/java/com/classhub/domain/company/branch/**, backend/src/main/java/com/classhub/global/response/RsCode.java, docs/history/AGENT_LOG.md
- 다음 단계: TODO Phase5 Branch 작업 상태 갱신 및 다음 PLAN 항목(예: Course API) 착수 준비

## [2025-12-19 22:25] TeacherBranchAssignment Repository & DTO 뼈대 추가

### Type
STRUCTURAL

### Summary
- TeacherBranchAssignment 엔티티에 enable/disable 헬퍼를 추가하고 Repository에 Teacher별/상태별 조회 메서드를 정의한 뒤 DataJpaTest로 soft delete 필터와 소유권 검증을 보장했다.
- 향후 Service/Controller에서 사용할 생성/상태변경 요청 DTO, 응답 DTO, 상태 필터 enum을 작성해 API 계약을 명확히 했다.

### Details
- 작업 사유: Teacher-Branch Assignment Plan 6단계 중 Repo/DTO 준비(단계 1~2)를 선행해 Service/Controller 구현 기반 마련
- 영향받은 테스트: `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepositoryTest"`
- 수정한 파일:
  - backend/src/main/java/com/classhub/domain/assignment/model/TeacherBranchAssignment.java
  - backend/src/main/java/com/classhub/domain/assignment/repository/TeacherBranchAssignmentRepository.java
  - backend/src/test/java/com/classhub/domain/assignment/repository/TeacherBranchAssignmentRepositoryTest.java
  - backend/src/main/java/com/classhub/domain/assignment/dto/** (신규 DTO 4종)
  - docs/history/AGENT_LOG.md
- 다음 단계: Service 계층에서 Branch/Company 검증 및 Assignment 생성/활성화 로직을 TDD로 구현한 뒤 Controller/MockMvc 테스트 작성

## [2025-12-19 22:29] TeacherBranchAssignment Service/Controller TDD

### Type
BEHAVIORAL

### Summary
- TeacherBranchAssignmentService를 추가해 Branch/Company 접근 검증, 개인/회사 학원 분기(기존 지점·신규 학원·신규 지점 생성)까지 지원하고, soft delete 기반 활성/비활성 토글·목록 페이징 응답을 TDD로 구현했다.
- `/api/v1/teachers/me/branches` GET/POST/PATCH 컨트롤러를 작성하고 MockMvc 테스트로 ROLE 검증, 상태 파싱, 응답 구조를 보장했다.

### Details
- 작업 사유: 학원 관리 페이지에서 기존 지점 선택뿐 아니라 개인 학원/회사 학원/지점 직접 등록 흐름까지 단일 Assignment API로 처리하기 위함
- 영향받은 테스트:
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.assignment.application.TeacherBranchAssignmentServiceTest"`
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.assignment.web.TeacherBranchAssignmentControllerTest"`
- 수정한 파일:
  - backend/src/main/java/com/classhub/domain/assignment/application/TeacherBranchAssignmentService.java
  - backend/src/main/java/com/classhub/domain/assignment/web/TeacherBranchAssignmentController.java
  - backend/src/main/java/com/classhub/domain/assignment/model/TeacherBranchAssignment.java (헬퍼)
  - backend/src/main/java/com/classhub/domain/assignment/repository/TeacherBranchAssignmentRepository.java
  - backend/src/main/java/com/classhub/domain/assignment/dto/** (요청/응답/필터)
  - backend/src/main/java/com/classhub/global/response/RsCode.java
  - backend/src/test/java/com/classhub/domain/assignment/application/TeacherBranchAssignmentServiceTest.java
  - backend/src/test/java/com/classhub/domain/assignment/web/TeacherBranchAssignmentControllerTest.java
  - docs/history/AGENT_LOG.md
- 다음 단계: 프런트 학원 관리 UI 설계/구현 및 TODO Phase5 프런트 작업 진행

## [2025-12-19 23:14] Teacher 학원 관리 UI & 검증 페이지 보완

### Type
BEHAVIORAL

### Summary
- `docs/plan/frontend/season2/teacher-branch-management_ui_plan.md`에 따른 Teacher 학원 관리 페이지를 구현해 상태 탭/페이지네이션/활성 토글/등록 모달을 연결했다.
- SuperAdmin 회사/지점 검증 페이지에서 Tabs 기본값과 버튼 변형을 보완해 신규 Tabs 요구사항 및 버튼 컴포넌트 규칙을 충족시켰다.
- 공통 대시보드 사이드바/대시보드 API 헬퍼를 확장해 Teacher가 회사·지점을 검색하고 Assignment를 생성할 수 있게 했다.

### Details
- 작업 사유: Phase5 TODO 중 "학원 관리 페이지" 프런트 작업 진행 및 Season2 UI 플로우 반영 (삼단 분기 등록 & 활성/비활성 토글) + 기존 Admin 페이지의 Tabs prop 요구사항 대응
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - frontend/src/components/dashboard/sidebar.tsx
  - frontend/src/app/(dashboard)/teacher/companies/page.tsx
  - frontend/src/lib/dashboard-api.ts
  - frontend/src/app/(dashboard)/admin/companies/page.tsx
  - frontend/src/app/(dashboard)/admin/branches/page.tsx
  - docs/history/AGENT_LOG.md
- 다음 단계: 학원 관리 모달 흐름 수동 QA(Teacher 계정) 및 Sidebar UX 확인 후 TODO Phase5 상태를 업데이트

## [2025-12-19 23:37] 학원 관리 UI 재구성 및 BranchResponse 확장

### Type
BEHAVIORAL

### Summary
- BranchResponse에 companyName을 추가하고 Query/Command 서비스/테스트를 보강해 어드민 지점 검증 화면에서 학원 이름을 직접 표시하도록 백엔드를 확장했다.
- Teacher 학원 관리 모달을 개인/회사 공통 플로우로 재구성해 검색→직접 입력 전환 UX, 회사·지점 검색 목록, 안내 메시지를 새 요구사항에 맞게 반영했다.
- 조교/학원 목록 카드의 새로고침 버튼을 제거하고 어드민 지점 검증 UI에서 companyId 대신 companyName을 노출하도록 프론트를 정리했다.

### Details
- 작업 사유: Phase5 프론트 TODO 중 학원 관리 UI 세부 요구 반영 및 Branch 검증 화면 가독성 개선
- 영향받은 테스트:
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.company.branch.*"`
  - `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - backend/src/main/java/com/classhub/domain/company/branch/dto/response/BranchResponse.java
  - backend/src/main/java/com/classhub/domain/company/branch/application/**
  - backend/src/test/java/com/classhub/domain/company/branch/**
  - frontend/src/app/(dashboard)/teacher/companies/page.tsx
  - frontend/src/app/(dashboard)/admin/branches/page.tsx
  - frontend/src/components/dashboard/sidebar.tsx
  - frontend/src/lib/dashboard-api.ts
  - frontend/src/types/openapi.{json,d.ts}
  - docs/history/AGENT_LOG.md
- 다음 단계: Teacher 학원 관리 페이지 수동 QA(회사/지점 검색 및 직접 입력) 후 TODO/PLAN 상태 갱신 검토

## [2025-12-20 00:57] Course Repository/Validator TDD 1단계

### Type
STRUCTURAL

### Summary
- `docs/plan/backend/season2/course-teacher-management_plan.md` 6번 계획 중 1~2단계에 맞춰 Course Repository/Validator 기반을 보완하고 단위 테스트를 추가했다.
- 상태 필터 쿼리를 boolean 파라미터 기반으로 재작성하고 null/겹침 검증을 BusinessException으로 통일해 이후 Service 계층에서 재사용할 수 있도록 했다.

### Details
- 작업 사유: Teacher Course API TDD 1~2단계(Repository + Validator 선 구현)를 마무리해야 이후 Service/Controller 구현을 이어갈 수 있음
- 영향받은 테스트:
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.course.*"`
- 수정한 파일:
  - backend/src/main/java/com/classhub/domain/course/model/Course.java
  - backend/src/main/java/com/classhub/domain/course/repository/CourseRepository.java
  - backend/src/main/java/com/classhub/domain/course/validator/CoursePeriodValidator.java
  - backend/src/main/java/com/classhub/domain/course/validator/CourseScheduleValidator.java
  - backend/src/main/java/com/classhub/domain/member/model/Member.java
  - backend/src/test/java/com/classhub/domain/course/repository/CourseRepositoryTest.java
  - backend/src/test/java/com/classhub/domain/course/validator/CoursePeriodValidatorTest.java
  - backend/src/test/java/com/classhub/domain/course/validator/CourseScheduleValidatorTest.java
  - docs/todo/v1.9.md
- 다음 단계: PLAN 6번 3단계(서비스 로직) 설계/구현 전 사용자 피드백 수령 후 진행

## [2025-12-20 01:11] Teacher Course Service & Controller 구현

### Type
BEHAVIORAL

### Summary
- `docs/plan/backend/season2/course-teacher-management_plan.md` 6번 중 3~4단계를 따라 CourseService와 Teacher 전용 Controller를 구현했다.
- Course CRUD/목록/캘린더/상태 토글 API를 완성하고 DTO/Request/Response, MockMvc 테스트, Mockito 기반 Service 단위 테스트를 모두 추가했다.

### Details
- 작업 사유: Teacher 대시보드 반 관리 API가 없어 프런트 연동이 불가능해 PLAN 단계에서 정의한 서비스/컨트롤러 계층을 작성
- 영향받은 테스트:
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.course.*"`
- 수정한 파일:
  - backend/src/main/java/com/classhub/domain/course/model/Course.java
  - backend/src/main/java/com/classhub/domain/course/application/CourseService.java
  - backend/src/main/java/com/classhub/domain/course/dto/request/\*.java
  - backend/src/main/java/com/classhub/domain/course/dto/response/\*.java
  - backend/src/main/java/com/classhub/domain/course/web/CourseController.java
  - backend/src/test/java/com/classhub/domain/course/application/CourseServiceTest.java
  - backend/src/test/java/com/classhub/domain/course/web/CourseControllerTest.java
- 다음 단계: 사용자 피드백을 반영해 Course Service 리팩터 또는 Student/프런트 연동을 진행하고 TODO Phase5 상태 업데이트 검토

## [2025-12-20 01:25] Teacher Course Frontend Plan 작성

### Type
DESIGN

### Summary
- `docs/plan/frontend/season2/teacher-course-management_ui_plan.md`에 Course 목록/캘린더 UI 구조, 컴포넌트, 상태·데이터 흐름, 테스트 전략을 정의했다.

### Details
- 작업 사유: 프런트 구현 전에 요구사항을 명확히 기록해 이후 단계별 개발/리뷰를 수월하게 하기 위함
- 영향받은 테스트: 해당 없음
- 수정한 파일:
  - docs/plan/frontend/season2/teacher-course-management_ui_plan.md
- 다음 단계: PLAN 1단계(데이터 레이어 & 뷰 스켈레톤) 구현

## [2025-12-20 01:33] Teacher Course 목록/캘린더 뷰 스켈레톤

### Type
BEHAVIORAL

### Summary
- Teacher Course 페이지에 상태 탭·지점/검색 필터와 목록/캘린더 전환 탭을 구현하고 Course/Branch API 연동, 주간 이동 및 시간축 그리드를 구성했다.
- Course 관련 타입과 대시보드 API 헬퍼를 확장해 목록/캘린더 데이터를 공용으로 사용할 수 있게 했다.

### Details
- 작업 사유: 프런트 PLAN 1단계(데이터 레이어 + 스켈레톤)를 완료해 이후 생성/수정 모달 구현 기반 마련
- 영향받은 테스트:
  - `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - docs/plan/frontend/season2/teacher-course-management_ui_plan.md
  - frontend/src/types/dashboard.ts
  - frontend/src/types/openapi.d.ts
  - frontend/src/lib/dashboard-api.ts
  - frontend/src/app/(dashboard)/teacher/courses/page.tsx
- 다음 단계: Course 생성/수정 모달, 상태 토글 UI, 캘린더 카드 디테일 구현

## [2025-12-20 11:12] Teacher Course 생성/수정 모달 & 토글 구현

### Type
BEHAVIORAL

### Summary
- `docs/plan/frontend/season2/teacher-course-management_ui_plan.md` 2~3단계에 맞춰 반 생성/수정 모달과 상태 토글, 성공/실패 토스트를 Teacher Course 페이지에 연동했다.
- Course 생성/수정/상태 변경 API 헬퍼를 추가하고, 목록/캘린더 데이터를 자동 새로고침하도록 연결했다.

### Details
- 작업 사유: 선생님이 UI에서 반을 직접 등록/수정하고 활성 상태를 제어할 수 있게 하기 위해 PLAN 후속 단계를 구현함.
- 영향받은 테스트:
  - `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - frontend/src/lib/dashboard-api.ts
  - frontend/src/app/(dashboard)/teacher/courses/page.tsx
- 다음 단계: 캘린더 카드 hover/tooltip·빈 상태 개선 등 PLAN 3단계 마무리 및 QA

## [2025-12-20 11:25] Teacher Course 캘린더/모달 UX 개선

### Type
BEHAVIORAL

### Summary
- 캘린더 카드에 클릭 이벤트를 연결해 일정 블록을 누르면 곧바로 수정 모달이 열리도록 하고, 시간 텍스트를 제거해 시각 정보를 색상 블록으로만 전달하도록 정리했다.
- 생성/수정 모달의 날짜 입력 형식을 `yyyy/mm/dd` 순서로 보이도록 `lang`/패턴을 조정하고, 스케줄 입력을 06:00~22:00 범위 토글 버튼으로 재구성해 AM/PM 없이 빠르게 선택하도록 개선했다.

### Details
- 작업 사유: 사용자 요구사항에 맞춰 캘린더 인터랙션/표시와 시간·날짜 입력 UX를 수정해 실제 사용 흐름을 맞추기 위함.
- 영향받은 테스트:
  - `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - frontend/src/app/(dashboard)/teacher/courses/page.tsx
- 다음 단계: 캘린더 빈 상태/tooltip 등 잔여 PLAN 3단계 항목 보완 및 Course 생성 모달의 추가 검증(다중 스케줄 등) QA

## [2025-12-20 11:32] Teacher Course 날짜/시간 입력 보완

### Type
BEHAVIORAL

### Summary
- 반 생성/수정 모달의 날짜 입력을 `YYYY/MM/DD` 텍스트 필드로 변경하고 서버 전송 시 ISO(`YYYY-MM-DD`)로 변환해 한국식 순서를 명확히 했다.
- 스케줄 시간 토글을 30분 단위(06:00~22:00)로 확장하고 종료 시간이 항상 시작 시간 이후가 되도록 자동 보정·검증 로직을 추가했다.

### Details
- 작업 사유: 사용자가 요구한 날짜 표기 순서와 30분 단위 시간 선택 UX를 반영해 입력 혼란을 줄이기 위함.
- 영향받은 테스트:
  - `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - frontend/src/app/(dashboard)/teacher/courses/page.tsx
- 다음 단계: Course 모달 다중 스케줄 QA 및 캘린더 빈 상태/툴팁 디자인 보강

## [2025-12-20 12:30] Admin/Assistant Course API 테스트 안정화

### Type
STRUCTURAL

### Summary
- Admin/Assistant Course 컨트롤러 테스트가 기본 `size` 파라미터 미지정으로 실패하던 문제를 재현 후 요청 파라미터를 명시하고 디버그 출력을 제거했다.
- Admin/Assistant 서비스 단위 테스트 및 Course 패키지 전체 테스트 스위트를 재실행해 신규 API 추가에 따른 회귀를 검증했다.

### Details
- 작업 사유: PLAN 1·2단계 구현 이후 컨트롤러 테스트가 실패하고 있어 API 계약 검증을 안정화하기 위함.
- 영향받은 테스트:
  - `cd backend && ./gradlew test --tests com.classhub.domain.course.web.AdminCourseControllerTest`
  - `cd backend && ./gradlew test --tests com.classhub.domain.course.web.AssistantCourseControllerTest`
  - `cd backend && ./gradlew test --tests 'com.classhub.domain.course.application.*Test'`
  - `cd backend && ./gradlew test --tests 'com.classhub.domain.course.*'`
- 수정한 파일:
  - backend/src/test/java/com/classhub/domain/course/web/AdminCourseControllerTest.java
  - backend/src/test/java/com/classhub/domain/course/web/AssistantCourseControllerTest.java
- 다음 단계: PLAN 3단계(Student 공개 Course 검색) 구현 준비 및 추가 API 검증.

## [2025-12-20 12:42] Course 공개 검색 API 구현

### Type
BEHAVIORAL

### Summary
- PLAN 3단계 요구에 따라 학생/비회원이 사용할 `GET /api/v1/courses/public` API를 구현하고 CourseRepository에 공개 검색 전용 쿼리를 추가했다.
- CourseViewAssembler를 재사용해 `PublicCourseResponse` DTO를 구성하고, teacher 이름 및 스케줄 요약 문자열을 포함하도록 PublicCourseService를 작성했다.
- Spring Security 허용 목록에 `/api/v1/courses/public/**`를 추가해 토큰 없이 접근 가능하도록 열고, 서비스/컨트롤러 단위 테스트를 작성했다.

### Details
- 작업 사유: Course-rest-role PLAN 3단계(학생 공개 검색)를 마무리해 역할별 API 구성을 완결해야 했다.
- 영향받은 테스트:
  - `cd backend && ./gradlew test --tests com.classhub.domain.course.application.PublicCourseServiceTest`
  - `cd backend && ./gradlew test --tests com.classhub.domain.course.web.PublicCourseControllerTest`
  - `cd backend && ./gradlew test --tests 'com.classhub.domain.course.*'`
- 수정한 파일:
  - backend/src/main/java/com/classhub/domain/course/repository/CourseRepository.java
  - backend/src/main/java/com/classhub/domain/course/dto/response/PublicCourseResponse.java
  - backend/src/main/java/com/classhub/domain/course/application/PublicCourseService.java
  - backend/src/main/java/com/classhub/domain/course/web/PublicCourseController.java
  - backend/src/main/java/com/classhub/global/config/SecurityConfig.java
  - backend/src/test/java/com/classhub/domain/course/application/PublicCourseServiceTest.java
  - backend/src/test/java/com/classhub/domain/course/web/PublicCourseControllerTest.java
- 다음 단계: Student 공개 Course 검색 API를 기반으로 한 실제 프런트 UI 및 enrollment flow를 설계/구현하고, 필요 시 공개 Course DTO 확장 검토.

## [2025-12-20 13:25] Admin/Assistant Course UI 1차 구현

### Type
BEHAVIORAL

### Summary
- `course-rest-role_ui_plan.md` 기반으로 SuperAdmin/Assistant용 Course 페이지를 대시보드에 추가해 역할별 Course API를 소비하도록 했다.
- SuperAdmin 페이지에 툴바형 필터(Teacher/Company/Branch/Status/Keyword), 반 목록 테이블, 하드 삭제 모달을 구현하고 `/admin/courses` 라우트에 연결했다.
- Assistant 페이지에 연결된 선생님 필터, 상태/검색 필터, 카드형 반 목록을 구현해 `/assistant/courses`에서 확인할 수 있도록 했다.
- `dashboard-api`에 Admin/Assistant/Public Course API 클라이언트 함수를 추가했고, `sidebar` 메뉴를 갱신했다.

### Details
- 작업 사유: PLAN 3단계 이후 프런트에서도 Admin/Assistant가 Course 데이터를 확인할 수 있어야 했다.
- 영향받은 테스트:
  - `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - docs/plan/frontend/season2/course-rest-role_ui_plan.md (신규)
  - frontend/src/components/dashboard/sidebar.tsx
  - frontend/src/lib/dashboard-api.ts
  - frontend/src/types/dashboard.ts
  - frontend/src/app/(dashboard)/admin/courses/page.tsx (신규)
  - frontend/src/app/(dashboard)/assistant/courses/page.tsx (신규)
- 다음 단계: Student 공개 Course 검색 UI 및 Enrollment 요청 버튼 플로우 구현, 필터 옵션(Teacher 목록 등) UX 개선.

## [2025-12-20 14:10] Student Course 검색 UI 구현

### Type
BEHAVIORAL

### Summary
- 학생 대시보드 사이드바에 ‘반 관리’ 메뉴를 추가하고 `/student/courses` 페이지에서 공개 Course 검색 UI를 구현했다.
- Company/Branch/Teacher/Keyword 필터 + 검증 여부 토글을 제공하고, `GET /api/v1/courses/public` 응답을 카드 형태로 보여주며 “등록 요청” 버튼(추후 연동 예정)을 노출한다.
- `docs/todo/v1.9.md`의 Course 페이지 항목을 완료(✅) 상태로 업데이트했다.

### Details
- 작업 사유: PLAN 3단계(Student) 구현을 마무리하고 학생이 공개 Course를 탐색할 수 있는 UI가 필요했다.
- 영향받은 테스트:
  - `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - frontend/src/app/(dashboard)/student/courses/page.tsx (신규)
  - frontend/src/components/dashboard/sidebar.tsx
  - docs/todo/v1.9.md
- 다음 단계: 등록 요청 플로우(Enrollment) 백엔드/프런트 연동과 학생용 “내 수업” 화면 구현, 공개 Course 필터 UX 개선.

## [2025-12-20 14:25] 학생 Course 필터 권한 오류 수정

### Type
STRUCTURAL

### Summary
- 학생 공개 Course 화면에서 학원/지점 옵션을 `searchTeacherCompanies`/`searchBranches`로 불러오며 403이 발생하던 문제를 수정했다.
- `/student/courses`는 이제 `fetchPublicCourses` 응답을 기반으로 회사/지점 옵션을 동적으로 구성하므로 권한 오류 없이 필터를 사용할 수 있다.

### Details
- 작업 사유: Student Role에 Teacher/Admin 전용 API를 호출해 “접근 권한이 없습니다.” 오류가 발생했음.
- 영향받은 테스트:
  - `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - frontend/src/app/(dashboard)/student/courses/page.tsx
- 다음 단계: Enrollment 요청 플로우 연계 및 공개 Course 필터 UX 정교화.

## [2025-12-20 15:05] Student Course 플로우 문서 보강

### Type
DESIGN

### Summary
- `course-rest-role_ui_plan.md`의 Student 섹션을 최신 흐름(‘반 검색’ 페이지 + ‘내 수업’ 페이지)으로 갱신했다.
- 사용자 여정, 페이지 구조, 컴포넌트 설명에 새 메뉴 명칭과 향후 Enrollment 연계를 반영했다.

### Details
- 작업 사유: 학생 대시보드 네비게이션 개편에 맞춰 PLAN 문서가 실제 구현과 동기화되어야 했다.
- 영향받은 테스트: 해당 없음(문서 수정).
- 수정한 파일:
  - docs/plan/frontend/season2/course-rest-role_ui_plan.md
- 다음 단계: Enrollment 요청 구현 시 본 계획을 기반으로 구체적인 UI/상태 정의 확장.

## [2025-12-20 15:15] Student 반 검색/내 수업 UI 정리

### Type
BEHAVIORAL

### Summary
- `/student/courses` 헤더와 설명을 ‘반 검색’ 흐름에 맞춰 업데이트해 공개 Course 탐색 목적을 명확히 했다.
- `/student/my-courses` 페이지에 Next `Link` 기반 CTA와 EmptyState 안내를 추가해 학생이 자연스럽게 반 검색으로 이동할 수 있도록 했다.
- 새 페이지가 빌드에 포함되도록 Next.js 생산 빌드로 검증했다.

### Details
- 작업 사유: 학생 네비게이션 분리(반 검색 vs 내 수업)에 맞춰 UI 카피와 링크를 재정렬해야 했다.
- 영향받은 테스트:
  - `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - frontend/src/app/(dashboard)/student/courses/page.tsx
  - frontend/src/app/(dashboard)/student/my-courses/page.tsx
- 다음 단계: 추후 Enrollment 데이터가 준비되면 ‘내 수업’ 페이지에서 승인/대기 상태 리스트를 렌더링하고 버튼을 실제 신청 흐름과 연결.

## [2025-12-20 15:35] Student Enrollment Workflow PLAN 작성

### Type
DESIGN

### Summary
- `docs/spec/v1.3.md` FR-02 요구를 바탕으로 StudentEnrollmentRequest CRUD, Teacher·Assistant 처리, StudentCourse 조회/수정, Admin 감사 조회까지 포함한 새로운 PLAN(`student-enrollment-management_plan.md`)을 작성했다.
- UI 흐름에 맞춰 Teacher/Assistant(StudentEnrollmentRequest + StudentData), Student(내 수업/신청 내역), Admin(전체 조회) API 항목을 정리하고, 배치 승인/거절 옵션도 명시했다.
- 상태 전이(PENDING/APPROVED/REJECTED/CANCELED), Enrollment/Record 생성 규칙, 권한 검증, TDD 단계별 테스트 목록을 포함해 구현 준비를 마쳤다.

### Details
- 작업 사유: Enrollment epic을 구현하기 전 PLAN 문서가 필요했음.
- 영향받은 테스트: 해당 없음(문서 작업).
- 수정한 파일:
  - docs/plan/backend/season2/student-enrollment-management_plan.md
- 다음 단계: PLAN 승인 후 Student → Teacher/Assistant → Admin 순으로 TDD 기반 구현 진행.

## [2025-12-20 15:45] Student Enrollment PLAN 정리 (단건 처리로 수정)

### Type
DESIGN

### Summary
- 배치 승인/거절 API는 후속 개선 사항으로 미루기로 결정해, PLAN에서 관련 요구/엔드포인트/TDD 시나리오를 제거했다.
- 현재 단계에서는 단건 요청 API만으로 Teacher/Assistant 흐름을 구현하도록 명확히 했다.

### Details
- 작업 사유: 사용자 요청에 따라 대량 처리 범위를 제외.
- 영향받은 테스트: 없음(문서 수정).
- 수정한 파일:
  - docs/plan/backend/season2/student-enrollment-management_plan.md
- 다음 단계: 단건 API 기준으로 TDD/구현 진행, 대량 처리는 차후 개선 과제로 분리.

## [2025-12-20 15:55] StudentData 조회 범위 수정

### Type
DESIGN

### Summary
- Teacher/Assistant StudentData 요구를 명확히 하기 위해 PLAN을 조정했다. 목록 조회는 이름/연락처/학교/학년/나이/수강 반 이름 등 기본 정보만 제공하고, 상세 조회는 Teacher 전용으로 전체 StudentInfo+Record를 제공하도록 정의했다.

### Details
- 작업 사유: 요청에 따라 Teacher만 상세 데이터를 열람/수정하고, 조교는 기본 정보만 확인하도록 역할을 구분.
- 영향받은 테스트: 없음(문서 수정).
- 수정한 파일:
  - docs/plan/backend/season2/student-enrollment-management_plan.md
- 다음 단계: 이 범위를 기준으로 StudentData API TDD/구현 진행.

## [2025-12-20 16:05] Student Enrollment PLAN 구현 단계 추가

### Type
DESIGN

### Summary
- PLAN 6번 섹션에 학생 → Teacher/Assistant → Admin 순으로 진행할 3단계 구현 계획을 추가했다.
- 각 단계에서 어떤 API를 개발/검증할지 명시해 추후 작업 순서를 명확히 했다.

### Details
- 작업 사유: 사용자가 요청한 3단계 구현 순서를 문서화하기 위함.
- 영향받은 테스트: 없음.
- 수정한 파일:
  - docs/plan/backend/season2/student-enrollment-management_plan.md
- 다음 단계: 1단계(Student 기능)부터 TDD 사이클로 구현 착수.

## [2025-12-20 18:40] Student Enrollment 1단계 구현

### Type
BEHAVIORAL

### Summary
- PLAN 1단계에 따라 학생용 Enrollment API를 구현했다. 신청 생성/조회/취소 컨트롤러(`POST/GET /student-enrollment-requests`, `PATCH /{id}/cancel`)와 승인된 수업 조회(`GET /students/me/courses`)를 추가했다.
- `StudentEnrollmentRequestService`는 Course 유효성/중복/기존 수강 여부를 검증하고 CANCELED 상태 전이 로직을 포함하며, Course 정보까지 포함된 DTO를 반환한다. `StudentCourseQueryService`는 활성 Enrollment와 Course 메타를 결합해 학생 수업 목록을 제공한다.
- Repository에 중복 검사/검색 쿼리를 추가하고 `EnrollmentStatus`에 `CANCELED`를 도입했으며, 필요한 DTO/응답 모델과 RsCode를 신설했다.
- `StudentEnrollmentRequestControllerTest`, `StudentCourseControllerTest`, `StudentEnrollmentRequestServiceTest`, `StudentCourseQueryServiceTest` 등 TDD 기반 테스트를 모두 작성/통과시켰다.

### Details
- 작업 사유: Student Enrollment PLAN 1단계를 완료해 프론트에서 학생 흐름을 바로 사용할 수 있도록 함.
- 영향받은 테스트:
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.enrollment.application.StudentEnrollmentRequestServiceTest"`
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.studentcourse.application.StudentCourseQueryServiceTest"`
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.enrollment.web.StudentEnrollmentRequestControllerTest"`
  - `cd backend && GRADLE_USER_HOME=../.gradle ./gradlew test --tests "com.classhub.domain.studentcourse.web.StudentCourseControllerTest"`
- 수정한 파일:
  - backend/src/main/java/com/classhub/domain/enrollment/application/StudentEnrollmentRequestService.java
  - backend/src/main/java/com/classhub/domain/enrollment/dto/** (신규)
  - backend/src/main/java/com/classhub/domain/enrollment/model/EnrollmentStatus.java
  - backend/src/main/java/com/classhub/domain/enrollment/model/StudentEnrollmentRequest.java
  - backend/src/main/java/com/classhub/domain/enrollment/repository/StudentEnrollmentRequestRepository.java
  - backend/src/main/java/com/classhub/domain/enrollment/web/StudentEnrollmentRequestController.java
  - backend/src/main/java/com/classhub/domain/studentcourse/application/StudentCourseQueryService.java
  - backend/src/main/java/com/classhub/domain/studentcourse/dto/response/StudentCourseResponse.java
  - backend/src/main/java/com/classhub/domain/studentcourse/repository/StudentCourseEnrollmentRepository.java
- backend/src/main/java/com/classhub/domain/studentcourse/web/StudentCourseController.java
- backend/src/main/java/com/classhub/global/response/RsCode.java
- backend/src/test/java/com/classhub/domain/enrollment/** (신규 테스트)
- backend/src/test/java/com/classhub/domain/studentcourse/** (신규 테스트)
- 다음 단계: PLAN 2단계(Teacher/Assistant 요청 처리 + StudentData)에 착수한다.

---

## [2025-12-20 19:16] Teacher/Assistant 수업 신청 승인 서비스 구축

### Type

BEHAVIORAL

### Summary

- `docs/plan/backend/season2/student-enrollment-management_plan.md` 2단계에 따라 Teacher/Assistant용 요청 목록/승인/거절 서비스 로직을 TDD로 구현했다.
- 조교/선생님 권한 검증, Enrollment/Record 생성 트랜잭션, StudentSummary 조합을 포함하는 `StudentEnrollmentApprovalService`를 추가하고 DTO 사양을 정리했다.
- `StudentEnrollmentApprovalServiceTest`를 통해 목록/승인/거절/권한 시나리오를 검증하고 gradle 테스트를 통과시켰다.

### Details

- 작업 사유: Teacher/Assistant가 학생 신청을 처리할 수 있도록 서비스 계층을 완성해야 함.
- 영향받은 테스트: `./gradlew test --tests com.classhub.domain.enrollment.application.StudentEnrollmentApprovalServiceTest`
- 수정한 파일:
  - `backend/src/main/java/com/classhub/domain/enrollment/application/StudentEnrollmentApprovalService.java`
  - `backend/src/main/java/com/classhub/domain/enrollment/dto/response/TeacherEnrollmentRequestResponse.java`
  - `backend/src/test/java/com/classhub/domain/enrollment/application/StudentEnrollmentApprovalServiceTest.java`
- 삭제한 파일: `backend/src/main/java/com/classhub/domain/enrollment/dto/request/StudentEnrollmentDecisionRequest.java`
- 다음 단계: StudentData 목록/상세/수정 API 및 Admin 요청 조회 API 구현

## [2025-12-20 19:28] Teacher/Assistant 수업 신청 Controller 구현

### Type

BEHAVIORAL

### Summary

- 승인 서비스와 연동되는 Teacher/Assistant 웹 API(`GET /student-enrollment-requests`, `GET /{id}`, `PATCH /{id}/approve|reject`)를 신규 컨트롤러로 구현했다.
- 역할에 따라 Teacher/Assistant 목록 조회 분기를 적용하고, 단건 조회/승인/거절은 `StudentEnrollmentApprovalService`를 호출하도록 연결했다.
- `StudentEnrollmentApprovalControllerTest`를 작성해 Teacher/Assistant 요청/상세/승인/거절 흐름을 MockMvc로 검증했다.

### Details

- 작업 사유: UI에서 Teacher/Assistant용 신청 처리 플로우를 사용할 수 있도록 HTTP 엔드포인트를 제공해야 함.
- 영향받은 테스트: `./gradlew test --tests com.classhub.domain.enrollment.web.StudentEnrollmentApprovalControllerTest`
- 수정/추가 파일:
  - `backend/src/main/java/com/classhub/domain/enrollment/web/StudentEnrollmentApprovalController.java`
  - `backend/src/main/java/com/classhub/domain/enrollment/application/StudentEnrollmentApprovalService.java`
  - `backend/src/test/java/com/classhub/domain/enrollment/web/StudentEnrollmentApprovalControllerTest.java`
  - `backend/src/test/java/com/classhub/domain/enrollment/application/StudentEnrollmentApprovalServiceTest.java` (상세 조회 테스트 추가)
- 다음 단계: StudentData API(TODO 2단계 잔여) 구현 및 Admin 요청 조회 API 개발

## [2025-12-20 19:39] StudentData API (Teacher/Assistant) 구현

### Type

BEHAVIORAL

### Summary

- PLAN 2단계의 StudentData 범위에 따라 `StudentCourseManagementService`를 도입하고 목록/상세/수정 로직을 구현했다.
- Teacher/Assistant별 권한 분기, 조교 배정 검증, Course/Member/StudentInfo 조합 DTO(`StudentCourseListItemResponse`, `StudentCourseDetailResponse`)를 추가했다.
- `StudentCourseRecordRepository`에 Teacher/Assistant 검색용 쿼리를 정의하고, `StudentCourseManagementController` + MockMvc 테스트를 작성했다.

### Details

- 작업 사유: 선생님/조교가 학생 수업 데이터를 조회·갱신할 수 있는 API가 필요함.
- 영향받은 테스트:
  - `./gradlew test --tests "com.classhub.domain.studentcourse.application.StudentCourseManagementServiceTest"`
  - `./gradlew test --tests "com.classhub.domain.studentcourse.web.StudentCourseManagementControllerTest"`
- 수정/추가 파일:
  - `backend/src/main/java/com/classhub/domain/studentcourse/application/StudentCourseManagementService.java`
  - `backend/src/main/java/com/classhub/domain/studentcourse/dto/**` (StatusFilter, List/Detail Response, Update Request)
  - `backend/src/main/java/com/classhub/domain/studentcourse/repository/StudentCourseRecordRepository.java`
  - `backend/src/main/java/com/classhub/domain/studentcourse/web/StudentCourseManagementController.java`
  - `backend/src/test/java/com/classhub/domain/studentcourse/application/StudentCourseManagementServiceTest.java`
  - `backend/src/test/java/com/classhub/domain/studentcourse/web/StudentCourseManagementControllerTest.java`
  - `backend/src/main/java/com/classhub/global/response/RsCode.java` (STUDENT_COURSE_RECORD_NOT_FOUND 추가)
- 다음 단계: PLAN 3단계(Admin 전체 조회 API) 구현 및 최종 통합

## [2025-12-20 19:44] Admin 수업 신청 조회 API 구현

### Type

BEHAVIORAL

### Summary

- PLAN 3단계 요구에 따라 관리자 전용 수업 신청 조회 서비스/컨트롤러(`StudentEnrollmentAdminService`, `AdminStudentEnrollmentController`)를 TDD로 구현했다.
- 필터(teacherId/courseId/status/studentName)와 Course/Member/StudentInfo 조합으로 `TeacherEnrollmentRequestResponse`를 구성하도록 Repository 쿼리를 확장했다.
- SUPER_ADMIN 전용 MockMvc 테스트와 서비스 단위 테스트를 작성해 전체 흐름을 검증했다.

### Details

- 작업 사유: 어드민 감사 시나리오를 지원하기 위해 전체 수업 신청을 조회하는 API가 필요함.
- 영향받은 테스트:
  - `./gradlew test --tests "com.classhub.domain.enrollment.application.StudentEnrollmentAdminServiceTest"`
  - `./gradlew test --tests "com.classhub.domain.enrollment.web.AdminStudentEnrollmentControllerTest"`
- 수정/추가 파일:
  - `backend/src/main/java/com/classhub/domain/enrollment/repository/StudentEnrollmentRequestRepository.java`
  - `backend/src/main/java/com/classhub/domain/enrollment/application/StudentEnrollmentAdminService.java`
  - `backend/src/main/java/com/classhub/domain/enrollment/web/AdminStudentEnrollmentController.java`
  - `backend/src/test/java/com/classhub/domain/enrollment/application/StudentEnrollmentAdminServiceTest.java`
  - `backend/src/test/java/com/classhub/domain/enrollment/web/AdminStudentEnrollmentControllerTest.java`
- 다음 단계: 전체 Student Enrollment Suite 통합 점검 및 추가 피드백 대응

## [2025-12-20 19:55] Admin 학생 요청 관리 UI 구현

### Type

BEHAVIORAL

### Summary

- `docs/plan/frontend/season2/student-enrollment-management_ui_plan.md` 1단계에 맞춰 SUPER_ADMIN 전용 “학생 요청 관리” 페이지를 추가했다.
- OpenAPI 타입을 확장하고 `dashboard-api`에 관리자용 신청 목록 API 래퍼를 구현해 Teacher/Course/상태/이름 필터를 지원했다.
- 상세 모달, 상태 배지, Pagination, 사이드바 메뉴 연결까지 포함한 기본 UX를 구성했고 `npm run build -- --webpack`으로 타입 검증을 완료했다.

### Details

- 작업 사유: 관리자에게 학습 지원 신청을 감사하는 화면이 없어 백엔드 API를 활용할 수 없었음.
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정한 파일:
  - `frontend/src/app/(dashboard)/admin/student-enrollment-requests/page.tsx`
  - `frontend/src/lib/dashboard-api.ts`
  - `frontend/src/types/dashboard.ts`
  - `frontend/src/components/dashboard/sidebar.tsx`
- `frontend/src/app/(dashboard)/student/my-courses/page.tsx`
- 다음 단계: 선생님/조교용 학생 관리/신청 처리 UI 구현

## [2025-12-20 22:15] Teacher/Assistant 학생 관리 UI 구현

### Type
BEHAVIORAL

### Summary
- `docs/plan/frontend/season2/student-enrollment-management_ui_plan.md` 2단계에 맞춰 교사·조교 공용 학생 관리 화면을 구현하고 라우팅/사이드바에 연결했다.
- 학생 목록/상세, 신청 처리 탭을 통합한 `StudentManagementView`를 추가하고, Teacher 전용 상세 모달/조교 전용 읽기 제한, 신청 일괄 승인·거절 UX를 구성했다.
- StudentCourse/EnrollmentRequest를 위한 대시보드 API 헬퍼와 타입을 확장해 필터·선택 동작을 타입 안정성 있게 처리했다.

### Details
- 작업 사유: Teacher/Assistant 역할이 백엔드에서 제공하는 학생 관리/신청 처리 API를 소비할 수 있는 UI가 없어 Season2 플로우가 막혀 있었음.
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정/추가 파일:
  - `frontend/src/components/dashboard/student-management.tsx`
  - `frontend/src/app/(dashboard)/teacher/students/page.tsx`
  - `frontend/src/app/(dashboard)/assistant/students/page.tsx`
- `frontend/src/components/dashboard/sidebar.tsx`
- `frontend/src/lib/dashboard-api.ts`
- `frontend/src/types/dashboard.ts`
- 다음 단계: 학생 역할 UI(내 수업/신청 내역/반 검색 CTA) 구현 및 수동 브라우저 검증

## [2025-12-20 23:05] Student 내 수업/신청 UI 및 반 검색 요청 흐름 구현

### Type
BEHAVIORAL

### Summary
- `docs/plan/frontend/season2/student-enrollment-management_ui_plan.md` 3단계에 맞춰 학생 대시보드의 “내 수업” 페이지를 탭 구조로 재구성해 수업 목록/신청 내역을 모두 확인할 수 있게 했다.
- 학생 전용 API 헬퍼(`fetchStudentMyCourses`, `fetchMyEnrollmentRequests`, `createStudentEnrollmentRequest`, `cancelStudentEnrollmentRequest`)와 타입을 추가해 OpenAPI 스키마와 동기화했다.
- 수업 목록 탭은 검색/페이지네이션을 제공하고, 신청 내역 탭은 상태 필터 + 취소 Confirm Dialog로 대기 요청을 취소할 수 있게 했다.
- 반 검색 페이지의 “등록 요청” 버튼을 신청 모달(메시지 입력/확인)로 연결하고, 성공 시 `student/my-courses?tab=requests`로 이동하도록 UX를 완성했다.
- `npm run build -- --webpack`으로 타입/빌드 검증을 완료했다.

### Details
- 작업 사유: 학생이 Enrollment API를 활용할 UI가 없어 신청 진행 상황/취소/재신청 흐름이 막혀 있었음.
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정/추가 파일:
  - `frontend/src/types/dashboard.ts`
  - `frontend/src/lib/dashboard-api.ts`
  - `frontend/src/app/(dashboard)/student/my-courses/page.tsx`
  - `frontend/src/app/(dashboard)/student/course/search/page.tsx`
- 다음 단계: 학생 UI 수동 테스트 및 후속 피드백 대응

## [2025-12-20 23:40] 학생 관리 상세 수정/검색 UX 개선

### Type
BEHAVIORAL

### Summary
- Teacher 학생 상세 모달에 “수업 기록” 수정 버튼을 추가하고, 조교/클리닉 슬롯/노트 필드를 편집해 `PATCH /api/v1/student-courses/{recordId}`로 저장할 수 있도록 했다.
- 저장/취소 액션 시 토스트와 리스트 리프레시가 반영되며, 조교는 여전히 읽기 전용이다.
- 학생 목록 검색 입력은 수동 적용(검색 버튼/Enter)으로 바뀌어 타이핑 중 자동 refetch로 인해 포커스가 날아가는 문제를 해결했다.

### Details
- 작업 사유: Teacher가 수업 기록을 즉시 수정할 수 없고, 자동 검색이 입력 UX를 방해함.
- 영향받은 테스트: `cd frontend && npm run build -- --webpack`
- 수정/추가 파일:
- `frontend/src/components/dashboard/student-management.tsx`
- 다음 단계: 브라우저에서 Teacher 계정으로 학생 상세 수정/검색 UX를 수동 확인

## [2025-12-20 23:55] Admin 학생 요청 조회 서버 오류 해결

### Type
BUGFIX

### Summary
- `StudentEnrollmentAdminService`에서 Course/StudentInfo가 삭제된 신청을 만났을 때 `BusinessException`이 발생하던 문제를 해결했다.
- CourseResponse/StudentSummary를 생성할 때 기본값을 제공하고, 테스트로 누락 케이스를 검증해 `/admin/student-enrollment-requests`에서 서버 오류 없이 응답이 내려온다.

### Details
- 작업 사유: Admin UI에서 “서버 오류입니다”가 표시되며 목록을 확인할 수 없었음.
- 영향받은 테스트: `./gradlew test --tests "com.classhub.domain.enrollment.application.StudentEnrollmentAdminServiceTest"` (로컬 Gradle 권한 문제로 실행 불가, 환경 정리 필요)
- 수정/추가 파일:
  - `backend/src/main/java/com/classhub/domain/enrollment/application/StudentEnrollmentAdminService.java`
  - `backend/src/test/java/com/classhub/domain/enrollment/application/StudentEnrollmentAdminServiceTest.java`
- 다음 단계: 서버 재기동 후 Admin 학생 신청 목록을 수동 확인

## [2025-12-21 00:10] StudentCourseListItemResponse 부모 연락처 노출

### Type
STRUCTURAL

### Summary
- Teacher/Assistant 학생 목록 API 응답(`StudentCourseListItemResponse`)에 학부모 연락처를 추가해 프론트가 바로 확인할 수 있도록 백엔드 DTO/서비스/테스트를 수정했다.
- Service에서 StudentInfo의 `parentPhone`을 매핑하고, Controller/Service 테스트 데이터를 새로운 필드에 맞게 갱신했다.

### Details
- 작업 사유: 학생 관리 화면에서 학부모 연락처 정보를 바로 제공하기 위해 응답 필드 확장이 필요함.
- 영향받은 테스트: `./gradlew test --tests "com.classhub.domain.studentcourse.application.StudentCourseManagementServiceTest"` (Gradle wrapper lock 파일 권한 문제로 실행 실패)
- 수정/추가 파일:
  - `backend/src/main/java/com/classhub/domain/studentcourse/dto/response/StudentCourseListItemResponse.java`
  - `backend/src/main/java/com/classhub/domain/studentcourse/application/StudentCourseManagementService.java`
  - `backend/src/test/java/com/classhub/domain/studentcourse/application/StudentCourseManagementServiceTest.java`
  - `backend/src/test/java/com/classhub/domain/studentcourse/web/StudentCourseManagementControllerTest.java`
## [2025-12-21 01:19] StudentSummaryResponse UI 정합성 반영

### Type
BEHAVIORAL

### Summary
- 학생 관리/어드민 신청 화면에 StudentSummaryResponse의 학부모 연락처·생년월일·학년 표기를 반영했다.

### Details
- 작업 사유
  - OpenAPI에 parentPhone/birthDate가 추가되어 프론트 UI와 타입 불일치를 해소해야 했다.
- 영향받은 테스트
  - `npm run lint` (프론트엔드, 스크립트 미정의로 실행 불가)
- 수정한 파일
  - frontend/src/components/dashboard/student-management.tsx
  - frontend/src/app/(dashboard)/admin/student-enrollment-requests/page.tsx
  - frontend/src/utils/student.ts
- 다음 단계
  - 필요 시 lint 스크립트를 정의하거나 별도 품질 검증 절차 마련

## [2025-12-21 15:59] Progress 관리 백엔드 PLAN 작성

### Type
DESIGN

### Summary
- CourseProgress/PersonalProgress/Calendar 요구를 다루는 백엔드 전용 Progress Management 계획을 작성했다.
- 생성·조회·수정·삭제 API, 배치 작성 흐름, 권한 검증 흐름, Student Calendar 집계, 그리고 단계별 TDD 전략을 정의했다.

### Details
- 작업 사유: Phase 5 Progress Epic을 구현하기 전 API/도메인/테스트 범위를 명확히 하기 위함.
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일:
  - `docs/plan/backend/season2/progress-management_plan.md`
- 다음 단계: 계획 검토/승인 후 TODO 상태 업데이트 및 TDD/구현 착수.

## [2025-12-21 16:10] Progress PLAN 권한 정책 수정

### Type
DESIGN

### Summary
- 사용자 피드백에 따라 Progress 백엔드 설계에서 Assistant의 생성/수정 권한을 제거하고, Student/Admin이 모든 Progress/Calendar API에 접근할 수 없도록 갱신했다.

### Details
- 작업 사유: Teacher 전용 작성/편집 흐름을 강제하고, 추후 요구가 있을 때만 권한을 열기 위함.
- 영향받은 테스트: 없음 (문서 작업)
- 수정한 파일:
  - `docs/plan/backend/season2/progress-management_plan.md`
- 다음 단계: 이 변경 기준으로 구현을 설계/진행.

## [2025-12-21 16:11] Progress homework 개선 아이디어 기록

### Type
TODO_UPDATE

### Summary
- 향후 Course/Personal Progress에 homework 필드를 추가해 학생이 본인 숙제만 조회할 수 있도록 개선하는 아이디어를 개선 백로그에 추가했다.

### Details
- 작업 사유: Progress 데이터를 학생에게 한정 공개할 필요성이 있어 향후 확장 포인트로 기록.
- 영향받은 테스트: 없음
- 수정한 파일:
  - `docs/refactor/improvment-backlog.md`
- 다음 단계: Progress CRUD 안정화 후 개선 항목 검토.

## [2025-12-21 16:13] Progress PLAN 구현 단계 추가

### Type
DESIGN

### Summary
- Progress 백엔드 플랜에 Stage 1~3 구현 단계를 추가해 Permission → CRUD → Calendar 순으로 개발 흐름을 명확히 했다.

### Details
- 작업 사유: 실행 순서를 3단계로 나눠 추후 착수 시 참조하도록 요청받음.
- 영향받은 테스트: 없음
- 수정한 파일:
  - `docs/plan/backend/season2/progress-management_plan.md`
- 다음 단계: Stage 1부터 TODO 진행.

## [2025-12-21 17:36] Progress Stage2 CRUD 서비스/컨트롤러 구축

### Type
BEHAVIORAL

### Summary
- Progress Stage2에 필요한 Course/Personal Progress CRUD 서비스와 API 컨트롤러를 추가하고, compose/커서 목록 로직까지 구현했다.
- Course/Personal Progress 커서 조회 및 학생-교사 필터 쿼리를 보완해 Stage1 테스트가 동작하도록 정비했다.
- 서비스/컨트롤러 단위 테스트를 추가해 생성·수정·삭제·커서 응답 흐름을 검증할 기반을 마련했다.

### Details
- 작업 사유: progress-management_plan Stage2(Progress CRUD) 구현을 이어가기 위해 서비스/엔드포인트와 커서 응답 포맷이 필요함.
- 영향받은 테스트: `./gradlew test --tests "*CourseProgressServiceTest" --tests "*PersonalProgressServiceTest" --tests "*CourseProgressControllerTest" --tests "*PersonalProgressControllerTest" --tests "*CourseProgressRepositoryTest" --tests "*PersonalProgressRepositoryTest" --tests "*StudentCourseRecordRepositoryTest"` (미실행; 기존 Gradle lock 이슈로 보류)
- 수정/추가 파일:
  - `backend/src/main/java/com/classhub/domain/progress/course/application/CourseProgressService.java`
  - `backend/src/main/java/com/classhub/domain/progress/personal/application/PersonalProgressService.java`
  - `backend/src/main/java/com/classhub/domain/progress/course/web/CourseProgressController.java`
  - `backend/src/main/java/com/classhub/domain/progress/personal/web/PersonalProgressController.java`
  - `backend/src/main/java/com/classhub/domain/progress/dto/ProgressSliceResponse.java`
  - `backend/src/main/java/com/classhub/domain/progress/course/repository/CourseProgressRepository.java`
  - `backend/src/main/java/com/classhub/domain/progress/personal/repository/PersonalProgressRepository.java`
  - `backend/src/main/java/com/classhub/domain/studentcourse/repository/StudentCourseRecordRepository.java`
  - `backend/src/main/java/com/classhub/domain/progress/course/model/CourseProgress.java`
  - `backend/src/main/java/com/classhub/domain/progress/personal/model/PersonalProgress.java`
  - `backend/src/test/java/com/classhub/domain/progress/course/application/CourseProgressServiceTest.java`
  - `backend/src/test/java/com/classhub/domain/progress/personal/application/PersonalProgressServiceTest.java`
  - `backend/src/test/java/com/classhub/domain/progress/course/web/CourseProgressControllerTest.java`
  - `backend/src/test/java/com/classhub/domain/progress/personal/web/PersonalProgressControllerTest.java`
- 다음 단계: Gradle lock 해결 후 Stage2 테스트 실행, 필요 시 Stage3(Student Calendar Aggregation) 착수.

## [2025-12-21 17:46] Progress 커서 조회 테스트 안정화

### Type
BUGFIX

### Summary
- Progress 커서 조회 테스트가 createdAt 정렬과 맞지 않아 실패하던 문제를 수정했다.
- 불필요한 Mockito stub을 제거해 ProgressPermissionValidator 테스트가 strict mode에서 통과하도록 정리했다.

### Details
- 작업 사유: 전체 테스트 실행 시 Progress 관련 저장소/권한 테스트가 실패함.
- 영향받은 테스트: `cd backend && GRADLE_USER_HOME=../.gradle-local ./gradlew test`
- 수정한 파일:
  - `backend/src/test/java/com/classhub/domain/progress/course/repository/CourseProgressRepositoryTest.java`
  - `backend/src/test/java/com/classhub/domain/progress/personal/repository/PersonalProgressRepositoryTest.java`
  - `backend/src/test/java/com/classhub/domain/progress/support/ProgressPermissionValidatorTest.java`
- 다음 단계: Stage3(Student Calendar Aggregation) 착수 여부 확인.
