# Feature: Clinic Domain (Slot/Session/Attendance/Record)

## 1. Problem Definition
- 기존 Clinic 도메인은 Course 기반 슬롯과 단일 출석 API 중심으로 설계되어 있어 teacher/branch 기준 슬롯 공유, 자동 세션 생성, 학생 자율 이동 등 v1.4 스펙을 충족하지 못한다.
- Slot → Session → Attendance → Record로 이어지는 일관된 라이프사이클과 권한/검증 규칙이 분산되어 있어, 배치 생성 및 학생/조교/선생님별 역할 구분을 보장하기 어렵다.
- StudentCourseRecord.defaultClinicSlotId 검증(teacher/branch 일치, 중복/겹침/정원 초과)이 명확히 구현되어 있지 않아 자동 배정 품질이 떨어진다.
- 세션 취소/출석 확정/학생 이동 제한 등 운영 규칙이 API 레벨에서 강제되지 않아 실데이터 오염 가능성이 있다.

## 2. Requirements
### Functional
#### 2.1 ClinicSlot
- Teacher가 `branchId`, `dayOfWeek`, `startTime`, `endTime`, `defaultCapacity`로 Slot을 생성한다.
- Slot은 teacher/branch 기준 시간표이며 동일 teacher/branch에 연결된 Course 학생이 동일 목록을 조회한다.
- Assistant는 `branchId + teacherId`로 특정 teacher/branch Slot 목록을 조회할 수 있다.
- Student는 본인 `courseId` 기준으로 Slot 목록을 조회한다. (해당 Course의 teacher/branch에 속한 Slot만 반환)
- Slot 수정은 Teacher만 가능하며, 시간/정원 변경 시 다음을 검증한다.
  - `startTime < endTime` 유지, `defaultCapacity >= 1`.
  - 요일/시간 변경 시 해당 Slot을 default로 가진 StudentCourseRecord는 모두 `defaultClinicSlotId = null`로 해제한다.
  - 정원 변경 시 현재 할당된 StudentCourseRecord 수보다 작아지면 409를 반환한다.
  - 이미 생성된 ClinicAttendance는 유지되며, 다음 주 자동 배정부터 변경된 Slot 규칙이 반영된다.
- Slot 삭제는 Soft Delete(`deletedAt`)로 처리하며 삭제된 Slot은 주간 Session 생성 대상에서 제외한다.

#### 2.2 StudentCourseRecord.defaultClinicSlotId
- Student가 본인 Course에 대해 `defaultClinicSlotId`를 지정/변경한다.
- Teacher가 `PATCH /student-courses/{recordId}/notes`로 `defaultClinicSlotId`를 변경/해제할 수 있다.
- 지정/변경 시 검증 규칙
  - Slot의 `teacherMemberId`/`branchId`가 Record의 Course와 동일해야 한다.
  - 동일 학생이 다른 Course에서 동일 Slot을 중복 선택할 수 없다.
  - 동일 학생의 다른 defaultSlot들과 요일/시간이 겹치면 안 된다.
  - Slot의 `defaultCapacity`를 초과하면 안 된다.
  - Slot이 Soft Delete 상태면 지정 불가.
- 동작 규칙
  - 신규 지정(Null → Slot) 시: 학생 최초 지정 포함, 이번 주 남은 Session(시작 전)까지 Attendance를 즉시 생성한다.
  - 변경(Slot A → Slot B) 시: 다음 주부터 적용하며, 이번 주 이동은 학생/선생님이 Attendance 이동 API로 처리한다.
  - Null로 해제 시: 다음 주 자동 Attendance 생성 중지 (이미 생성된 Attendance는 삭제하지 않음).

#### 2.3 ClinicSession
- Teacher는 `POST /clinic-slots/{slotId}/sessions`로 정규 Session을 수동 생성할 수 있다.
- Teacher/Assistant는 `POST /clinic-sessions/emergency`로 EMERGENCY Session을 생성한다.
  - slotId=null, creatorMemberId 기록, date/startTime/endTime/capacity를 직접 입력.
  - Teacher는 branchId만 전달, Assistant는 branchId+teacherId 전달.
