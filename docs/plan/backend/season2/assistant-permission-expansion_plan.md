# Feature: Assistant Permission Expansion (Course Students + Progress)

## 1. Problem Definition
- 조교가 `assistant/clinics/attendance`에서 학생 추가를 하려면 `/api/v1/courses/{courseId}/students` 조회가 필요한데, 현재 TEACHER 전용이라 접근이 막힌다.
- Progress 도메인의 생성/수정/삭제 API는 TEACHER 전용으로 제한되어 있어 조교가 배정된 반/학생의 진도를 기록할 수 없다.
- 권한을 확장하되, 조교는 **배정 관계가 유효한 경우(TeacherAssistantAssignment.deletedAt is null)** 에만 접근/작성할 수 있어야 한다.

## 2. Requirements
### Functional
- **Course Students 조회 권한 확대**
  - `GET /api/v1/courses/{courseId}/students`에 ASSISTANT 권한을 추가한다.
  - 서비스에서 조교가 해당 반의 담당 교사와 **활성 배정 관계**인지 검증한다.
- **Progress 권한 확대 (Course/Personal)**
  - CourseProgress/PersonalProgress의 생성/수정/삭제 API에 ASSISTANT 권한을 추가한다.
  - 서비스 로직에서 `TeacherAssistantAssignment.deletedAt is null` 기반 배정 관계를 검증한다.
  - 조회 API는 기존처럼 TEACHER/ASSISTANT 허용을 유지한다.
- 권한이 없는 사용자는 기존과 동일하게 `RsCode.FORBIDDEN`으로 차단한다.

### Non-functional
- API 스펙/응답 스키마는 변경하지 않는다(권한만 변경).
- 권한 검증은 Controller + Service 양쪽에서 중복 방어한다.
- 모든 변경은 기존 `RsData`/`RsCode` 규칙을 따른다.

## 3. API Design (Draft)
### Course Students
- `GET /api/v1/courses/{courseId}/students`
  - 기존: TEACHER
  - 변경: TEACHER, ASSISTANT
  - Service: 조교 접근 시 `TeacherAssistantAssignment.deletedAt is null` 확인

### CourseProgress
- `POST /api/v1/courses/{courseId}/course-progress`
- `POST /api/v1/courses/{courseId}/course-progress/compose`
- `PATCH /api/v1/course-progress/{progressId}`
- `DELETE /api/v1/course-progress/{progressId}`
  - 기존: TEACHER
  - 변경: TEACHER, ASSISTANT
  - Service: 조교 접근 시 배정 관계 확인

### PersonalProgress
- `POST /api/v1/student-courses/{recordId}/personal-progress`
- `PATCH /api/v1/personal-progress/{progressId}`
- `DELETE /api/v1/personal-progress/{progressId}`
  - 기존: TEACHER
  - 변경: TEACHER, ASSISTANT
  - Service: 조교 접근 시 배정 관계 확인

## 4. Domain Model (Draft)
- `CourseAssignmentService.getCourseStudents`
  - `ensureTeacherPermission` → `ensurePermission`으로 변경하여 조교 배정 관계 검증 포함.
- `ProgressPermissionValidator`
  - `ensureCourseAccess`/`ensureRecordAccess`에서 ASSISTANT의 WRITE 접근을 허용하되,
    `TeacherAssistantAssignment.deletedAt is null` 조건을 통과해야 한다.
- Progress 서비스는 기존처럼 `ProgressPermissionValidator`를 호출하되, WRITE 모드에서도 조교가 통과하도록 규칙을 업데이트한다.

## 5. TDD Plan
1. **CourseAssignmentService**
   - Red: 조교가 배정된 교사의 반에 대해 `getCourseStudents` 호출 시 성공.
   - Red: 배정이 없는 조교는 `FORBIDDEN`.
   - Green: `ensurePermission` 적용으로 테스트 통과.
2. **CourseAssignmentController**
   - MockMvc: ASSISTANT 인증으로 `/courses/{courseId}/students` 200 응답 확인.
3. **ProgressPermissionValidator**
   - Red: 조교가 WRITE 모드에서 Course/Record 접근 가능(배정 관계 존재).
   - Red: 배정 관계 없음/삭제됨이면 `FORBIDDEN`.
4. **CourseProgressService / PersonalProgressService**
   - Red: ASSISTANT가 create/update/delete 수행 가능(배정 관계 통과 시).
   - 기존 TEACHER 테스트는 유지.
5. **CourseProgressController / PersonalProgressController**
   - MockMvc: ASSISTANT 권한으로 create/update/delete 200/201 응답 확인.

## 6. Implementation Notes
- 권한 변경은 Controller의 `@PreAuthorize`와 Service의 Permission 검증 기억 보완을 함께 수행한다.
- 스펙 변경이 없으므로 OpenAPI 스키마 수정은 불필요하나, 문서에 권한 변경을 반영한다.
