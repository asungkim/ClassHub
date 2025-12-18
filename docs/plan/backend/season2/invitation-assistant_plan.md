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
4. **조교 등록 API (`POST /api/v1/members/register/assistant`)**  
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

### Service Layer Design
- `InvitationService` 단일 클래스로 조교 초대 전 과정을 조율한다. 의존성: `InvitationRepository`, `MemberRepository`, `RegisterService`, `TeacherAssistantAssignmentRepository`, `ClockHolder(또는 LocalDateTime.now 래핑)`(테스트 용이성 확보), `AuthService` 또는 토큰 발급 서비스.
- Public 메서드
  1. `createAssistantInvitation(senderId, request)`  
     - senderId 인증 주체(Teacher) 검증 → targetEmail 정규화 → 중복 Pending 여부 확인 → 랜덤 코드 생성 후 기본 7일 만료(요청에 만료값 있으면 override) → Invitation 저장 → `InvitationResponse` 반환.
  2. `verifyCode(code)`  
     - 코드 정규화 → Repository 조회 → `canUse(now)` 검사 실패 시 `RsCode.INVALID_INVITATION` → sender Member 조회해 이름/역할 확인 → `InvitationVerifyResponse` 반환.
  3. `registerAssistantViaInvitation(request)`  
     - `verifyCode` 재사용 → RegisterService를 통해 Member(Role=ASSISTANT, email=targetEmail) 생성(비밀번호/이름/전화번호는 request.memberRequest) → `TeacherAssistantAssignment` 생성 및 저장 → Invitation `markAccepted()` 처리 → AuthService 로그인으로 토큰 발급.
  4. `revokeInvitation(senderId, code)`  
     - 코드 조회 및 senderId 일치 여부 검사 → 이미 처리된 초대면 `INVALID_INVITATION` → `revoke()` → 저장.
- 예외 규칙: soft-delete 된 초대나 만료는 `RsCode.INVALID_INVITATION`, 중복 초대는 `RsCode.INVITATION_ALREADY_EXISTS`.

### Controller Layer Design
- **InvitationController (Teacher 인증 필요)**  
  - `POST /api/v1/invitations`: Principal.id와 Request를 `InvitationService.createAssistantInvitation`에 전달. 성공 시 `RsCode.CREATED`. 실패 케이스는 중복/만료 예외 메시지 그대로 전달.  
  - `PATCH /api/v1/invitations/{code}/revoke`: Principal.id와 code 전달, 성공 시 `RsCode.SUCCESS`.
- **AuthController (permitAll)**  
  - `POST /api/v1/auth/invitations/verify`: `@Valid InvitationVerifyRequest`를 받아 `InvitationService.verifyCode` 호출 → 결과를 `RsData`로 반환.
- **MemberController (permitAll)**  
  - `POST /api/v1/members/register/assistant`: `RegisterAssistantByInvitationRequest` + HttpServletResponse (refresh cookie) → `InvitationService.registerAssistantViaInvitation` 호출 → `LoginResponse` 반환 후 기존 Register 흐름과 동일하게 쿠키 세팅.
- Security: `SecurityConfig`/`JwtAuthenticationFilter`에 `/api/v1/auth/invitations/**`와 `/api/v1/members/register/assistant`을 permitAll 화이트리스트로 추가, `/api/v1/invitations/**`는 TEACHER 권한 확인.

## 3. API Design (Draft)

| Method | URL                                  | Request                                                                 | Response | Notes |
| ------ | ------------------------------------ | ----------------------------------------------------------------------- | -------- | ----- |
| POST   | `/api/v1/invitations`                | `{ "targetEmail": "assistant@classhub.com", "expiredAt": "2025-01-01T00:00" }` | `RsData<InvitationResponse>` (code, expiredAt) | Teacher-Auth required |
| POST   | `/api/v1/auth/invitations/verify`    | `{ "code": "INV-XYZ" }`                                                 | `RsData<InvitationVerifyResponse>` | No auth |
| POST   | `/api/v1/members/register/assistant`| `{ "code": "INV-XYZ", "name": "...", "password": "...", "phoneNumber": "..." }` | `RsData<LoginResponse>` + Refresh cookie | No auth |
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
   - `InvitationService.createAssistantInvitation`: 중복 여부/기본 만료일/커스텀 만료일 반영, code generator 호출 여부.  
   - `InvitationService.verifyCode`: 만료/상태/soft delete 예외, sender 정보 매핑.  
   - `InvitationService.registerAssistantViaInvitation`: RegisterService 협력(Mock), Assignment 저장, Invitation 상태/soft delete, 토큰 반환, 트랜잭션 롤백 케이스.  
   - `InvitationService.revokeInvitation`: sender mismatch/이미 처리된 코드 예외.
3. **Controller 테스트**  
   - `/api/v1/invitations` Teacher 인증 필요, 성공/중복/권한 없는 사용자 차단.  
   - `/api/v1/auth/invitations/verify` & `/members/register/assistant` MockMvc (성공/invalid code/refresh 쿠키 세팅).  
   - SecurityConfig 화이트리스트가 실제로 동작하는지 permitAll 경로에서 인증 없이 호출되는지 확인.
4. **회귀**  
   - SecurityConfig/JwtFilter 화이트리스트(`auth/invitations/**`, `members/register/assistant`) 확인.  
   - `./gradlew test` 전체 실행.

## 6. Implementation Steps (3단계)
1. **도메인/Repository 구성**  
   - Invitation 엔티티, enums, repository, DTO 정의.  
   - DataJpaTest로 기본 CRUD/만료/상태 조작 검증.
2. **Service 로직 + 유닛 테스트**  
   - `InvitationService` 구현: 랜덤 코드 생성 전략, 중복 체크, 만료 일자 기본값(설정값 없으면 7일), `RegisterService` 협력으로 조교 멤버 생성, `TeacherAssistantAssignmentRepository`를 통한 연결 저장, Invitation 상태 전환 및 soft delete 보장.  
   - Mockito 기반 테스트에서 Repository/Service/Assignment 의존성 mocking, Clock 고정, 예외 코드(`RsCode`) 검증, 트랜잭션 실패 시 Invitation 상태 롤백 여부 확인.
3. **Controller + Security + 통합 테스트**  
   - `InvitationController`에 create/revoke API 구현(Principal 주입, validation, 201 응답); AuthController/MemberController에 각각 verify/register endpoint 추가.  
   - SecurityConfig/JwtFilter 업데이트 및 E2E MockMvc 테스트: permitAll 경로 무인증 허용, teacher 전용 경로는 ROLE_TEACHER 필요.  
   - Controller 테스트에서 RefreshToken 쿠키 세팅, `RsData` 응답 구조, 예외 변환(Invalid code → 400, 중복 → 409) 검증 후 전체 `./gradlew test` 실행.
