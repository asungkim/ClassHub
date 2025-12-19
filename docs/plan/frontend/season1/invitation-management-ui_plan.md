# Feature: 초대 관리 UI (Frontend)

## 0. 사전 준비

### 환경변수 설정
- `.env.local.example`에 다음 환경변수 추가:
  ```env
  # Frontend application base URL
  NEXT_PUBLIC_APP_URL=http://localhost:3000

  # Backend API base URL (include /api/v1)
  NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
  ```
- 로컬 환경: `http://localhost:3000`
- 운영 환경: 실제 도메인 (예: `https://classhub.com`)

### 컴포넌트 설치
- ✅ Checkbox 컴포넌트: 이미 존재 (`src/components/ui/checkbox.tsx`)
- ❌ Table 컴포넌트: shadcn/ui에서 설치 필요
  ```bash
  cd frontend
  npx shadcn@latest add table
  ```

### 초대 URL 형식 (옵션 B: 통합 경로)
- **공통 경로**: `{APP_URL}/auth/register?code={invitationCode}`
- 회원가입 페이지에서 `GET /api/v1/auth/invitations/verify` API를 호출하여 `inviteeRole`을 확인하고, 역할에 맞는 폼을 동적으로 표시합니다.
- 일반 회원가입(초대 없음)은 존재하지 않습니다. 모든 회원가입은 초대 코드를 통해서만 가능합니다.

## 1. Problem Definition

- 백엔드의 신규 초대 기능(조교 공용 링크, 학생 개별 링크)이 구현되었지만, 이를 관리할 수 있는 프론트엔드 UI가 부재합니다.
- 강사(Teacher)와 조교(Assistant)는 역할에 따라 초대 링크를 생성하고, 초대 대상 목록을 관리하며, 생성된 초대 현황을 조회할 수 있는 전용 페이지가 필요합니다.
- 이 계획은 `invitation-link-flow_plan.md`에 정의된 API를 활용하여 역할 기반 초대 관리 페이지를 설계하고 구현하는 것을 목표로 합니다.

## 2. Requirements

### Functional

- **접근 제어**: `useRoleGuard` 훅(`src/hooks/use-role-guard.tsx`)을 사용하여 Teacher/Assistant만 접근하도록 제한합니다.
- **위치**: 대시보드 좌측 메뉴에 "초대 관리" 항목을 추가하고, 각각 별도 페이지로 진입합니다.
  - 경로: `/dashboard/invitations/assistant` (Teacher 전용), `/dashboard/invitations/student` (Teacher/Assistant)
  - **구현 시**: `src/components/dashboard/dashboard-shell.tsx`의 `sidebarItems` 배열에 아래처럼 역할별 조건부 렌더링:

    **Teacher:**
    ```tsx
    {
      label: "초대 관리",
      icon: <UsersIcon />,
      sub: [
        { label: "조교 초대", href: "/dashboard/invitations/assistant" },
        { label: "학생 초대", href: "/dashboard/invitations/student" }
      ]
    }
    ```

    **Assistant:**
    ```tsx
    {
      label: "초대 관리",
      href: "/dashboard/invitations/student",
      icon: <UsersIcon />
    }
    ```

#### 조교 초대 페이지 (`/dashboard/invitations/assistant`) - Teacher 전용

- **목록 조회**:
  - API: `GET /api/v1/invitations/assistant?status={PENDING|REVOKED|...}`
  - 컬럼: 초대 코드, 상태(PENDING, REVOKED 등), 누적 사용 횟수, 만료일, 생성일
  - 필터: 상태(전체/활성/만료/취소) 필터링 기능
- **링크 생성/회전**:
  - 버튼: 페이지 상단 "조교 초대 링크 생성/갱신" 버튼
  - API: `POST /api/v1/invitations/assistant/link`
  - 성공 시: 목록 리프레시 + 성공 토스트("새로운 조교 초대 링크가 생성되었습니다.")
  - 실패 시: 에러 토스트
- **링크 복사**:
  - 각 행의 활성(PENDING) 링크 옆에 "복사" 버튼
  - 복사할 URL: `${process.env.NEXT_PUBLIC_APP_URL}/auth/register?code=${invitation.code}`
  - 복사 성공 시 토스트: "초대 링크가 복사되었습니다."