- Session 취소는 `PATCH /clinic-sessions/{sessionId}/cancel`로 처리하며 `isCanceled=true`만 변경한다.
  - 시작 이후 취소 불가.
  - 취소 후 Attendance는 유지한다.
- Session 조회는 `dateRange + branchId (+ teacherId)` 기반으로 제공하며 취소 여부를 포함한다.
  - Student는 해당 teacher/branch에 연결된 StudentCourseRecord가 있을 때만 조회 가능하다.

#### 2.4 ClinicAttendance
- 자동 생성: 주간 Session 생성 후 `defaultClinicSlotId`가 있는 StudentCourseRecord마다 Attendance를 만든다.
  - 생성 순서: Session 생성 → Slot 매칭 → Attendance 생성.
  - 중복(세션+레코드)은 생성하지 않는다.
- Teacher/Assistant는 `POST /clinic-sessions/{sessionId}/attendances`로 수동 참석자를 추가한다.
  - recordId가 세션의 teacher/branch와 일치해야 하며, Record가 활성 상태여야 한다.
  - Session capacity 초과 또는 시간 겹침 발생 시 409.
- Teacher/Assistant는 `DELETE /clinic-attendances/{attendanceId}`로 수동 삭제할 수 있다.
- Student는 `POST /students/me/clinic-attendances`로 추가 참석을 즉시 신청한다.
  - `clinicSessionId`, `studentCourseRecordId`를 전달한다.
  - 세션이 취소/과거일 경우 409, capacity 초과 시 409.
  - 동일 학생의 다른 Attendance와 시간이 겹치면 409.
- Student는 `PATCH /students/me/clinic-attendances`로 동일 주차 이동을 수행한다.
  - `fromSessionId`, `toSessionId` 모두 본인 Attendance로 검증.
  - 동일 Course, 동일 주차(월~일), 시작 30분 전까지만 허용.
  - 이동 성공 시 기존 Attendance 삭제 → 신규 Attendance 생성.
- 출석명단 확정: 세션 시작 10분 전 이후에는 Attendance 변경 API(추가/삭제/이동)가 모두 409를 반환한다.
- Student 조회 API는 본인 Attendance만 반환하며, Session 정보(날짜/시간/취소 여부/branchId/teacherId)만 포함한다.

#### 2.5 ClinicRecord
- `clinicAttendanceId` 기준 1:1 기록을 생성/조회/수정/삭제한다.
- Teacher/Assistant만 작성/수정/삭제 가능하며, **학생은 조회 불가**.
- Assistant는 TeacherAssistantAssignment가 ACTIVE인 경우에만 기록 작성 가능.
- ClinicRecord가 존재하면 해당 Attendance는 COMPLETED로 간주된다. (상태는 Record 존재 여부로 계산)
- ClinicRecord 삭제 시 Attendance는 유지되며, 캘린더 집계 시 record가 제거된 상태로 반영한다.
- 출석부 조회(`GET /clinic-attendances?clinicSessionId=...`) 결과에 `recordId`를 포함하여 작성/수정 UI 상태를 판단한다.
- 기록 작성 흐름
  - 출석부 목록에서 `recordId == null`이면 “작성” 버튼 노출
  - `recordId != null`이면 “수정/삭제” 버튼 노출
  - 작성 완료 시 출석부 재조회 시 `recordId`가 채워짐

#### 2.6 권한/검증
- Teacher 권한
  - 본인 teacherMemberId와 branchId에 속한 데이터만 접근 가능.
  - Course.teacherMemberId/branchId와 일치해야 StudentCourseRecord/Attendance/Record 처리 가능.
- Assistant 권한
  - TeacherAssistantAssignment가 ACTIVE이며 teacher/branch가 일치해야 한다.
  - Slot/Session/Attendance/Record에 대해 조회 및 제한된 쓰기 권한을 갖는다 (spec 4.8 기준).
- Student 권한
  - StudentCourseRecord가 ACTIVE이고 본인 소유일 때만 조회/추가/이동 가능.
  - Session 조회는 해당 teacher/branch로 연결된 Record가 있을 때만 허용.

