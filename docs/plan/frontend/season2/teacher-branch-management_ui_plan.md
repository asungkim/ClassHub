# Feature: Teacher/Admin Branch Management UI

## 1. Problem Definition
- Season2 요구사항에 따라 Teacher가 출강 학원을 직접 등록/연결하고, SuperAdmin이 Company/Branch를 검증/비활성화할 수 있는 UI가 필요하다.
- 백엔드 API(Company/Branch CRUD, TeacherBranchAssignment, 검증 토글)가 TDD로 준비되었으나, 프런트에서는 기존 Season1 구조를 재사용하고 있어 신규 흐름(모달 분기, 상태 필터, verifications 페이지)이 전혀 반영되지 않았다.
- 목표는 관리자와 선생님 UI 모두에서 Company/Branch 상태를 조회·변경하고 분기별 Assignment 생성(개인/회사 학원, 신규 회사/지점 입력)을 지원하는 화면을 완성하는 것이다.

## 2. User Flows & Use Cases
### SuperAdmin (검증 관리)
1. 대시보드 사이드바 → “검증 관리” → 하위 메뉴 “회사 검증”, “지점 검증”.
2. 회사 검증 페이지: UNVERIFIED / VERIFIED 전체 목록 + 상태 필터 → 각 행에서 `PATCH /admin/companies/{id}/verified-status` 호출.
3. 지점 검증 페이지: 동일 패턴으로 `/admin/branches/{id}/verified-status`.

### Teacher (학원 관리)
1. 사이드바 “학원 관리” → 학원 관리 페이지 진입.
2. 페이지 상단: TeacherBranchAssignment 목록(활성/비활성 필터, pagination).
3. “학원 등록” 버튼 → 모달에서 개인/회사 학원 분기를 선택.
   - 개인 학원: 회사명 + 지점명 입력 → `mode=NEW_INDIVIDUAL`.
   - 회사 학원:
     - 기존 회사/지점 선택: API로 검색 후 branchId 전달(`mode=EXISTING_BRANCH`).
     - 회사 없음: 회사명/지점명 입력(`mode=NEW_COMPANY`).
     - 회사 있음+지점 없음: 회사 선택 + 지점명 입력(`mode=NEW_BRANCH`).
4. 목록 카드/행마다 활성/비활성 토글 버튼 → `PATCH /teachers/me/branches/{assignmentId}`.

## 3. Page & Layout Structure
- 라우트: `/dashboard/admin/approvals/company`, `/dashboard/admin/approvals/branch`, `/dashboard/teacher/companies`.
- 공통 레이아웃 재사용(NavigationBar, Sidebar).
- 각 페이지: 상단 필터/버튼 바 + 데이터 테이블(또는 카드) + 페이지네이션.
- 모달 컴포넌트: 단계별 입력(Form Wizard 느낌 없이 단일 모달 내 탭/라디오 + 동적 폼).

## 4. Component Breakdown
- `CompanyVerificationTable`: props => data list, onToggle(status).
- `BranchVerificationTable`: 동일 구조 + branch-specific columns.
- `TeacherBranchList`: Teacher assignment 리스트 + status 필터/페이지네이션.
- `BranchAssignmentModal`: mode 선택 라디오 + 각 모드별 입력 필드, 제출 시 API 호출.
- 공통 Button/Form/Input은 기존 UI 컴포넌트 재사용.

## 5. State & Data Flow
- 데이터 소스: `paths["/api/v1/admin/companies"]["get"]`, `/admin/branches`, `/teachers/me/branches`, `/teachers/me/branches/search helper? (companies/branches)`.
- React Query(or existing fetch hook)로 목록 캐싱.
- 모달 내부: mode state + form state; 제출 성공 시 목록 refetch.
- 오류 시 `getApiErrorMessage`로 토스트 표준화.

## 6. Interaction & UX Details
- 목록 페이지: 상태 필터 드롭다운(VERIFIED/UNVERIFIED/all) → API query params 매핑.
- 모달: mode 선택 시 해당 필드만 노출; Submit 버튼 disabled 조건 = 필수 입력 여부.
- 활성/비활성 토글: optimistic update or refetch.
- 로딩/빈 상태 UI: Skeleton + Empty state 문구.
- 접근성: mode 라디오/입력에 label 연결, 모달 focus trap.

## 7. Test & Verification Plan
- Type check: `cd frontend && npm run build -- --webpack`.
- Manual QA:
  1. SuperAdmin 계정으로 회사/지점 검증 페이지에서 상태 전환 확인.
  2. Teacher 계정에서 학원 관리 페이지 진입 → 기존 지점 선택, 개인 학원/회사 학원 신규 입력 흐름 각각 테스트.
  3. Assignment 활성/비활성 토글, 상태 필터/페이지네이션 확인.
- 필요 시 Storybook/visual 검증 TBD.
