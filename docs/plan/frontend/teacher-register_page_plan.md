# Feature: Teacher Register Page

## 1. Problem Definition
- 홈 화면에서 “선생님 회원가입” 버튼을 누르면 이동할 실제 가입 페이지가 없다.
- Teacher는 초대 없이 스스로 가입할 수 있어야 하므로, 이메일/비밀번호/이름 입력 + 정책 안내 + 성공 시 로그인 연결 흐름을 제공해야 한다.

## 2. Requirements

### Functional
1. **페이지 구조**
   - `/auth/register/teacher` 라우트에 Glassmorphism 카드 또는 2열 레이아웃을 구성한다 (Hero + 폼).
2. **폼 필드**
   - 이메일, 비밀번호, 비밀번호 확인, 이름을 받는다.
   - 비밀번호는 정책(대/소문자, 숫자, 특수문자 1개 이상 등)을 표시하고 실시간 검증 메시지를 제공한다.
3. **이용약관/동의**
   - 필수 약관 체크박스를 포함하고 동의하지 않으면 제출할 수 없다.
4. **제출/오류 처리**
   - `api.POST("/api/v1/auth/register/teacher")` (OpenAPI `TeacherRegisterRequest`)를 호출해 가입을 요청한다.
   - 성공 시 `LoginResponse` 없으므로 가입 완료 메시지 + 로그인 페이지 링크를 보여준다.
   - 중복 이메일 등 오류는 InlineError/폼 오류로 안내한다.
5. **CTA/Navigation**
   - “이미 계정이 있으신가요? 로그인하기” 링크를 포함하고, 홈/로그인 페이지로 이동 가능해야 한다.

### Non-functional
- 반응형 레이아웃 (모바일 360px).
- `useSession`을 사용해 이미 로그인된 사용자가 접근하면 홈으로 리다이렉트하거나 정보 메시지를 보여준다.
- Submit 버튼은 로딩 상태를 보여주고 중복 클릭을 방지한다.
- 모든 API 타입은 `components["schemas"]["TeacherRegisterRequest"]`/`RsDataTeacherRegisterResponse` 등을 기반으로 한다.

## 3. API Design (Draft)
- `POST /api/v1/auth/register/teacher`
  - Request: `{ email, password, name }`
  - Response: `{ code, message, data: { memberId, email, authority, createdAt, updatedAt } }`
- 등록 성공 후 자동 로그인은 하지 않고, 완료 메시지를 보여준 뒤 로그인 페이지로 이동하도록 한다.

## 4. Domain Model (Draft)
- **TeacherRegisterForm**
  - `email`, `password`, `confirmPassword`, `name`, `termsAccepted`.
- **TeacherRegisterState**
  - `isSubmitting`, `errorMessage`, `successMemberEmail`.
- **TeacherRegisterResponse**
  - `memberId`, `email`, `authority`, `createdAt`, `updatedAt`.

## 5. Test Plan
1. **Form Validation (React Testing Library)**
   - 각 필드가 비어 있을 때/비밀번호 불일치 시 버튼이 비활성화되거나 오류가 표시되는지 테스트한다.
2. **API Mock Test**
   - `api.POST("/auth/register/teacher")`를 mock해 성공/실패 시 상태 메시지와 CTA가 올바르게 표시되는지 검증한다.
3. **Session Redirect**
   - `useSession`이 authenticated일 경우 홈으로 redirect 혹은 안내 문구가 나타나는지 확인한다.
4. **Manual Verification**
   - 실제 백엔드 또는 mock 서버와 연결해 성공/중복 이메일/네트워크 오류 시나리오를 브라우저에서 확인한다.
