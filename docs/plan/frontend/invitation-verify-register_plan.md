# Feature: 초대 검증 및 회원가입 페이지 (조교/학생)

## 1. Problem Definition

조교(Assistant)와 학생(Student)은 선생님(Teacher)이 생성한 초대 링크를 통해서만 회원가입할 수 있다. 현재 백엔드 API는 준비되어 있으나 프론트엔드 UI가 없어 조교/학생이 초대 링크를 클릭했을 때 회원가입을 진행할 수 없는 상태다.

**해결해야 할 문제**:
- 초대 링크에 포함된 코드를 검증하고, 초대한 선생님 정보를 표시하는 검증 페이지 부재
- 검증 후 회원가입을 완료할 수 있는 회원가입 페이지 부재

## 2. Requirements

### Functional

**FR-1. 초대 검증 페이지**
- **현재 초대 링크 형태**: `https://localhost:3000/auth/register?code=xxx`
- **목표 경로**: `/auth/invitation/verify?code=xxx` (검증 페이지 신규 생성)
- URL 쿼리 파라미터에서 초대 코드(`code`)를 읽어온다
- `POST /api/v1/auth/invitations/verify`를 호출해 초대 코드를 검증한다
- 검증 성공 시 응답(`InvitationVerifyResponse`)에서 아래 정보를 표시:
  - 초대한 선생님 이름 (`inviterName`) - **조교/학생 공통**
  - 역할 (`inviteeRole`: ASSISTANT 또는 STUDENT) - **조교/학생 공통**
  - 만료 시각 (`expiresAt`)
  - 학생인 경우 학생 프로필 정보 (`studentProfile.name`만 표시) - **학생만**
- 조교 초대:
  - 선생님 이름 + 역할 표시
  - "확인" 버튼 클릭 시 회원가입 페이지로 이동 (`/auth/register/invited?code=xxx`)
- 학생 초대:
  - 선생님 이름 + 역할 + 학생 이름(`studentProfile.name`) 표시
  - 사용자가 본인 정보가 맞는지 확인 후 "확인" 버튼 클릭 시 회원가입 페이지로 이동
- 검증 실패 시 에러 메시지 표시 및 홈으로 돌아가기 버튼 제공

**FR-2. 초대 기반 회원가입 페이지 (`/auth/register/invited?code=xxx`)**
- URL 쿼리 파라미터에서 초대 코드(`code`)를 읽어온다
- 이메일, 비밀번호, 비밀번호 확인, 이름 입력 필드 제공 (선생님 회원가입 페이지와 동일)
- 비밀번호 강도 힌트 표시 (8자 이상, 영문+숫자+특수문자)
- 비밀번호 확인 불일치 시 인라인 에러 표시
- 서비스 이용약관 동의 체크박스
- `POST /api/v1/auth/register/invited`를 호출해 회원가입 처리:
  - Request Body: `InvitationRegisterRequest` (code, email, password, name)
  - Response: `LoginResponse` (accessToken 포함)
- 회원가입 성공 시:
  - 리디렉션 안내 메시지 표시
  - 1.5초 후 역할별 대시보드로 자동 리디렉트 (`/dashboard/assistant` 또는 `/dashboard/student`)
- 회원가입 실패 시 에러 메시지 표시

**FR-3. 라우팅 및 검증 흐름**
- **현재 상태**: 초대 링크가 `https://localhost:3000/auth/register?code=xxx` 형태로 복사됨
- **목표 흐름**:
  1. 초대 링크를 `/auth/invitation/verify?code=xxx`로 변경 (백엔드 수정 필요 시 프론트 우선 구현)
  2. 검증 페이지에서 "확인" 버튼 → `/auth/register/invited?code=xxx`로 이동
  3. 회원가입 완료 → 역할별 대시보드로 리디렉트
     - ASSISTANT → `/dashboard/assistant` (구현 완료)
     - STUDENT → `/dashboard/student` (구현 완료)

### Non-functional

