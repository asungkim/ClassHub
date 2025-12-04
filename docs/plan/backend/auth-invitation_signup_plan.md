# Feature: 초대 코드 검증 & 초대 기반 회원가입

## 1. Problem Definition
- Invitation 엔티티/상태 전환은 존재하지만, 실제로 초대 코드를 검증하거나 초대받은 사용자가 회원가입을 완료할 API가 없다.
- Teacher→Assistant, Assistant→Student 초대 흐름을 완성하려면 초대 코드 확인, 역할 제한, 가입 후 상태 전환을 처리해야 한다.

## 2. Requirements

### Functional
- `POST /api/v1/auth/invitations/verify`
  - Request: `{ "code": "string" }`
  - Flow:
    1. InvitationRepository에서 code 조회.
    2. 상태가 `PENDING`이고 만료 시간이 지나지 않았는지 확인.
    3. Response: `RsData<InvitationVerifyResponse>` (senderName, inviteeRole, expiresAt 등).
  - 실패 시: 존재하지 않거나 만료/취소 시 `RsCode.INVALID_INVITATION`(신규) 반환.
- `POST /api/v1/auth/register/invited`
  - Request: `{ "code": "", "email": "", "password": "", "name": "" }`
  - Flow:
    1. 위 verify 로직 재사용.
    2. Invitation role에 따라 Member 생성 (Assistant/Student).
    3. teacherId/초대한 memberId 관계 매핑.
    4. Invitation 상태 `ACCEPTED`, acceptedAt 기록.
    5. Access/Refresh 토큰 발급(optional) → Teacher와 동일한 LoginResponse 재사용.
  - Validation: 이메일/비밀번호 규칙, 이미 가입된 이메일이면 `DUPLICATE_EMAIL`.
- 초대 만료 처리:
  - 초대 생성 시 `expiresAt`이 필수. 만료 시간은 Invitation 엔티티에 이미 존재한다고 가정, 검증은 API에서 수행.
- 초대 취소(`REVOKED`), 이미 사용(ACCEPTED) 상태의 코드는 모두 실패 응답.

### Non-functional
- InvitationRepository에 `findByCode` + 상태 조건을 위한 쿼리 메서드 추가.
- 초대 검증 로직을 Service 레이어(`InvitationAuthService`)로 분리해 Controller에서 재사용.
- 이메일 비교 시 소문자 normalize, 비밀번호는 기존 PasswordEncoder 사용.
- 실패 로그에 code 전체를 남기지 않고 일부만 마스킹.

## 3. API Design (Draft)
| Method | URL | Request | Response | Notes |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/invitations/verify` | `{ code }` | `RsData<{ inviterName, inviteeRole, expiresAt }>` | 단순 검증, 상태 반환 |
| POST | `/api/v1/auth/register/invited` | `{ code, email, password, name }` | `RsData<LoginResponse>` | 성공 시 Member 생성 + Invitation ACCEPTED |

## 4. Domain Model (Draft)
- DTO
  - `InvitationVerifyRequest`
  - `InvitationVerifyResponse(inviterName, inviteeRole, expiresAt)`
  - `InvitationRegisterRequest(code, email, password, name)`
- Service
  - `InvitationAuthService.verify(code)`
  - `InvitationAuthService.registerInvited(request)` → Member + Invitation 상태 전환 + 토큰 발급
- Repository
  - `InvitationRepository.findByCode(String code)`
  - 필요 시 `existsByCodeAndStatus` 등 헬퍼 추가
- Invitation 엔티티
  - `boolean isExpired()` 헬퍼 메서드 추가 고려

## 5. TDD Plan
1. **Service 테스트**
   - 유효 코드 검증 성공/만료/취소/없는 코드 실패.
   - registerInvited: Member 생성, Invitation 상태 ACCEPTED, Teacher/Assistant 관계 필드 확인.
   - 중복 이메일, 만료 코드 등 예외 시 `BusinessException` 발생 검증.
2. **Controller 테스트 (MockMvc)**
   - `/auth/invitations/verify`: 성공 200, 실패 400/409(정책에 따라) 확인.
   - `/auth/register/invited`: Validation 실패(잘못된 이메일/비밀번호) 400, 성공 시 LoginResponse 구조, 실패 시 코드 확인.
3. **Repository/엔티티 헬퍼 테스트**
   - `InvitationRepository.findByCode`가 상태/만료 필터링을 지원하는지 검증.
   - Invitation의 상태 전환 메서드(accept/revoke) 테스트 업데이트.
