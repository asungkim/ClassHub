# Feature: 초대 링크 재설계 (조교/학생)

## 1. Problem Definition

- 기존 초대 로직은 이메일 기반 단일 사용 흐름만 지원해 학생/조교 UI 요구사항과 맞지 않는다.
  - 조교 초대 링크는 공용·무기한으로 여러 번 사용되어야 하나, 현재는 7일 만료 + 1회 사용 후 ACCEPTED 상태가 되어 막힌다.
  - 학생 초대는 StudentProfile과 연동되어야 하지만, 현재는 studentProfileId 검증/연결 없이 이메일만 비교하고 가입 후에도 StudentProfile.memberId를 채우지 않는다.
  - 학생 초대 화면에서 "아직 초대되지 않은 StudentProfile 목록"과 기존 초대 목록을 구분해 보여줄 수 있는 API가 없다.
  - 초대 검증 응답이 초대 대상(StudentProfile 이름 등)을 포함하지 않아 가입자 확인 UX가 부족하다.
  - 현재 `Invitation.targetEmail`이 `nullable = false`로 설정되어 있어 이메일 없이 초대 링크만 생성하는 흐름을 지원할 수 없다.

## 2. Requirements

### Functional

#### 학생 초대 (Teacher/Assistant)

- **대상**: Teacher 또는 Teacher에 소속된 Assistant.
- **후보 목록 조회**: `GET /api/v1/invitations/student/candidates`
  - `memberId=null`이고 `active=true`인 StudentProfile만 노출
  - Teacher: 자신이 소유한(`teacherId=본인`) 모든 StudentProfile
  - Assistant: 자신이 담당하는(`assistantId=본인`) StudentProfile만
  - 이미 PENDING 상태의 초대가 있는 StudentProfile은 제외
  - 검색 필터: `name`(부분 일치), `assistantId`(Teacher가 조교별 필터)
  - 응답: StudentProfileSummary (id, name, phoneNumber, age, grade, assistantId 등)
- **배치 생성**: `POST /api/v1/invitations/student/batch`
  - 여러 학생을 한 번에 초대 (프론트에서 체크박스 선택 → 전송 흐름)
  - Body: `{ "invites": [ { "studentProfileId": "UUID", "targetEmail": "optional" }, ... ] }`
  - 각 StudentProfile별 유효성 검증:
    - 소유/담당 권한 확인 (Teacher는 teacherId, Assistant는 assistantId 검증)
    - active=true, memberId=null 확인
    - 이미 PENDING 초대 있는지 확인
  - `targetEmail` 처리:
    - 있으면: 이메일 중복 검사(PENDING 초대 + 기존 회원), 초대 생성 시 이메일 포함
    - 없으면: 초대 링크만 생성, 회원가입 시 이메일 입력
  - 초대 속성: `expiredAt = now + 7일`, `maxUses = 1`, `useCount = 0`
  - 응답: `List<InvitationResponse>` (생성된 초대 목록, studentProfile 정보 포함)
- **목록 조회**: `GET /api/v1/invitations/student?status=`
  - 응답 전 만료된 초대를 자동으로 EXPIRED로 전환
  - 응답: 초대 코드, 상태, 만료 시각, StudentProfile(id, name), targetEmail, useCount, maxUses
- **취소**: `DELETE /api/v1/invitations/{code}`
  - sender(생성자)만 취소 가능
  - PENDING 상태일 때만 REVOKED로 전환

#### 학생 초대 검증/가입

- **검증**: `POST /api/v1/auth/invitations/verify`
  - **프론트 흐름**: 학생이 초대 링크를 클릭 → 검증 API 호출 → "연결된 선생님이 맞는지" 확인 화면 표시
  - **검증 조건**:
    - 초대 상태: PENDING
    - 만료 안 됨: `expiredAt > now`
    - 사용 가능: `useCount < maxUses(1)`
    - StudentProfile 연결 가능: `studentProfileId != null`, `StudentProfile.memberId = null`, `StudentProfile.active = true`
  - **응답**: `InvitationVerifyResponse`
    - `inviterId`: 초대한 사람의 ID (Teacher 또는 Assistant)
    - `inviterName`: 초대한 사람의 이름
    - `inviteeRole`: 초대 대상 역할 (STUDENT)
    - `expiresAt`: 만료 시각
    - `studentProfile`: StudentProfile 요약 정보 (id, name, age, grade 등) - 가입자가 "본인이 맞는지" 확인