**NFR-1. 일관된 UI/UX**
- 선생님 회원가입 페이지(`/auth/register/teacher`)와 동일한 디자인 시스템 사용
- 그라데이션 배경, 카드 스타일, 애니메이션 blob 등 공통 레이아웃 유지
- 공통 컴포넌트 재사용: `InlineError`, `Button` (필요 시)

**NFR-2. 타입 안전성**
- `frontend/src/types/openapi.d.ts`의 타입 사용:
  - `InvitationVerifyRequest`, `InvitationVerifyResponse`, `StudentCandidateResponse`
  - `InvitationRegisterRequest`, `LoginResponse`
- API 호출은 `frontend/src/lib/api.ts`의 클라이언트 사용
- 에러 처리는 `frontend/src/lib/api-error.ts`의 `getApiErrorMessage` 사용

**NFR-3. 접근성 및 보안**
- 이미 로그인된 사용자가 접근 시 대시보드로 리디렉트
- 초대 코드가 URL에 없거나 검증 실패 시 명확한 에러 메시지 제공
- 만료된 초대 코드 처리 (백엔드 에러 메시지 표시)

## 3. API Design (Draft)

### 3.1. 초대 검증 API

**Endpoint**: `POST /api/v1/auth/invitations/verify`

**Request Body** (`InvitationVerifyRequest`):
```typescript
{
  code: string; // 초대 코드 (URL 쿼리에서 가져옴)
}
```

**Response** (`RsDataInvitationVerifyResponse`):
```typescript
{
  code: number;
  message: string;
  data: {
    inviterId: string;           // 선생님 UUID
    inviterName: string;         // 선생님 이름
    inviteeRole: string;         // "ASSISTANT" | "STUDENT"
    expiresAt: string;           // ISO 8601 날짜
    studentProfile?: {           // 학생인 경우에만 존재
      id: string;
      name: string;
      phoneNumber?: string;
      parentPhone?: string;
      schoolName?: string;
      grade?: string;
      age?: number;
    }
  }
}
```

### 3.2. 초대 기반 회원가입 API

**Endpoint**: `POST /api/v1/auth/register/invited`

**Request Body** (`InvitationRegisterRequest`):
```typescript
{
  code: string;      // 초대 코드
  email: string;     // 이메일
  password: string;  // 비밀번호
  name: string;      // 이름
}
```

**Response** (`RsDataLoginResponse`):
```typescript
{
  code: number;
  message: string;
  data: {
    memberId: string;
    accessToken: string;
    accessTokenExpiresAt: string;
  }
}
```

## 4. UI Design (Draft)

### 4.1. 초대 검증 페이지 (`/auth/invitation/verify`)

**레이아웃**:
- 중앙 정렬 카드 (흰색 배경, 그림자, 둥근 모서리)
- 그라데이션 배경 (blue-purple-pink)
- 애니메이션 blob 효과 (선생님 회원가입 페이지와 동일)

**콘텐츠 (조교)**:
```
[로고/타이틀]
초대 확인

[카드]
  선생님: 홍길동
  역할: 조교 (Assistant)
  만료일: 2025-12-31 23:59

  [확인 버튼] → 회원가입 페이지로 이동
  [취소 버튼] → 홈으로 이동
```

**콘텐츠 (학생)**:
```
[로고/타이틀]
초대 확인

[카드]
  선생님: 홍길동
  역할: 학생 (Student)
  만료일: 2025-12-31 23:59

  [학생 정보]
  이름: 김학생

  ⚠️ 위 정보가 본인 이름과 일치하는지 확인해주세요.

  [확인 버튼] → 회원가입 페이지로 이동
  [취소 버튼] → 홈으로 이동
```

**에러 상태**:
- 초대 코드가 없거나 유효하지 않을 경우:
  ```
  [에러 아이콘]
  초대 코드를 확인할 수 없습니다.
  초대 링크가 만료되었거나 잘못된 주소입니다.

  [홈으로 돌아가기]
  ```

