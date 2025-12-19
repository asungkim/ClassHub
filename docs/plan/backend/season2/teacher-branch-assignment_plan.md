# Feature: Teacher Branch Assignment API

## 1. Problem Definition
- Season2에서는 선생님이 여러 학원/지점에 출강하며, UI에서 Company/Branch를 선택하거나 직접 생성한 뒤 자신과 지점을 연결(TeacherBranchAssignment)해야 한다.
- 기존에는 Company/Branch CRUD API만 존재하고, TeacherBranchAssignment를 만들거나 비활성화하는 API가 없어 실제 출강 관리 플로우를 진행할 수 없었다.
- “학원 관리” 페이지에서 개인/회사 학원을 선택, 회사/지점을 검색 또는 직접 입력해 연결하고, 더 이상 출강하지 않을 때는 비활성화하는 전 과정을 지원하는 백엔드 API가 필요하다.

## 2. Requirements
### Functional
1. **Assignment 목록 조회**
   - `GET /api/v1/teachers/me/branches?status=ACTIVE|INACTIVE` 로 현재 교사가 연결한 지점 목록을 페이지네이션 형태로 제공.
   - 각 항목에는 `assignmentId`, `branchId`, `branchName`, `companyName`, `companyType`, `verifiedStatus`, `assignmentRole`, `connectedAt`, `disabledAt`가 포함된다.
2. **Assignment 생성**
   - `POST /api/v1/teachers/me/branches`  
     ```json
     { "branchId": "UUID", "role": "OWNER|FREELANCE?" }
     ```
   - ROLE_TEACHER 본인만 호출 가능. branchId는 기존 Branch API로 확보(회사/지점 직접 입력 시 선행 API 호출).
   - 검증 규칙
     - Branch가 soft delete 상태거나 미검증인데 작성자와 다른 Teacher이면 `RsCode.FORBIDDEN`.
     - **동일 교사**가 같은 branch에 대해 이미 활성 Assignment를 보유하고 있으면 `RsCode.DUPLICATE_REQUEST`.
     - 비활성 Assignment가 존재하면 `deletedAt` 제거(`restore()` 호출)하여 재활성화하고, 최초 연결 시점은 `createdAt`으로 추적.
     - INDIVIDUAL Company → role은 자동 OWNER로 고정. ACADEMY → 기본 FREELANCE.
     - Assignment 생성 시 Branch verifiedStatus가 `VERIFIED`면 전체 Teacher에게 공개, `UNVERIFIED`면 해당 Creator/Assignment만 조회 가능.
3. **Assignment 활성/비활성화**
   - `PATCH /api/v1/teachers/me/branches/{assignmentId}`  
     ```json
     { "enabled": true|false }
     ```
   - enable → `deletedAt=null`, disable → `deletedAt=now` (BaseTimeEntity soft delete 재사용).
   - 해당 교사 외 접근/수정 요청은 `RsCode.FORBIDDEN`.
4. **보조 흐름**
   - 개인 학원 직접 입력: `POST /api/v1/companies`(INDIVIDUAL) → 응답의 기본 Branch ID 사용 → Assignment 생성 시 role=OWNER.
   - 회사 학원 직접 입력: `POST /api/v1/companies(type=ACADEMY)` + 필요 시 `POST /api/v1/branches` → Assignment 생성.
   - 회사/지점이 미리 등록된 경우 `GET /api/v1/companies` + `GET /api/v1/branches` 결과로 branchId 확보.

### Non-functional
- 모든 응답은 `RsData`를 사용하고, ROLE 검증은 `@PreAuthorize("hasAuthority('TEACHER')")` 및 추가 BusinessException으로 처리.
- `TeacherBranchAssignment`는 soft delete(`deletedAt`)로 활성/비활성 상태를 표현하며, 동일 `teacherId+branchId` 조합의 유니크 제약을 유지.
- 서비스 계층은 트랜잭션으로 감싸 race condition 시에도 중복 생성이 없도록 한다.
- 페이지 API는 기본 page=0, size=20를 제공하며, status 미지정 시 ACTIVE만 반환.

## 3. API Design (Draft)

