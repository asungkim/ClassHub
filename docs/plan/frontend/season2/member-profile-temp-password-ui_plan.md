# Feature: 내 정보/임시 비밀번호 UI

## 1. Problem Definition
- 로그인 사용자가 자신의 정보를 확인/수정할 수 있는 전용 화면이 필요하다.
- 이메일/휴대폰 입력으로 임시 비밀번호를 발급받는 “비밀번호 찾기” 화면이 필요하다.
- Student는 Member 정보와 StudentInfo를 함께 보여줘야 한다.

## 2. User Flows & Use Cases
### 2.1 내 정보 (로그인 상태)
1. 사용자는 사이드바의 사용자 카드(이름/이메일 영역)를 클릭한다.
2. 역할별 내 정보 페이지로 이동한다.
3. 서버에서 `/api/v1/members/me`를 호출해 현재 정보를 조회한다.
4. 사용자는 필요한 항목만 수정하고 저장한다.
5. 성공 시 저장 완료 메시지 + 사이드바 표시 정보도 갱신된다.

### 2.2 임시 비밀번호 발급 (로그아웃 상태)
1. 로그인 화면의 “비밀번호 찾기”를 클릭한다.
2. 이메일 + 휴대폰 번호 입력 후 발급 버튼을 누른다.
3. 서버가 본인 확인 후 `Classmate####!` 형식의 임시 비밀번호를 응답한다.
4. 화면에 `임시 비밀번호: Classmate1234!` 형태로 표시한다.
5. 사용자는 해당 비밀번호로 로그인하고, 내 정보 페이지에서 새 비밀번호로 변경한다.

### 2.3 임시 비밀번호 로그인 후 안내
1. 사용자가 로그인 폼에서 `Classmate####!` 패턴으로 로그인하면 “비밀번호 변경 안내” 모달을 즉시 노출한다.
2. 확인 버튼을 누르면 역할별 내 정보 수정 페이지로 이동한다.
3. 안내 모달은 한 번만 표시되도록 라우팅 상태로 제어한다.

## 3. Page & Layout Structure
### 3.1 내 정보 페이지 (Dashboard 영역)
- 경로: `/teacher/profile`, `/assistant/profile`, `/student/profile`
- 레이아웃
  - 상단: 페이지 타이틀 + 안내 메시지
  - 본문: 카드 2단 구성
    - 카드 A: 기본 정보 (이메일/이름/전화번호/비밀번호 변경)
    - 카드 B: 학생 정보 (Student 역할에만 노출)
  - 하단: 저장 버튼 + 처리 결과 메시지
- 반응형
  - Desktop: 2열 카드
  - Mobile: 카드가 세로로 스택

### 3.2 임시 비밀번호 페이지 (Public 영역)
- 경로: `/auth/temp-password`
- 레이아웃
  - 상단: 안내 문구 + 주의사항
  - 입력 폼: 이메일/휴대폰
  - 결과 영역: 성공 시 임시 비밀번호를 강조 카드로 표시
  - 하단: 로그인 페이지로 돌아가기 링크

## 4. Component Breakdown
- **DashboardSidebar 사용자 카드**
  - 왜 필요? 내 정보 화면으로 이동하는 진입점이 필요함.
  - 어떻게 동작? 사용자 카드 전체를 Link로 감싸 클릭 시 역할별 profile 경로로 이동.
  - 어디에 붙나? `frontend/src/components/dashboard/sidebar.tsx`

- **ProfilePage (역할별 page.tsx)**
  - 왜 필요? 내 정보 조회/수정을 역할별로 동일 UI로 제공하기 위함.
  - 어떻게 동작? `useSession`으로 role 확인 → `/members/me` 호출 → 폼 초기값 설정 → PUT 요청으로 저장.
  - 어디에 붙나? `frontend/src/app/(dashboard)/teacher/profile/page.tsx` 등