#### 2.7 Student Calendar 연동
- ClinicAttendance/ClinicRecord는 StudentCalendar 집계 대상이므로, 월간 범위 조회를 위한 Repository Query를 제공한다.
- `ClinicAttendanceEventProjection`은 Session/Attendance/Record를 조인하여 날짜/시간/취소 여부/Record 여부를 반환한다.

#### 2.8 Batch & Scheduler (자동 생성)
- **배치 실행 시간**: 매주 일요일 00:00 (월~일 주차 대상, Asia/Seoul 기준)
- **대상 슬롯**: `deletedAt IS NULL`인 활성 ClinicSlot
- **세션 생성 로직**
  - Slot의 `dayOfWeek`에 해당하는 날짜를 계산해 REGULAR Session 생성
  - SessionType=REGULAR, slotId 설정, startTime/endTime/capacity는 Slot 값 복사
  - `(slotId, date)` 중복 존재 시 스킵 (idempotent)
- **Attendance 자동 생성 로직**
  - Session 생성 후 `defaultClinicSlotId = slotId`인 활성 StudentCourseRecord 대상으로 Attendance 생성
  - `(clinicSessionId, studentCourseRecordId)` 중복은 스킵
  - 정원 초과는 원칙적으로 defaultClinicSlotId 설정 시 차단되며, 배치에서 초과 감지 시 **스킵 + 로그 기록**
- **주중 Slot 생성 처리**
  - Slot이 주중에 생성되면, 해당 주에서 **시간이 지나지 않은 요일만** 즉시 Session 생성
  - 생성 후 Attendance 자동 생성 규칙 동일 적용
- **defaultClinicSlotId 신규 지정 처리**
  - 신규 지정(Null → Slot) 시 이번 주 남은 Session(시작 전)에 Attendance를 즉시 생성
  - 세션 시작 10분 이내라도 자동 생성은 허용 (변경 API만 제한)
- **Slot 요일/시간 변경 처리**
  - 변경으로 인해 **이번 주 Session이 이미 존재**하면 Teacher가 **유지/재생성**을 선택
  - 이번 주 Session이 없는 경우에는 추가 선택 없이 변경만 적용
- **Slot 삭제(soft delete) 처리**
  - 이번 주 Session이 존재하면 Teacher가 **유지/재생성**을 선택
  - 이후 배치에서는 삭제된 Slot을 생성 대상에서 제외
- **로그/모니터링**
  - 배치 실행 시 생성/스킵/정원 초과 스킵을 구조화 로그로 기록 (slotId, branchId, teacherId, sessionDate)

### Non-functional
- 모든 쓰기 요청은 DTO `@Valid` 검증, 존재 여부 검증, 권한 검증을 순차로 수행한다.
- `RsData`/`RsCode` 표준 응답 포맷을 유지한다.
- Slot 삭제는 Soft Delete만 허용하고, Attendance/Record는 물리 삭제로 처리한다.
- Session/Attendance 생성은 트랜잭션으로 묶어 부분 생성이 발생하지 않도록 한다.
- 배치/수동 생성은 idempotent하게 설계한다. (중복 생성 시 조회 후 스킵)
- 시간 비교는 서버 `Asia/Seoul` 기준으로 통일한다.
- 조회 API는 `deletedAt IS NULL` 조건으로 활성 데이터만 반환한다.

## 3. API Design (Draft)
### 3.1 ClinicSlot
- `POST /api/v1/clinic-slots`
  - Body: `{ "branchId": "uuid", "dayOfWeek": "MON", "startTime": "18:00", "endTime": "19:00", "defaultCapacity": 8 }`
  - Response: `RsData<ClinicSlotResponse>`
  - Errors: `CLINIC_SLOT_TIME_INVALID`, `BRANCH_NOT_FOUND`, `TEACHER_BRANCH_MISMATCH`
- `GET /api/v1/clinic-slots?branchId=uuid`
  - Teacher: 본인 branch 기준 목록
- `GET /api/v1/clinic-slots?branchId=uuid&teacherId=uuid`
  - Assistant: teacher/branch 기준 목록
- `GET /api/v1/clinic-slots?courseId=uuid`
  - Student: 본인 Course 기준 목록
- `PATCH /api/v1/clinic-slots/{slotId}`
  - Body: `{ "dayOfWeek": "TUE", "startTime": "19:00", "endTime": "20:00", "defaultCapacity": 10 }`
  - Errors: `CLINIC_SLOT_CAPACITY_CONFLICT`, `CLINIC_SLOT_TIME_OVERLAP`