| Method | URL | Request | Response | Role |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/teachers/me/branches` | `status?, page?, size?` | `RsData<Page<TeacherBranchAssignmentResponse>>` | TEACHER |
| POST | `/api/v1/teachers/me/branches` | `{ branchId, role? }` | `RsData<TeacherBranchAssignmentResponse>` | TEACHER |
| PATCH | `/api/v1/teachers/me/branches/{assignmentId}` | `{ enabled }` | `RsData<TeacherBranchAssignmentResponse>` | TEACHER |

`TeacherBranchAssignmentResponse` 예시
```json
{
  "assignmentId": "UUID",
  "branchId": "UUID",
  "branchName": "강남",
  "companyId": "UUID",
  "companyName": "러셀",
  "companyType": "ACADEMY",
  "verifiedStatus": "UNVERIFIED",
  "role": "FREELANCE",
  "createdAt": "2025-12-19T10:00:00",
  "deletedAt": null
}
```

## 4. Domain Model (Draft)
- **TeacherBranchAssignment**
  - 필드: `teacherMemberId`, `branchId`, `role (OWNER/FREELANCE)` (`BaseTimeEntity`의 `createdAt/updatedAt/deletedAt` 활용).
  - 메서드: `create`, `disable`, `enable`, `isActive`.
  - 유니크 제약: `(teacherMemberId, branchId)`.
- **Branch/Company**
  - 기존 모델 재사용. Branch가 UNVERIFIED일 경우 creator나 이미 Assignment를 가진 Teacher만 조회 가능.
  - INDIVIDUAL Company는 Branch 1개, Creator = Teacher → Assignment role=OWNER.
- **DTO**
  - `TeacherBranchAssignmentCreateRequest` (branchId, optional role).
  - `TeacherBranchAssignmentStatusUpdateRequest` (enabled).
  - `TeacherBranchAssignmentResponse`.
  - `TeacherBranchAssignmentStatusFilter` enum (ACTIVE/INACTIVE/ALL).

## 5. TDD Plan
1. **Repository Tests**
   - `TeacherBranchAssignmentRepositoryTest`
     - `findByTeacherMemberIdAndBranchId` 존재/미존재 케이스.
     - `findByTeacherMemberIdAndDeletedAtIsNull` 페이지 조회 및 soft delete 필터 검증.
     - 중복 생성 시 유니크 제약 예외 확인.
2. **Service Tests (`TeacherBranchAssignmentServiceTest`)**
   - `createAssignment`:
     - Branch UNVERIFIED && creator != teacher → `FORBIDDEN`.
     - 이미 활성 Assignment 존재 → `DUPLICATE_REQUEST`.
     - 비활성 Assignment 존재 → `enable()` + save.
     - INDIVIDUAL Branch → role 강제 OWNER.
   - `getAssignments` status 필터별 반환/페이지 파라미터/Branch & Company fetch join 검증.
   - `updateAssignmentStatus` enable/disable 분기, 남의 assignment 접근 시 `FORBIDDEN`.
3. **Controller Tests**
   - `GET /teachers/me/branches`: 인증/인증 실패, status 파라미터 파싱(`BAD_REQUEST`), PageResponse 직렬화.
   - `POST /teachers/me/branches`: 본문 검증, ROLE mismatch, 성공 시 `RsCode.CREATED`.
   - `PATCH /teachers/me/branches/{id}`: enabled=true/false 시나리오, 존재하지 않는 assignment 예외 핸들링.

---
### 계획 요약 (한국어)
- 학원 관리 UI에서 선생님이 지점을 검색/직접 입력 후 연결하고, 더 이상 출강하지 않을 때 비활성화하는 전체 플로우를 백엔드 API로 지원한다.
- Company/Branch API는 그대로 재사용하고, TeacherBranchAssignment 생성/조회/토글 API를 추가해 CRUD를 완성한다.
- Repository → Service → Controller 순으로 TDD하며, INDIVIDUAL/ACADEMY, verifiedStatus, soft delete 규칙을 테스트로 보장한 뒤 UI 연동을 위한 안정적인 기반을 마련한다.
