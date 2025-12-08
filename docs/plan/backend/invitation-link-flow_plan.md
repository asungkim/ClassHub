# Feature: 초대 링크 재설계 (조교/학생)

## 1. Problem Definition

- 기존 초대 로직은 이메일 기반 단일 사용 흐름만 지원해 학생/조교 UI 요구사항과 맞지 않는다.
  - 조교 초대 링크는 공용·무기한으로 여러 번 사용되어야 하나, 현재는 7일 만료 + 1회 사용 후 ACCEPTED 상태가 되어 막힌다.
  - 학생 초대는 StudentProfile과 연동되어야 하지만, 현재는 studentProfileId 검증/연결 없이 이메일만 비교하고 가입 후에도 StudentProfile.memberId를 채우지 않는다.
  - 학생 초대 화면에서 “아직 초대되지 않은 StudentProfile 목록”과 기존 초대 목록을 구분해 보여줄 수 있는 API가 없다.
  - 초대 검증 응답이 초대 대상(StudentProfile 이름 등)을 포함하지 않아 가입자 확인 UX가 부족하다.

## 2. Requirements

### Functional

#### 학생 초대 (Teacher/Assistant)

- 대상: Teacher 또는 Teacher에 소속된 Assistant.
- 후보 목록: `memberId=null`이고 `active=true`이며, 해당 Teacher 소유 + (Assistant라면) 자신이 담당(`assistantId=본인`)인 StudentProfile만 노출. 이미 PENDING 초대가 있는 StudentProfile은 제외.
- 생성: 여러 학생을 한 번에 초대할 수 있도록 요청 본문에 배열로 받는다.
  - Body 예시: `{ "invites": [ { "studentProfileId": "UUID", "targetEmail": "optional" }, ... ] }`
  - 각 StudentProfile은 유효성 검증(소유/담당/활성/미연결/미초대) 후 초대 코드 생성.
  - `expiredAt = now + 7일`, `maxUses = 1`, `useCount = 0`.
  - `targetEmail`이 있으면 이메일 중복/기존 회원 여부 검사. 없으면 링크만 생성(가입 시 이메일 입력).
- 목록: `GET /api/v1/invitations/student`는 초대 코드, 상태, 만료, StudentProfile(id, name, assistantId), targetEmail, useCount를 반환. 응답 전에 만료된 초대를 EXPIRED로 갱신.
- 취소: `DELETE /api/v1/invitations/{code}` → sender만 가능, PENDING일 때만 REVOKED.

#### 학생 초대 검증/가입

- 검증(`POST /api/v1/auth/invitations/verify`):
  - 상태: PENDING, 만료 안 됨, `useCount < maxUses(1)`, StudentProfile이 여전히 `memberId=null`이고 isActive=true.
  - 응답: 초대자(Teacher) 이름/id, 역할(STUDENT), 만료 시각, StudentProfile 이름/id를 포함해 가입자가 확인 가능하게 한다.
- 가입(`POST /api/v1/auth/register/invited`):
  - targetEmail이 존재할 때만 이메일 일치 검사; 없으면 건너뜀.
  - Member.role=STUDENT, Member.teacherId=StudentProfile.teacherId로 설정.
  - StudentProfile.memberId에 새 Member.id를 저장(한 번만 가능).
  - useCount 증가 후 maxUses(1)에 도달하면 status=ACCEPTED로 전환해 재사용 차단.

#### 조교 초대 (Teacher)

- Teacher 전용. 조교 초대 링크는 공용/무제한 사용.
- 생성/회전: `POST /api/v1/invitations/assistant/link` (기존 /assistant를 회전용으로 사용해도 되나, 기능 명확화를 위해 별도 엔드포인트 가정)
  - 현재 ACTIVE/PENDING 조교 초대는 REVOKED 처리 후 새 코드 생성.
  - `expiredAt`은 사실상 무제한(예: now + 10년) 또는 nullable 정책으로 처리, `maxUses = -1`(무제한), `useCount`는 누적 기록.
- 조회: `GET /api/v1/invitations/assistant`는 최신 활성 코드와 과거 코드 목록을 반환(필터 status 지원).
- 취소: `DELETE /api/v1/invitations/{code}`로 수동 회수 가능.
- 가입 흐름: verify/register 시 maxUses=-1이면 status는 PENDING 유지, useCount만 증가하여 반복 사용을 허용. Member.role=ASSISTANT, teacherId=senderId.

#### 공통 정책

- 상태: PENDING/ACCEPTED/REVOKED/EXPIRED 유지. maxUses가 있는 초대는 사용 한도 도달 시 ACCEPTED로 전환, 무제한은 PENDING 유지.
- 만료: Student 초대는 7일 기본, 조교 초대는 무제한. expiredAt이 지난 PENDING 초대는 조회 시 EXPIRED로 전환.
- 중복 방지:
  - 학생: 동일 StudentProfile에 PENDING 초대가 있으면 새 초대 불가(회전 필요 시 기존 초대 REVOKE 후 생성).
  - 조교: Teacher당 활성 초대는 1개만 유지(새로 생성 시 기존 REVOKE).
  - 이메일: targetEmail 제공 시 PENDING 초대 중복 + 기 가입자 이메일 중복 차단.
- 보안: code는 UUID 랜덤, verify/register 모두 code + 상태/만료/사용 가능 횟수/StudentProfile 연결을 재확인.

### Non-functional

