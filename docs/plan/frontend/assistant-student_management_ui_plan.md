# Feature: 조교/학생 목록 관리 UI (Frontend)

## 1. Problem Definition

- Teacher가 조교/학생을 관리할 화면이 없어 백엔드 목록/비활성화/조회 API를 사용할 수 없다.
- Assistant는 학생 목록을 읽기만 해야 하지만, 이를 구분하는 UI/권한 처리가 없다.
- 활성/비활성 필터, 이름 검색 등을 제공해 실제 데이터를 빠르게 조회할 수 있는 화면이 필요하다.
- StudentProfile 생성/수정/삭제(퇴원) 기능을 통해 Teacher가 학생 데이터를 관리할 수 있어야 한다.
- 모든 화면은 대시보드 내 좌측 메뉴(Teacher: 조교 관리/학생 관리, Assistant: 학생 관리) 하위 섹션으로 진입한다.

## 2. Requirements

### Functional

- **접근 제어**: `useRoleGuard`로 Teacher/Assistant만 접근. Teacher는 조교/학생 관리 모두 가능, Assistant는 학생 관리만 가능.
- **위치**: 대시보드 좌측 메뉴에 "조교 관리"(Teacher 전용), "학생 관리"(Teacher/Assistant) 항목을 추가하고, 각각 별도 페이지로 진입.
  - 경로: `/dashboard/assistants` (Teacher 전용), `/dashboard/students` (Teacher/Assistant)
  - **구현 시**: `src/components/dashboard/dashboard-shell.tsx`의 `sidebarItems` 배열에 아래 항목 추가:
    - `{ label: "조교 관리", href: "/dashboard/assistants" as Route }` (Teacher일 때만)
    - `{ label: "학생 관리", href: "/dashboard/students" as Route }` (Teacher/Assistant)

#### 조교 관리 (Teacher 전용)

- **목록 조회**:
  - API: `GET /api/v1/members?role=ASSISTANT&active={true|false}&name={검색어}&page={0}&size={20}`
  - 컬럼: 이름, 이메일, 상태(active), 생성일
  - 필터: active 토글(전체/활성/비활성), 이름 검색
  - 페이지네이션: 하단에 페이지 번호 버튼, 기본 size=20, sort=createdAt,desc
- **비활성화**:
  - 버튼: 각 행의 "비활성화" 버튼 (active=false인 경우 disabled)
  - API: `PATCH /api/v1/members/{id}/deactivate`
  - 성공 시: 목록 리프레시 + 성공 토스트("{이름} 조교를 비활성화했습니다.")
  - 실패 시: 에러 토스트 + 재시도 버튼

#### 학생 관리 (Teacher/Assistant)

- **목록 조회**:
  - API: `GET /api/v1/student-profiles?active={true|false}&name={검색어}&page={0}&size={20}`
  - 컬럼: 이름, 전화번호, 상태(active), 나이, 학년, 생성일
  - 필터: active 토글(전체/활성/비활성), 이름 검색
  - 페이지네이션: 조교 목록과 동일
  - **코스 필터는 Phase 4 Course CRUD 이후 추가 예정**
- **생성 (Teacher 전용)**:
  - 버튼: 목록 우상단 "학생 등록" 버튼
  - 폼: 이름, 전화번호, 나이, 학년 등 입력 (StudentProfileCreateRequest 타입 기준)
  - API: `POST /api/v1/student-profiles`
  - 성공 시: 목록 페이지로 이동 + 성공 토스트
- **수정 (Teacher 전용)**:
  - 버튼: 각 행의 "수정" 버튼 (Assistant에게는 미노출)
  - 폼: 생성과 동일한 필드 (기존 값 pre-fill)
  - API: `PATCH /api/v1/student-profiles/{id}`
  - 성공 시: 목록으로 복귀 + 성공 토스트
- **퇴원/삭제 (Teacher 전용)**:
  - 버튼: 각 행의 "퇴원" 버튼, 확인 모달 필요
  - API: `DELETE /api/v1/student-profiles/{id}`
  - 성공 시: 목록 리프레시 + 성공 토스트("{이름} 학생을 퇴원 처리했습니다.")
  - **주의**: 퇴원 시 해당 학생의 Member.active도 false가 되어 로그인 차단됨

#### UI 상태

