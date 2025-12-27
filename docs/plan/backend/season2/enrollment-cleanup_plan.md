# Feature: Enrollment Legacy Cleanup

## 1. Problem Definition
- v1.5 흐름(학생-선생님 연결 + 반 배치)로 전환되었지만, 기존 Enrollment 흐름(StudentEnrollmentRequest/StudentCourseEnrollment)이 코드와 API에 잔존한다.
- 동일한 목적의 개념이 공존하면서 유지보수 비용이 높아지고, 도메인 패키지 구조도 혼란스럽다.

## 2. Requirements
### Functional
- StudentEnrollmentRequest 관련 엔티티/레포/서비스/컨트롤러/테스트/문서(OpenAPI 포함)를 제거한다.
- StudentCourseEnrollment 관련 엔티티/레포/서비스/컨트롤러/테스트/문서를 제거한다.
- 기존 Enrollment 관련 API는 전부 제거한다.
- 삭제로 인해 빌드/테스트가 깨지지 않도록 참조를 정리한다.

### Non-functional
- API 경로는 이번 작업에서 변경하지 않는다(기존 v1.5 플로우 API 유지).
- 동작 변경이 아닌 구조 정리로 처리하며, 테스트는 기존 범위를 통과시키는 것을 기준으로 한다.

## 3. API Design (Draft)
- 기존 Enrollment API 엔드포인트는 제거한다.
  - 예: `/api/v1/student-enrollment-requests/**`, `/api/v1/students/me/courses`, `/api/v1/student-courses/**` (v1.4 플로우 기반)
- 학생-선생님 연결/반 배치 관련 API는 유지한다.

## 4. Domain Model (Draft)
- **삭제 대상**
  - StudentEnrollmentRequest
  - StudentCourseEnrollment
- **유지 대상**
  - StudentCourseAssignment
  - StudentCourseRecord
  - TeacherStudentAssignment
  - StudentTeacherRequest

### 패키지 기준 제안
- `domain.assignment`: 사람 간/권한 연결을 담당하는 관계
  - TeacherStudentAssignment
  - StudentTeacherRequest (요청도 관계 도메인으로 이동)
- `domain.studentcourse`: 학생-반 관계 및 그 부가 데이터
  - StudentCourseAssignment
  - StudentCourseRecord

> 정리 목표: `domain.enrollment` 패키지를 제거하고 관계 도메인을 위 두 축으로 정리한다.

## 5. TDD Plan
- 구조 정리 작업이므로 신규 테스트는 작성하지 않는다.
- 삭제/이동 후 기존 테스트가 모두 통과하는지 확인한다.

## 6. Implementation Steps
1. **Legacy Enrollment 제거**
   - StudentEnrollmentRequest 전 구성 요소(모델/레포/서비스/컨트롤러/DTO/테스트/문서) 삭제
2. **StudentCourseEnrollment 제거**
   - 엔티티/레포/서비스/컨트롤러/DTO/테스트/문서 삭제
   - StudentCourseRecord/Assignment가 Enrollment에 의존하던 참조가 있으면 제거/대체
3. **패키지 정리**
   - StudentTeacherRequest를 `domain.assignment`로 이동
   - `domain.enrollment` 패키지 제거 및 import 정리
   - 관련 패키지/테스트 경로 정리
4. **검증**
   - 백엔드 테스트 실행 및 컴파일 오류/사용되지 않는 참조 제거