#### 학생 초대 페이지 (`/dashboard/invitations/student`) - Teacher/Assistant

**Tab 구성 (2개 탭):**
- **탭 1: "초대 가능한 학생"** - 후보 목록 + 일괄 선택/초대 생성
- **탭 2: "생성된 초대"** - 이미 생성된 학생 초대 목록 (PENDING, ACCEPTED 등)

**탭 1: 초대 가능한 학생**
- **후보 목록 조회**:
  - API: `GET /api/v1/invitations/student/candidates?name={검색어}`
  - 컬럼: 체크박스, 학생 이름, 나이, 학년, 소속 코스명
  - 필터: 이름 검색
- **일괄 초대**:
  - 테이블 헤더에 "전체 선택/해제" 체크박스 제공
  - "초대 생성" 버튼은 1명 이상 선택 시 활성화
  - API: `POST /api/v1/invitations/student` (body: `{ studentProfileIds: [...] }`)
  - 성공 시:
    1. 선택 체크박스 전체 초기화 (`setSelectedIds([])`)
    2. 후보 목록 리프레시 (React Query invalidate)
    3. 성공 토스트: "{N}명의 학생에게 초대가 생성되었습니다."
    4. 탭 2 (생성된 초대)의 목록도 자동 리프레시

**탭 2: 생성된 초대**
- API: `GET /api/v1/invitations/student?status=...`
- 컬럼: 학생 이름, 초대 코드, 상태, 생성일
- 필터: 상태(전체/대기/수락/만료) 필터링 기능

#### UI 상태 및 메시지

- **로딩**: 스켈레톤 UI (테이블 행 5개)
- **빈 상태**:
  - 조교 초대: "생성된 조교 초대 링크가 없습니다."
  - 학생 후보: "초대할 수 있는 학생이 없습니다."
  - 생성된 학생 초대: "생성된 학생 초대가 없습니다."
- **에러**: API 실패 시 테이블 영역에 에러 메시지 + "재시도" 버튼 표시 (토스트 X)
- **Toast 라이브러리**: `sonner` 사용 (이미 설치됨)
- **반응형**: Desktop(md 이상)은 테이블, Mobile은 카드형 리스트로 자동 전환

### Non-functional

- **컴포넌트 재사용**: 기존 `Button`, `Card`, `Badge`, `Input`, `Skeleton`, `Checkbox` 등을 최대한 활용합니다.
- **타입**: `src/types/openapi.d.ts`의 API 스키마를 기반으로 타입을 선언합니다.
- **상태 관리**: React Query (`@tanstack/react-query`)를 사용하여 서버 상태를 관리합니다.
- **빌드 검증**: 구현 후 `cd frontend && npm run build -- --webpack`을 실행하여 타입/빌드 오류를 확인합니다.

## 3. API Design (Frontend usage)

| 목적 | Method | URL | Query/Body | Notes |
|---|---|---|---|---|
| 조교 초대 링크 생성 | POST | `/api/v1/invitations/assistant/link` | - | Teacher 전용 |
| 조교 초대 목록 | GET | `/api/v1/invitations/assistant` | `status?` | Teacher 전용 |
| 학생 초대 후보 | GET | `/api/v1/invitations/student/candidates` | `name?` | Teacher/Assistant |
| 학생 초대 생성 | POST | `/api/v1/invitations/student` | `{ studentProfileIds: [...] }` | Teacher/Assistant |
| 학생 초대 목록 | GET | `/api/v1/invitations/student` | `status?` | Teacher/Assistant |

## 4. UI/State Structure

### 페이지 구조

- `/dashboard/invitations/assistant` (Teacher 전용)
- `/dashboard/invitations/student` (Teacher/Assistant)

### React Query Hooks