- **가입**: `POST /api/v1/auth/register/invited`
  - **프론트 흐름**: 확인 버튼 클릭 → 회원가입 화면 이동 → 이메일/비밀번호/이름 입력 → 가입 요청
  - **Request Body**: `{ code, email, password, name }`
    - `email`: targetEmail이 있으면 pre-fill, 없으면 사용자 입력
  - **처리 로직**:
    1. 초대 코드 재검증 (PENDING, 만료 안 됨, 사용 가능)
    2. 이메일 검증:
       - `targetEmail`이 있으면: 입력한 이메일과 일치해야 함 (대소문자 무시)
       - `targetEmail`이 없으면: 이메일 검증 건너뜀 (단, 기존 회원 중복 체크는 수행)
    3. Member 생성:
       - `role = STUDENT`
       - `teacherId = StudentProfile.teacherId` (StudentProfile에서 가져옴)
       - 비밀번호 BCrypt 해싱
    4. StudentProfile 연결:
       - `StudentProfile.memberId = 생성된 Member.id`
       - 이미 memberId가 있으면 예외 (한 번만 연결 가능)
    5. 초대 상태 업데이트:
       - `useCount += 1`
       - `useCount >= maxUses(1)`이면 `status = ACCEPTED`
  - **응답**: `AuthTokens` (Access/Refresh 토큰) - 가입 즉시 로그인

#### 조교 초대 (Teacher)

- **대상**: Teacher 전용. 조교 초대 링크는 공용/무제한 사용.
- **생성/회전**: `POST /api/v1/invitations/assistant/link`
  - **프론트 흐름**: Teacher가 조교 초대 페이지 → "조교 초대 링크 생성" 버튼 클릭 → 새 링크 생성
  - **처리 로직**:
    1. 기존 활성 조교 초대 자동 REVOKE:
       - 같은 Teacher의 PENDING 상태 조교 초대를 모두 찾아서 REVOKED로 전환
       - Teacher당 활성 조교 초대는 1개만 유지
    2. 새 초대 코드 생성:
       - `inviteeRole = ASSISTANT`
       - `studentProfileId = null` (조교는 StudentProfile과 무관)
       - `targetEmail = null` (공용 링크, 이메일 없음)
       - `expiredAt = now + 10년` (사실상 무제한, DB 제약상 null 불가)
       - `maxUses = -1` (무제한)
       - `useCount = 0`
  - **응답**: `InvitationResponse` (새로 생성된 초대 링크)
- **목록 조회**: `GET /api/v1/invitations/assistant?status=`
  - 최신 활성 코드(PENDING)와 과거 코드(REVOKED, EXPIRED) 목록 반환
  - 응답: 초대 코드, 상태, 만료 시각, useCount (누적 사용 횟수)
- **수동 취소**: `DELETE /api/v1/invitations/{code}`
  - Teacher가 조교 초대 링크를 수동으로 회수 가능
  - PENDING → REVOKED
- **검증/가입 흐름**:
  - **검증**: `POST /api/v1/auth/invitations/verify`
    - **프론트 흐름**: 조교가 초대 링크 클릭 → 검증 → "연결된 선생님이 맞는지" 확인
    - 조건: PENDING, 만료 안 됨, `maxUses = -1` (무제한이므로 useCount 무시)
    - 응답: inviterId(Teacher), inviterName, inviteeRole(ASSISTANT), expiresAt
  - **가입**: `POST /api/v1/auth/register/invited`
    - **프론트 흐름**: 확인 → 회원가입 화면 → 이메일/비밀번호/이름 입력 → 가입
    - Request: `{ code, email, password, name }` (targetEmail이 없으므로 사용자가 직접 입력)
    - 처리 로직:
      1. 초대 코드 재검증
      2. 이메일 중복 체크 (기존 회원)
      3. Member 생성: `role = ASSISTANT`, `teacherId = senderId` (초대한 Teacher)
      4. 초대 상태 업데이트:
         - `useCount += 1` (사용 횟수 누적)
         - `maxUses = -1`이므로 `status = PENDING` 유지 (재사용 허용)
    - 응답: `AuthTokens` (가입 즉시 로그인)

