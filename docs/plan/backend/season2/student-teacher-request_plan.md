# Feature: 학생 선생님 검색/요청 API (Student)

## 1. Problem Definition

- 학생이 선생님을 검색하고 연결 요청을 보내는 흐름이 필요하다.
- 학생은 본인이 보낸 요청을 조회/취소할 수 있어야 한다.
- 학생 화면에서는 선생님 목록 관리가 아니라 요청/연결 흐름만 제공한다.

## 2. Requirements

### Functional

- 학생이 선생님을 검색한다.
  - 키워드(선생님 이름) + 회사/지점 필터를 지원한다.
  - 검증된(VERIFIED) 회사/지점만 노출한다.
  - 이미 요청했거나 연결된 선생님은 검색 결과에서 제외한다.
- 학생이 선생님에게 연결 요청을 생성한다.
  - 중복 요청은 금지한다.
- 학생이 본인 요청 목록을 조회한다.
  - 상태 필터(PENDING/APPROVED/REJECTED/CANCELLED)를 지원한다.
- 학생이 PENDING 요청을 취소한다.

### Non-functional

- 필요한 엔티티/레포지토리를 신규 생성한다. (개발 단계이므로 마이그레이션 고려 제외)
- 요청 처리 로직은 Teacher 승인/거절과 분리되어야 한다.
- 응답은 RsData/RsCode 규칙을 따른다.

## 3. API Design (Draft)

- GET /teachers?keyword&companyId&branchId&page&size

  - 설명: 학생 선생님 검색
  - 필터: keyword(이름), companyId, branchId
  - 제약: VERIFIED 회사/지점만, 이미 요청/연결된 선생님 제외

- POST /teacher-student-requests

  - 설명: 학생 요청 생성
  - body: teacherId, message(optional)

- GET /teacher-student-requests?status&page&size

  - 설명: 학생 본인 요청 목록 조회
  - status: PENDING/APPROVED/REJECTED/CANCELLED

- PATCH /teacher-student-requests/{id}/cancel
  - 설명: 학생 요청 취소
  - 제약: PENDING만 취소 가능

## 4. Domain Model (Draft)

- StudentTeacherRequest
  - fields: studentMemberId, teacherMemberId, status, message, processedByMemberId, processedAt
  - 상태 기본값: PENDING
  - 인덱스: (teacherMemberId), (studentMemberId), (status)
  - 검증: 동일 student/teacher 요청 중복 금지 (PENDING 기준)
- TeacherStudentAssignment
  - fields: teacherMemberId, studentMemberId, deletedAt
  - 유니크: (teacherMemberId, studentMemberId)
- RequestStatus: PENDING / APPROVED / REJECTED / CANCELLED

### Repository (Draft)

- StudentTeacherRequestRepository
  - existsByStudentMemberIdAndTeacherMemberIdAndStatus(PENDING)
  - findByStudentMemberIdAndStatusIn(...) with paging
  - findByTeacherMemberIdAndStatusIn(...) with paging (Teacher 화면 대비)
- TeacherStudentAssignmentRepository
  - existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull

### Service (Draft)

- TeacherSearchService (또는 기존 검색 서비스 확장)
  - VERIFIED 회사/지점만 조회
  - 요청/연결된 teacher 제외 조건 추가
- StudentTeacherRequestService (Student 전용)
  - 요청 생성 / 목록 조회 / 취소
  - 상태 전이 검증 (PENDING만 취소)

## 5. TDD Plan

1. Repository 테스트 (DataJpaTest)

- StudentTeacherRequest 저장/조회/중복 체크
- TeacherStudentAssignment 유니크 및 exists 검증

2. Teacher 검색 서비스 테스트

- VERIFIED 회사/지점만 조회되는지
- 이미 요청/연결된 teacher가 결과에서 제외되는지

3. 학생 요청 생성 서비스 테스트

- 정상 생성
- 동일 teacher 중복 요청 차단
- 이미 연결된 teacher 요청 차단

4. 학생 요청 목록 조회 테스트

- status 필터 적용
- 본인 요청만 반환

5. 학생 요청 취소 테스트

- PENDING 요청만 취소 가능
- APPROVED/REJECTED/CANCELLED 요청은 실패

6. 컨트롤러 테스트

- 학생 권한에서 각 API 응답 코드/스키마 확인

## 6. 개발 순서

- StudentTeacherRequest 엔티티/레포지토리 → Repository 테스트
- TeacherStudentAssignment 엔티티/레포지토리 → Repository 테스트
- Teacher 검색 서비스 확장 → Service 테스트
- StudentTeacherRequest 서비스 → Service 테스트
- StudentTeacherRequest 컨트롤러 → Controller 테스트
