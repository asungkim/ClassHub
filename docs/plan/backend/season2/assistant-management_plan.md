# Feature: Assistant Management & Invitation Tracking

## 1. Problem Definition
- Phase5 TODO("초대 시스템 개발")에서 교사가 조교 초대 이후의 운영 상황을 파악할 수 있는 도구가 전혀 없어 진행 현황을 통제할 수 없다.
- 현재 백엔드에는 초대 생성/검증/가입 API만 존재하고, **조교 목록 조회**나 **초대 목록 조회**, **조교 비활성화(soft delete) 제어** API가 없다.
- 조교가 퇴사하거나 잠시 권한을 내려야 하는 경우 선생님이 직접 `TeacherAssistantAssignment`를 비활성화해야 하는데, 이를 수행할 수단이 없어 보안/업무 통제가 불가능하다.

## 2. Requirements

### Functional
1. **조교 목록 조회 API**  
   - 교사가 자신에게 배정된 `TeacherAssistantAssignment` 목록을 조회한다.  
   - Query 파라미터 `status`(ACTIVE/INACTIVE/ALL)를 받아 soft delete 여부(`deletedAt == null` ↔ `!= null`)로 필터링한다.  
   - 응답에는 `assignmentId`, `assistantMemberId`, `assistantName`, `assistantEmail`, `assistantPhone`, `assignedAt`, `disabledAt`(=deletedAt)와 `isActive`(deletedAt null 여부를 변환한 파생 값) 등을 포함한다.
2. **조교 활성/비활성화 API**  
   - `assignmentId`로 대상 조교를 식별하고, 현재 교사와 매칭되는지 검증한다.  
   - Request body는 `{ "enabled": true | false }` 하나만 사용해, enabled=false면 soft delete(삭제 시각 기록), enabled=true면 deletedAt을 null로 되돌린다.  
   - 상태 전환 내역은 `TeacherAssistantAssignment` 도메인 메서드(`disable(now)`, `enable()`)로 관리한다.
3. **초대 목록 조회 API**  
   - 교사가 생성한 Invitation 중 ASSISTANT 타입을 status별(PENDING/REVOKED/EXPIRED/ACCEPTED/ALL)로 필터링해 반환한다.  
   - 응답 필드: `code`, `targetEmail`, `status`, `expiredAt`, `createdAt`, `lastUsedAt(optional)`.  
   - 정렬은 기본적으로 `createdAt DESC`. 페이지네이션은 쿼리 파라미터 `page`, `size`로 단순 구현(기본 size=20).

### Non-functional
- 모든 API는 기존 `RsData<RsCode>` 포맷을 준수하고, 교사 인증이 된 Principal만 접근 가능해야 한다. (`ROLE_TEACHER` 강제)
- soft delete 복구/적용 시 트랜잭션 내에서 처리하여 Assignment/Invitation 상태의 일관성을 유지한다.
- 목록 API는 향후 대시보드 카드에 사용될 예정이므로 1초 이내 응답을 목표로 하되, `TeacherAssistantAssignment`와 `Invitation`에 teacherId 복합 인덱스를 추가/재사용한다.

## 3. API Design (Draft)

| Method | URL | Query/Body | Response | Notes |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/teachers/me/assistants` | `status?=ACTIVE\|INACTIVE\|ALL`, `page?`, `size?` | `RsData<PagedAssistantAssignmentResponse>` | ROLE_TEACHER |
| PATCH | `/api/v1/teachers/me/assistants/{assignmentId}` | `{ "enabled": true \| false }` | `RsData<AssistantAssignmentResponse>` | ROLE_TEACHER |
| GET | `/api/v1/teachers/me/invitations` | `status?=PENDING\|ACCEPTED\|REVOKED\|EXPIRED\|ALL`, `page?`, `size?` | `RsData<PagedInvitationResponse>` | ROLE_TEACHER |

### Response Sketches
```json
// AssistantAssignmentResponse
{
  "assignmentId": "UUID",
  "assistant": {
    "memberId": "UUID",
    "name": "조교 이름",
    "email": "assistant@classhub.com",
    "phone": "+821012345678"
  },
  "status": "ACTIVE",
  "assignedAt": "2025-12-20T10:00:00Z",
  "disabledAt": null
}

