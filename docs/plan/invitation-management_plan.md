# Feature: Invitation 생성/관리 API

## 1. Problem Definition

- Teacher가 조교나 학생을, Assistant가 학생을 초대하는 기능은 Requirement/Spec에 명시되어 있지만, 실제 초대 코드 생성/목록/취소 API가 없다.
- 현재는 초대 코드 기반 가입만 가능하며, 초대 자체를 발급하거나 관리할 수 없으므로 Auth 흐름이 완성되지 않는다.

## 2. Requirements

### Functional

1. **Teacher → Assistant 초대**

   - `POST /api/v1/invitations/assistant`
     - Request: `{ targetEmail }` (email만 입력)
     - Constraints: sender role = TEACHER, targetEmail 미가입/미초대 상태인지 확인.
     - Response: `{ code, inviteeRole=ASSISTANT, expiresAt(시스템 기본값), status }`
   - `GET /api/v1/invitations/assistant`
     - Teacher가 생성한 초대 목록 (PENDING/ACCEPTED/EXPIRED/REVOKED 필터).
   - `DELETE /api/v1/invitations/{code}` or `POST /{code}/revoke`
     - 취소 → status=REVOKED (PENDING일 때만).

2. **Teacher/Assistant → Student 초대**

   - `POST /api/v1/invitations/student`
     - Request: `{ targetEmail, studentProfileId }`
     - Constraints: sender role = TEACHER 또는 ASSISTANT.  
       - TEACHER는 직접 학생 초대 가능.  
       - ASSISTANT는 sender.teacherId가 존재해야 하며, 추후 StudentProfile이 해당 Teacher 소속인지 검증(현재는 입력값만 저장).
     - targetEmail은 UI 입력만 받고 StudentProfile과 이메일 일치 여부는 검사하지 않는다.
   - `GET /api/v1/invitations/student`, revoke API 흐름 동일.

3. **초대 만료 처리**

   - API 입력값으로 만료 기간을 따로 받지 않고, 시스템 기본 값(환경 변수 혹은 7일 등)을 사용.
   - API 호출 시 만료된 초대는 자동 EXPIRED 전환.
   - (옵션) Batch/스케줄러는 추후 작업으로 남기고, repository query에서 expiredAt < now 이면 status 업데이트.

4. **중복/재발급 정책**
   - 동일 이메일에 PENDING 초대가 존재하면 새 초대를 허용할지 여부 → 기본: 기존 초대 cancel 후 새 초대 허용.
   - Teacher 자신(본인) 초대 금지, Assistant는 자신과 동일 teacherId가 있어야 student 초대 가능 등 권한 체크 필요.

### Non-functional

- Controller는 `/api/v1/invitations/**` prefix 사용, Role 기반 접근 제어.
- InvitationService에 비즈니스 로직(생성, revoke, list, expire)을 모듈화.
- 이벤트/메일 전송 Hook는 별도 TODO로 남김(현재는 코드를 응답으로 돌려주는 수준).
- 만료 시간을 Config에서 기본값(예: 7일)로 설정 가능하게 한다.

## 3. API Design (Draft)

| Method      | URL                                 | Request                          | Response                | Notes                    |
| ----------- | ----------------------------------- | -------------------------------- | ----------------------- | ------------------------ |
| POST        | `/api/v1/invitations/assistant`     | targetEmail                      | InvitationSummary       | TEACHER 권한             |
| GET         | `/api/v1/invitations/assistant`     | query: status                    | List<InvitationSummary> | TEACHER 권한             |
| POST/DELETE | `/api/v1/invitations/{code}/revoke` | -                                | 업데이트된 status       | sender만 가능            |
| POST        | `/api/v1/invitations/student`       | targetEmail, studentProfileId    | InvitationSummary       | TEACHER/ASSISTANT 권한   |
| GET         | `/api/v1/invitations/student`       | query: status                    | List<InvitationSummary> | TEACHER/ASSISTANT 권한   |

`InvitationSummary` 예시: `{ code, targetEmail, inviteeRole, status, expiresAt, createdAt }`

## 4. Domain Model (Draft)

- `InvitationService`
  - `createAssistantInvitation(Member sender, InvitationCreateRequest request)`
  - `createStudentInvitation(Member sender, InvitationCreateRequest request)`
  - `listInvitations(Member sender, InvitationRole role, InvitationStatus status?)`
  - `revokeInvitation(Member sender, String code)`
  - `expireIfNeeded()` helper (repository-level)
- DTO
  - `InvitationCreateRequest` (targetEmail, expiresAt?, studentProfileId?)
  - `InvitationSummaryResponse`
  - `InvitationListResponse`
- Repository
  - `findAllBySenderIdAndInviteeRoleAndStatusIn(...)`
  - `existsByTargetEmailAndStatus(...)`
  - `findByCodeAndSenderId(...)`

## 5. TDD Plan

1. **Service 단위 테스트**
   - Teacher/Assistant 권한 체크, 중복 초대 방지, revoke 동작, 만료 시 EXPIRED 처리.
   - Student 초대 시 sender.teacherId가 null이면 실패 등 권한/관계 검사.
2. **Controller 테스트**
   - Role 기반 접근 제어 (TEACHER/ASSISTANT).
   - 생성/목록/취소 정상 및 Validation 실패 케이스.
3. **Repository 테스트**
   - `existsByTargetEmailAndStatus` 동작, `findAllBySenderId` 필터.
4. **Integration 고려**
   - 초대 생성 후 verify/register API와 이어지는 시나리오는 E2E 테스트에서 검증 (추후 초대 프론트 PoC 작업과 연결).