#### 공통 정책

- **상태 관리**:
  - PENDING: 사용 가능한 초대
  - ACCEPTED: 사용 완료된 초대 (학생 초대, maxUses=1에 도달)
  - REVOKED: 수동 취소 또는 자동 회전으로 무효화된 초대
  - EXPIRED: 만료 시각 지난 초대
  - 상태 전환:
    - 학생 초대: useCount가 maxUses(1)에 도달하면 PENDING → ACCEPTED
    - 조교 초대: maxUses=-1이므로 항상 PENDING 유지 (useCount만 누적)
- **만료 정책**:
  - 학생 초대: `expiredAt = now + 7일` (기본)
  - 조교 초대: `expiredAt = now + 10년` (사실상 무제한, DB 제약상 null 불가)
  - 목록 조회 시 `expiredAt < now`인 PENDING 초대를 자동으로 EXPIRED로 전환
- **중복 방지**:
  - 학생 초대:
    - 동일 StudentProfile에 PENDING 초대가 있으면 새 초대 생성 불가
    - 회전 필요 시: 기존 초대 REVOKE → 새 초대 생성
    - targetEmail 제공 시: PENDING 초대 중복 + 기존 회원 이메일 중복 차단
  - 조교 초대:
    - Teacher당 활성 PENDING 초대는 1개만 유지
    - 새 링크 생성 시 기존 PENDING 초대 자동 REVOKE
- **보안**:
  - 초대 코드는 UUID 랜덤 생성 (추측 불가)
  - verify/register 모두에서 초대 유효성 재검증:
    - 상태(PENDING), 만료(expiredAt), 사용 가능 횟수(useCount < maxUses)
    - 학생 초대: StudentProfile 연결 가능 여부(`memberId=null`, `active=true`)
  - 권한 검증:
    - 학생 초대 생성: Teacher는 teacherId, Assistant는 assistantId 확인
    - 조교 초대 생성: Teacher만 가능

### Non-functional

- Controller 경로는 `/api/v1/invitations/**`, `/api/v1/auth/**` 유지. Role 기반 `@PreAuthorize` 적용.
- 서비스에서 StudentProfile 소유/담당자 검증을 수행해 데이터 누수 방지.
- 응답 DTO에 StudentProfile 요약을 포함해 프런트 목록/확인용 데이터를 한 번에 전달.
- `expiredAt` null 허용 여부는 DB 스키마를 고려해 결정; null 불가 시 장기 만료 시각 상수(`NO_EXPIRY`) 사용.
- 기존 코드는 이메일 기반 단일 초대에 맞춰져 있으므로, 호환성 영향 범위를 테스트로 증명한다.

## 3. API Design (Draft)

| 목적                     | Method | URL                                      | 권한              | Request                                                      | Response                                                                                             | 비고                                       |
| ------------------------ | ------ | ---------------------------------------- | ----------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| 학생 초대 후보 조회      | GET    | `/api/v1/invitations/student/candidates` | TEACHER/ASSISTANT | query: `name?`, `assistantId?`                               | `List<StudentProfileSummary>` (id, name, phoneNumber, age, grade, assistantId)                       | memberId=null, active=true, 미초대만 반환  |
| 학생 초대 배치 생성      | POST   | `/api/v1/invitations/student/batch`      | TEACHER/ASSISTANT | `{ invites: [{ studentProfileId, targetEmail? }] }`          | `List<InvitationResponse>` (code, studentProfile 정보, targetEmail, useCount, maxUses, expiredAt)    | 여러 학생 한 번에 초대                     |
| 학생 초대 목록           | GET    | `/api/v1/invitations/student?status=`    | TEACHER/ASSISTANT | query: `status?` (PENDING/ACCEPTED/REVOKED/EXPIRED)         | `List<InvitationResponse>`                                                                           | 만료된 초대 자동 EXPIRED 전환              |
| 초대 취소                | DELETE | `/api/v1/invitations/{code}`             | TEACHER/ASSISTANT | path: `code`                                                 | `RsData<Void>`                                                                                       | sender만 가능, PENDING만 REVOKED           |
| 조교 초대 링크 생성/회전 | POST   | `/api/v1/invitations/assistant/link`     | TEACHER           | -                                                            | `InvitationResponse` (code, useCount=0, maxUses=-1, expiredAt, targetEmail=null)                     | 기존 PENDING 자동 REVOKE                   |
| 조교 초대 목록           | GET    | `/api/v1/invitations/assistant?status=`  | TEACHER           | query: `status?`                                             | `List<InvitationResponse>`                                                                           | 최신 활성 + 과거 코드                      |
| 초대 검증                | POST   | `/api/v1/auth/invitations/verify`        | 공개 (인증 불필요) | `{ code }`                                                   | `{ inviterId, inviterName, inviteeRole, expiresAt, studentProfile? }` (studentProfile는 학생 초대만) | 링크 클릭 후 "선생님 확인" 화면용          |
| 초대 가입                | POST   | `/api/v1/auth/register/invited`          | 공개 (인증 불필요) | `{ code, email, password, name }`                            | `AuthTokens` (accessToken, refreshToken)                                                             | 가입 즉시 로그인, StudentProfile 연결 처리 |

