# Feature: Season2 InitData 구성

## 1. Problem Definition

- Phase4 TODO의 “InitData 구성하기”가 비어 있어 신규 엔티티/기능을 검증할 기본 데이터가 없음.
- Season2 패키지 구조로 엔티티를 재작성했으나, 기존 Season1 InitData는 `.bak` 상태거나 스키마가 달라 그대로 사용할 수 없다.
- 현재는 Member/Company/Branch/StudentInfo만 먼저 시드해도 프론트엔드 연동 테스트(로그인 → 학원 선택)가 가능하므로, 우선 이 범위를 빠르게 채워 기능별 InitData를 단계적으로 확장한다.

## 2. Requirements

### Functional

1. 현재 단계에서는 Member + Company + Branch + StudentInfo InitData를 작성해 프론트엔드에서 Season2 로그인/학원 흐름을 검증한다.
2. InitData는 `global.init` 패키지로 구성하고, Spring `@Profile("local")` 또는 CommandLineRunner 기반으로 제공한다.
3. Stage 1 기본 시나리오:
   - Member: SuperAdmin 1명, Teacher 2명, Assistant 2명, Student 4명 + 각 Student에 대한 StudentInfo 사전 생성
   - Company: INDIVIDUAL 1개 + 공용 ACADEMY 4종(러셀/두각/시대인재/미래탐구)
   - Branch: INDIVIDUAL 전용 1개 + 각 ACADEMY별 지정된 지점 리스트(총 40개 이상) + verifiedStatus 구분
4. 각 InitData는 종속 관계 순서를 고려하여 실행되며, 중복 실행 시 안전하도록 `exists` 체크 또는 `@Transactional`로 초기화한다.
5. Member/StudentInfo 시드는 로컬/테스트 프로필에서만 자동 실행되며, 운영 환경에는 투입하지 않는다.
6. Company/Branch 데이터는 로컬/테스트뿐 아니라 운영 환경에서도 배포 가능해야 하므로, 별도의 프로필(`local`, `test`, `prod-seed` 등)이나 SQL export 방식으로 재사용할 수 있게 구성한다.

### Non-functional

- 데이터는 Season2 엔티티와 Enum을 사용하고, FK UUID를 명시적으로 연결한다.
- 가독성을 위해 Builder 또는 헬퍼 메서드 사용, 하드코딩 값은 `InitMembers`, `InitCompanies`, `InitBranches` 등 클래스로 분리한다.
- 추후 테스트에서 재사용할 수 있도록 주요 ID/이메일/코드를 상수로 관리한다.
- InitData 클래스/상수 구조는 `global.init`에 이미 존재하는 `BootstrapDataRunner`, `SeedKeys`, `data/*` 패턴을 그대로 재사용해 일관되게 관리한다. 현재 `BootstrapDataRunner`는 `@Profile({"local","dev"})`로 한정되어 있으므로, prod에서 Company/Branch를 주입하려면 `ProdOrganizationSeedRunner` 같은 별도 Runner를 두거나, 기존 Runner를 확장한 `@Profile({"local","dev","prod-seed"})` 등으로 교체해야 한다.

## 3. InitData Design (Draft)

| 순서 | 도메인      | 내용                                                                            |
| ---- | ----------- | ------------------------------------------------------------------------------- |
| 1    | Member      | SuperAdmin/Teacher/Assistant/Student 계정 생성 (비밀번호 `{noop}` or Encoder)   |
| 2    | StudentInfo | Stage 1 Student 4명에 대한 학적 정보/보호자 정보 생성                           |
| 3    | Company     | INDIVIDUAL/ACADEMY 샘플 생성, creator/verifiedStatus 구분                       |
| 4    | Branch      | 회사별 Branch 생성, 생성자 및 verifiedStatus 조합으로 OWNER/FREELANCE 흐름 표현 |

## 4. Implementation Steps (3단계)