### 4.2. 회원가입 페이지 (`/auth/register/invited`)

**레이아웃**:
- 선생님 회원가입 페이지 (`/auth/register/teacher`)와 동일한 레이아웃
- 좌측: 설명 섹션 (역할에 따라 텍스트 변경)
- 우측: 회원가입 폼

**좌측 설명 섹션 (조교)**:
```
[배지] Assistant 전용

ClassHub와 함께 조교 업무를 시작하세요.

조교 계정을 생성하면 선생님을 도와 학생을 관리할 수 있습니다.

• 학생 초대 및 관리
• 학생 정보 조회
• 수업 기록 조회
```

**좌측 설명 섹션 (학생)**:
```
[배지] Student 전용

ClassHub와 함께 학습을 시작하세요.

학생 계정을 생성하면 나만의 대시보드에서 수업 내용을 확인할 수 있습니다.

• 내 수업 기록 조회
• 클리닉 일정 확인
• 선생님과 소통
```

**우측 폼**:
```
[제목] 조교 회원가입 (또는 학생 회원가입)
[부제] ClassHub 대시보드를 사용하기 위한 계정을 만듭니다.

[이메일 입력]
[비밀번호 입력]
  → 비밀번호 힌트: 8자 이상, 영문/숫자/특수문자 포함
[비밀번호 확인]
[이름 입력]

[약관 동의 체크박스]
서비스 이용약관과 개인정보 처리방침에 동의합니다.

[회원가입 버튼] → "조교 계정 만들기" (또는 "학생 계정 만들기")

이미 계정이 있으신가요? [로그인하기]
```

**성공 시 리디렉션 패널**:
```
대시보드로 이동합니다.

{email} 계정으로 가입이 완료되었습니다.
잠시 후 대시보드로 이동합니다.
```

## 5. Implementation Plan

### 5.1. 파일 구조

```
frontend/src/app/auth/
├── invitation/
│   └── verify/
│       └── page.tsx          # 초대 검증 페이지
└── register/
    ├── teacher/
    │   └── page.tsx          # 기존 선생님 회원가입
    └── invited/
        └── page.tsx          # 조교/학생 회원가입 페이지
```

### 5.2. 구현 단계

#### Step 1: 초대 검증 페이지 구현
- [ ] `/auth/invitation/verify/page.tsx` 생성
- [ ] URL 쿼리에서 `code` 파라미터 추출 (`useSearchParams`)
- [ ] `POST /api/v1/auth/invitations/verify` 호출
- [ ] `InvitationVerifyResponse` 타입 기반 상태 관리
- [ ] 조교/학생 구분 UI 렌더링
- [ ] 학생인 경우 `StudentCandidateResponse` 정보 표시
- [ ] "확인" 버튼 클릭 시 `/auth/register/invited?code={code}`로 라우팅
- [ ] 에러 처리 (코드 없음, 검증 실패, 만료)

#### Step 2: 초대 기반 회원가입 페이지 구현
- [ ] `/auth/register/invited/page.tsx` 생성
- [ ] URL 쿼리에서 `code` 파라미터 추출
- [ ] 선생님 회원가입 페이지(`/auth/register/teacher/page.tsx`) 구조 재사용
- [ ] 역할 추론 로직 (회원가입 전에는 역할을 모르므로, 일단 공통 UI 사용)
- [ ] `POST /api/v1/auth/register/invited` 호출
  - Request: `InvitationRegisterRequest` (code, email, password, name)
  - Response: `LoginResponse`
- [ ] 회원가입 성공 시:
  - 토큰 저장 (SessionProvider 연동)
  - 역할별 대시보드로 리디렉트 (`/dashboard/assistant` or `/dashboard/student`)
  - **문제**: 응답에 역할 정보가 없으므로 `/api/v1/auth/me` 호출 필요 또는 SessionProvider가 자동으로 세션 갱신
- [ ] 비밀번호 검증 UI (PasswordHint 컴포넌트 재사용)
- [ ] 에러 처리 (코드 없음, 회원가입 실패)