## 4. Domain Model (Draft)

### 4.1 Invitation 엔티티 변경

**현재 상태**:
- `targetEmail`: `nullable = false` → **nullable = true**로 변경 필요 (조교 초대 링크는 이메일 없음)
- 누락 필드: `useCount`, `maxUses`

**변경 사항**:
- **필드 추가**:
  - `useCount`: `int`, 기본값 0 (사용 횟수 누적)
  - `maxUses`: `Integer`, nullable (null은 무제한, 1은 1회 사용, -1로 표현할 수도 있음)
- **필드 수정**:
  - `targetEmail`: `nullable = true`로 변경
- **메서드 추가**:
  - `canUse(LocalDateTime now)`: PENDING, 만료 안 됨, useCount < maxUses 확인 (maxUses=-1이면 항상 true)
  - `increaseUseCount()`: useCount += 1
  - `acceptIfLimitReached()`: useCount >= maxUses이면 status = ACCEPTED (maxUses=-1이면 건너뜀)
  - `expireIfPast(LocalDateTime now)`: 기존 메서드 유지
  - `revoke()`: 기존 메서드 유지

**제약 조건**:
- 학생 초대: `studentProfileId != null`, `maxUses = 1`, `targetEmail` optional
- 조교 초대: `studentProfileId = null`, `maxUses = -1`, `targetEmail = null`

### 4.2 Service 계층

#### InvitationService

- **`findStudentCandidates(senderId, name, assistantId)`**:
  - Teacher: `teacherId=senderId`, `memberId=null`, `active=true`, PENDING 초대 없음
  - Assistant: `assistantId=senderId`, `memberId=null`, `active=true`, PENDING 초대 없음
  - 이름 필터링 + 조교 필터링 지원
  - 반환: `List<StudentProfileSummary>`

- **`createStudentInvitationBatch(senderId, requests)`**:
  - 요청: `List<{ studentProfileId, targetEmail? }>`
  - 각 StudentProfile별 검증:
    1. StudentProfile 존재 확인
    2. 소유/담당 권한 확인 (Teacher: teacherId, Assistant: assistantId)
    3. `active=true`, `memberId=null` 확인
    4. PENDING 초대 중복 확인
    5. targetEmail이 있으면 이메일 중복 확인 (PENDING 초대 + 기존 회원)
  - 초대 생성: code 생성, expiredAt=now+7일, maxUses=1, useCount=0
  - 반환: `List<InvitationResponse>` (studentProfile 정보 포함)

- **`createAssistantLink(senderId)`**:
  - Teacher 권한 확인
  - 기존 PENDING 조교 초대 찾아서 모두 REVOKE
  - 새 초대 생성: inviteeRole=ASSISTANT, targetEmail=null, studentProfileId=null, maxUses=-1, expiredAt=now+10년
  - 반환: `InvitationResponse`

- **`listInvitations(senderId, role, status)`**:
  - 기존 로직 유지, 만료된 초대 자동 EXPIRED 전환
  - 반환: `List<InvitationResponse>` (useCount, maxUses 포함)

- **`revokeInvitation(senderId, code)`**:
  - 기존 로직 유지 (sender 확인, PENDING만 REVOKED)

#### InvitationAuthService

