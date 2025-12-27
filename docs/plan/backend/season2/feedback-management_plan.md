# Feature: Feedback Management

## 1. Problem Definition
- 교사/조교/학생이 시스템 개선점을 제출할 공식 채널이 필요하다.
- 관리자는 피드백을 목록으로 확인하고 해결 상태로 전환해야 한다.
- 작성자는 대시보드에서 본인 피드백이 해결되었는지 확인할 수 있어야 한다.

## 2. Requirements

### Functional
1. **피드백 작성**
   - TEACHER/ASSISTANT/STUDENT가 `content`를 제출한다.
   - 작성자는 인증 Principal에서 가져오며, `status=SUBMITTED`로 저장한다.
   - `content`는 trim 처리 후 빈 문자열이면 400 처리한다.
2. **관리자 목록 조회**
   - SUPER_ADMIN만 전체 피드백을 조회한다.
   - Query 파라미터 `status`(SUBMITTED/RESOLVED/ALL), `page`, `size`를 지원한다.
   - 기본 정렬은 `createdAt DESC`이며, 응답에는 작성자 기본 정보(아이디/이름/역할/이메일/전화번호)를 포함한다.
3. **해결 처리**
   - SUPER_ADMIN이 `status=RESOLVED`로 전환한다.
   - `resolvedAt`, `resolvedByMemberId`를 기록한다.
   - 이미 RESOLVED 상태인 경우 409(CONFLICT)로 응답한다.
4. **작성자 목록 조회**
   - 로그인 사용자가 본인 피드백만 조회한다.
   - Query 파라미터 `status`, `page`, `size` 지원.
   - 응답에는 `status`, `resolvedAt`을 포함해 대시보드에서 해결 여부를 표시할 수 있게 한다.

### Non-functional
- 모든 응답은 `RsData<RsCode>` 포맷을 사용한다.
- Role 기반 접근제어를 강제한다(SUPER_ADMIN/일반 사용자 구분).
- 목록 조회는 1초 이내 응답을 목표로 하며, `status`, `memberId` 인덱스를 고려한다.
- 피드백 삭제 API는 요구사항에는 있으나 현재 스펙에 없으므로 이번 범위에서는 제외하고 필요 시 별도 스펙 업데이트로 추가한다.

## 3. API Design (Draft)

| Method | URL | Query/Body | Response | Notes |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/feedback` | `{ "content": "..." }` | `RsData<FeedbackResponse>` | TEACHER/ASSISTANT/STUDENT |
| GET | `/api/v1/feedback` | `status?`, `page?`, `size?` | `RsData<PagedFeedbackAdminResponse>` | SUPER_ADMIN |
| PATCH | `/api/v1/feedback/{feedbackId}/resolve` | - | `RsData<FeedbackResponse>` | SUPER_ADMIN |
| GET | `/api/v1/feedback/me` | `status?`, `page?`, `size?` | `RsData<PagedFeedbackMyResponse>` | TEACHER/ASSISTANT/STUDENT |

### Response Sketches
```json
// FeedbackResponse (admin/my 공통 요약)
{
  "id": "UUID",
  "content": "텍스트",
  "status": "SUBMITTED",
  "createdAt": "2025-12-27T12:00:00Z",
  "resolvedAt": null,
  "resolvedByMemberId": null,
  "writer": {
    "memberId": "UUID",
    "name": "홍길동",
    "role": "TEACHER",
    "email": "teacher@classhub.com",
    "phoneNumber": "+821012345678"
  }
}
```

## 4. Domain Model (Draft)
- **Feedback**
  - 필드: `memberId`, `content`, `status`, `resolvedAt`, `resolvedByMemberId`.
  - 상태 전환 메서드: `resolve(UUID adminId, LocalDateTime now)`.
  - `content`는 trim 후 저장하며 빈 문자열이면 예외 처리.
- **FeedbackStatus**: `SUBMITTED`, `RESOLVED`.
- **Repository**
  - `findAllByStatus(Pageable)`
  - `findAllByMemberId(UUID, Pageable)`
  - `findAllByStatusAndMemberId(UUID, Pageable)`
- **Service**
  - `createFeedback(memberId, content)`
  - `listFeedbacks(status, pageable)` (admin)
  - `listMyFeedbacks(memberId, status, pageable)`
  - `resolveFeedback(feedbackId, adminId)`
- **RsCode**
  - `FEEDBACK_NOT_FOUND` (404)
  - `FEEDBACK_ALREADY_RESOLVED` (409)

## 5. TDD Plan
1. **Repository Tests**
   - `FeedbackRepositoryTest`: status 필터링과 memberId 필터링이 정확한지 검증.
   - 정렬(createdAt DESC) 및 페이징 동작 확인.
2. **Service Tests (`FeedbackServiceTest`)**
   - `createFeedback`가 SUBMITTED 상태로 저장되는지.
   - `listMyFeedbacks`가 본인 것만 반환하는지.
   - `resolveFeedback`가 상태/해결자/시간을 기록하는지.
   - 이미 RESOLVED면 `FEEDBACK_ALREADY_RESOLVED` 예외를 던지는지.
3. **Controller Tests**
   - POST `/api/v1/feedback`: 로그인 사용자만 접근 가능, content 빈값 400.
   - GET `/api/v1/feedback`: SUPER_ADMIN만 접근 가능.
   - GET `/api/v1/feedback/me`: 본인 데이터만 노출.
   - PATCH `/api/v1/feedback/{id}/resolve`: SUPER_ADMIN만 접근 가능, 중복 처리 409.

---

### 계획 요약 (한국어)
- 사용자 피드백을 생성하고 관리자가 해결 상태로 전환하며, 작성자는 `/feedback/me`를 통해 해결 여부를 확인하도록 설계한다.
- Feedback 엔티티에 `resolvedAt/resolvedByMemberId`를 추가하고, 목록/해결 API와 권한 제어를 포함한 서비스/컨트롤러 테스트를 통해 안정성을 보장한다.
- 피드백 삭제는 요구사항에 있으나 현재 스펙에 없으므로 이번 범위에서는 제외하고, 필요 시 별도 스펙 업데이트 후 추가한다.