1. **설계/데이터 정의 (Design)**
   - Member/StudentInfo/Company/Branch 데이터를 표로 재정리하고, 사용할 상수명을 확정한다.
2. **InitData 클래스 구현 (Structural)**
   - Spring `CommandLineRunner` 또는 `ApplicationRunner`로 시드 실행.
   - 순서: Member → StudentInfo → Company → Branch. (Branch 생성 시 company/creator FK 연결)
   - Stage 1 Runner는 `global.init`의 `BootstrapDataRunner`/`SeedKeys`/`data/*` 레이아웃을 그대로 사용해 `InitMembersRunner`, `InitOrganizationRunner` 등 책임을 분리한다.
   - 현재 `BootstrapDataRunner`가 `@Profile({"local","dev"})`로 제한되어 있으므로, Company/Branch를 prod에서도 주입하려면
     1. `BootstrapDataRunner`를 `@Profile({"local","dev","prod-seed"})` 등으로 확장하거나,
     2. prod 전용 `OrganizationSeedRunner`를 별도로 정의하고 동일한 `BaseInitData` 구현체를 재사용한다.
   - Member/StudentInfo Runner는 `@Profile({"local","test"})`로 한정해 운영 환경에서 생성되지 않도록 하고, Company/Branch Runner/데이터 Bean은 prod에서도 로드 가능한 프로필 조합(`local`,`test`,`prod-seed`)을 반드시 제공한다.
3. **검증/로그**
   - `./gradlew test` 또는 로컬 실행으로 시드가 정상 적용되는지 확인한 뒤 프론트 dev 서버에서 로그인/학원 선택 플로우를 검증한다.
   - AGENT_LOG STRUCTURAL 기록, TODO(InitData) 상태 업데이트.

## 5. Seed Dataset Detail (Stage 1)

### 5.1 Member

| 상수                              | 역할        | 이메일                        | 비밀번호              | 비고                         |
| --------------------------------- | ----------- | ----------------------------- | --------------------- | ---------------------------- |
| `InitMembers.SUPER_ADMIN_ID`      | SUPER_ADMIN | superadmin@classhub.dev       | `{noop}Admin!123`     | 시스템 전체 제어 계정        |
| `InitMembers.TEACHER_ALICE_ID`    | TEACHER     | teacher.alice@classhub.dev    | `{noop}Teacher!123`   | Season2 대표 Teacher         |
| `InitMembers.TEACHER_BOB_ID`      | TEACHER     | teacher.bob@classhub.dev      | `{noop}Teacher!123`   | 추가 Teacher (Branch 2 전담) |
| `InitMembers.ASSISTANT_MINA_ID`   | ASSISTANT   | assistant.mina@classhub.dev   | `{noop}Assistant!123` | 조교 예제 (Alice 기본 배정)  |
| `InitMembers.ASSISTANT_JISOO_ID`  | ASSISTANT   | assistant.jisoo@classhub.dev  | `{noop}Assistant!123` | 추가 조교 (초대/배정 검증용) |
| `InitMembers.STUDENT_JAEKYUNG_ID` | STUDENT     | student.jaekyung@classhub.dev | `{noop}Student!123`   | 고등생 시나리오              |
| `InitMembers.STUDENT_ARIN_ID`     | STUDENT     | student.arin@classhub.dev     | `{noop}Student!123`   | 중학생 시나리오              |
| `InitMembers.STUDENT_DONGHYUK_ID` | STUDENT     | student.donghyuk@classhub.dev | `{noop}Student!123`   | 초등생 시나리오              |
| `InitMembers.STUDENT_SUMIN_ID`    | STUDENT     | student.sumin@classhub.dev    | `{noop}Student!123`   | Gap Year 시나리오            |

### 5.2 StudentInfo

