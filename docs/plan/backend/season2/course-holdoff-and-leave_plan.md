# Feature: 휴원/재원 연동 & 반 자동 보관

## 1. Problem Definition
- 현재 휴원/재원은 `StudentCourseAssignment`의 on/off에만 반영되어 있어, 실제 활동 단위인 `StudentCourseRecord`/`ClinicAttendance`와의 정합성이 깨진다.
- 반 종료 이후에도 자동 배치/세션 생성이 계속될 수 있어, 종료된 반의 클리닉 운영이 꼬인다.
- 종료된 반의 “보관 모드” 전환(soft delete) 정책이 부재해 운영 기준이 일관되지 않다.

## 2. Requirements

### Functional
1. **휴원 처리 (StudentCourseAssignment 비활성화)**
   - `StudentCourseAssignment.deactivate()` 호출 시 해당 학생/반의 `StudentCourseRecord`를 soft delete한다.
   - 휴원 시점 이후의 ClinicAttendance만 삭제한다.
     - 삭제 범위: 해당 `StudentCourseRecord` + `clinicSession.date/time >= now(KST)`
   - 휴원 상태에서는 기본 슬롯 설정/클리닉 참석 신청/이동을 차단한다.
2. **재원 처리 (StudentCourseAssignment 활성화)**
   - `StudentCourseAssignment.activate()` 호출 시 해당 `StudentCourseRecord`를 restore한다.
   - 기본 슬롯이 설정된 경우 **현재 주차의 남은 세션**에 대한 ClinicAttendance를 재생성한다.
3. **반 자동 보관 (Course soft delete)**
   - `Course.endDate + 7일`이 지나면 자동으로 `Course.delete()` 처리한다.
   - 보관된 반은 ClinicBatch로 자동 생성되는 ClinicAttendance 대상에서 제외한다.

### Non-functional
- 휴원/재원 연동은 **서비스 계층**에서 처리한다. 엔티티 메서드는 순수 상태 변화만 유지한다.
- 삭제/복구 로직은 트랜잭션 범위 내에서 수행한다.
- 기존 RsData/RsCode 규칙을 유지하고, 오류 케이스는 기존 코드 체계를 따른다.

## 3. API Design (Draft)
- API 변경 없음 (기존 endpoint 사용)
  - `PATCH /api/v1/courses/assignments/{assignmentId}/activate`
  - `PATCH /api/v1/courses/assignments/{assignmentId}/deactivate`
  - `PATCH /api/v1/students/me/courses/{courseId}/clinic-slot`
  - `POST /api/v1/students/me/clinic-attendances`
  - `POST /api/v1/students/me/clinic-attendances/move`

## 4. Domain Model (Draft)

### 상태 전이
- StudentCourseAssignment (active ↔ inactive)
  - inactive: StudentCourseRecord 삭제 + 미래 ClinicAttendance 삭제
  - active: StudentCourseRecord 복구 + 기본 슬롯 기반 출석 재생성
- Course (active → archived)
  - endDate + 7일 후 soft delete

### 데이터 연동
- StudentCourseRecord는 **Assignment 상태에 종속**된다.
- ClinicAttendance는 StudentCourseRecord 기준으로 생성/삭제된다.

## 5. TDD Plan
1. **StudentCourseAssignment 휴원/재원 테스트 (Service)**
   - deactivate 시 StudentCourseRecord deletedAt 설정 확인
   - deactivate 시 미래 ClinicAttendance 삭제 확인
   - activate 시 StudentCourseRecord restore 확인
   - activate 시 기본 슬롯이 있으면 이번 주 남은 ClinicAttendance 재생성 확인
2. **ClinicAttendance 삭제 범위 테스트 (Service/Repository)**
   - `now(KST)` 기준으로 이후 세션만 삭제되는지 검증
3. **ClinicBatch 차단 테스트 (Service)**
   - Course.deletedAt이 있는 경우 Attendance 자동 생성이 발생하지 않는지 검증
4. **Course 자동 보관 스케줄러 테스트**
   - endDate + 7일 경과 시 Course soft delete 여부 검증

## 6. Implementation Steps (3단계)
1. **Step 1: 휴원/재원 연동**
   - `CourseAssignmentService.activate/deactivate`에 StudentCourseRecord 및 ClinicAttendance 연동 로직 추가
   - ClinicAttendance 삭제/재생성 유틸리티 추가
2. **Step 2: 반 자동 보관 스케줄러**
   - Course 종료 + 7일 기준 soft delete 배치 작업 추가
3. **Step 3: 자동 배치 중단**
   - ClinicBatchService/ClinicDefaultSlotService에서 Course.deletedAt 확인 후 자동 생성 중단

---

### 계획 요약 (한국어)
- 휴원/재원 상태가 StudentCourseAssignment 기준으로만 처리되어 있어, StudentCourseRecord/ClinicAttendance와 동기화되도록 개선한다.
- 반 종료 후 7일이 지나면 자동 보관 모드로 전환하고, 보관된 반은 클리닉 자동 배치 대상에서 제외한다.
- 기존 API는 유지하며 서비스 계층에서 TDD로 동작을 보강한다.