- **`verify(code)`**:
  - 현재 로직 확장:
    - `canUse(now)` 체크 (PENDING, 만료 안 됨, useCount < maxUses)
    - 학생 초대: StudentProfile `memberId=null`, `active=true` 확인
  - 응답에 `studentProfile` 추가 (학생 초대인 경우만, 조교 초대는 null)
  - 반환: `InvitationVerifyResponse { inviterId, inviterName, inviteeRole, expiresAt, studentProfile? }`

- **`registerInvited(request)`**:
  - 현재 로직 확장:
    1. 초대 코드 재검증 (`canUse(now)`)
    2. 이메일 검증:
       - `targetEmail != null`이면 입력 이메일과 일치 확인 (대소문자 무시)
       - `targetEmail = null`이면 건너뜀
       - 기존 회원 이메일 중복 체크는 항상 수행
    3. Member 생성:
       - 학생: `role=STUDENT`, `teacherId=StudentProfile.teacherId`
       - 조교: `role=ASSISTANT`, `teacherId=senderId`
    4. StudentProfile 연결 (학생 초대만):
       - `StudentProfile.memberId = 생성된 Member.id`
       - 이미 memberId가 있으면 예외
    5. 초대 상태 업데이트:
       - `increaseUseCount()`
       - `acceptIfLimitReached()` (학생 초대는 ACCEPTED, 조교 초대는 PENDING 유지)
  - 반환: `AuthTokens` (가입 즉시 로그인)

### 4.3 DTO

- **`StudentInvitationBatchRequest`**:
  ```java
  record StudentInvitationBatchRequest(
      List<StudentInviteTarget> invites
  ) {}

  record StudentInviteTarget(
      UUID studentProfileId,
      String targetEmail  // optional, null 가능
  ) {}
  ```

- **`InvitationResponse` 확장**:
  - 추가 필드: `studentProfileId?`, `studentProfileName?`, `useCount`, `maxUses`
  - 기존: `code`, `targetEmail`, `inviteeRole`, `status`, `expiredAt`, `createdAt`

- **`InvitationVerifyResponse` 확장**:
  - 추가: `studentProfile?` (학생 초대만, `StudentProfileSummary` 타입)
  - 기존: `inviterId`, `inviterName`, `inviteeRole`, `expiresAt`

### 4.4 Repository

#### InvitationRepository

- **기존 유지**:
  - `findByCode(code)`
  - `findByCodeAndSenderId(code, senderId)`
  - `findAllBySenderIdAndInviteeRoleAndStatusIn(senderId, role, statuses)`
  - `existsByTargetEmailIgnoreCaseAndInviteeRoleAndStatusIn(email, role, statuses)`

- **추가 메서드**:
  - `existsByStudentProfileIdAndStatusIn(studentProfileId, List<InvitationStatus>)`
  - `findAllBySenderIdAndInviteeRoleAndStatus(senderId, InvitationRole.ASSISTANT, InvitationStatus.PENDING)`
    - 조교 초대 회전 시 기존 PENDING 찾기용

#### StudentProfileRepository

- **추가 메서드**:
  - `findAllByTeacherIdAndMemberIdIsNullAndActiveTrue(teacherId)`
  - `findAllByAssistantIdAndMemberIdIsNullAndActiveTrue(assistantId)`
  - `findAllByTeacherIdAndMemberIdIsNullAndActiveTrueAndNameContainingIgnoreCase(teacherId, name)`
  - `findAllByAssistantIdAndMemberIdIsNullAndActiveTrueAndNameContainingIgnoreCase(assistantId, name)`
  - 조교 필터링 지원 (Teacher가 특정 조교의 학생만 보기):
    - `findAllByTeacherIdAndAssistantIdAndMemberIdIsNullAndActiveTrue(teacherId, assistantId)`

## 5. TDD Plan

### 5.1 Invitation 엔티티 메서드 테스트

- **`canUse(now)`**:
  - PENDING + 만료 안 됨 + useCount < maxUses → true
  - REVOKED/ACCEPTED/EXPIRED → false
  - 만료된 경우 → false
  - maxUses=-1 (무제한) + PENDING + 만료 안 됨 → true (useCount 무시)
- **`increaseUseCount()`**: useCount 1 증가 확인
- **`acceptIfLimitReached()`**:
  - useCount=1, maxUses=1 → status=ACCEPTED
  - useCount=2, maxUses=-1 → status=PENDING (무제한)