#### Step 3: 라우팅 및 세션 연동
- [ ] 회원가입 성공 후 `SessionProvider`를 통해 세션 갱신
- [ ] 역할 정보를 기반으로 대시보드 경로 결정:
  - `ASSISTANT` → `/dashboard/assistant`
  - `STUDENT` → `/dashboard/student`
- [ ] 이미 로그인된 사용자가 검증/회원가입 페이지 접근 시 대시보드로 리디렉트

#### Step 4: 스타일 및 공통 컴포넌트 활용
- [ ] 선생님 회원가입 페이지의 스타일 재사용 (그라데이션, blob 애니메이션)
- [ ] `InlineError` 컴포넌트 재사용
- [ ] 공통 레이아웃 컴포넌트 추출 (필요 시)

### 5.3. 검증 및 테스트 계획

#### 타입 검증
- `cd frontend && npm run build -- --webpack` 실행하여 컴파일 에러 0개 확인

#### 수동 테스트 시나리오

**시나리오 1: 조교 초대 검증 및 회원가입**
1. Teacher 계정으로 로그인
2. 조교 초대 링크 생성 (`/dashboard/teacher/invitations`)
3. 초대 링크 복사 (`/auth/invitation/verify?code=xxx`)
4. 로그아웃
5. 초대 링크로 접속
6. 검증 페이지에서 선생님 이름 확인
7. "확인" 버튼 클릭
8. 회원가입 페이지에서 이메일/비밀번호/이름 입력
9. 약관 동의 후 "조교 계정 만들기" 클릭
10. 회원가입 성공 메시지 표시
11. 1.5초 후 `/dashboard/assistant`로 자동 이동
12. Assistant 대시보드 확인

**시나리오 2: 학생 초대 검증 및 회원가입**
1. Assistant 계정으로 로그인
2. 학생 프로필 생성 후 초대 링크 생성
3. 초대 링크 복사
4. 로그아웃
5. 초대 링크로 접속
6. 검증 페이지에서 선생님 이름 + 학생 프로필 정보 확인
7. "확인" 버튼 클릭
8. 회원가입 페이지에서 이메일/비밀번호/이름 입력
9. 약관 동의 후 "학생 계정 만들기" 클릭
10. 회원가입 성공 메시지 표시
11. 1.5초 후 `/dashboard/student`로 자동 이동
12. Student 대시보드 확인

**시나리오 3: 에러 처리**
- 잘못된 초대 코드로 검증 페이지 접속 → 에러 메시지 표시
- 만료된 초대 코드로 검증 → 백엔드 에러 메시지 표시
- 회원가입 시 이미 사용 중인 이메일 → 에러 메시지 표시
- 이미 로그인된 상태에서 검증/회원가입 페이지 접속 → 대시보드로 리디렉트

**시나리오 4: 접근성 및 UX**
- 비밀번호 강도 힌트 실시간 업데이트 확인
- 비밀번호 확인 불일치 시 인라인 에러 표시 확인
- 약관 동의 체크박스 미체크 시 버튼 비활성화 확인
- 모바일 반응형 확인 (작은 화면에서 레이아웃 검증)

#### 기록
- 모든 테스트 결과를 `docs/history/AGENT_LOG.md`에 기록
- 빌드 성공 여부, 테스트 시나리오별 결과, 발견된 이슈 및 해결 방법 기록

## 6. Technical Decisions

### 6.1. 역할 판별 문제 해결

**문제**: `/auth/register/invited` API의 응답(`LoginResponse`)에는 역할 정보가 없어, 회원가입 후 어느 대시보드로 리디렉트할지 알 수 없음.

**해결 방안**:
1. **옵션 A**: 검증 API 응답(`InvitationVerifyResponse`)의 `inviteeRole`을 세션 스토리지에 임시 저장
   - 검증 페이지에서 `inviteeRole`을 `sessionStorage`에 저장
   - 회원가입 페이지에서 세션 스토리지에서 읽어와 대시보드 경로 결정
   - 단점: 세션 스토리지 의존성 추가