```typescript
// 조교 초대 목록
useAssistantInvitations(filters: { status?: string })
  → queryKey: ["assistant-invitations", filters]
  → API: GET /api/v1/invitations/assistant

// 조교 링크 생성
useCreateAssistantLink()
  → mutationFn: POST /api/v1/invitations/assistant/link
  → onSuccess: invalidateQueries(["assistant-invitations"]), toast.success()

// 학생 초대 후보 목록
useStudentCandidates(filters: { name?: string })
  → queryKey: ["student-candidates", filters]
  → API: GET /api/v1/invitations/student/candidates

// 학생 초대 생성
useCreateStudentInvitations()
  → mutationFn: POST /api/v1/invitations/student
  → onSuccess:
    - invalidateQueries(["student-candidates", "student-invitations"])
    - toast.success("{N}명의 학생에게 초대가 생성되었습니다.")

// 학생 초대 목록
useStudentInvitations(filters: { status?: string })
  → queryKey: ["student-invitations", filters]
  → API: GET /api/v1/invitations/student
```

### 공유 컴포넌트

#### InvitationStatusBadge 색상 매핑

| 상태 | 색상 variant | 텍스트 |
|---|---|---|
| PENDING | blue/default | 대기 중 |
| ACCEPTED | green/success | 수락됨 |
| REVOKED | gray/secondary | 취소됨 |
| EXPIRED | red/destructive | 만료됨 |

#### 기타 공유 컴포넌트

- **`EmptyState`**: 데이터가 없을 때 표시할 메시지 컴포넌트
- **`LoadingSkeleton`**: 테이블 로딩 시 표시할 스켈레톤 UI

### 반응형 Mobile 카드 레이아웃

**조교 초대 카드:**
- 초대 코드 (앞 8자 표시, 예: `a1b2c3d4...`)
- 상태 뱃지 (우측 상단)
- 사용 횟수 (예: `5회 사용`) / 만료일
- 복사 버튼 (하단)

**학생 초대 후보 카드:**
- 체크박스 (좌측)
- 학생 이름 (중앙 상단, 강조)
- 학년, 나이 (중앙 하단)
- 코스명 (하단, 작은 텍스트)

**생성된 학생 초대 카드:**
- 학생 이름 (상단)
- 초대 코드 (일부 마스킹, 예: `a1b2c...xyz`)
- 상태 뱃지 (우측)
- 생성일 (하단)

## 5. 테스트 & 검증 계획

### 1. 타입 검증 (필수)

- **실행**: `cd frontend && npm run build -- --webpack`
- **확인**: TypeScript 컴파일 에러 0개, 빌드 성공

### 2. 수동 시나리오 테스트 (필수)

#### Teacher 계정

**조교 초대 페이지 (`/dashboard/invitations/assistant`)**
1. 페이지 진입 → 초대 목록 로딩 및 표시 확인
2. "링크 생성" 버튼 클릭 → 성공 토스트 → 새 링크가 목록 최상단에 PENDING 상태로 추가되는지 확인
3. 기존 PENDING 링크가 있었다면 REVOKED 상태로 변경되는지 확인
4. 활성 링크의 "복사" 버튼 클릭 → 클립보드에 URL 복사 확인 → "초대 링크가 복사되었습니다." 토스트 표시

**학생 초대 페이지 (`/dashboard/invitations/student`)**
1. 페이지 진입 → 탭 1 "초대 가능한 학생" 활성화 → 후보 목록 표시 확인
2. 이름으로 학생 검색 기능 동작 확인
3. "전체 선택" 체크박스 클릭 → 모든 학생 선택/해제 확인
4. 여러 학생 선택 후 "초대 생성" 클릭 → 성공 토스트 → 선택 체크박스 초기화 → 선택했던 학생들이 후보 목록에서 사라지는지 확인
5. 탭 2 "생성된 초대"로 이동 → 방금 생성된 초대들이 PENDING 상태로 표시되는지 확인

#### Assistant 계정

1. 사이드바에서 '초대 관리' 클릭 시 학생 초대 페이지(`/dashboard/invitations/student`)로 바로 이동하는지 확인
2. `/dashboard/invitations/assistant` URL로 직접 접근 시도 시 대시보드 홈으로 리디렉션되는지 확인 (useRoleGuard)
3. 학생 초대 페이지의 모든 기능(목록 조회, 검색, 선택, 생성, 탭 전환)이 Teacher와 동일하게 동작하는지 확인