### 5.2 InvitationService 단위 테스트

#### findStudentCandidates
- Teacher: 자신의 teacherId인 StudentProfile만 반환 (memberId=null, active=true, 미초대)
- Assistant: 자신의 assistantId인 StudentProfile만 반환
- 이름 검색 필터링 동작 확인
- 조교 필터링 동작 확인 (Teacher가 assistantId 파라미터 전달)
- PENDING 초대가 있는 StudentProfile은 제외

#### createStudentInvitationBatch
- Teacher가 자신의 StudentProfile 배치 초대 성공
- Assistant가 자신이 담당하는 StudentProfile 배치 초대 성공
- 권한 없는 StudentProfile 초대 시도 → 예외
- memberId가 이미 있는 StudentProfile 초대 → 예외
- active=false인 StudentProfile 초대 → 예외
- 이미 PENDING 초대가 있는 StudentProfile 재초대 → 예외
- targetEmail 제공 시 기존 회원 이메일 중복 → 예외
- targetEmail 제공 시 다른 PENDING 초대 중복 → 예외
- targetEmail 없이 초대 생성 성공 (targetEmail=null)
- 생성된 초대의 maxUses=1, useCount=0, expiredAt=now+7일 확인

#### createAssistantLink
- Teacher만 생성 가능 (Assistant 시도 → 예외)
- 기존 PENDING 조교 초대 자동 REVOKE 확인
- 새 초대: targetEmail=null, studentProfileId=null, maxUses=-1, expiredAt=now+10년 확인

#### listInvitations
- 만료된 PENDING 초대 자동 EXPIRED 전환 확인
- status 필터링 동작 확인

#### revokeInvitation
- sender만 취소 가능 (다른 사용자 시도 → 예외)
- PENDING만 REVOKED로 전환 (ACCEPTED/REVOKED 재시도 → 예외)

### 5.3 InvitationAuthService 단위 테스트

#### verify
- 정상 학생 초대: studentProfile 정보 포함하여 반환
- 정상 조교 초대: studentProfile=null 반환
- REVOKED/ACCEPTED/EXPIRED 초대 → 예외
- 만료된 초대 → 예외
- useCount >= maxUses인 초대 → 예외
- StudentProfile.memberId가 이미 있는 학생 초대 → 예외

#### registerInvited
- **학생 초대 (targetEmail 있음)**:
  - 이메일 일치 → Member 생성 + StudentProfile.memberId 설정 + useCount 증가 + ACCEPTED
  - 이메일 불일치 → 예외
- **학생 초대 (targetEmail 없음)**:
  - 임의 이메일로 가입 → Member 생성 + StudentProfile.memberId 설정 + useCount 증가 + ACCEPTED
- **조교 초대 (targetEmail 없음)**:
  - 임의 이메일로 가입 → Member 생성 + useCount 증가 + PENDING 유지 (무제한)
  - 동일 링크로 2회 가입 → 두 번 모두 성공, useCount=2, status=PENDING
- **이미 memberId가 있는 StudentProfile** → 예외
- **기존 회원 이메일 중복** → 예외
- **REVOKED/EXPIRED 초대로 가입 시도** → 예외

### 5.4 Controller (MockMvc) 테스트

#### GET /api/v1/invitations/student/candidates
- Teacher/Assistant 접근 허용, 다른 역할 403
- name, assistantId 파라미터 동작 확인
- 권한별 반환 데이터 확인 (Teacher vs Assistant)

#### POST /api/v1/invitations/student/batch
- Teacher/Assistant 접근 허용
- 유효한 요청 → 201 Created + 초대 목록 반환
- 잘못된 studentProfileId → 400 또는 404
- 권한 없는 StudentProfile → 403
- targetEmail 중복 → 409

#### POST /api/v1/invitations/assistant/link
- Teacher만 접근 허용, Assistant 403
- 기존 PENDING 자동 REVOKE 확인
- 201 Created + 새 초대 반환

#### GET /api/v1/invitations/student, /assistant
- 권한별 접근 제어
- status 필터링 동작 확인
- 만료된 초대 자동 EXPIRED 전환 확인

