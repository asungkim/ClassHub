# Feature: Season2 Teacher Register Page

## 1. Problem Definition
- Season2 요구사항(v1.3)에서는 선생님이 이메일/비밀번호/이름/전화번호로 직접 가입해야 Company/Branch/Invitation 플로우를 진행할 수 있다.
- 기존 Season1 페이지는 제거되었고, 새로운 Register API(`/api/v1/members/register/teacher`)는 LoginResponse(토큰 포함)를 즉시 반환하므로 가입 직후 세션을 복원해 메인 페이지로 이동해야 한다.
- 전화번호는 한국 번호만 허용하고 `010-1234-5678`처럼 하이픈이 포함된 포맷으로 저장해야 하지만, 사용자가 하이픈을 입력하지 않아도 자동으로 마스킹되는 UX가 필요하다.

## 2. User Flows & Use Cases
1. **로그인 안 된 방문자**
   1. `/auth/register/teacher` 진입 → 세션이 unauthenticated인 경우 등록 폼 표시.
   2. 이메일/비밀번호/비밀번호 확인/이름/전화번호(숫자만 입력) 작성.
   3. 비밀번호 정책(영문 + 숫자 + 특수문자 필수 조합, 대소문자 구분 없음) 미충족 시 실시간 오류 메시지 노출.
   4. Submit 클릭 → `POST /api/v1/members/register/teacher` 호출.
   5. 성공 시 반환된 Access Token/Refresh 쿠키 기반으로 `SessionProvider` 재검증(또는 `me` 요청) 후 `/` 메인 페이지로 push 및 Toast/Alert.
2. **이미 로그인된 사용자**
   - 페이지 접근 시 `useSession`에서 authenticated이면 아무 토스트 없이 즉시 `/`로 리다이렉트한다.
3. **오류 시나리오**
   - `DUPLICATE_EMAIL`: 이메일 입력 아래에 “이미 가입된 이메일입니다” 오류 라벨 표기.
   - Validation 실패(약한 비밀번호, 전화번호 잘못된 패턴): 각 필드 하단에 에러 문구, Submit 버튼 disabled 유지.
   - 네트워크/서버 오류: 폼 상단 Alert + 재시도 안내.

## 3. Page & Layout Structure
- **Full-height Split Layout** (Season1과 동일한 그라데이션/Glassmorphism 톤)
  - 좌측(Desktop ≥1024px): Hero 영역
    - ClassHub 로고 + 태그라인
    - CTA bullet: “수업/학생/조교를 한곳에서 관리”
  - 우측: 등록 카드
    - 제목/설명
    - Form fields
    - Submit 버튼 + “이미 계정이 있으신가요? 로그인” 링크
- **Mobile (<768px)**: Hero 섹션이 Form 위로 스택돼 하나의 화면에 모두 표시되고, 상단 CTA + 폼이 스크롤 없이 대부분 보이도록 spacing을 재조정한다. blob 애니메이션은 투명도 낮춰 성능 고려.
- Footer/NavigationBar는 홈과 동일한 공통 컴포넌트 재사용.

## 4. Component Breakdown
- `TeacherRegisterPage` (page.tsx)
  - 역할: `/auth/register/teacher` 경로 렌더, 세션 검사 후 레이아웃 출력
  - 상태: `formState` (email, password, confirmPassword, name, phone), `error`, `isSubmitting`
  - 이벤트: `handleSubmit`, `handlePhoneInput`(formatPhoneNumber)

- `RegisterFormCard`
  - props: `onSubmit`, `error`, `isSubmitting`
  - 하위 구성:
    - `TextField` (공통 컴포넌트) 5개: email, password, confirmPassword, name, phone
    - phone 필드는 `type="tel"`, `inputMode="numeric"`, onChange에서 formatPhoneNumber 적용
    - `PasswordRequirementList` (선택): 비밀번호 정책 체크리스트 (영문/숫자/특수문자/길이)
  - 스타일: glass 효과 + shadow, Season1과 동일한 gradient border

- `PasswordRequirementList` (신규, 선택)
  - 역할: 비밀번호 정책 충족 여부를 실시간 표시
  - props: `password` string
  - UI: 체크아이콘 + 조건 텍스트 (충족 시 초록색, 미충족 시 회색)

## 5. State & Data Flow
- **Form State**: `useState`로 `email`, `password`, `confirmPassword`, `name`, `phone` 관리
- **Derived Validation**:
  - `isPasswordValid` → 정규식 (백엔드 스펙과 동일):
    ```ts
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-={}:;"'`~<>,.?/\\|\[\]]).{8,64}$/
    ```
    (영문 + 숫자 + 특수문자 동시 포함, 대소문자 무관, 8~64자)
  - `isPhoneValid` → 정규식 `/^010-\d{3,4}-\d{4}$/` + 길이 12~13자 (하이픈 포함)
  - `isFormValid` = 모든 필드 채움 + 비밀번호 일치 + 위 조건 만족

- **전화번호 포맷팅 유틸**:
  ```ts
  function formatPhoneNumber(value: string): string {
    const digits = value.replace(/\D/g, ''); // 숫자만 추출

    if (digits.length <= 3) return digits;
    if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
    return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7, 11)}`;
  }
  ```

