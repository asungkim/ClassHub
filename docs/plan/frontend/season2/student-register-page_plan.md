# Feature: Season2 Student Register Page

## 1. Problem Definition
- Season2 요구사항(v1.3)에서는 학생이 자유 가입을 통해 Course 검색과 Enrollment Request를 생성할 수 있어야 한다.
- 백엔드 `/api/v1/members/register/student`가 도입되었지만 프론트엔드 페이지가 없어 학생이 직접 계정을 만들 수 없는 상태다.
- 학생 가입에는 개인 정보 외에도 `schoolName`, `grade`, `birthDate`, `parentPhone` 등 입력 요소가 많아 모바일에서도 쉽게 작성할 수 있는 UI/UX가 필수다(세그먼트 선택, date picker, 자동 포맷 등).
- 가입 성공 시 Access/Refresh 토큰이 즉시 발급되므로 자동 로그인 후 학생 대시보드(`/student`)로 리다이렉트해야 한다.

## 2. User Flows & Use Cases
1. **비로그인 학생**
   1. `/auth/register/student` 진입 → 세션이 unauthenticated인 경우 등록 폼 표시.
   2. 필수 정보 입력:
      - 기본: 이메일, 비밀번호, 비밀번호 확인, 이름, 전화번호.
      - 학교/학년: `학교명`, `schoolLevel` 선택(초/중/고/N수) → 레벨별 학년 옵션(초1~초6, 중1~중3, 고1~고3, N수=검정고시), `birthDate`(모바일 친화 date picker).
      - 보호자: `parentPhone`(한국 번호 자동 하이픈).
   3. Submit 클릭 → `POST /api/v1/members/register/student` 호출.
   4. 성공 시 반환된 Access Token을 SessionProvider에 저장하고 `router.push("/student")`.
2. **이미 로그인된 사용자**
   - 페이지 접근 시 `useSession`으로 authenticated 확인 → 즉시 `/student` 또는 role에 맞는 대시보드로 리다이렉트 (선생님/조교도 동일).
3. **오류 시나리오**
   - `DUPLICATE_EMAIL`, `INVALID_STUDENT_INFO`, `INVALID_PHONE_FORMAT`.
   - 입력 필드별 오류를 inline으로 표시.
   - 네트워크/기타 오류는 상단 Alert.

## 3. Page & Layout Structure
- 선생님 등록과 동일한 Hero + Form 2열 레이아웃 (Desktop), Mobile에서는 순차 스택.
- Hero 영역 copy:
  - Title: "학생 회원가입"
  - Subtitle: "내 수업 시간표와 클리닉을 한눈에 확인하세요."
- Form card는 2-column grid + 모바일 1-column으로 그룹화:
  1. 계정 정보 섹션 (이메일/비밀번호/연락처)
  2. 학생 정보 섹션 (학교명, `schoolLevel` segmented control, grade selector, 생년월일)
  3. 보호자 연락처 섹션
- 각 섹션 상단에 Badge/Label을 넣어 단계 구분.

## 4. Component Breakdown
- `StudentRegisterPage` (page.tsx):
  - 상태: email, password, confirmPassword, name, phone, schoolName, schoolLevel, grade, birthDate, parentPhone.
  - 파생 상태: password checklist, grade options preset, formatted phone/birth date string.
  - 이벤트: `handleSubmit`, `handlePhoneInput`, `handleParentPhoneInput`, `handleSchoolSelect`.
- `SchoolLevelSelector` + `GradeSelect`:
  - `SchoolLevelSelector`: segmented control (`초등`, `중등`, `고등`, `N수`) → 클릭 시 grade 옵션 변경.
  - `GradeSelect`: 동적 옵션 (초: 1~6, 중/고: 1~3, N수: `GAP_YEAR` 등). Pill buttons 또는 dropdown.
- `BirthDatePicker`:
  - 모바일 친화 `input[type="date"]` + icon, helper text(예: YYYY-MM-DD). 필요 시 calendar icon 버튼.
  - props: `value`, `onChange`.
- `TextField`, `PasswordRequirementList`, `Button` 기존 컴포넌트 재사용.

## 5. State & Data Flow
- `useReducer`로 form state 관리 (필드 많음).
- Validation:
  - 이메일/비밀번호: 선생님 페이지와 동일 요구사항 (영문+숫자+특수문자, 8~64자).
  - 전화번호: `formatPhoneNumber`/`validatePhoneNumber`.
- schoolName: free text + placeholder 예시(“대치중”) 제공, `schoolLevel` 선택으로 초/중/고 구분.
- grade: `components["schemas"]["RegisterStudentRequest"]["grade"]` enum 사용 (selector 결과와 매핑).
  - birthDate: `YYYY-MM-DD`.
- API 호출:
  ```ts
  type RegisterStudentRequest = components["schemas"]["RegisterStudentRequest"];
  await apiClient.POST("/api/v1/members/register/student", { body: request });
  ```
- 성공 후:
  - `setToken(response.data.data?.accessToken)` → `router.push("/student")`.
- 에러 처리: `getApiErrorMessage`.

## 6. Interaction & UX Details
- 모바일 키보드: phone/parentPhone는 `inputMode="numeric"`.
- 학교/학년: 상단에 school level segmented control, 선택에 따라 학년 옵션이 바뀌도록 하여 입력 횟수를 최소화.
- birthDate: 날짜 input에 아이콘/placeholder를 추가해 모바일에서도 쉽게 선택.
- Form submit button: disable until form valid + spinner.
- 로그인된 상태에서 접근 시 즉시 redirect, 토스트 표시 없음.

## 7. Test & Verification Plan
1. 타입/빌드: `cd frontend && npm run build -- --webpack`.
2. 수동 QA:
   - 정상 입력 → 성공 → 학생 대시보드 이동.
   - 중복 이메일 응답 → email field error.
   - 잘못된 전화/birthDate → inline error.
3. 경로 가드: 로그인된 사용자 접근 시 자동 redirect 동작 확인.
4. AGENT_LOG 기록 (BEHAVIORAL).

---

## 계획 요약 (한국어)
- 학생 회원가입 페이지를 Season2 백엔드의 `/api/v1/members/register/student`에 맞춰 구현하며, 이메일/비밀번호/이름 + 학교/학년/생년월일 + 보호자 전화번호를 입력하도록 구성합니다.
- 폼은 선생님 가입 레이아웃을 재사용하되, 학교명 입력 + 학년 선택(중/고) + 생년월일 Date Picker를 포함해 UX를 개선하고, 가입 후에는 자동 로그인 상태에서 학생 대시보드(`/student`)로 이동시킵니다.