- `DELETE /api/v1/clinic-slots/{slotId}`
  - Soft delete 응답만 반환

### 3.2 StudentCourseRecord defaultClinicSlotId
- `PATCH /api/v1/students/me/courses/{courseId}/clinic-slot`
  - Body: `{ "defaultClinicSlotId": "uuid" }`
  - Errors: `CLINIC_SLOT_NOT_FOUND`, `CLINIC_SLOT_CAPACITY_EXCEEDED`, `CLINIC_SLOT_TIME_OVERLAP`, `CLINIC_SLOT_DUPLICATED`
- `PATCH /api/v1/student-courses/{recordId}/notes`
  - Body: `{ "defaultClinicSlotId": "uuid", "teacherNotes": "...", "assistantMemberId": "uuid" }`
  - Errors: `CLINIC_SLOT_NOT_FOUND`, `CLINIC_SLOT_CAPACITY_EXCEEDED`, `CLINIC_SLOT_TIME_OVERLAP`, `CLINIC_SLOT_DUPLICATED`
  - 성공 시 다음 주 적용 규칙과 이번 주 즉시 반영(Null → Slot)의 처리 결과를 응답 메시지에 포함

### 3.3 ClinicSession
- `GET /api/v1/clinic-sessions?dateRange=2025-03-01,2025-03-07&branchId=uuid`
  - Response includes `sessionType`, `startTime`, `endTime`, `capacity`, `isCanceled`
- `GET /api/v1/clinic-sessions?dateRange=...&branchId=uuid&teacherId=uuid`
  - Assistant/Student 공용
- `POST /api/v1/clinic-slots/{slotId}/sessions`
  - Body: `{ "date": "2025-03-04" }`
  - Response: `RsData<ClinicSessionResponse>`
  - Errors: `CLINIC_SESSION_ALREADY_EXISTS`, `CLINIC_SLOT_NOT_FOUND`
- `POST /api/v1/clinic-sessions/emergency`
  - Body: `{ "branchId": "uuid", "teacherId": "uuid?", "date": "2025-03-04", "startTime": "18:30", "endTime": "19:30", "capacity": 6 }`
  - Errors: `CLINIC_SESSION_TIME_INVALID`, `TEACHER_BRANCH_MISMATCH`
- `PATCH /api/v1/clinic-sessions/{sessionId}/cancel`
  - Response: `RsData<Void>` (취소 상태 반환)

### 3.4 ClinicAttendance (Teacher/Assistant)
- `GET /api/v1/clinic-attendances?clinicSessionId=uuid`
  - Response: Attendance 리스트 + Student 요약 정보
- `POST /api/v1/clinic-sessions/{sessionId}/attendances`
  - Body: `{ "studentCourseRecordId": "uuid" }`
  - Errors: `CLINIC_SESSION_FULL`, `CLINIC_ATTENDANCE_TIME_OVERLAP`, `CLINIC_ATTENDANCE_LOCKED`
- `DELETE /api/v1/clinic-attendances/{attendanceId}`
  - Errors: `CLINIC_ATTENDANCE_LOCKED`

### 3.5 ClinicAttendance (Student)
- `POST /api/v1/students/me/clinic-attendances`
  - Body: `{ "clinicSessionId": "uuid", "studentCourseRecordId": "uuid" }`
  - Errors: `CLINIC_SESSION_FULL`, `CLINIC_ATTENDANCE_TIME_OVERLAP`, `CLINIC_SESSION_CANCELED`, `CLINIC_ATTENDANCE_LOCKED`
- `GET /api/v1/students/me/clinic-attendances?dateRange=2025-03-01,2025-03-31`
  - Response: `{ "items": [ { "attendanceId": "uuid", "clinicSessionId": "uuid", "date": "2025-03-04", "startTime": "18:00", "endTime": "19:00", "sessionType": "REGULAR", "isCanceled": false, "branchId": "uuid", "teacherId": "uuid" } ] }`