- Controller 경로는 `/api/v1/invitations/**`, `/api/v1/auth/**` 유지. Role 기반 `@PreAuthorize` 적용.
- 서비스에서 StudentProfile 소유/담당자 검증을 수행해 데이터 누수 방지.
- 응답 DTO에 StudentProfile 요약을 포함해 프런트 목록/확인용 데이터를 한 번에 전달.
- `expiredAt` null 허용 여부는 DB 스키마를 고려해 결정; null 불가 시 장기 만료 시각 상수(`NO_EXPIRY`) 사용.
- 기존 코드는 이메일 기반 단일 초대에 맞춰져 있으므로, 호환성 영향 범위를 테스트로 증명한다.

## 3. API Design (Draft)

| 목적                     | Method | URL                                      | Request                                                      | Response/비고                                                        |
| ------------------------ | ------ | ---------------------------------------- | ------------------------------------------------------------ | -------------------------------------------------------------------- |
| 학생 초대 후보 조회      | GET    | `/api/v1/invitations/student/candidates` | query: `name?`, `assistantId?`(Teacher가 조교별 필터할 경우) | List<StudentProfileSummary> (memberId=null, active, 미초대)          |
| 학생 초대 생성(배치)     | POST   | `/api/v1/invitations/student`            | `{ invites: [{ studentProfileId, targetEmail? }] }`          | List<InvitationResponse> (studentProfile 정보 포함)                  |
| 학생 초대 목록           | GET    | `/api/v1/invitations/student?status=`    | -                                                            | List<InvitationResponse> (useCount/maxUses 포함)                     |
| 초대 취소                | DELETE | `/api/v1/invitations/{code}`             | -                                                            | PENDING → REVOKED                                                    |
| 조교 초대 링크 생성/회전 | POST   | `/api/v1/invitations/assistant/link`     | -                                                            | InvitationResponse (무제한 링크 코드)                                |
| 조교 초대 목록           | GET    | `/api/v1/invitations/assistant?status=`  | -                                                            | List<InvitationResponse>                                             |
| 초대 검증                | POST   | `/api/v1/auth/invitations/verify`        | `{ code }`                                                   | `{ inviterId, inviterName, inviteeRole, expiresAt, studentProfile }` |
| 초대 가입                | POST   | `/api/v1/auth/register/invited`          | `{ code, email, password, name }`                            | LoginResponse (가입 + 토큰)                                          |

## 4. Domain Model (Draft)

- **Invitation 엔티티**
  - 필드 추가: `useCount(int)`, `maxUses(Integer, -1=무제한)`.
  - 메서드: `canUse(now)`, `increaseUseAndMaybeAccept()`, `expireIfPast(now)`, `revoke()`.
  - Student 초대: `studentProfileId` 필수, maxUses=1; 조교 초대: studentProfileId=null, maxUses=-1.
- **Service**
  - `InvitationService.createStudentInvitations(senderId, requests)` : 다중 생성, StudentProfile 소유/담당/활성/미연결/미초대 검증, 이메일 중복 검사.
  - `InvitationService.createAssistantLink(senderId)` : Teacher 전용, 기존 활성 REVOKE 후 무제한 초대 발급.
  - `InvitationService.listInvitations(senderId, role, status)` : 만료 반영 후 반환.
  - `InvitationService.revokeInvitation(senderId, code)`.
  - `InvitationService.findStudentCandidates(senderId, filters)` : memberId=null + 미초대 StudentProfile 조회.
- **Auth/가입**
  - `InvitationAuthService.verify(code)` : 상태/만료/사용 가능 여부 + StudentProfile 연결 검증, 응답에 StudentProfile 요약 추가.
  - `InvitationAuthService.registerInvited(request)` : targetEmail(optional) 비교, Member 생성, StudentProfile.memberId 설정, useCount/상태 업데이트, 무제한 초대는 status 유지.
- **DTO**
  - `StudentInvitationBatchRequest { List<StudentInviteTarget> invites }`
  - `InvitationResponse` 확장: `studentProfileId`, `studentProfileName`, `useCount`, `maxUses`.
- **Repository**
  - `existsByStudentProfileIdAndStatusIn(...)`
  - `findAllByStudentProfileIdInAndStatusIn(...)`
  - `findByCodeAndInviteeRole(...)` (조교/학생 구분)
  - StudentProfileRepository: `findAllByTeacherIdAndMemberIdIsNullAndActiveTrue(...)`, `...AndAssistantId(...)` 등 후보 조회용 메서드 추가.

## 5. TDD Plan

1. **InvitationService 단위 테스트**
   - Teacher/Assistant 권한 검증(학생 초대), StudentProfile 소유/담당/활성/미연결/미초대 조건 검증.
   - 배치 생성 시 유효한 초대 목록 반환, 중복 StudentProfile이면 예외.
   - 조교 초대 회전: 기존 PENDING 초대 REVOKE, 새 코드 생성, 만료 없음 검증.
2. **InvitationAuthService 단위 테스트**
   - verify: 만료/REVOKED/ACCEPTED/useCount 초과/StudentProfile 이미 연결 시 실패, 정상 시 StudentProfile 정보 포함.
   - register: targetEmail 일치/불일치, StudentProfile.memberId 설정, useCount 증가 및 상태 전환, 무제한 초대 재사용 허용 검증.
3. **Controller(MockMvc)**
   - 권한별 접근 제어(Teacher/Assistant), 요청 검증 실패 시 400, 권한 실패 시 403.
   - 학생 초대 생성/목록/취소, 조교 초대 회전/목록 엔드포인트 성공/실패 케이스.
4. **통합 시나리오(간단 E2E)**
   - Teacher가 StudentProfile 생성 → 학생 초대 발급 → verify → register → StudentProfile.memberId 업데이트 → 초대 상태/사용 횟수 확인.
   - Teacher가 조교 초대 회전 → 초대 링크로 2회 가입 시도 → 두 번 모두 성공(useCount=2, status=PENDING 유지).
