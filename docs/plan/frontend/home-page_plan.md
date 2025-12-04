# Feature: Frontend Home Page (Auth/Invitation Entry)

## 1. Problem Definition
- `/` 페이지를 ClassHub 메인 엔트리(선생님/조교/학생 공용)로 사용하고자 하며, 로그인 폼/초대 안내/Teacher 가입 CTA를 한 화면에서 제공해야 한다.
- 실제 대시보드 페이지에서는 각 역할별 레이아웃이 다를 예정이므로, 홈 화면에서는 독립적인 Hero + Login 카드 구조를 사용해 첫 인상을 책임진다.

## 2. Requirements

### Functional
1. **Glassmorphism Hero**
   - 화면 전체에 블롭 애니메이션 배경을 깔고 중앙에 로고/카피/CTA 텍스트를 배치한다. 브랜드명과 메시지는 “ClassHub” 기준으로 작성한다.
2. **중앙 로그인 카드**
   - 이메일/비밀번호 필드에 아이콘, 패스워드 표시 토글, 로그인 상태 유지 체크박스, 비밀번호 찾기 링크를 포함한다.
   - 로그인 버튼에는 로딩 스피너 애니메이션을 제공하고, Enter 키 입력으로 제출된다.
3. **Teacher 회원가입 CTA**
   - 로그인 카드 하단에 Teacher 회원가입 버튼을 두고, 별도 API 페이지로 이동하도록 연결한다.
4. **Footer 메시지**
   - “© 2025 ClassHub. All rights reserved.”와 같은 간단한 저작권 문구를 포함한다.

### Non-functional
- 반응형 (모바일 360px) 기준으로 로그인 카드가 화면 가운데 정렬되고 배경 블롭이 과하지 않도록 opacity/blur를 조정한다.
- Hero/카드 구성은 `Button`, `TextField`, `Logo` 등 기존 UI를 재사용하되, 필요 시 Tailwind 유틸 클래스로 확장할 수 있다.
- SessionProvider 상태(loading/authenticated/unauthenticated)에 따라 로그인 폼을 disable하거나 상태 메시지를 보여주며, 인증 오류 시 ErrorState를 활용한다.
- Nav/Footer는 실제 대시보드에 들어갔을 때 적용될 예정이므로 홈 페이지에는 독립된 Hero/카드 레이아웃만 사용한다.

## 3. API Design (Draft)
- `GET /api/v1/auth/me`
  - Already implemented; 홈 화면에서 SessionProvider를 통해 간접적으로 사용.
- 추가 API 호출은 없음. 홈 화면의 CTA는 다른 페이지 (로그인/초대 검증 UI)로 라우팅하거나 문서 링크를 제공한다.

## 4. Domain Model (Draft)
- **HeroContent**
  - `title`, `description`, `brandIcon`, `ctaLabel`.
- **LoginFormState**
  - `email`, `password`, `showPassword`, `rememberMe`, `isLoading`.
- **SessionBanner**
  - `status`, `memberName`, `role`, `errorMessage`.

## 5. Test Plan
1. **Form Interaction Test**
   - React Testing Library로 이메일/비밀번호 입력, 패스워드 토글, Enter key 제출이 제대로 작동하는지 테스트한다.
2. **Session Banner Test**
   - SessionProvider mock으로 authenticated/unauthenticated/에러 상태를 만들어 해당 메시지와 CTA 표시가 맞는지 확인한다.
3. **Teacher CTA Routing Test**
   - `next/navigation`을 mock해 “선생님 회원가입” 버튼 클릭 시 올바른 경로로 push 호출을 확인한다.
4. **Manual Verification**
   - mock 토큰 유무에 따라 홈 화면의 상태 메시지/버튼이 어떻게 달라지는지 실제 dev 서버에서 확인하고, 블롭 애니메이션과 포커스 상태가 접근성에 문제 없는지 검증한다.