- `PATCH /api/v1/students/me/clinic-attendances`
  - Body: `{ "fromSessionId": "uuid", "toSessionId": "uuid" }`
  - Errors: `CLINIC_ATTENDANCE_MOVE_FORBIDDEN`, `CLINIC_ATTENDANCE_LOCKED`

### 3.6 ClinicRecord
- `POST /api/v1/clinic-records`
  - Body: `{ "clinicAttendanceId": "uuid", "title": "...", "content": "...", "homeworkProgress": "..." }`
  - Errors: `CLINIC_RECORD_ALREADY_EXISTS`, `CLINIC_ATTENDANCE_NOT_FOUND`
- `GET /api/v1/clinic-records?clinicAttendanceId=uuid`
  - Teacher/Assistant 전용 (학생 조회 불가)
- `PATCH /api/v1/clinic-records/{recordId}`
  - Body: `{ "title": "...", "content": "...", "homeworkProgress": "..." }`
- `DELETE /api/v1/clinic-records/{recordId}`

## 4. Domain Model (Draft)
### 4.1 Entities & Relations
- `ClinicSlot`
  - `teacherMemberId`, `creatorMemberId`, `branchId`, `dayOfWeek`, `startTime`, `endTime`, `defaultCapacity`, `deletedAt`
  - Relation: Slot 1:N Session (REGULAR)
- `ClinicSession`
  - `slotId(nullable)`, `sessionType`, `creatorMemberId(nullable)`, `date`, `startTime`, `endTime`, `capacity`, `isCanceled`
  - Constraint: REGULAR은 slotId 필수/creatorMemberId null, EMERGENCY는 slotId null/creatorMemberId 필수
  - Unique: `(slotId, date)` for REGULAR
- `ClinicAttendance`
  - `clinicSessionId`, `studentCourseRecordId`
  - Unique: `(clinicSessionId, studentCourseRecordId)`
- `ClinicRecord`
  - `clinicAttendanceId (unique)`, `writerId`, `title`, `content`, `homeworkProgress`

### 4.2 Domain Services
- `ClinicSlotService`
  - create/update/delete, slot list by role
  - update 시 요일/시간 변경이 있으면 해당 Slot의 defaultClinicSlotId를 모두 해제
  - update 시 정원 변경은 기존 배정 인원보다 작아지지 않도록 검증
  - slot 삭제 시 `delete()` 호출 (soft delete)
- `ClinicDefaultSlotService`
  - StudentCourseRecord.defaultClinicSlotId 지정/변경 전담
  - 중복/겹침/정원 검증 + null 처리
  - 신규 지정 시 남은 Session Attendance 즉시 생성
- `ClinicSessionService`
- `ClinicSessionService`
  - slot 기반 수동 생성, emergency 생성, 취소 처리, 역할별 조회
- `ClinicBatchService`
  - 주간 Session/Attendance 자동 생성
  - 주중 Slot 생성 시 잔여 주차 생성
  - 정원 초과/중복 스킵 및 로그 기록
- `ClinicAttendanceService`
  - auto 생성, teacher/assistant 추가/삭제, student 추가/이동/조회
  - 시간 겹침 검사, capacity 검사, 10분 lock/30분 이동 제한
- `ClinicRecordService`
  - record CRUD, 존재 여부로 Attendance 완료 상태 계산
  - Assistant 권한은 TeacherAssistantAssignment ACTIVE 여부로 검증
- `ClinicPermissionValidator`
  - Teacher/Assistant/Student 접근 규칙을 공통으로 검증

### 4.3 Repository Queries
- `ClinicSlotRepository`
  - `findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull`
  - `findByBranchIdAndTeacherMemberIdAndDeletedAtIsNull`
- `ClinicSessionRepository`
  - `findBySlotIdAndDate` (중복 체크)
  - `findByDateBetweenAndBranchId` (teacher 기준)
  - `findByDateBetweenAndBranchIdAndTeacherId` (assistant/student)
- `ClinicAttendanceRepository`
  - `existsByClinicSessionIdAndStudentCourseRecordId`
  - `countByClinicSessionId`
  - `findByStudentCourseRecordIdAndDateRange` (겹침 검사)
  - `findEventsByRecordIdsAndDateRange` (StudentCalendar)
- `ClinicRecordRepository`
  - `findByClinicAttendanceId` (1:1)

