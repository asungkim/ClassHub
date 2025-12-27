# Feature: Assistant Permission Expansion (Course Students + Progress)

## 1. Problem Definition
- 조교가 `assistant/clinics/attendance`에서 학생 추가를 하려면 `/api/v1/courses/{courseId}/students` 조회가 필요한데, 현재 TEACHER 전용이라 접근이 막힌다.
- Progress 도메인의 생성/수정/삭제 API는 TEACHER 전용으로 제한되어 있어 조교가 배정된 반/학생의 진도를 기록할 수 없다.
- 권한을 확장하되, 조교는 **배정 관계가 유효한 경우(TeacherAssistantAssignment.deletedAt is null)** 에만 접근/작성할 수 있어야 한다.
- 조교도 진도/클리닉 기록을 수정할 수 있게 되므로 **수정/삭제 권한 범위(작성자 제한)** 를 명확히 해야 한다.

## 2. Requirements
### Functional
- **Course Students 조회 권한 확대**
  - `GET /api/v1/courses/{courseId}/students`에 ASSISTANT 권한을 추가한다.
  - 서비스에서 조교가 해당 반의 담당 교사와 **활성 배정 관계**인지 검증한다.
- **Progress 권한 확대 (Course/Personal)**
  - CourseProgress/PersonalProgress의 생성/수정/삭제 API에 ASSISTANT 권한을 추가한다.
  - 서비스 로직에서 `TeacherAssistantAssignment.deletedAt is null` 기반 배정 관계를 검증한다.
  - 조회 API는 기존처럼 TEACHER/ASSISTANT 허용을 유지한다.
- **작성자 기반 수정/삭제 정책**
  - TEACHER는 배정 범위 내 모든 기록을 수정/삭제할 수 있다.
  - ASSISTANT는 본인이 작성한 기록만 수정/삭제할 수 있다.
  - 동일 정책을 ClinicRecord에도 적용한다.
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
  - Edit/Delete: 조교는 writerId가 본인인 경우만 허용

### PersonalProgress
- `POST /api/v1/student-courses/{recordId}/personal-progress`
- `PATCH /api/v1/personal-progress/{progressId}`
- `DELETE /api/v1/personal-progress/{progressId}`
  - 기존: TEACHER
  - 변경: TEACHER, ASSISTANT
  - Service: 조교 접근 시 배정 관계 확인
  - Edit/Delete: 조교는 writerId가 본인인 경우만 허용

### ClinicRecord
- `PATCH /api/v1/clinic-records/{recordId}`
- `DELETE /api/v1/clinic-records/{recordId}`
  - 기존: TEACHER, ASSISTANT (유지)
  - Service: 조교 접근 시 배정 관계 확인 + writerId 본인 여부 확인

## 4. Domain Model (Draft)
- `CourseAssignmentService.getCourseStudents`
  - `ensureTeacherPermission` → `ensurePermission`으로 변경하여 조교 배정 관계 검증 포함.
- `ProgressPermissionValidator`
  - `ensureCourseAccess`/`ensureRecordAccess`에서 ASSISTANT의 WRITE 접근을 허용하되,
    `TeacherAssistantAssignment.deletedAt is null` 조건을 통과해야 한다.
- Progress 서비스는 기존처럼 `ProgressPermissionValidator`를 호출하되, WRITE 모드에서도 조교가 통과하도록 규칙을 업데이트한다.
- ClinicRecord 서비스는 `ClinicPermissionValidator.ensureStaffAccess`로 교사/조교 접근을 검증하고,
  조교인 경우 `writerId == principal.id()` 조건을 추가로 검증한다.

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
  - Red: ASSISTANT가 본인 작성이 아닌 기록 수정/삭제 시 `FORBIDDEN`.
5. **ClinicRecordService**
  - Red: ASSISTANT는 본인 기록만 수정/삭제 가능.
  - Red: TEACHER는 배정 범위 내 모든 기록 수정/삭제 가능.
6. **CourseProgressController / PersonalProgressController**
  - MockMvc: ASSISTANT 권한으로 create/update/delete 200/201 응답 확인.

## 6. Implementation Notes
- 권한 변경은 Controller의 `@PreAuthorize`와 Service의 Permission 검증 기억 보완을 함께 수행한다.
- 스펙 변경이 없으므로 OpenAPI 스키마 수정은 불필요하나, 문서에 권한 변경을 반영한다.

## 7. Implementation Stages
1. **Stage 1 - Course Students 권한 확대**
   - `/courses/{courseId}/students`에 ASSISTANT 허용
   - Service에서 조교 배정 관계 검증 추가
2. **Stage 2 - Progress 권한 확대 + 작성자 정책**
   - Progress CRUD에 ASSISTANT 허용
   - 조교 작성자 제한(본인 작성만 수정/삭제) 적용
3. **Stage 3 - ClinicRecord 작성자 정책**
   - ClinicRecord 수정/삭제에 작성자 제한 적용
   - 조교 배정 관계 검증 보강
