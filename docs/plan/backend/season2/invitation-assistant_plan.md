# Feature: Assistant Invitation & Verification

## 1. Problem Definition
- Phase4 TODO에서 “조교 초대 + verify API 개발”이 미완료 상태다. 현재 RegisterService는 Teacher/Student만 지원하고, 초대 기반 조교 가입/검증 API가 비어있어 Teacher가 조교를 초대할 수 없다.
- `docs/design/final-entity-spec.md`의 INVITATION 스펙(senderId, targetEmail, inviteeRole=ASSISTANT, status, code, expiredAt)이 아직 구현되지 않아 초대 코드 저장/만료/상태 전환 로직이 없다.
- PLAN/TODO 지침상 새 기능은 설계 문서를 통해 요구/도메인/테스트를 확정해야 하므로, Assistant 초대/검증/가입 흐름을 정의해야 한다.

## 2. Requirements

### Functional
1. **엔티티/Repository 구현**  
   - Invitation 엔티티: `senderId`, `targetEmail`, `inviteeRole`(초기에는 ASSISTANT만 사용하지만 확장을 고려해 enum으로 구현), `status (PENDING/ACCEPTED/EXPIRED/REVOKED)`, `code`, `expiredAt`. Spec에 맞춰 unique index 및 soft delete 지원.
   - Repository: `findByCode`, `existsByTargetEmailAndStatus`, 만료/삭제 업데이트 쿼리 등.
2. **초대 생성 API (`POST /api/v1/invitations`)**  
   - Teacher만 호출 가능. 요청: `{ targetEmail, expiredAt(optional) }`. 기본 만료 7일.
   - 중복 초대(같은 targetEmail & 상태 PENDING) 시 `RsCode.INVITATION_ALREADY_EXISTS`.
   - 응답: 초대 코드 + 만료일.
3. **초대 검증 API (`POST /api/v1/auth/invitations/verify`)**  
   - Request: `{ code }`.
   - 검증 항목: 코드 존재 여부, 상태 PENDING, 만료 미도래.  
   - 응답: `senderId`, `senderName`, `expiredAt`.
4. **조교 등록 API (`POST /api/v1/members/register/assistant/invitation`)**  
   - Request: `{ code, name, password, phoneNumber }` (타겟 이메일은 초대에 저장되어 있으므로 입력 불필요).
   - 흐름: 코드 검증 → Member(role=ASSISTANT) 생성 → Invitation 상태 ACCEPTED → TeacherAssistantAssignment 자동 생성(teacherMemberId=senderId, assistantMemberId=새 사용자).  
   - 성공 시 Access/Refresh 토큰 발급.
5. **만료/취소 로직**  
   - 만료 시 `status=EXPIRED` + Soft delete (deletedAt).  
   - Teacher가 수동 취소 API(`PATCH /api/v1/invitations/{code}/revoke`)를 요청하면 `status=REVOKED`.

### Non-functional
- 모든 변환/검증은 트랜잭션 내부에서 처리해 Invitation/Member/Assignment 데이터 일관성을 유지한다.
- Invitation 코드는 UUID 기반 랜덤 문자열(예: base62)로 생성해 추측 어려움 확보.
- Controller/Service는 backend AGENTS 원칙(Repository: DataJpaTest, Service: Mockito, Controller: SpringBootTest + MockMvc)으로 개발한다.

## 3. API Design (Draft)

| Method | URL                                  | Request                                                                 | Response | Notes |
| ------ | ------------------------------------ | ----------------------------------------------------------------------- | -------- | ----- |
| POST   | `/api/v1/invitations`                | `{ "targetEmail": "assistant@classhub.com", "expiredAt": "2025-01-01T00:00" }` | `RsData<InvitationResponse>` (code, expiredAt) | Teacher-Auth required |
| POST   | `/api/v1/auth/invitations/verify`    | `{ "code": "INV-XYZ" }`                                                 | `RsData<InvitationVerifyResponse>` | No auth |
| POST   | `/api/v1/members/register/assistant/invitation`| `{ "code": "INV-XYZ", "name": "...", "password": "...", "phoneNumber": "..." }` | `RsData<LoginResponse>` + Refresh cookie | No auth |
| PATCH  | `/api/v1/invitations/{code}/revoke`  | Path param code                                                         | `RsData<Void>` | Teacher-Auth required |

## 4. Domain Model (Draft)
- **Invitation**: Entity per spec, includes helper methods `canUse(now)`, `markAccepted()`, `markExpired()`, `revoke()`.
- **InvitationRole** enum: `ASSISTANT`.
- **InvitationStatus** enum: `PENDING`, `ACCEPTED`, `EXPIRED`, `REVOKED`.
- **TeacherAssistantAssignment**: Already in spec; ensure creation occurs when invitation accepted.
- **DTOs**: `InvitationCreateRequest`, `InvitationVerifyRequest`, `InvitationRegisterRequest`, `InvitationResponse`.

## 5. TDD Plan
1. **Repository 테스트 (`InvitationRepositoryTest`)**  
   - `findByCode`, `save`, 만료 조건, status 업데이트 검증.
2. **Service 테스트**  
   - `InvitationService.createAssistantInvitation`: 중복/만료 로직, code generation.  
   - `InvitationService.verifyCode`: 만료/상태 예외 플로우.  
   - `InvitationService.acceptInvitation`: Member + Assignment + status 변경.
3. **Controller 테스트**  
   - `/api/v1/invitations` Teacher 인증 필요, 성공/중복/만료 테스트.  
   - `/api/v1/auth/invitations/verify` & `/register/by-invitation` MockMvc (success & invalid code).  
4. **회귀**  
   - SecurityConfig/JwtFilter 화이트리스트(`auth/invitations/**`, `auth/register/by-invitation`) 확인.  
   - `./gradlew test` 전체 실행.

## 6. Implementation Steps (3단계)
1. **도메인/Repository 구성**  
   - Invitation 엔티티, enums, repository, DTO 정의.  
   - DataJpaTest로 기본 CRUD/만료/상태 조작 검증.
2. **Service 로직 + 유닛 테스트**  
   - `InvitationService`에 create/verify/accept/revoke 메서드 구현 및 Mockito 테스트 작성.  
   - TeacherAssistantAssignment 자동 생성 로직 포함.
3. **Controller + Security + 통합 테스트**  
   - `InvitationController`(teacher endpoint)와 AuthController(`/auth/invitations/verify`), MemberController(`/members/register/assistant/invitation`)에 API를 추가한다.  
   - SecurityConfig/JwtFilter에서 `/api/v1/invitations/**`, `/api/v1/auth/invitations/**`, `/api/v1/members/register/assistant/invitation`을 permitAll/whitelist로 처리하고, MockMvc + 전체 `./gradlew test`로 회귀 검증한다.