#### DELETE /api/v1/invitations/{code}
- sender만 취소 가능 (다른 사용자 403)
- PENDING만 REVOKED (이미 ACCEPTED/REVOKED 시 400)

#### POST /api/v1/auth/invitations/verify
- 유효한 code → 200 + verify response (studentProfile 포함/미포함)
- 잘못된 code → 400

#### POST /api/v1/auth/register/invited
- 유효한 요청 → 201 + AuthTokens
- 이메일 불일치 → 400
- 이미 사용된 초대 → 400
- 기존 회원 이메일 → 409

### 5.5 통합 시나리오 (E2E)

#### 학생 초대 흐름 (targetEmail 있음)
1. Teacher가 StudentProfile 생성 (memberId=null)
2. Teacher가 학생 초대 배치 생성 (targetEmail 포함)
3. 초대 목록 조회 → PENDING 확인
4. verify 호출 → studentProfile 정보 반환
5. register 호출 (targetEmail과 일치하는 이메일)
6. Member 생성, StudentProfile.memberId 업데이트 확인
7. 초대 상태 ACCEPTED, useCount=1 확인
8. 동일 code로 재가입 시도 → 실패

#### 학생 초대 흐름 (targetEmail 없음)
1. Teacher가 StudentProfile 생성
2. Teacher가 학생 초대 배치 생성 (targetEmail=null)
3. verify 호출 → studentProfile 정보 반환
4. register 호출 (임의 이메일 입력)
5. Member 생성, StudentProfile.memberId 업데이트 확인
6. 초대 상태 ACCEPTED

#### 조교 초대 흐름 (무제한)
1. Teacher가 조교 초대 링크 생성
2. verify 호출 → inviterId, inviterName 반환
3. 첫 번째 가입 (이메일 A) → Member 생성, useCount=1, status=PENDING
4. 두 번째 가입 (이메일 B) → Member 생성, useCount=2, status=PENDING
5. 조교 목록 조회 → useCount=2 확인

#### 조교 초대 회전
1. Teacher가 조교 초대 링크 생성 (code1)
2. Teacher가 다시 링크 생성 (code2)
3. code1 상태 REVOKED 확인
4. code2로 가입 성공, code1로 가입 시도 → 실패

---

## 6. 구현 순서 (권장)

### Phase 1: DB 스키마 변경
1. Invitation 엔티티 수정:
   - `targetEmail` nullable=true
   - `useCount`, `maxUses` 필드 추가
   - 메서드 추가 (`canUse`, `increaseUseCount`, `acceptIfLimitReached`)
2. 마이그레이션 스크립트 작성 (또는 JPA auto-update 사용)

### Phase 2: 학생 초대 후보 조회
1. StudentProfileRepository 메서드 추가
2. InvitationService.findStudentCandidates() 구현
3. Controller GET /api/v1/invitations/student/candidates 구현
4. 단위 테스트 + MockMvc 테스트

### Phase 3: 학생 초대 배치 생성
1. DTO 추가 (StudentInvitationBatchRequest, StudentInviteTarget)
2. InvitationService.createStudentInvitationBatch() 구현
3. Controller POST /api/v1/invitations/student/batch 구현
4. 단위 테스트 + MockMvc 테스트

### Phase 4: 조교 초대 링크 생성/회전
1. InvitationService.createAssistantLink() 구현
2. Controller POST /api/v1/invitations/assistant/link 구현
3. 단위 테스트 + MockMvc 테스트

### Phase 5: 초대 검증/가입 로직 확장
1. InvitationVerifyResponse에 studentProfile 필드 추가
2. InvitationAuthService.verify() 확장 (studentProfile 포함)
3. InvitationAuthService.registerInvited() 확장:
   - targetEmail optional 처리
   - StudentProfile.memberId 설정
   - useCount 증가 + acceptIfLimitReached 호출
4. 단위 테스트

### Phase 6: InvitationResponse 확장
1. InvitationResponse에 useCount, maxUses, studentProfile 정보 추가
2. 기존 목록 API 응답 확인

### Phase 7: 통합 테스트
1. E2E 시나리오 작성 (학생 초대 with/without targetEmail, 조교 초대 무제한)
2. 전체 흐름 검증

### Phase 8: 문서화
1. OpenAPI 스키마 업데이트
2. AGENT_LOG 기록
