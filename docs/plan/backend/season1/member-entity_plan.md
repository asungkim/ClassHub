# Feature: Member 엔티티

## 1. Problem Definition

- 주요 사용자(Member: Teacher/Assistant/Student/Admin)의 기본 정보를 저장/관리할 엔티티가 필요하다.
- Role, 활성 상태, Teacher-Assistant 관계 등 기본 속성이 정의되지 않으면 이후 인증/권한/초대 흐름을 구현할 수 없다.

## 2. Requirements

### Functional

- Member 속성: email, password, name, role(enum: TEACHER/ASSISTANT/STUDENT/SUPERADMIN), isActive, teacherId(Assistant/Student 전용 FK).
- email은 unique, password는 암호화 저장.
- teacherId는 Assistant, Student가 어떤 Teacher에 속해 있는지 표현(Teacher는 null).
- Auditing 필드(BaseEntity 기반)로 createdAt/updatedAt 유지.
- Role/활성 상태 변경 API에서 사용할 수 있도록 상태/role 검증 로직을 염두에 둔다.

### Non-functional

- 엔티티는 `global.entity.BaseEntity` 상속.
- Lombok `@Builder`, `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PROTECTED)`를 사용해 생성 패턴을 통일한다.
- 테이블 명은 member(소문자 복수화 규칙 X)로 유지.
- 이메일 unique 제약 + teacherId 인덱스 고려.

## 3. API Design (Draft)

- Member CRUD는 추후 PLAN(예: Member 서비스)에서 상세화한다. 엔티티 PLAN에서는 API를 정의하지 않는다.

## 4. Domain Model (Draft)

- Member(id:UUID, email, password, name, role, isActive, teacherId).
- Role은 Enum으로 관리.
- Assistant는 teacherId 필수, 나머지 Role은 null.

## 5. TDD Plan

1. JPA 엔티티 저장/조회 테스트: email unique 제약 검증.
2. Assistant 생성 시 teacherId 필수 조건 검증 (도메인 로직 or DB constraint).
3. Role별 활성 상태 변경 시 Auditing 필드가 정상 업데이트되는지 확인.