| 상수                               | memberId            | schoolName         | grade        | birthDate  | parentPhone   |
| ---------------------------------- | ------------------- | ------------------ | ------------ | ---------- | ------------- |
| `InitStudentInfo.JAEKYUNG_INFO_ID` | STUDENT_JAEKYUNG_ID | "서울과학고등학교" | HIGH_2       | 2008-03-14 | 010-9988-7766 |
| `InitStudentInfo.ARIN_INFO_ID`     | STUDENT_ARIN_ID     | "분당중학교"       | MIDDLE_2     | 2011-09-02 | 010-4444-8888 |
| `InitStudentInfo.DONGHYUK_INFO_ID` | STUDENT_DONGHYUK_ID | "신촌초등학교"     | ELEMENTARY_5 | 2014-01-28 | 010-1111-5555 |
| `InitStudentInfo.SUMIN_INFO_ID`    | STUDENT_SUMIN_ID    | "대치고등학교"     | GAP_YEAR     | 2006-07-19 | 010-2222-6666 |

### 5.3 Company & Branch

#### INDIVIDUAL Preset

| 상수                          | 타입       | 이름                | Branch                                | creatorMemberId | 비고            |
| ----------------------------- | ---------- | ------------------- | ------------------------------------- | --------------- | --------------- |
| `InitCompanies.INDIVIDUAL_ID` | INDIVIDUAL | "Alice Private Lab" | `InitBranches.INDIVIDUAL_MAIN_ID` 1개 | Teacher Alice   | OWNER 흐름 검증 |

| Branch 상수                       | companyId     | 이름                   | verifiedStatus | creatorMemberId | 비고            |
| --------------------------------- | ------------- | ---------------------- | -------------- | --------------- | --------------- |
| `InitBranches.INDIVIDUAL_MAIN_ID` | INDIVIDUAL_ID | "Alice 랩 메인 캠퍼스" | VERIFIED       | Teacher Alice   | OWNER 자동 배정 |

#### ACADEMY Presets

Stage 1에서 바로 사용할 대형 학원 4종. 각 Branch는 기본 VERIFIED로 두되, 운영 중 검증 흐름이 필요하면 일부를 UNVERIFIED로 전환할 수 있다.

- **러셀 (`InitCompanies.RUSSELL_ID`)**
  - Branch 목록: `강남`, `대치`, `목동`, `부천`, `분당`, `영통`, `중계`, `평촌`, `대구`, `대전`, `센텀`, `울산`, `광주`, `원주`, `전주`, `청주`
  - 각 Branch는 `InitBranches.RUSSELL_<NAME>_ID` 네이밍을 사용 (예: `RUSSELL_GANGNAM_ID`)
- **두각 (`InitCompanies.DUGAK_ID`)**
  - Branch 목록: `본관`, `태성관`, `S관`, `우전관`, `비전관`, `하늘관`, `오름관`, `오름관3`, `창비관`, `진학관`, `이룸관`, `K관`, `누리관`, `입시센터`, `분당`
  - Branch 상수 패턴: `InitBranches.DUGAK_<NAME>_ID`
- **시대인재 (`InitCompanies.SIDAE_ID`)**
  - Branch 목록: `대치`, `목동`, `반포`, `분당`, `대전`
  - Branch 상수 패턴: `InitBranches.SIDAE_<NAME>_ID`
- **미래탐구 (`InitCompanies.MIRAETAMGU_ID`)**
  - Branch 목록: `대치`, `성북`, `중계`, `목동`, `마포`, `동작`, `광진`, `분당`, `미사`, `물금`, `화명`, `사직`, `금정`, `해운대`, `센텀`, `송도`, `전주`
  - Branch 상수 패턴: `InitBranches.MIRAETAMGU_<NAME>_ID`

모든 학원 Branch는 creatorMemberId를 `null`로 두고 SuperAdmin이 선행 등록한 시나리오를 표현한다. 필요 시 일부 Branch(예: 러셀 전주, 두각 분당)는 UNVERIFIED 상태로 설정해 검증 흐름을 테스트한다.