2. **옵션 B**: 회원가입 성공 후 SessionProvider.setToken() 호출하여 자동으로 역할 정보 확보
   - 회원가입 후 accessToken을 `SessionProvider.setToken()`에 전달
   - SessionProvider가 내부적으로 `/api/v1/auth/me`를 호출하여 역할 정보 확보 (session-provider.tsx:145-172 참고)
   - `member.role` 값으로 대시보드 경로 결정
   - 장점: 기존 구조 활용, 추가 API 호출 불필요

**선택**: **옵션 B** (SessionProvider.setToken() 활용)
- 이유: `SessionProvider.setToken()`이 이미 `/me`를 호출하여 역할 정보를 자동으로 가져옴 (확인 완료)
- 구현:
  1. 회원가입 성공 시 `setToken(accessToken)` 호출
  2. SessionProvider가 자동으로 `member.role` 업데이트
  3. `member.role`로 대시보드 경로 결정 (`ASSISTANT` → `/dashboard/assistant`, `STUDENT` → `/dashboard/student`)

### 6.2. UI 텍스트 역할별 분기

**문제**: 회원가입 페이지에서 조교/학생 구분 UI를 표시해야 하지만, 검증 단계를 거치지 않고 직접 접근할 경우 역할을 알 수 없음.

**해결 방안**:
- 검증 페이지의 `inviteeRole`을 쿼리 파라미터 또는 세션 스토리지로 전달
- 회원가입 페이지에서 역할에 따라 UI 텍스트 분기:
  - `ASSISTANT` → "조교 회원가입", "조교 계정 만들기"
  - `STUDENT` → "학생 회원가입", "학생 계정 만들기"
- 역할 정보가 없으면 기본 텍스트 사용 ("회원가입", "계정 만들기")

**선택**: 세션 스토리지에 `inviteeRole` 저장 (옵션 A)
- 검증 페이지에서 `sessionStorage.setItem("inviteeRole", inviteeRole)`
- 회원가입 페이지에서 `sessionStorage.getItem("inviteeRole")`로 읽기
- 회원가입 완료 후 `sessionStorage.removeItem("inviteeRole")`로 정리

### 6.3. 기존 컴포넌트 재사용

- `InlineError`: 에러 메시지 표시
- `PasswordHint`: 비밀번호 강도 힌트 (선생님 회원가입 페이지에서 추출)
- `RedirectPanel`: 회원가입 성공 후 리디렉션 안내 (선생님 회원가입 페이지에서 추출)

## 7. Confirmed Information

✅ **확인 완료**:
1. `/api/v1/auth/me`는 `role` 필드를 포함하는 `MemberSummary` 반환 (session-provider.tsx:48-53 참고)
2. `SessionProvider.setToken()`이 자동으로 `/me` 호출하여 세션 갱신 (session-provider.tsx:145-172 참고)
3. 대시보드 경로 구현 완료:
   - `/dashboard/assistant/page.tsx` ✅
   - `/dashboard/student/page.tsx` ✅
4. 현재 초대 링크 형태: `${NEXT_PUBLIC_APP_URL}/auth/register?code=xxx`
   - `NEXT_PUBLIC_APP_URL`은 `frontend/.env.local`에서 정의 (예: `https://localhost:3000`)
   - 백엔드에서 생성되는 링크를 `${NEXT_PUBLIC_APP_URL}/auth/invitation/verify?code=xxx`로 변경 필요 (프론트 구현 후 백엔드 수정 요청)

## 8. Implementation Notes

- 선생님 회원가입 페이지의 `PasswordHint`, `RedirectPanel` 컴포넌트를 동일 파일 내에서 재사용
- `InvitationVerifyResponse`의 `studentProfile.name`만 표시 (기타 필드는 UI에 표시하지 않음)
- SessionProvider 통합: `setToken()` 호출 후 `member.role`로 라우팅 결정