- **ProfileForm 섹션 (기본 정보)**
  - 왜 필요? Member 정보를 수정하는 핵심 영역.
  - 어떻게 동작? `TextField`, `Button` 조합. 비밀번호는 빈 값이면 변경하지 않음.
  - 어디에 붙나? ProfilePage 내부 카드 A

- **StudentInfoForm 섹션**
  - 왜 필요? 학생만 추가 정보를 수정해야 함.
  - 어떻게 동작? `TextField`, `Select`, `DatePicker`로 구성. Student 역할에서만 렌더링.
  - 어디에 붙나? ProfilePage 내부 카드 B

- **TempPasswordPage**
  - 왜 필요? 로그인하지 못한 사용자의 비밀번호 복구 경로 제공.
  - 어떻게 동작? 이메일/휴대폰 입력 → `/auth/temp-password` 호출 → 결과 카드 표시.
  - 어디에 붙나? `frontend/src/app/(public)/auth/temp-password/page.tsx`

- **TempPasswordLoginPrompt (기존 모달 사용)**
  - 왜 필요? 임시 비밀번호로 로그인한 사용자를 즉시 안내해야 함.
  - 어떻게 동작? 로그인 성공 직후 임시 비밀번호 패턴이면 `?forcePasswordChange=1` 쿼리로 이동 → 대시보드 레이아웃에서 `ConfirmDialog`로 안내 → 확인 시 프로필 페이지로 이동.
  - 어디에 붙나? 로그인 페이지 + `frontend/src/app/(dashboard)/layout.tsx`

## 5. State & Data Flow
- 전역 상태
  - `useSession()`으로 로그인 사용자 정보 및 역할 확인
  - 저장 성공 후 `refreshSession()` 호출로 사이드바 정보 동기화
  - 임시 비밀번호 로그인 여부는 로그인 페이지에서 패턴 검사 후 URL 쿼리로 전달
- API 타입
  - `type ProfileResponse = paths["/api/v1/members/me"]["get"]["responses"]["200"]["content"]["application/json"];`
  - `type ProfileUpdateRequest = components["schemas"]["MemberProfileUpdateRequest"];`
  - `type TempPasswordRequest = components["schemas"]["TempPasswordRequest"];`
  - `type TempPasswordResponse = components["schemas"]["TempPasswordResponse"];`
- 에러 처리
  - `getApiErrorMessage`로 에러 메시지 표준화
  - 실패 시 `InlineError`로 표시

## 6. Interaction & UX Details
- 내 정보
  - 입력값 변경 전에는 저장 버튼 비활성화(변경 감지)
  - 저장 성공 시 상단에 “저장 완료” 메시지 표시
  - 비밀번호 필드에는 “입력 시에만 변경됩니다” helper 텍스트 표시
  - StudentInfo는 role이 STUDENT일 때만 렌더링
- 임시 비밀번호
  - 발급 성공 시 결과 카드에 굵게 강조
  - 실패 시 오류 메시지 표시
  - 발급 후 로그인으로 이동할 수 있는 CTA 제공
- 임시 비밀번호 로그인 안내
  - 로그인 성공 직후 모달 표시
  - 확인 시 역할별 내 정보 수정 페이지로 즉시 이동
- 접근성
  - 모든 입력에 label/aria 연결 유지
  - 키보드 Enter로 제출 가능

## 7. Test & Verification Plan
- 단위/통합 테스트
  - 현재는 수동 QA 중심 (테스트 프레임워크 미도입)
- 수동 QA 체크리스트
  - 내 정보: TEACHER/ASSISTANT/Student 각각 정보 조회 성공
  - 내 정보: StudentInfo 수정 시 저장 반영
  - 내 정보: 비밀번호 변경 시 재로그인 가능
  - 임시 비밀번호: 이메일/휴대폰 검증 성공/실패 시 메시지 확인
  - 사이드바 카드 클릭 → profile 경로 이동 확인
