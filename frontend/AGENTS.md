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
- API를 호출하거나 타입을 정의할 때는 반드시 `frontend/src/types/openapi.d.ts`에서 제공하는 `paths`, `operations`, `components` 타입을 참고해 Request/Response를 선언한다. 임의의 string/path를 쓰지 말고, 오픈API 스키마를 근거로 한 타입 안전한 구현을 유지한다. 예: `type LoginRequestBody = components["schemas"]["LoginRequest"]`.

## 타입/빌드 워크플로

- **OpenAPI 타입 기반 개발 (필수)**
  - 모든 API 요청/응답 타입은 `frontend/src/types/openapi.d.ts`에서 자동 생성된 타입을 **반드시** 사용한다.
  - 임의의 인터페이스나 타입을 직접 정의하지 않고, `paths`, `operations`, `components` 타입을 alias로 선언한다.
  - 예시:
    ```typescript
    // ✅ 올바른 방법: OpenAPI 스키마 기반 타입 alias
    type LoginRequestBody = components["schemas"]["LoginRequest"];
    type LoginResponse = paths["/api/v1/auth/login"]["post"]["responses"]["200"]["content"]["application/json"];

    // ❌ 잘못된 방법: 수동으로 타입 정의
    interface LoginRequestBody {
      email: string;
      password: string;
    }
    ```
  - 백엔드 API 변경 시 `openapi.json` 재생성 → `npm run generate:types`로 타입 동기화.
- API 호출은 `frontend/src/lib/api.ts`의 클라이언트를 사용하고, 경로는 항상 `/api/v1/...` 전체 문자열을 전달한다.
- 에러 핸들링은 `frontend/src/lib/api-error.ts`의 `getApiErrorMessage`, `getFetchError` 헬퍼를 사용해 `response.error`를 직접 건드리지 않는다.
- 링크/리다이렉트는 Next.js `Route` 타입을 지키기 위해 `getDashboardRoute`, `isInternalRoute` 패턴을 따르며, 외부 URL은 `<a>` 요소로 처리한다. 공통 컴포넌트(Button, NavigationBar, Hero, Footer 등)도 이 규칙을 따른다.
- `SessionProvider`/훅 작업처럼 상태 로직을 수정할 때는 `npm run build -- --webpack`을 실행해 타입 검사를 통과했는지 즉시 확인한다. (macOS에서 Turbopack 권한 이슈가 있어 webpack 모드를 기본으로 사용한다.)
- 새 기능을 마무리할 때는 최소한 **수동 화면 확인 경로 + `npm run build -- --webpack` 결과**를 사용자에게 공유한다.

## 프론트엔드 테스트 & 검증 프로세스

### 개발 단계별 검증
프론트엔드 기능 개발 시 아래 순서로 검증을 수행한다:

#### 1. 타입 검증 (필수)
- **시점**: 코드 작성 완료 후, 커밋 전
- **명령어**: `cd frontend && npm run build -- --webpack`
- **통과 조건**: TypeScript 컴파일 에러 0개, 빌드 성공
- **실패 시**: 타입 에러 수정 후 재검증

#### 2. 수동 기능 테스트 (필수)
- **시점**: 타입 검증 통과 후
- **방법**: 개발 서버(`npm run dev`)에서 실제 UI 조작
- **확인 사항**:
  - 주요 사용자 시나리오(예: 로그인 → 대시보드 → 목록 조회)
  - 역할별 접근 제어(Teacher/Assistant/Student)
  - API 호출 성공/실패 처리
  - 로딩/에러/빈 상태 UI
  - 반응형 동작(Desktop/Mobile)
- **기록**: 테스트한 경로, 사용한 계정, 확인한 기능을 AGENT_LOG에 간략히 기록

#### 3. 엣지 케이스 검증 (권장)
- **빈 데이터**: 목록이 0건일 때 빈 상태 메시지 표시 확인
- **API 에러**: 백엔드 중단 또는 잘못된 요청 시 에러 메시지 + 재시도 버튼 확인
- **권한 없음**: 타 역할 전용 페이지 접근 시 리다이렉트/403 확인
- **네트워크 느림**: 개발자 도구에서 throttling 켜고 로딩 스켈레톤 확인

#### 4. 자동 테스트 (미래, 현재는 선택)
현재 프로젝트에는 테스트 프레임워크(React Testing Library, Playwright 등)가 없으므로 **수동 검증 우선**. 향후 도입 시:
- 컴포넌트 단위: React Testing Library로 렌더링/이벤트/상태 검증
- E2E: Playwright로 사용자 플로우 자동화
- 설정 후 AGENTS.md 업데이트

### 기능 완료 기준
아래 항목을 모두 체크해야 기능 완료로 간주:
- [ ] `npm run build -- --webpack` 통과
- [ ] 주요 시나리오 수동 테스트 완료
- [ ] 역할별 접근 제어 확인
- [ ] 에러/로딩/빈 상태 UI 확인
- [ ] 반응형(Desktop/Mobile) 동작 확인
- [ ] 테스트 결과 AGENT_LOG에 기록

### 검증 실패 시 대응
- **타입 에러**: 즉시 수정 후 재빌드
- **기능 버그**: 원인 파악 → 수정 → 재테스트
- **접근성/UX 이슈**: 개선 사항 TODO에 추가 또는 즉시 수정
- **심각한 문제**: 사용자에게 보고 후 함께 해결 방안 논의
