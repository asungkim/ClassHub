# Feature: Core Domain Entities Implementation

## 1. Problem Definition

- Phase4 TODO “핵심 엔티티 생성”이 미완료여서 Company/Branch/Course/Clinic 등 주요 도메인을 구현할 기반이 없다.
- Season2 설계(`docs/design/final-entity-spec.md`)에서 정의한 엔티티 구조는 기존 Season1 코드와 다르다.
- Enum/패키지 구조가 확정된 상태에서 실제 엔티티 + Repository + 기본 규칙을 마련하지 않으면 이후 API 작업(TODO Phase5 이후)을 진행할 수 없다.

## 2. Requirements

### Functional

1. `docs/design/final-entity-spec.md` 기준 핵심 엔티티(Company, Branch, Course, StudentCourseEnrollment, StudentCourseRecord, TeacherBranchAssignment, TeacherAssistantAssignment, ClinicSlot, ClinicSession, ClinicAttendance, ClinicRecord, SharedLesson, PersonalLesson, Notice, NoticeRead, WorkLog 등)를 도메인 패키지 구조에 맞게 생성한다. 연관 관계는 대부분 FK UUID 필드로만 관리하고, JPA `@ManyToOne`/`@OneToMany`는 꼭 필요한 경우에만 사용한다.
2. 모든 엔티티는 `BaseEntity` 상속 + UUID PK + soft-delete 규칙을 따른다(명시된 경우 실제 DELETE 허용).
3. 연관관계/제약조건을 명확하게 모델링한다:
   - Company ↔ Branch (1:N)
   - Course ↔ SharedLesson/ClinicSlot 등
   - StudentCourseRecord ↔ PersonalLesson/ClinicAttendance
   - ClinicSession REGULAR/EMERGENCY 필드 제약
4. 필요한 곳에 Cascade/OnDelete/Unique 제약을 정의한다 (spec에 명시).
5. JPA Repository 인터페이스를 도메인별로 추가하고, 필수 쿼리 메서드는 TODO “Repository TDD 구현” 단계에서 채울 수 있도록 최소 구조만 잡는다.

### Non-functional

- 패키지 구조는 `domain/<feature>/<subdomain>` (`model`, `repository`, `application`, `web`, `dto`) 규칙 준수.
- 엔티티 클래스에는 Lombok `@Getter`, `@NoArgsConstructor(access = PROTECTED)`, `@Builder` 조합을 기본으로 사용하고, 생성자/정적 팩토리로 도메인 유효성 보장.
- 추후 테스트/TDD 작업을 고려해 엔티티별 생성-helper(예: `create()` 팩토리)와 setter 대신 명시적 메서드를 추가한다.
- 변경 사항은 STRUCTURAL 로그로 기록, TODO 상태는 구현 완료 후 별도 BEHAVIORAL/STRUCTURAL 이벤트에서 업데이트한다.

## 3. API Design (Draft)

- 본 PLAN은 엔티티/Repository 정의가 목적이라 API 스펙을 직접 다루지 않는다. 다만 추후 API 구현 시 엔티티 구조를 기준으로 Controller/Service 설계가 진행됨을 명시.

## 4. Domain Model (Draft)

| Domain         | Entities                                                                      |
| -------------- | ----------------------------------------------------------------------------- |
| Company/Branch | Company, Branch, TeacherBranchAssignment                                      |
| Member         | Member(기존), StudentInfo(기존), StudentCourseEnrollment, StudentCourseRecord |
| Course/Lesson  | Course, SharedLesson, PersonalLesson                                          |
| Invitation     | Invitation (구현됨), TeacherAssistantAssignment (구현됨)                      |
| Clinic         | ClinicSlot, ClinicSession, ClinicAttendance, ClinicRecord                     |
| Notice/WorkLog | Notice, NoticeRead, WorkLog                                                   |

- 각 엔티티는 spec의 필드/인덱스/삭제 정책을 그대로 따른다. 예: ClinicSession의 REGULAR/EMERGENCY 분기, Notice/WorkLog는 실제 DELETE 허용.

## 5. Implementation Steps (3단계)

1. **모델 설계 상세화 (Design)**
   - `final-entity-spec.md`를 도메인별로 재구성하여 필요한 필드/연관관계/인덱스를 표로 정리.
   - Season1 `.bak` 파일 중 재사용 가능한 부분 여부 점검.
   - 패키지별 TODO 우선순위(예: Company→Branch→Course→Clinic) 정의하고 사용자에게 공유.
2. **엔티티 + Repository 생성 (Structural)**
   - 도메인별 패키지 생성, 엔티티/Repository 클래스 추가.
   - 연관관계는 기본적으로 FK UUID 필드로만 표현하고, 실제 객체 그래프는 필요시(Service, Query 단계)에서 조립한다.
   - 필요 시 기본 생성 로직(정적 팩토리) 포함.
3. **정리 및 롤링 검증**
   - 전역 컴파일 + `./gradlew test`로 엔티티 정의 검증.
   - AGENT_LOG STRUCTURAL 이벤트 기록, TODO 상태는 추후 BEHAVIORAL 단계에서 업데이트.

## Appendix A. Core Entity Checklist

| Domain     | Entity                                                                | Status                      |
| ---------- | --------------------------------------------------------------------- | --------------------------- |
| Company    | Company, Branch                                                       | 미구현                      |
| Assignment | TeacherBranchAssignment (exists), TeacherAssistantAssignment (구현됨) | 진행 필요(BranchAssignment) |
| Course     | Course                                                                | 미구현                      |
| Student    | StudentCourseEnrollment, StudentCourseRecord                          | 미구현                      |
| Lesson     | SharedLesson, PersonalLesson                                          | 미구현                      |
| Clinic     | ClinicSlot, ClinicSession, ClinicAttendance, ClinicRecord             | 미구현                      |
| Notice     | Notice, NoticeRead                                                    | 미구현                      |
| WorkLog    | WorkLog                                                               | 미구현                      |
