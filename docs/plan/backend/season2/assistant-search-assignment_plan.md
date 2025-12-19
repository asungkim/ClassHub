# Feature: Assistant Email Search & Direct Assignment

## 1. Problem Definition
- Season2 요구사항(v1.3)에서는 조교 초대 절차를 제거했고, 조교는 일반 회원가입 후 선생님이 직접 연결해야 한다.
- 현재 교사는 이미 가입한 조교를 찾을 수단이 없고, 설령 이메일을 알아도 `TeacherAssistantAssignment`를 직접 생성할 API가 존재하지 않는다.
- 초대 시스템을 제거한 상태에서 교사가 조교를 탐색하고 기존 연결 여부(Active/Inactive)를 한눈에 확인하고, 즉시 Assignment를 생성/복구할 수 있는 API 세트가 필요하다.

## 2. Requirements
### Functional
1. **조교 이메일 검색 API**
   - `GET /api/v1/teachers/me/assistants/search?email=` 엔드포인트를 제공한다.
   - ROLE_TEACHER 인증 필요, 다른 롤/미인증 요청은 403/401 처리.
   - `email` 파라미터는 트림 후 빈 문자열이면 즉시 빈 배열을 반환한다(불필요한 전체 목록 노출 방지).
   - 이메일 부분 일치(대소문자 무시)로 `Member.role = ASSISTANT` 이며 `deletedAt IS NULL` 인 계정을 최대 5건까지 반환한다.
   - 응답 항목에는 `assistantMemberId`, `name`, `email`, `phoneNumber`, `assignmentStatus`, `assignmentId`, `connectedAt`, `disabledAt` 를 포함한다.
     - `assignmentStatus`: `NOT_ASSIGNED`, `ACTIVE`, `INACTIVE` 중 하나.
     - `assignmentId`, `connectedAt`, `disabledAt` 는 해당 교사와 매핑 기록이 있을 때만 노출한다.
   - 이미 다른 선생님에게 연결된 조교도 검색 대상이지만, 응답에 현재 교사와의 관계 상태만 표현한다(다른 교사 연결 여부는 고려하지 않음).
   - Rate-limit 목적상 동일 요청 값에는 캐시 가능한 순수 조회 로직으로 유지하고, 1초 내 응답을 목표로 한다.
2. **조교 Assignment 생성 API**
   - `POST /api/v1/teachers/me/assistants` 엔드포인트에서 `{ "assistantMemberId": "UUID" }` 요청을 받아 `TeacherAssistantAssignment` 를 생성하거나 비활성 상태라면 복구한다.
   - ROLE_TEACHER 인증 필요, 요청 본문이 없거나 잘못된 UUID면 `RsCode.BAD_REQUEST`.
   - 대상 조교는 `Member.role = ASSISTANT`, `deletedAt IS NULL` 이어야 하며, 자기 자신이나 Teacher 계정을 연결하려 할 경우 `RsCode.INVALID_REQUEST`.
   - 이미 활성화된 Assignment가 있으면 `RsCode.DUPLICATE_REQUEST` 로 예외 처리한다.
   - 비활성 Assignment 존재 시 `enable()` 호출만 수행하고 `connectedAt` 갱신 없이 disabledAt 삭제.
   - 신규 생성 시 `TeacherAssistantAssignment.create(teacherId, assistantMemberId)` 를 저장하고, 응답은 기존 `AssistantAssignmentResponse` 포맷을 재사용한다.

### Non-functional
- Repository 계층에서 Case-insensitive `LIKE` 검색을 수행하고 인덱스를 고려해 prefix 검색을 우선한다.
- 파라미터 검증 실패 시 `RsCode.BAD_REQUEST` 로 래핑된 예외를 던지고, `RsData` 응답 규약을 유지한다.
- Soft delete (`deletedAt`) 를 일관되게 존중하며, 이미 비활성화된 회원은 검색 결과에서 제외한다.
- Assignment 생성 로직은 트랜잭션 내에서 실행해 Race condition 시 정합성을 유지하고, `teacherId + assistantId` 유니크 제약 위반은 적절히 처리한다.