- **로딩**: 스켈레톤 UI (테이블 행 5개 placeholder)
- **빈 상태**:
  - 조교 목록: "등록된 조교가 없습니다. 초대 코드를 생성해 조교를 초대하세요."
  - 학생 목록: "등록된 학생이 없습니다. 학생을 등록하거나 초대하세요."
- **에러**: API 실패 시 에러 메시지 + "재시도" 버튼
- **반응형**: Desktop(md 이상)은 테이블, Mobile은 카드형 리스트 자동 전환

#### 메시지

- **Toast 라이브러리**: `sonner` 사용
- **성공 메시지**:
  - 조교 비활성화: "{이름} 조교를 비활성화했습니다."
  - 학생 생성: "{이름} 학생을 등록했습니다."
  - 학생 수정: "{이름} 학생 정보를 수정했습니다."
  - 학생 퇴원: "{이름} 학생을 퇴원 처리했습니다."
- **에러 메시지**: `getApiErrorMessage(error)` 기본 사용

### Non-functional

- **컴포넌트 재사용 우선**: 기존 Button/Card/Badge/Input/Skeleton 활용. Table 컴포넌트가 없다면 간단한 `<table>` 마크업 사용. 모달/확인 다이얼로그는 HTML `<dialog>` 또는 간단한 조건부 렌더링. 새 베이스 컴포넌트 추가는 지양.
- **타입**: 최신 `src/types/openapi.d.ts` 기준.
  - 조교: `paths["/api/v1/members"]["get"]`, `paths["/api/v1/members/{memberId}/deactivate"]["patch"]`
  - 학생: `paths["/api/v1/student-profiles"]["get"]`, `paths["/api/v1/student-profiles"]["post"]`, `paths["/api/v1/student-profiles/{id}"]["patch"]`, `paths["/api/v1/student-profiles/{id}"]["delete"]`
  - Request/Response는 `components["schemas"][...]` alias 사용
- **상태 관리**: React Query (`@tanstack/react-query`) 사용
  - queryKey: `["assistants", { active, name, page }]`, `["student-profiles", { active, name, page }]`
  - mutation 후 해당 queryKey invalidate하여 자동 리프레시
- **반응형**: Tailwind breakpoint `md:` (768px) 기준. Desktop은 `<table>`, Mobile은 카드형 `<div>` 리스트로 조건부 렌더링 또는 CSS 클래스 분기.
- **빌드 검증**: 구현 후 반드시 `cd frontend && npm run build -- --webpack` 실행하여 타입/빌드 오류 확인.

## 3. API Design (Frontend usage)

### 조교 관리

| 목적          | Method | URL                                     | Query/Body                                                                          | Notes              |
| ------------- | ------ | --------------------------------------- | ----------------------------------------------------------------------------------- | ------------------ |
| 조교 목록     | GET    | `/api/v1/members`                       | `role=ASSISTANT&active={bool}&name={string}&page={int}&size=20&sort=createdAt,desc` | Teacher 전용       |
| 조교 비활성화 | PATCH  | `/api/v1/members/{memberId}/deactivate` | body `{}` (empty)                                                                   | Teacher 전용, 멱등 |

### 학생 관리

| 목적      | Method | URL                             | Query/Body                                                           | Notes                     |
| --------- | ------ | ------------------------------- | -------------------------------------------------------------------- | ------------------------- |
| 학생 목록 | GET    | `/api/v1/student-profiles`      | `active={bool}&name={string}&page={int}&size=20&sort=createdAt,desc` | Teacher/Assistant         |
| 학생 생성 | POST   | `/api/v1/student-profiles`      | `StudentProfileCreateRequest`                                        | Teacher 전용              |
| 학생 수정 | PATCH  | `/api/v1/student-profiles/{id}` | `StudentProfileUpdateRequest`                                        | Teacher 전용              |
| 학생 퇴원 | DELETE | `/api/v1/student-profiles/{id}` | -                                                                    | Teacher 전용, soft delete |

## 4. UI/State Structure

### 페이지 구조

#### `/dashboard/assistants` (Teacher 전용)

- **레이아웃**: DashboardShell로 감싸기, title="조교 관리"
- **필터 바**:
  - active 선택(전체/활성/비활성) - `<select>` 또는 버튼 그룹
  - 이름 검색 - `<Input type="text" placeholder="이름 검색...">`
  - 검색 버튼 또는 onChange 자동 반영
