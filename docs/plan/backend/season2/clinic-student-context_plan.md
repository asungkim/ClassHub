# Feature: Student Clinic Context API & Attendance Request Simplification

## 1. Problem Definition
- 학생 클리닉 UI에서 teacher+branch 기준으로 슬롯/세션을 묶어 보여줘야 하지만, 현재 학생용 API에는 해당 컨텍스트 데이터가 없다.
- 추가 참석 신청 API가 `studentCourseRecordId`를 요구해 프론트에서 흐름이 복잡해지고, 사용자가 반 선택만으로 신청하기 어렵다.
- 목표: 학생 화면에서 필요한 컨텍스트 데이터를 제공하고, 추가 참석 요청을 `courseId` 기반으로 단순화한다.

## 2. Requirements
### Functional
1. **학생 클리닉 컨텍스트 조회**
   - 학생이 수강 중인 활성 Course 기준으로 컨텍스트 목록을 조회한다.
   - 응답은 `courseId`, `courseName`, `recordId`, `defaultClinicSlotId`, `teacherId`, `teacherName`, `branchId`, `branchName`, `companyId`, `companyName`를 포함한다.
   - StudentCourseRecord가 Soft Delete 상태면 제외한다.
2. **학생 추가 참석 신청 단순화**
   - `POST /api/v1/students/me/clinic-attendances` 요청은 `clinicSessionId` + `courseId`를 받는다.
   - 서버는 `studentId + courseId`로 StudentCourseRecord를 찾아 출석을 생성한다.
   - 기존 유효성 검증(세션 취소/잠금/정원/시간 겹침/teacher+branch 일치)은 동일하게 유지한다.

### Non-functional
- 응답은 `RsData`/`RsCode` 표준을 유지한다.
- 조회 API는 활성 데이터(`deletedAt IS NULL`)만 반환한다.
- 변경 API는 기존 권한/검증 흐름을 유지한다.

## 3. API Design (Draft)
### 3.1 Student Clinic Context
- `GET /api/v1/students/me/clinic-contexts`
  - Response: `RsData<List<StudentClinicContextResponse>>`
  - Response fields:
    - `courseId`, `courseName`
    - `recordId`, `defaultClinicSlotId`
    - `teacherId`, `teacherName`
    - `branchId`, `branchName`
    - `companyId`, `companyName`

### 3.2 Student Attendance Request
- `POST /api/v1/students/me/clinic-attendances`
  - Body: `{ "clinicSessionId": "uuid", "courseId": "uuid" }`
  - Response: `RsData<ClinicAttendanceResponse>`
  - Errors: 기존 `CLINIC_SESSION_FULL`, `CLINIC_ATTENDANCE_TIME_OVERLAP`, `CLINIC_ATTENDANCE_LOCKED` 등 유지
  - Note: 기존 query param 방식(`studentCourseRecordId`)은 제거/대체한다.

## 4. Domain Model (Draft)
- 신규 엔티티 없음.
- 신규 DTO
  - `StudentClinicContextResponse`
  - `StudentClinicAttendanceRequest` (또는 기존 요청 DTO 확장)

## 5. TDD Plan
1. `StudentClinicContextQueryServiceTest`
   - 활성 StudentCourseRecord만 반환되는지 검증
   - 응답 필드(teacher/branch/company 이름 포함) 매핑 확인
2. `StudentClinicContextControllerTest`
   - `GET /api/v1/students/me/clinic-contexts` 정상 응답 확인
3. `ClinicAttendanceControllerTest` (student request 변경)
   - `POST /api/v1/students/me/clinic-attendances`에 courseId 기반 요청 성공/실패 시나리오 검증
4. `ClinicAttendanceServiceTest`
   - `courseId`로 record를 찾고 세션/권한/시간 검증이 기존과 동일하게 동작하는지 확인

---

### 설계 참고
- 컨텍스트 응답은 프론트에서 teacher+branch 기준으로 그룹핑해 사용한다.
- `courseName`, `branchName`, `companyName`은 기존 `CourseViewAssembler`를 재사용해 조립한다.