## 3. API Design (Draft)
| Method | URL | Request | Response | Notes |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/teachers/me/assistants/search` | `email`(query, required) | `RsData<List<AssistantSearchResponse>>` | ROLE_TEACHER 전용, email blank → [] |
| POST | `/api/v1/teachers/me/assistants` | `{ \"assistantMemberId\": \"UUID\" }` | `RsData<AssistantAssignmentResponse>` | ROLE_TEACHER 전용, 활성 중복 시 400 |

### AssistantSearchResponse
```json
{
  "assistantMemberId": "UUID",
  "name": "조교 이름",
  "email": "assistant@classhub.com",
  "phoneNumber": "010-1234-5678",
  "assignmentStatus": "ACTIVE",
  "assignmentId": "UUID",
  "connectedAt": "2025-12-19T10:00:00Z",
  "disabledAt": null
}
```

### AssistantAssignmentCreateRequest
```json
{
  "assistantMemberId": "UUID"
}
```

## 4. Domain Model (Draft)
- **MemberRepository**
  - `List<Member> findTop5ByRoleAndDeletedAtIsNullAndEmailContainingIgnoreCaseOrderByEmailAsc(...)` 추가.
- **TeacherAssistantAssignmentRepository**
  - `List<TeacherAssistantAssignment> findByTeacherMemberIdAndAssistantMemberIdIn(UUID teacherId, Collection<UUID> assistantIds)` 로 일괄 조회하여 상태 매핑.
   - `Optional<TeacherAssistantAssignment> findByTeacherMemberIdAndAssistantMemberId(UUID teacherId, UUID assistantMemberId)` 로 생성 중복 여부 확인.
- **AssistantManagementService**
  - 신규 메서드 `searchAssistants(teacherId, rawEmail)` 구현.
  - 신규 메서드 `assignAssistant(teacherId, assistantMemberId)` 추가: 활성 존재 여부 검증, 비활성→활성 복구 또는 새 Assignment 생성.
  - 결과 DTO 매핑 시 `assignmentMap` 을 이용해 ACTIVE/INACTIVE 판별 (`deletedAt == null`).
- **DTO**
  - `AssistantSearchResponse` 레코드 추가.
  - `AssistantAssignmentCreateRequest` 레코드(assistantMemberId) 추가.
  - `AssistantAssignmentStatus` enum을 재활용하거나 검색 전용 status enum 정의.

## 5. TDD Plan
1. **Repository Tests**
   - `MemberRepositoryTest`: `findTop5...ContainingIgnoreCase` 가 부분 일치 + 대소문자 무시 + 최대 5건 반환을 검증.
   - `TeacherAssistantAssignmentRepositoryTest`: 
     - `findByTeacherMemberIdAndAssistantMemberIdIn` 으로 여러 조교 상태를 한 번에 로드하는 시나리오 추가.
     - `findByTeacherMemberIdAndAssistantMemberId` 가 존재/미존재를 명확히 반환하는지 검증.
2. **Service Tests (`AssistantManagementServiceTest`)**
   - `searchAssistants` 가 빈 이메일에서 빈 리스트를 반환하는지 확인.
   - 다수 조교 검색 시 Member/Assignment 리포지토리 호출과 상태 매핑(Active/Inactive/NotAssigned) 검증.
   - `assignAssistant`:
     - 대상 조교가 없거나 ROLE이 다른 경우 예외 처리.
     - 이미 활성 Assignment가 있으면 `RsCode.DUPLICATE_REQUEST` 예외.
     - 비활성 Assignment 존재 시 `enable()` 호출 여부 및 저장 결과 검증.
     - 신규 생성 시 repository save 호출과 반환 DTO 필드 검증.
3. **Controller Tests**
   - `/api/v1/teachers/me/assistants/search` GET: email 전달 + 인증할 때 200 & JSON 구조 검증.
   - 이메일 누락/빈 문자열일 때 200 + 빈 배열 응답, ROLE mismatch 시 403.
   - `/api/v1/teachers/me/assistants` POST: 정상 요청 시 201(또는 200) + AssignmentResponse, 잘못된 본문/ROLE mismatch 시 에러 경로 확인.

---
### 계획 요약 (한국어)
- 초대 시스템 제거 이후 교사가 이메일로 이미 가입한 조교를 찾아 연결할 수 없으므로, `GET /api/v1/teachers/me/assistants/search` API와 `POST /api/v1/teachers/me/assistants` Assignment 생성 API를 함께 제공해 ROLE_TEACHER 인증 하에 검색→연결 플로우를 완성한다.
- Member/Assignment 리포지토리를 확장해 최대 5건까지 빠르게 조회하고, 이미 존재하는 Assignment를 복구/중복 방지하여 안전하게 연결할 수 있도록 Service/Controller/Repository 테스트를 통해 검색·생성·검증 로직을 TDD로 보장한다.