### 3. 에러/엣지 케이스 테스트

- API 실패 시 테이블 영역에 "재시도" 버튼이 포함된 에러 메시지 표시 확인
- 초대할 학생이 없을 때 "초대할 수 있는 학생이 없습니다." 메시지 표시 확인
- 생성된 초대가 없을 때 "생성된 학생 초대가 없습니다." 메시지 표시 확인
- 네트워크가 느릴 때 로딩 스켈레톤 UI가 잘 표시되는지 확인
- 학생을 선택하지 않고 "초대 생성" 버튼이 비활성화되는지 확인

### 4. 검증 완료 체크리스트

- [ ] `npm run build -- --webpack` 성공
- [ ] Table 컴포넌트 shadcn/ui 설치 완료
- [ ] 환경변수 `NEXT_PUBLIC_APP_URL` 설정 완료
- [ ] Teacher: 조교 초대 링크 생성/회전/목록 조회/복사 기능 동작
- [ ] Teacher/Assistant: 학생 후보 목록 조회 및 일괄 초대 기능 동작
- [ ] Teacher/Assistant: 생성된 학생 초대 목록 조회 기능 동작 (탭 2)
- [ ] 역할에 따른 페이지 접근 및 메뉴 노출 제어 확인
- [ ] 모든 API 연동 기능(로딩, 성공, 실패, 빈 상태) UI 확인
- [ ] 반응형 UI (Desktop 테이블 / Mobile 카드) 확인

## 6. 구현 순서 (권장)

1.  **사전 준비**:
    - 환경변수 `.env.local.example` 업데이트 및 로컬 `.env` 파일 생성
    - Table 컴포넌트 설치: `npx shadcn@latest add table`

2.  **공통 컴포넌트 구현**:
    - `InvitationStatusBadge` (상태별 색상 뱃지)
    - `EmptyState` (빈 상태 메시지)
    - `LoadingSkeleton` (테이블 로딩)

3.  **API Hooks 구현**:
    - `src/hooks/queries/invitations.ts` 파일 생성
    - 위 "React Query Hooks" 섹션에 정의된 5개 훅 모두 구현:
      - `useAssistantInvitations`
      - `useCreateAssistantLink`
      - `useStudentCandidates`
      - `useCreateStudentInvitations`
      - `useStudentInvitations`

4.  **조교 초대 페이지 구현**:
    - `/dashboard/invitations/assistant/page.tsx` 생성
    - `useRoleGuard(["TEACHER"])` 적용
    - 정적 UI: 헤더 + "링크 생성" 버튼 + 테이블
    - `useAssistantInvitations` 훅 연동 (목록 표시)
    - `useCreateAssistantLink` 훅 연동 (링크 생성)
    - 클립보드 복사 기능 구현
    - 반응형: Desktop 테이블 / Mobile 카드

5.  **학생 초대 페이지 구현**:
    - `/dashboard/invitations/student/page.tsx` 생성
    - `useRoleGuard(["TEACHER", "ASSISTANT"])` 적용
    - 탭 UI 구현 (Tabs 컴포넌트 활용)
    - **탭 1**: 후보 테이블 + 검색 + 체크박스 선택 + "초대 생성" 버튼
      - `useStudentCandidates` 훅 연동
      - `useState`로 선택 상태 관리
      - `useCreateStudentInvitations` 훅 연동
    - **탭 2**: 생성된 초대 목록 테이블
      - `useStudentInvitations` 훅 연동
    - 반응형: Desktop 테이블 / Mobile 카드

6.  **네비게이션 및 접근 제어**:
    - `dashboard-shell.tsx` 사이드바 메뉴 수정
      - Teacher: "초대 관리" 서브메뉴 (조교 초대, 학생 초대)
      - Assistant: "초대 관리" 단일 링크 (학생 초대)
    - `useSession` 훅으로 역할 확인하여 조건부 렌더링

7.  **최종 검증**:
    - 타입 빌드 검증: `cd frontend && npm run build -- --webpack`
    - "테스트 & 검증 계획" 섹션의 모든 시나리오 테스트
    - `docs/history/AGENT_LOG.md` 업데이트
