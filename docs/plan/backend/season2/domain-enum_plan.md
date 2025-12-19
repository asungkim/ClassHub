# Feature: Domain Enum Consolidation

## 1. Problem Definition

- Phase4 TODO의 첫 작업 “도메인 ENUM 작성”이 미완료 상태라 이후 엔티티/레포/TDD를 진행할 근간이 없다.
- `docs/design/final-entity-spec.md`와 `docs/spec/v1.3.md`에 정의된 Enum 목록(BRANCH, COURSE, CLINIC 등)이 코드에 반영되지 않았다.
- 동일 Enum을 여러 곳에서 중복 선언하거나 문자열을 직접 사용하면서 타입 안정성이 떨어진다. 엔티티/DTO/테스트에서 일관된 Enum 패키지 구조가 필요하다.

## 2. Requirements

### Functional

1. `docs/design/final-entity-spec.md` 및 `docs/spec/v1.3.md`에서 정의한 모든 도메인 Enum을 Season2 패키지 구조(`domain/<feature>/model`)에 생성한다.
2. 최소한 다음 Enum을 포함한다:
   - CompanyType, VerifiedStatus
   - BranchRole
   - MemberRole (기존 유지 확인)
   - StudentGrade (기존 유지, spec과 비교)
   - StudentEnrollmentStatus / EnrollmentRequestStatus (spec 1.3)
   - ClinicSessionType, ClinicReason 등 spec에서 명시된 Clinic 관련 Enum
3. Enum 간 상호 참조가 필요한 경우 값/메서드를 정의하되, PLAN 범위에서는 순수 정의까지만 한다.
4. 각 Enum은 JPA Entity의 컬럼 요구(길이 20/30 등)에 맞춰 `@Enumerated(EnumType.STRING)`을 사용할 수 있게 설계하고, 필요 시 헬퍼 메서드(예: `public boolean isVerified()`)를 내부에 추가한다.
5. 기존 코드에 이미 존재하는 Enum은 spec과 불일치하면 업데이트 또는 신규 Enum으로 교체한다 (예: InvitationRole에서 STUDENT 제거).

### Non-functional

- 패키지 구조는 `docs/design/**` 기준 도메인별 분리: 예) `domain/company/model/CompanyType`, `domain/clinic/model/enum/ClinicAttendanceStatus`.
- 테스트는 Enum 자체에 대한 단위 테스트(값/메서드)까지만 작성하고, 실제 엔티티 매핑은 후속 Phase에서 진행한다.
- 변경 내역은 AGENT_LOG에 DESIGN/STRUCTURAL 구분으로 기록한다.
- 세부 패키지 구조:
  1. `domain/company` 안에 `company`와 `branch` 서브패키지를 두고, 각각 `model/application/web/repository/dto` 등 동일한 계층 구조를 갖는다 (예: `com.classhub.domain.company.company.model.Company`, `com.classhub.domain.company.branch.web.BranchController`).
  2. `domain/clinic` 하위에 `clinicslot`, `clinicsession`, `clinicattendance`, `clinicrecord` 등을 두고, 각 하위 패키지는 위와 동일한 패턴으로 `model/application/web/repository/dto` 구조를 유지한다. Enum은 해당 하위 도메인의 `model` 혹은 `model/enum`에 배치한다.

## 3. API Design (Draft)

- 해당 PLAN은 API를 직접 추가하지 않는다. 단, Enum 노출이 필요한 DTO/Response는 이후 차수에서 Enum을 올바르게 참조하도록 가이드만 제공한다.

## 4. Domain Model (Draft)

- **CompanyType**: `INDIVIDUAL`, `ACADEMY`
- **VerifiedStatus**: `UNVERIFIED`, `VERIFIED`
- **BranchRole**: `OWNER`, `FREELANCE`
- **MemberRole**: 기존 `TEACHER/ASSISTANT/STUDENT/ADMIN/SUPER_ADMIN`
- **StudentGrade**: 기존 Season2 정의 유지
- **EnrollmentRequestStatus**: `PENDING`, `APPROVED`, `REJECTED`
- **StudentEnrollmentStatus**: `ACTIVE`, `INACTIVE`
- **ClinicSessionType**: `REGULAR`, `EMERGENCY` 등 spec 반영
- **InvitationRole**: 현재 `ASSISTANT`만 유지 (기존 코드 정합성 확인)
- 기타 스펙 정의 Enum 전체 목록을 appendix로 명시하고 우선순위대로 생성한다.

## 5. Implementation Steps (3단계)

1. **Enum 인벤토리 작성 (Design)**
   - `docs/design/final-entity-spec.md` 전체에서 Enum 키워드 추출 목록화.
   - PLAN 요약(한국어) 공유.
2. **Enum 클래스 생성 및 패키지 구조 정리 (Structural)**
   - 도메인별 패키지 생성 후 Enum 파일 추가.
   - 기존 Enum 업데이트/삭제(예: InvitationRole) 및 사용처 컴파일 확인.
   - 단위 테스트 추가 (Enum 값/헬퍼 메서드).
3. **정리 및 로그 작성**
   - 전역 검색으로 Enum 사용처 업데이트, 컴파일 + `./gradlew test` 실행.
   - AGENT_LOG에 STRUCTURAL 기록, TODO 상태 유지(다음 작업에서 ✅ 처리).

## Appendix A. Enum Inventory

| Domain | Enum | Spec Values | Source | Current Status |
| --- | --- | --- | --- | --- |
| Company | CompanyType | INDIVIDUAL, ACADEMY | `final-entity-spec.md` §1 | 미구현 |
| Company/Branch | VerifiedStatus | UNVERIFIED, VERIFIED | §1 | 미구현 |
| Member | MemberRole | TEACHER, ASSISTANT, STUDENT, ADMIN, SUPER_ADMIN | §2 | 구현됨 (`domain/member/model/MemberRole`) |
| Student | StudentGrade | ELEMENTARY_1~6, MIDDLE_1~3, HIGH_1~3, GAP_YEAR | §2 | 구현됨 (`domain/member/model/StudentGrade`) |
| Assignment | BranchRole | OWNER, FREELANCE | §3 | 미구현 |
| Enrollment | EnrollmentStatus (EnrollmentRequestStatus) | PENDING, APPROVED, REJECTED | §5 | 미구현 |
| Clinic | SessionType (ClinicSessionType) | REGULAR, EMERGENCY | §7 | 미구현 |
| Feedback | FeedbackStatus | SUBMITTED, RESOLVED | §8 | 미구현 |
| Invitation | InvitationRole | ASSISTANT | §11 | 구현됨 (`domain/invitation/model/InvitationRole`) |
| Invitation | InvitationStatus | PENDING, ACCEPTED, EXPIRED, REVOKED | §11 | 구현됨 (`domain/invitation/model/InvitationStatus`) |
