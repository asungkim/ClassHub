# Feature: 학생 목록/상세 조회 개선 (Teacher/Assistant)

## 1. Problem Definition
- 현재 학생 목록이 StudentCourseRecord 기준이라 동일 학생이 여러 Course에 참여하면 중복 노출된다.
- Course가 비활성화(soft delete)되어도 학생 목록에서 계속 노출된다.
- 목록은 학생 단위로 묶고, 상세 모달에서 Course/Record를 확인하도록 분리해야 한다.

## 2. Requirements
### Functional
- Teacher/Assistant가 학생 목록을 학생 단위로 조회한다.
- 학생 상태는 학생 단위로 계산한다.
  - ACTIVE: 해당 Teacher(또는 Assistant가 연결된 Teacher)의 활성 Course가 1개 이상 존재
  - INACTIVE: 활성 Course가 0개
  - ALL: 전체
- `courseId`는 목록을 좁히는 필터로만 동작한다.
  - `courseId`가 있어도 상태 판단은 전체 Course 기준으로 계산한다.
- 학생 목록 응답은 `member + studentInfo + active`를 포함한다.
  - `memberId`는 상세 모달 조회에 사용되므로 반드시 포함한다.
- 학생 상세 조회는 `memberId` 기준으로 Course 목록과 Record 목록을 함께 제공한다.
  - 기본 응답은 활성/비활성 Course 모두 포함한다.
- StudentCourseRecord 수정 API는 기존 경로를 유지한다.
- 접근 제어
  - Teacher: 본인 Course에 등록된 학생만 조회
  - Assistant: 연결된 Teacher들의 Course에 등록된 학생만 조회

### Non-functional
- 기존 RsData 응답 포맷을 유지한다.
- 중복 없이 학생 단위로 응답한다.
- 페이징/정렬(학생 이름 기준)을 유지한다.
- Record soft delete는 휴원/퇴소 의미로 사용하지 않는다.

## 3. API Design (Draft)
- GET `/api/v1/student-courses/students`
  - Query: `status=ACTIVE|INACTIVE|ALL`, `courseId?`, `keyword?`, `page`, `size`
  - Response: `PageResponse<StudentStudentListItemResponse>`
  - `StudentStudentListItemResponse`
    - `student`(StudentSummaryResponse)
    - `active`(boolean)

- GET `/api/v1/student-courses/students/{studentId}`
  - Query: 없음 (기본 전체)
  - Response: `StudentStudentDetailResponse`
    - `student`(StudentSummaryResponse)
    - `courses`(List<CourseResponse>)
    - `records`(List<StudentCourseRecordSummary>)

- PATCH `/api/v1/student-courses/{recordId}`
  - 기존 유지 (record 수정)

## 4. Domain Model (Draft)
- StudentCourseEnrollment + Course + Member + StudentInfo를 기준으로 목록을 구성한다.
- 학생 상태 계산은 다음 기준을 따른다.
  - 활성 Course 존재 여부(Teacher 기준)로 판단한다.
  - `courseId` 필터는 목록 대상만 제한하며 상태 계산에는 영향이 없다.
- StudentCourseRecord는 상세 화면의 Record 목록에 포함한다.
  - `recordActive = !record.isDeleted()` 필드를 포함한다.
- 데이터 모델 변경 없음.

## 5. TDD Plan
1. `StudentCourseEnrollmentRepository` 목록 쿼리 테스트
   - 동일 학생이 여러 Course에 등록되어도 학생 단위로 1건만 반환
   - `courseId` 필터 적용 시 해당 Course 등록 학생만 반환
2. 학생 상태 계산 테스트
   - 활성 Course가 1개 이상이면 ACTIVE
   - 활성 Course가 0개면 INACTIVE
   - `courseId` 필터가 있어도 상태 판단은 전체 Course 기준
3. Assistant 접근 테스트
   - 연결된 Teacher 범위 내 학생만 조회
4. 상세 조회 테스트
   - 학생 기준으로 Course 목록과 Record 목록이 함께 반환
   - Course active 값과 Record active 값이 함께 내려감
