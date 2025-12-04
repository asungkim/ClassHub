# Feature: Invitation 엔티티

## 1. Problem Definition

- Teacher가 Assistant를, Assistant가 Student를 초대하는 흐름을 지원하려면 초대 정보(대상, 발신자, 상태, 코드)가 필요하다.
- 초대 코드/상태를 관리하지 않으면 Auth 플로우(초대 기반 회원가입)나 CourseAssistant 연결이 구현되지 않는다.

## 2. Requirements

### Functional

- 필드: senderId(Member UUID), targetEmail, inviteeRole(ASSISTANT/STUDENT), status(PENDING/ACCEPTED/EXPIRED/REVOKED), code(고유 문자열), expiredAt, studentProfileId(optional).
- Teacher→Assistant 초대: senderId=Teacher, inviteeRole=ASSISTANT. Course ID는 사용하지 않고, 승인 후 CourseAssistant 관계를 별도로 생성한다.
- Assistant→Student 초대: senderId=Assistant, inviteeRole=STUDENT, studentProfileId optional(사전 등록된 학생 정보 연결).
- status 전환: accept → ACCEPTED, revoke → REVOKED, 만료 시간 지나면 EXPIRED.
- 고유 초대 코드(랜덤 문자열, unique index) 발급.

### Non-functional

- `global.entity.BaseEntity` 상속, Auditing 포함.
- `@Builder`, `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PROTECTED)` 사용.
- 인덱스: code unique, senderId, courseId, status 조합 고려. -> 당장 적용할 필요없음.
- 삭제 대신 status 업데이트로 관리(soft delete 성격).

## 3. API Design (Draft)

- Invitation CRUD/상태 변경 API는 추후 PLAN(초대 서비스)에서 정의한다. 엔티티 PLAN에서는 API 세부 정의 생략.

## 4. Domain Model (Draft)

- Invitation(id, senderId, studentProfileId, targetEmail, inviteeRole, status, code, expiredAt).
- inviteeRole: ASSISTANT, STUDENT.
- status: PENDING, ACCEPTED, EXPIRED, REVOKED.

## 5. TDD Plan

1. JPA 저장/조회 테스트: code unique 검증, status 기본값 PENDING.
2. accept()/revoke()/expire() 도메인 메서드 구현 시 상태 전환 테스트.
3. expiredAt 지난 초대를 만료 처리하는 메서드 테스트 (추후 서비스에서 활용).
