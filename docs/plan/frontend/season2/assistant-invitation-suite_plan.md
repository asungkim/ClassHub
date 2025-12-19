# Feature: Assistant Invitation Management Suite

## 1. Problem Definition

- 교사는 조교 초대 현황과 배정 상태를 즉시 파악하고, 조교 권한을 켜/끄거나 초대 링크를 재사용해야 한다. 하지만 Season2 프론트에는 새로운 백엔드 API(`/api/v1/teachers/me/assistants`, `/api/v1/teachers/me/invitations`, `/api/v1/invitations`, `/api/v1/invitations/{code}/revoke`)를 연결하는 UI가 존재하지 않는다.
- 조교가 받은 초대 링크를 클릭해도 기존 페이지는 Season1 설계를 기준으로 만들어져 있어, Season2 백엔드(`/api/v1/auth/invitations/verify`, `/api/v1/members/register/assistant`) 스펙과 입력 항목(전화번호, 비밀번호 규칙 등)이 맞지 않는다.
- 결과적으로 교사/조교 모두 초대 → 검증 → 가입 플로우를 완성할 수 없으며, 진행 상황을 추적하기도 어렵다. 세 페이지(교사용 조교 관리, 조교 초대 검증, 조교 회원가입)를 Season2 기준으로 재설계해야 한다.

## 2. User Flows & Use Cases

1. **Teacher – 조교 관리**
   1. `/dashboard/teacher/invitations` (가칭) 진입 → 인증된 Teacher만 접근.
   2. 좌측에는 “조교 배정” 섹션: `/api/v1/teachers/me/assistants?status=`로 목록 조회, 카드별로 이름·이메일·연락처·배정일을 표시하고 토글 스위치로 활성/비활성 전환(`PATCH ...assistants/{id}`).
   3. 우측에는 “초대 관리” 섹션: `/api/v1/teachers/me/invitations?status=`로 초대 코드, 대상 이메일, 만료일, 상태를 표 형태로 보여주고, 각 행에 복사 버튼/취소 버튼을 둔다.
   4. “새 초대 만들기” 버튼 클릭 → 모달에서 이메일 입력 → `POST /api/v1/invitations` 호출 후 응답 코드 복사/공유.
2. **Assistant – 초대 검증**
   1. 초대 링크 `/auth/invitation/verify?code=...` 접근 → `POST /api/v1/auth/invitations/verify`.
   2. 검증 성공 시 초대한 Teacher 이름, 초대 상태, 만료 시각을 보여주고 “회원가입 계속하기” 버튼 → `/auth/register/assistant?code=...`.
   3. 실패 시 안내 메시지 + 홈 이동 버튼.
3. **Assistant – 초대 기반 회원가입**
   1. `/auth/register/assistant?code=...` 접근.
   2. 비로그인 사용자: 폼 입력(이름, 비밀번호/확인, 전화번호, 개인정보 동의). 이메일은 초대 정보로 숨김 처리.
   3. Submit → `POST /api/v1/members/register/assistant` 호출, 성공 시 토큰 저장 후 `/assistant` 대시보드로 이동.
   4. 로그인 상태일 경우 즉시 역할별 대시보드로 리디렉션.

## 3. Page & Layout Structure
- **Teacher 조교 관리 페이지**
  - 상단 헤더: “조교 관리” Title + 초대 생성 버튼.
  - 두 개의 Card 섹션(Grid 2열, 모바일 1열):
    - 조교 배정 목록: 탭(활성/비활성/전체), 리스트 아이템에 프로필 아이콘·이름·연락처·배정일·Toggle.
    - 초대 목록: 상태 필터(Select) + Table (코드, 이메일, 만료일, 상태, 복사/취소 버튼).
  - 빈 상태: “등록된 조교가 없습니다” / “진행 중인 초대가 없습니다”.
- **초대 검증 페이지**
  - 기존 Hero/폼 레이아웃과 동일하되, 중앙 카드에 초대 정보 표시.
  - 성공 카드: 선생님 이름, 초대 대상 이메일(마스킹), 만료 시각, 주의 안내.
  - 실패 카드: 에러 아이콘 + 메시지 + 홈 버튼.
- **조교 회원가입 페이지**
  - 선생님/학생 가입 페이지와 동일한 풀스크린 Hero + Form 2열 레이아웃.
  - 좌측: 역할 소개 텍스트(“ClassHub 조교용 계정”).
  - 우측 Form: 이름, 비밀번호, 비밀번호 확인, 전화번호, 약관 체크.
  - Submit 버튼 아래에 안내(“가입 즉시 대시보드로 이동합니다”).