### 4.4 Validation Rules
- Time overlap: 동일 학생의 Attendance 구간이 겹치면 생성/이동 불가.
- Capacity: Session capacity 초과 시 Attendance 생성 불가.
- Slot capacity: defaultClinicSlotId 지정 인원이 `defaultCapacity` 초과 시 지정 불가.
- Lock time: `sessionStart - 10min` 이후 변경 금지.
- Move time: `sessionStart - 30min` 이전까지만 이동 허용.

## 5. TDD Plan
1. **Repository Tests (DataJpaTest)**
   - ClinicSlot: soft delete 조회(`deletedAt IS NULL`) 검증.
   - ClinicSession: `(slotId, date)` 중복 시 저장 실패.
   - ClinicAttendance: unique constraint 동작, 날짜 범위 조회 쿼리 검증.
   - ClinicRecord: 1:1 unique constraint 검증.
2. **Permission Validator Tests**
   - Teacher/Assistant/Student 권한 매트릭스 검증 (branch/teacher mismatch 차단).
3. **ClinicSlotService Tests**
   - create 성공/실패(시간 역전, 정원<1, branch mismatch).
   - update 시 요일/시간 변경으로 배정된 defaultClinicSlotId가 해제되는지 확인.
   - update 시 정원 감소가 배정 인원보다 작으면 409.
   - delete 시 soft delete 플래그 확인.
4. **ClinicDefaultSlotService Tests**
   - teacher/branch mismatch, duplicate slot, overlap, capacity 초과 검증.
   - 신규 지정 시 남은 세션 Attendance 생성 확인.
   - Slot 변경 시 이번 주 자동 이동 미발생 확인.
5. **ClinicSessionService Tests**
   - emergency 생성 규칙(slotId null, creatorMemberId not null) 검증.
   - cancel 시 isCanceled true + attendance 유지 확인.
6. **ClinicBatchService Tests**
   - 주간 배치 생성: slot 순회 + session 생성 idempotent 검증.
   - mid-week slot 생성 시 남은 날짜 session 생성 확인.
   - 정원 초과 시 Attendance 생성 스킵 + 로그 기록 확인.
7. **ClinicAttendanceService Tests**
   - auto 생성 시 capacity/중복 방지 검증.
   - teacher/assistant 추가/삭제 + 10분 lock 규칙.
   - student 추가 신청 성공/실패(취소, capacity, overlap) 케이스.
   - student 이동: 동일 주차/30분 제한/삭제+생성 로직 검증.
8. **ClinicRecordService Tests**
   - record 생성/수정/삭제, 1:1 제약 위반 검증.
   - Assistant 권한 미부여 시 작성 불가 확인.
   - Student 조회 차단 확인.
9. **Controller Tests (MockMvc)**
   - 각 엔드포인트별 200/400/403/409 응답 검증.
   - 학생 조회 응답 스키마(시간표 형태) 검증.

## 6. Implementation Stages (Backend)
1. **Phase 0 – Schema & Migration**
   - ClinicSlot: Course FK 제거, teacher/branch 기반으로 컬럼 정리.
   - ClinicSession: startTime/endTime 추가, sessionType/creatorMemberId 제약 적용.
   - Soft delete/unique/index 정리.
2. **Phase 1 – ClinicSlot**
   - Slot CRUD + validator + 기본 조회 API.
3. **Phase 2 – ClinicSession**
   - 조회/수동/긴급 생성 + 취소 처리.
4. **Phase 3 – ClinicAttendance**
   - 자동 생성, teacher/assistant 추가/삭제, student 신청/이동, 시간표 조회.
5. **Phase 4 – ClinicRecord**
   - 출석 기록 CRUD + StudentCalendar 연동 쿼리 확정.
6. **Phase 5 – Batch/Scheduler**
   - 주간 자동 생성(세션+출석) + 주중 Slot 생성 처리.
   - 정원 초과/중복 스킵 로깅.
7. **Phase 6 – Permission/Policy 정리**
   - 권한 validator 통합 + RsCode 매핑 정리.
8. **Phase 7 – 테스트/시나리오 정리**
   - 배치/학생 이동/취소 케이스 통합 테스트.
