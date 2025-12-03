# Feature: Bootstrap Data & StudentProfile Seed Flow

## 1. Problem Definition

- Auth/Invitation/StudentProfile/PersonalLesson 기능은 구현됐지만, 로컬에서 즉시 시나리오를 재현할 고정 데이터(교사 2명, 조교, 학생, Course, StudentProfile, PersonalLesson 등)가 없어 테스트가 번거롭다.
- Playwright 데모, Swagger 수동 검증, 프런트 PoC를 진행하려면 부팅 시 자동으로 계정/반/학생 정보가 채워지고 반복 실행 시에도 안전하게 갱신되는 시드/업데이트 흐름이 필요하다.
- 현재 TODO(`Phase 2 - StudentProfile 기반 데이터 시드/업데이트 흐름`)는 범위/정책이 없어서 어떤 데이터를 만들고 어떻게 유지할지 정해지지 않았다.

## 2. Requirements

### Functional

1. **ApplicationRunner 기반 부트스트랩**
   - `global.init` 패키지에 `BaseInitData` 추상 클래스와 `BootstrapDataRunner`를 정의해 시드 로직을 모듈화한다.
   - Runner는 `local`, `dev` 프로필에서만 동작하며 Member → Course → StudentProfile → Invitation → PersonalLesson 순으로 데이터를 주입한다.
2. **고정 Member 시드**
   - SuperAdmin 1명, Teacher 2명(각각 `teacher_alpha@classhub.dev`, `teacher_beta@classhub.dev`)을 생성한다.
   - 각 Teacher마다 Assistant 3명(`assistant_alpha_1@classhub.dev` …)과 Student 계정 최소 1명을 연결해 권한/소유 검증을 반복할 수 있게 한다.
3. **Course/Invitation 시드**
   - 각 Teacher에게 최소 3개의 Course(`Alpha Course A/B/C`, `Beta Course A/B/C`)를 매핑한다.
   - Assistant/Student 초대 코드를 Course/StudentProfile에 연결해 저장하고, 초대 상태 별 예시(PENDING/ACCEPTED)를 포함한다.
4. **StudentProfile/PersonalLesson 시드**
   - Teacher별로 30개의 StudentProfile을 생성하고, 전화번호/학부모 연락처/학년/기본 클리닉 슬롯 기본값을 채운다.
   - 각 StudentProfile에 최근 PersonalLesson 3개씩을 생성해 타임라인 조회를 테스트할 수 있게 한다.
5. **업데이트 흐름**
   - 시드는 idempotent 해야 하므로 자연키(email, course name + teacher email, phone number 등)로 엔티티를 찾고 존재하면 `isActive`, 프로필 정보, 진도 내용 등을 업데이트한다.
   - 재실행 시 수동으로 수정한 데이터를 덮을지, skip할지 정책을 정리하고 기본은 “모델 필드 기준 덮어쓰기”로 한다.
6. **재실행 제어**
   - seed 완료 여부와 최근 실행 시각을 로그로 남기고, 필요 시 `?force=true` 프로퍼티(예: `bootstrap.data.force=true`)로 강제 재시드를 허용한다.

### Non-functional

- Runner/Seed 관련 코드는 `global.init` 패키지 내에서만 관리해 도메인 패키지 오염을 방지한다.
- Password/초대 코드 등 민감 정보는 `.env.example`/문서에 공유 가능한 값으로 고정하고, 운영 프로필에서는 Runner Bean이 생성되지 않도록 `@Profile`/`@ConditionalOnProperty`를 조합한다.
- 트랜잭션 단위로 엔티티 묶음을 처리해 중간 실패 시 롤백되도록 하고, 실패 로그를 명확히 남긴다.
- Seed 데이터 정의는 코드 상수보다는 `records`/`List<SeedDescriptor>` 등 구조화된 객체로 관리해 향후 JSON/YAML 외부화가 쉽도록 한다.

## 3. API Design (Draft)

| Trigger                        | 형태                                                | 설명                                                                                         |
| ------------------------------ | --------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| Spring ApplicationRunner       | `BootstrapDataRunner` (`@Profile({"local","dev"})`) | 애플리케이션 부팅 시 자동 실행. force 플래그/Property 로직 포함.                             |
| Admin 재실행 엔드포인트 (옵션) | `POST /ops/bootstrap/reload`                        | 향후 필요 시 Runner 로직을 서비스로 분리해 수동 재실행 지원. 1차 스코프에서는 Runner만 구현. |

## 4. Domain Model (Draft)

- **Member**: SuperAdmin 1명, Teacher 2명, 각 Teacher마다 Assistant 3명(총 6명)과 Student 계정 최소 1명. 모든 Member는 `global.entity.BaseEntity` 상속 + role 필수.
- **Course**: Teacher당 최소 3개. 현재는 목록/소유권 확인 용도로 사용하며 향후 다른 도메인에서도 재사용 가능하도록 이름/UUID를 고정한다.
- **Invitation**: Teacher→Assistant, Assistant→Student 각각 최소 2개. `status`는 `PENDING`, `ACCEPTED` 케이스를 모두 포함한다.
- **StudentProfile**: Teacher별 30명. `memberId`, `assistantId`, `phoneNumber`, `defaultClinicSlotId`(optional) 등을 채우고 Course 분포를 균등하게 유지한다.
- **PersonalLesson**: 각 StudentProfile당 3건씩 생성하며, 날짜를 1~3일 간격으로 두어 정렬/필터 테스트를 지원한다.

## 5. TDD Plan

1. **BootstrapDataRunnerTest (Unit)**
   - Runner 실행 시 Member/Course/StudentProfile/PersonalLesson 생성이 호출되는지 Mock Repository/Service로 검증.
   - force 플래그 없을 때 이미 시드된 경우 skip 또는 업데이트 로직이 작동하는지 확인.
2. **BootstrapDataIntegrationTest**
   - `@SpringBootTest` + `@ActiveProfiles("test")`에서 Runner 비활성화 확인.
   - `@ActiveProfiles("local")` 환경에서 Runner 실행 후 Repository 상태(Teacher 2명, Assistant 6명, Course 6개 이상, Teacher별 StudentProfile 30명, PersonalLesson 90건)를 검증한다.
3. **Idempotency Test**
   - 동일 Runner 두 번 실행 시 엔티티 수가 증가하지 않고 업데이트만 일어나는지, Course가 단순 조회 용도로 유지되는지 확인.
4. **Documentation**
   - README/AGENT_LOG에 seed 계정/비밀번호/사용 방법을 기록하고, 재실행 옵션을 설명한다.