- **목록**:
  - Desktop: `<table>` 마크업 (th: 이름, 이메일, 상태, 생성일, 액션)
  - Mobile: 카드형 리스트
  - 각 행: 비활성화 버튼 (active=false면 disabled)
- **페이지네이션**: 하단에 `<< 1 2 3 ... >>` 버튼

#### `/dashboard/students` (Teacher/Assistant)

- **레이아웃**: DashboardShell, title="학생 관리"
- **상단**: Teacher일 경우 "학생 등록" 버튼 노출
- **필터 바**: active 선택, 이름 검색 (코스 필터는 미래 추가)
- **목록**:
  - Desktop: `<table>` (th: 이름, 전화, 나이, 학년, 상태, 생성일, 액션)
  - Mobile: 카드형
  - Teacher: 수정/퇴원 버튼, Assistant: 버튼 없음
- **페이지네이션**: 조교 목록과 동일

#### `/dashboard/students/new` (Teacher 전용)

- **레이아웃**: DashboardShell, title="학생 등록"
- **폼**: 이름, 전화번호, 나이, 학년 등 입력 필드 (openapi 스키마 참고)
- **버튼**: 등록 / 취소

#### `/dashboard/students/{id}/edit` (Teacher 전용)

- **레이아웃**: DashboardShell, title="학생 정보 수정"
- **폼**: 생성 폼과 동일, 기존 값 pre-fill
- **버튼**: 저장 / 취소

### React Query Hooks

```typescript
// 조교 목록
useAssistantList(filters: { active?: boolean; name?: string; page: number })
  → queryKey: ["assistants", filters]
  → API: GET /api/v1/members?role=ASSISTANT&...

// 조교 비활성화
useDeactivateAssistant()
  → mutationFn: PATCH /api/v1/members/{id}/deactivate
  → onSuccess: invalidateQueries(["assistants"])

// 학생 목록
useStudentProfileList(filters: { active?: boolean; name?: string; page: number })
  → queryKey: ["student-profiles", filters]
  → API: GET /api/v1/student-profiles?...

// 학생 생성
useCreateStudentProfile()
  → mutationFn: POST /api/v1/student-profiles
  → onSuccess: router.push('/dashboard/students'), toast.success()

// 학생 수정
useUpdateStudentProfile()
  → mutationFn: PATCH /api/v1/student-profiles/{id}
  → onSuccess: invalidateQueries(["student-profiles"]), toast.success()

// 학생 퇴원
useDeleteStudentProfile()
  → mutationFn: DELETE /api/v1/student-profiles/{id}
  → onSuccess: invalidateQueries(["student-profiles"]), toast.success()
```

### 공유 컴포넌트

- **Skeleton**: 로딩 중 테이블/카드 placeholder (기존 컴포넌트 재사용)
- **Empty**: 빈 상태 메시지 표시
- **ErrorMessage**: 에러 표시 + 재시도 버튼
- **Toast**: `sonner`의 `<Toaster />` 컴포넌트를 layout에 추가

## 5. 테스트 & 검증 계획

### 1. 타입 검증

- **실행**: `cd frontend && npm run build -- --webpack`
- **확인**: TypeScript 컴파일 에러 0개, 빌드 성공

### 2. 컴포넌트 단위 테스트 (선택, 시간 허용 시)

현재 프로젝트에 테스트 프레임워크가 없으므로 **수동 검증 우선**. 향후 React Testing Library 도입 시:

- 필터 변경 → queryKey 갱신 확인
- Teacher/Assistant role별 버튼 노출 여부
- Mutation 성공 시 invalidate 호출 확인

### 3. 수동 시나리오 테스트 (필수)

#### Teacher 계정

**조교 관리 (`/dashboard/assistants`)**

1. 페이지 진입 → 목록 로딩 → 데이터 표시 확인
2. 필터 테스트:
   - active="전체" → 모든 조교 표시
   - active="활성" → active=true만 표시
   - active="비활성" → active=false만 표시
   - 이름 검색 → 부분 일치 조교 표시
3. 비활성화:
   - 활성 조교 행의 "비활성화" 클릭 → 확인 → 성공 토스트 → 목록 리프레시
   - 이미 비활성된 조교는 버튼 disabled 확인
4. 페이지네이션:
   - 페이지 2 클릭 → URL 변경 → 다음 20개 표시
   - 이전/다음 버튼 동작 확인

