# Frontend Agent Guidance

## 공통 컴포넌트 사용 원칙

- `/components` 페이지에 정의된 공통 UI(Button, Card, Carousel, Hero, NavigationBar, Footer, TextField, Checkbox 등)를 **모든 신규 화면 개발에 우선 사용**한다. 스타일을 조금 바꿀 때도 해당 컴포넌트를 확장/조합하는 방식을 먼저 고려한다.
- 이미 존재하는 컴포넌트로 요구사항을 구현할 수 없는 경우에는 임의로 새 컴포넌트를 만들지 말고, 사용자에게 아래 정보를 반드시 확인한다:
  1. 만들고자 하는 추가 컴포넌트의 이름/역할
  2. 기존 공통 컴포넌트로 해결이 어려운 이유
  3. 이 컴포넌트를 사용할 페이지/기능
- 사용자로부터 위 내용을 확인해 승인받은 뒤에만 새 컴포넌트를 추가하거나 기존 컴포넌트의 패턴을 변경한다.

## 구현 시 참고

- 공통 레이아웃(NavigationBar, Footer, Hero 등)은 `frontend/src/components/ui/` 하위에 이미 준비되어 있으므로 신규 페이지에서도 동일한 구조를 사용한다.
- 컴포넌트 예시는 `/components` 쇼케이스 페이지에서 시각적으로 확인할 수 있으며, 디자인 토큰은 `classhub-theme.ts` 와 `globals.css` 를 통해 공유된다.