// InvitationSummary
{
  "code": "INV-ABC123",
  "targetEmail": "assistant@classhub.com",
  "status": "PENDING",
  "expiredAt": "2025-12-25T09:00:00Z",
  "createdAt": "2025-12-18T09:00:00Z",
  "lastUsedAt": null
}
```

## 4. Domain Model (Draft)
- **TeacherAssistantAssignment**  
  - 기존 엔티티에 `disable(LocalDateTime now)` / `enable()` 도메인 메서드를 추가해 BaseTimeEntity의 `deletedAt`을 통해 상태를 제어한다(별도 status 필드 없음).  
  - Repository에 `findAllByTeacherIdAndDeletedAtIsNull/NotNull` 및 페이지네이션 가능한 메서드 추가.
- **Invitation**  
  - 이미 생성된 스펙을 재사용하되, teacherId 필터용 인덱스를 보장한다.  
  - Repository에 `findAllBySenderIdAndInviteeRoleOrderByCreatedAtDesc` + status 필터 조건을 추가.
- **DTOs**  
  - `AssistantAssignmentResponse`, `PagedAssistantAssignmentResponse`(content + pagination metadata).  
  - `InvitationSummaryResponse`, `PagedInvitationResponse`.  
  - `AssistantAssignmentStatusUpdateRequest` (`action` enum).

## 5. TDD Plan
1. **Repository Tests**  
   - `TeacherAssistantAssignmentRepositoryTest`: teacherId별 필터링, soft delete 상태 전환 시 조회 결과가 올바른지 검증.  
   - `InvitationRepositoryTest`: senderId + status 조합으로 페이징 조회되는지 확인.
2. **Service Tests (`AssistantManagementServiceTest`)**  
   - `listAssistants`가 status 파라미터별로 Repository 메서드를 호출하고 DTO를 올바르게 매핑하는지.  
   - `updateAssistantStatus`에서 teacherId 불일치, 이미 비활성화된 항목 등을 예외로 처리(`RsCode.ACCESS_DENIED`, `RsCode.INVALID_REQUEST`).  
   - `listInvitations`가 status=ALL과 특정 status 필터에서 각각 다른 repository 경로를 쓰는지.  
   - soft delete 토글 시 `disable()`/`enable()` 호출 여부 및 `save` 호출 검증.
3. **Controller Tests**  
   - `/api/v1/teachers/me/assistants` GET: ROLE_TEACHER만 접근 가능, status 파라미터 기본값이 ACTIVE인지 확인.  
   - `/api/v1/teachers/me/assistants/{id}` PATCH: enabled=true/false 조합 검증, teacherId 불일치 시 접근 거부.  
   - `/api/v1/teachers/me/invitations` GET: status 미지정 시 PENDING만 반환, 인증 없으면 401.  
   - MockMvc + `@SpringBootTest` 조합으로 SecurityConfig 통합 검증.
4. **Regression**  
   - 기존 초대 생성/가입 플로우와 충돌하지 않도록 `./gradlew test` 전체 실행.  
   - Soft delete 전환 이후 재조회 시 목록이 즉시 반영되는지 Smoke 테스트.

---

### 계획 요약 (한국어)
- 교사가 조교/초대 현황을 통제하지 못하는 문제를 해결하기 위해 조교 목록 조회, 조교 활성/비활성 토글, 초대 목록 조회 API를 추가한다.  
- 모든 API는 교사 인증을 요구하며 soft delete 상태를 존중하도록 `TeacherAssistantAssignment`와 `Invitation` 리포지토리를 확장하고, Repository/Service/Controller 테스트를 통해 페이징·필터 로직과 권한 검증을 보장한다.