**학생 관리 (`/dashboard/students`)**

1. 목록 조회:
   - 페이지 진입 → 데이터 표시
   - 필터(active, 이름 검색) 동작 확인
2. 학생 등록:
   - "학생 등록" 버튼 → `/dashboard/students/new` 이동
   - 폼 입력 → "등록" 클릭 → 성공 토스트 → 목록으로 복귀
   - 목록에서 새 학생 확인
3. 학생 수정:
   - 학생 행의 "수정" 버튼 → `/dashboard/students/{id}/edit` 이동
   - 기존 값 pre-fill 확인
   - 값 변경 → "저장" → 성공 토스트 → 목록 복귀
4. 학생 퇴원:
   - "퇴원" 버튼 → 확인 모달 → "확인" 클릭
   - 성공 토스트 → 목록 리프레시 → 해당 학생 active=false 확인
5. 페이지네이션: 조교 관리와 동일

#### Assistant 계정

1. `/dashboard/assistants` 접근 → 403 또는 리다이렉트 (Teacher 전용)
2. `/dashboard/students` 접근 → 목록 조회 가능
3. "학생 등록" 버튼 **미노출** 확인
4. 각 학생 행에 "수정", "퇴원" 버튼 **미노출** 확인
5. 필터/페이지네이션 정상 동작

### 4. 에러/엣지 케이스 테스트

- **API 실패**: 백엔드 중단 시 에러 메시지 + "재시도" 버튼 확인
- **빈 데이터**: 조교/학생 0건일 때 빈 상태 메시지 표시
- **로딩 상태**: 네트워크 느릴 때 스켈레톤 UI 표시
- **권한 없음**: Assistant가 Teacher 전용 페이지 접근 시 차단

### 5. 반응형 테스트

- **Desktop (≥768px)**: 테이블 레이아웃 확인
- **Mobile (<768px)**: 카드형 리스트로 전환 확인
- **사이드바**: 모바일에서 햄버거 메뉴 동작 확인

### 6. 검증 완료 체크리스트

- [ ] `npm run build -- --webpack` 성공
- [ ] Teacher: 조교 목록/필터/비활성화 동작
- [ ] Teacher: 학생 목록/등록/수정/퇴원 동작
- [ ] Assistant: 학생 목록 조회만 가능, 수정 버튼 미노출
- [ ] Toast 메시지 정상 표시
- [ ] 페이지네이션 정상 동작
- [ ] 에러 처리 (API 실패, 빈 데이터)
- [ ] 반응형 UI (Desktop/Mobile)

---

## 6. 구현 순서 (권장)

1. **설정**:

   - `sonner` 설치: `npm install sonner`
   - `<Toaster />` 컴포넌트를 `app/layout.tsx`에 추가

2. **조교 관리** (단순한 쪽부터):

   - `/dashboard/assistants` 페이지 생성
   - `useAssistantList` 훅 구현
   - 필터 바 + 테이블 렌더링
   - `useDeactivateAssistant` 훅 + 비활성화 버튼
   - 페이지네이션
   - 빌드 검증 + 수동 테스트

3. **학생 목록**:

   - `/dashboard/students` 페이지 생성
   - `useStudentProfileList` 훅
   - 필터 바 + 테이블
   - Teacher/Assistant role별 버튼 분기
   - 페이지네이션
   - 빌드 검증 + 수동 테스트

4. **학생 생성**:

   - `/dashboard/students/new` 페이지
   - `useCreateStudentProfile` 훅
   - 폼 입력 + 검증
   - 성공 시 목록으로 이동
   - 테스트

5. **학생 수정**:

   - `/dashboard/students/[id]/edit` 페이지
   - `useUpdateStudentProfile` 훅
   - 기존 데이터 fetch + pre-fill
   - 성공 시 목록으로 복귀
   - 테스트

6. **학생 퇴원**:

   - `useDeleteStudentProfile` 훅
   - 확인 모달 (HTML `<dialog>` 또는 조건부 렌더링)
   - 성공 시 목록 리프레시
   - 테스트

7. **반응형 처리**:

   - 모바일 카드형 컴포넌트 추가
   - Tailwind breakpoint 적용
   - 모바일 기기에서 테스트

8. **최종 검증**:
   - 전체 시나리오 재테스트
   - 빌드 + 타입 체크
   - AGENT_LOG 업데이트
