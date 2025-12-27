# Feature: InitData 확장 (테스트용 시드 데이터)

## 1. Problem Definition
- 현재 `global.init` 기본 시드는 인원 수/관계 데이터가 적어, 조교/학생/클리닉 등 실제 플로우를 검증하기 어렵다.
- QA/개발에서 반복적으로 사용하는 테스트 시나리오(조교 배정, 지점/반, 학생 요청 등)를 매번 수동 생성해야 한다.
- 테스트 편의성을 위해 **일관된 이메일 패턴 + 충분한 관계 데이터**를 가진 확장 시드가 필요하다.

## 2. Requirements

### Functional
1. **Member 시드 (패턴 고정)**
   - Admin 1명: `ad@n.com`
   - Teacher 2명: `te1@n.com`, `te2@n.com`
   - Assistant 4명: `as1@n.com` ~ `as4@n.com`
   - Student 100명: `st1@n.com` ~ `st100@n.com`
   - 이름: 한국식 실명(예: 김철수) 형태, 중복 최소화
   - 비밀번호: 전원 `Qwer123!` (PasswordEncoder 적용)
2. **StudentInfo 시드**
   - 100명 모두 StudentInfo 생성
   - 학교/학년/생년월일/보호자 연락처를 한국식으로 다양하게 구성
3. **조교 배정 (Teacher ↔ Assistant)**
   - 각 Teacher는 4명의 Assistant 모두와 연결된 상태
4. **출강 지점 & 반 생성**
   - 각 Teacher는 2개의 Branch에 배정
   - 각 Branch마다 2개의 Course 생성 (Teacher당 4개 Course)
   - 각 Course는 스케줄 2개만 생성
5. **학생-선생님 요청**
   - 50명: `te1`, `te2` 모두 요청
   - 25명: `te1`만 요청
   - 25명: `te2`만 요청
   - 요청 상태는 `PENDING`, 메시지는 테스트용 고정 문구

### Non-functional
- 시드는 **결정적(deterministic)** 으로 생성되어 매번 동일한 결과를 제공해야 한다.
- `bootstrap.data.force=true`일 때는 이름/역할/비밀번호 등 시드 정보가 덮어써져야 한다.
- 확장 시드는 **local/test 프로필 중심**으로 활성화하고, 운영 환경에는 적용되지 않도록 제어한다.
- 기존 `global.init` 구조(`BaseInitData`, `BootstrapDataRunner`, `seeds` 패턴)를 그대로 사용한다.
- 시드 규모가 큰 만큼 **실행 여부를 설정으로 제어**할 수 있어야 한다.

## 3. API Design (Draft)
- API 변경 없음 (InitData 전용 변경)

## 4. Domain Model (Draft)

### 대상 엔티티
- `Member`, `StudentInfo`
- `TeacherAssistantAssignment`
- `TeacherBranchAssignment`
- `Course` + `CourseSchedule`
- `StudentTeacherRequest`

### 관계 요약
- Teacher(2) ↔ Assistant(4) = 8건 배정
- Teacher별 Branch 2개 → Branch별 Course 2개
- Course별 스케줄 2개
- Student(100) → StudentTeacherRequest (총 150건)

### 시드 구성 메모
- Branch 선택은 기존 `InitBranches`의 정해진 지점 중 Teacher별로 2개씩 매핑
- Course 일정은 Teacher별로 요일을 고정해 일정 분포를 안정적으로 유지

## 5. TDD Plan
1. **Seed 데이터 생성 규칙 테스트 (Unit)**
   - `InitMembers`에서 생성된 이메일 패턴/총 수/비밀번호 통일 여부 검증
   - `InitStudentInfos`에서 100건 생성 및 필수 필드 채움 여부 확인
2. **InitData 실행 결과 검증 (Integration)**
   - `@SpringBootTest` + `@ActiveProfiles("test")`
   - `bootstrap.data.enabled=true`, `bootstrap.data.force=true`, `bootstrap.data.mode=extended`
   - 실행 후 repository로 카운트/관계 확인
     - Member/StudentInfo 수
     - TeacherAssistantAssignment 수
     - TeacherBranchAssignment 수 + BranchRole 분포
     - Course 수 + 일정 요일 분포
     - StudentTeacherRequest 수/분포

## 6. Seed Execution Order (검증용)
1. Company
2. Branch
3. Member
4. StudentInfo
5. TeacherBranchAssignment
6. Course
7. StudentTeacherRequest

---

### 계획 요약 (한국어)
- 테스트 편의성을 위해 대규모(Member 107명) 시드와 관계 데이터를 `global.init`에 확장한다.
- Teacher/Assistant 배정, 지점/반, 학생 요청까지 포함해 실제 기능 흐름을 바로 검증 가능하게 구성한다.
- 실행은 local/test 프로필 + 설정 값으로 제어하며, 데이터는 결정적으로 생성되도록 설계한다.
