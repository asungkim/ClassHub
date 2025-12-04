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
- 기능을 작은 단위(컴포넌트/훅/상태)로 나눠 작업하고 **각 단위가 끝날 때마다 어떤 자동/수동 테스트를 실행했는지, 어떤 화면에서 확인했는지를 반드시 기록**한다.
- API를 호출하거나 타입을 정의할 때는 반드시 `frontend/src/types/openapi.d.ts`에서 제공하는 `paths`, `operations`, `components` 타입을 참고해 Request/Response를 선언한다. 임의의 string/path를 쓰지 말고, 오픈API 스키마를 근거로 한 타입 안전한 구현을 유지한다.

## 타입/빌드 워크플로

- API 호출은 `frontend/src/lib/api.ts`의 클라이언트를 사용하고, 경로는 항상 `/api/v1/...` 전체 문자열을 전달한다. Request/Response 타입은 `components["schemas"][...]` 혹은 `paths["/auth/login"]["post"]`처럼 **OpenAPI 타입을 alias**로 선언해서 사용한다.
- 에러 핸들링은 `frontend/src/lib/api-error.ts`의 `getApiErrorMessage`, `getFetchError` 헬퍼를 사용해 `response.error`를 직접 건드리지 않는다.
- 링크/리다이렉트는 Next.js `Route` 타입을 지키기 위해 `getDashboardRoute`, `isInternalRoute` 패턴을 따르며, 외부 URL은 `<a>` 요소로 처리한다. 공통 컴포넌트(Button, NavigationBar, Hero, Footer 등)도 이 규칙을 따른다.
- `SessionProvider`/훅 작업처럼 상태 로직을 수정할 때는 `npm run build -- --webpack`을 실행해 타입 검사를 통과했는지 즉시 확인한다. (macOS에서 Turbopack 권한 이슈가 있어 webpack 모드를 기본으로 사용한다.)
- 새 기능을 마무리할 때는 최소한 **수동 화면 확인 경로 + `npm run build -- --webpack` 결과**를 사용자에게 공유한다.