## 4. Component Breakdown
- `AssistantManagementPage`
  - 하위 컴포넌트:
    - `AssistantListCard`: props `{ assistants, statusFilter, onToggle }`, 내부에서 `Switch`, `Badge`, `EmptyState`.
    - `InvitationTable`: props `{ invites, filterStatus, onCopy, onRevoke }`, `Table`, `Badge`, `Button`.
    - `CreateInvitationDialog`: `Dialog` + `Form` + `TextField`, `useForm`로 이메일 입력 검증.
  - 상태: `assistants`, `assistantStatusFilter`, `invitations`, `invitationStatusFilter`, `isCreating`.
  - Effects: `useEffect`로 필터 변경 시 fetch.
- `AssistantInvitationVerifyPage`
  - `useSearchParams`로 code → `apiClient.POST("/api/v1/auth/invitations/verify")`.
  - 상태: `verifyState`(idle/loading/success/error), `invitationInfo`.
  - 컴포넌트: `VerificationCard`, `ErrorState`.
- `AssistantRegisterPage`
  - `useSession`로 로그인 상태 확인, `useRouter` redirect.
    037676  - `useForm` with schema: name, password, confirm, phone, terms.
  - 요청 타입: `components["schemas"]["RegisterAssistantByInvitationRequest"]`.
  - 성공 시 `setSessionTokens` → `router.push("/assistant")`.

## 5. State & Data Flow
- API 타입: `paths["/teachers/me/assistants"]["get"]["responses"]["200"]["content"]["application/json"]...` etc. 실제 구현 시 `AssistantAssignmentResponse`/`InvitationSummaryResponse` DTO 맞춰 타입 alias 정의.
- 데이터 흐름:
  1. Teacher 페이지 진입 시 병렬로 assistants/invitations fetch.
  2. 토글 → `PATCH /assistants/{id}` → 로컬 목록 즉시 업데이트.
  3. 초대 생성 → 모달 닫고 invitations refetch → code 복사.
  4. Verify/Register는 `useMutation` 패턴으로 상태 관리, 에러 메시지는 `getApiErrorMessage`.
- 상태 저장소는 페이지 로컬 상태 중심, 전역 store 불필요.

## 6. Interaction & UX Details
- Teacher 페이지:
  - 토글 시 Optimistic UI + Toast (“조교가 비활성화되었습니다.”).
  - 초대 복사 버튼 클릭 시 clipboard 복사 및 Toast.
  - 모달에서 이메일 형식 검증, 서버 에러 메시지 카드 상단 표시.
  - 반응형: 모바일에서는 세로 스택, 리스트 → Accordion 형태.
- Verify 페이지:
  - 로딩 스켈레톤(blurred card), 성공 시 CTA 버튼, 실패 시 홈 이동 버튼.
  - 접근성: 초대 정보 텍스트에 `<dl>` 구조.
- Register 페이지:
  - 비밀번호 규칙 체크 리스트, 전화번호 자동 포맷, Terms 체크 미완 시 disabled.
  - Submit 중 로딩 스피너, 오류 입력 필드 하이라이트.

## 7. Test & Verification Plan
1. **타입/빌드 검증**: `cd frontend && npm run build -- --webpack`.
2. **수동 플로우**
   - Teacher 로그인 → `/dashboard/teacher/invitations` → 목록 표시, 토글/초대 생성/취소가 백엔드 API와 연동되는지 확인.
   - 초대 복사 → 새 브라우저에서 `/auth/invitation/verify?code=...` → 성공/실패 시각 확인.
   - 검증 페이지에서 “계속” 클릭 → `/auth/register/assistant?code=...` → 폼 제출 → `/assistant` 리디렉트 및 토큰 저장.
3. **엣지 케이스**
   - Assistants 빈 상태, 초대 만료 상태 필터.
   - Verify API 에러 (만료, 미존재) 표시.
   - Register에서 비밀번호 불일치, 전화번호 잘못된 포맷.
4. **로그 작성**: 구현 완료 후 AGENT_LOG에 BEHAVIORAL 기록 + TODO 상태 업데이트.

---

### 계획 요약 (한국어)
- 교사용 조교 관리 페이지에서 조교 목록/토글/초대 관리 UI를 구현하고, 조교 초대 검증 및 초대 기반 회원가입 페이지를 Season2 API에 맞춰 재작성한다.  
- Teacher 페이지는 새 백엔드 API들(`/api/v1/teachers/me/assistants`, `/api/v1/teachers/me/invitations`, `/api/v1/invitations`)과 모달/토글/필터 UI를 포함하며, 검증/회원가입 페이지는 `/api/v1/auth/invitations/verify`, `/api/v1/members/register/assistant` 플로우를 기준으로 상태/에러/리다이렉션을 정의한다.  