- **API 호출**:
  ```ts
  import { api } from "@/lib/api";
  import { useSession } from "@/components/session/session-provider";

  type RegisterTeacherRequest = components["schemas"]["RegisterMemberRequest"];
  type RegisterTeacherResponse = components["schemas"]["RsDataLoginResponse"];

  const { data, error } = await api.POST("/api/v1/members/register/teacher", {
    body: {
      email,
      password,
      name,
      phoneNumber: phone, // 이미 "010-1234-5678" 포맷
    } satisfies RegisterTeacherRequest,
  });

  // 성공 시 세션 복원
  if (data?.data?.accessToken) {
    await session.setToken(data.data.accessToken);
    // SessionProvider의 setToken 메서드가 자동으로:
    // 1. setAuthToken(token) - 메모리에 Access Token 저장
    // 2. fetchSession() - /api/v1/auth/me 호출
    // 3. setStatus("authenticated") + setMember(...)
    // 4. queryClient.invalidateQueries() - React Query 캐시 갱신

    router.push("/"); // 메인 페이지로 이동
  }
  ```
  - Refresh 쿠키는 백엔드가 HttpOnly로 자동 설정하므로 프론트 처리 불필요

- **Error Handling**: `getApiErrorMessage(error)` 또는 `getFetchError(response)`로 사용자 메시지 추출

## 6. Interaction & UX Details
- **입력 Validation**:
  - 모든 필드는 `onChange` 시 즉시 업데이트, `onBlur` 시 Validation 메시지 표시
  - 비밀번호 입력 중에는 `PasswordRequirementList`가 실시간으로 충족 여부 표시

- **전화번호 필드**:
  - 숫자만 입력 가능 (`pattern="[0-9-]*"`)
  - 모바일에서는 숫자 키패드 (`inputMode="numeric"`)
  - 자동 하이픈 삽입 (입력: `01012345678` → 표시: `010-1234-5678`)

- **Submit 동작**:
  - 클릭 시 버튼에 spinner 표시, disabled 상태 전환
  - 성공 시: `session.setToken(accessToken)` 호출 → 세션 복원 → `router.push("/")`
  - 실패 시: Form 상단 Alert + 해당 필드 하단 error state 표시

- **페이지 접근 제어**:
  - 로그인 안 된 사용자 (`status === "unauthenticated"`): 폼 표시
  - 이미 로그인된 사용자 (`status === "authenticated"`): 토스트 없이 즉시 `/` 리다이렉트
  - 로딩 중 (`status === "loading"`): 스켈레톤 또는 로딩 스피너 표시

- **접근성**:
  - 각 input에 `aria-describedby`로 에러/헬프 텍스트 연결
  - keyboard tab 순서: Hero → Form fields → Submit → 로그인 링크
  - 에러 발생 시 첫 번째 에러 필드로 포커스 이동

## 7. Test & Verification Plan
1. **타입/빌드 검증 (필수)**
   - `cd frontend && npm run build -- --webpack`
   - TypeScript 컴파일 에러 0개 확인

2. **수동 QA (필수)**
   - **시나리오 A** (정상 가입):
     - 모든 필드 올바르게 입력 → Submit → 성공 응답 → 세션 복원 → `/` 이동 확인
   - **시나리오 B** (중복 이메일):
     - 이미 가입된 이메일 입력 → Submit → "이미 가입된 이메일입니다" 에러 표시
   - **시나리오 C** (Validation 실패):
     - 약한 비밀번호/010 외 번호/비밀번호 불일치 → Submit 버튼 disabled 유지
     - 각 필드 하단에 에러 메시지 표시
   - **시나리오 D** (이미 로그인):
     - 로그인 상태에서 `/auth/register/teacher` 접근 → 즉시 `/` 리다이렉트
   - **시나리오 E** (전화번호 포맷팅):
     - `01012345678` 입력 → 자동으로 `010-1234-5678` 표시 확인

3. **엣지 케이스 검증 (권장)**
   - 네트워크 오류 시 Form 상단 Alert 표시
   - Submit 중 재클릭 방지 (disabled + spinner)
   - 모바일 숫자 키패드 동작 확인

4. **자동 테스트 (후순위)**
   - `formatPhoneNumber` 유틸 함수 단위 테스트
   - React Testing Library로 `handleSubmit` API mock 테스트

5. **AGENT_LOG 기록**
   - PLAN 승인 후 구현 시작 시 BEHAVIORAL 이벤트 로그
   - 구현 완료 후 테스트 결과와 함께 로그 append

---

## 계획 요약 (한국어)

### 핵심 변경 사항
- **Season2 API 연동**: `/api/v1/members/register/teacher` 엔드포인트 사용, LoginResponse(토큰 포함) 반환
- **세션 복원**: 가입 성공 시 `SessionProvider.setToken()`으로 자동 세션 복원 후 메인 페이지 이동
- **기존 컴포넌트 재사용**: 공통 `TextField` 사용, 신규 컴포넌트 최소화

### 주요 기능
1. **비밀번호 검증**: 백엔드 스펙과 동일한 정규식 적용 (영문+숫자+특수문자, 8~64자)
2. **전화번호 자동 포맷팅**: 사용자가 숫자만 입력하면 `010-1234-5678` 형식으로 자동 변환
3. **실시간 Validation**: 입력 중 `PasswordRequirementList`로 정책 충족 여부 표시
4. **접근 제어**: 이미 로그인된 사용자는 토스트 없이 즉시 메인 페이지로 리다이렉트

### UI/UX
- Season1과 동일한 그라데이션/글래스모피즘 레이아웃 유지
- 모바일 반응형 (Hero + Form 스택 레이아웃)
- 접근성 고려 (keyboard navigation, aria-describedby)
