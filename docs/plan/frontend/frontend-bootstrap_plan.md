# Feature: Frontend Bootstrap (Next.js 16)

## 1. Problem Definition

- 백엔드 API는 준비되어 있으나, 이를 검증하고 향후 UI 개발을 진행할 Next.js 기반 프런트엔드 스캐폴딩이 없다.
- `/frontend` 디렉터리를 Next.js 16 + TypeScript + Tailwind 조합으로 초기화하고, 공통 설정·디렉터리 구조·명령어 등을 표준화할 필요가 있다.

## 2. Requirements

### Functional
1. `create-next-app@16`으로 프로젝트 생성 (TypeScript, App Router, Tailwind, ESLint 포함).
2. 기본 페이지(`app/page.tsx`)는 단순 Welcome/Health 체크 용도로 두고, 추후 Auth UI가 들어올 준비를 한다.
3. `@/` import alias, `src/` 디렉터리 사용, ESLint + Next Recommended 설정 유지.
4. Tailwind 초기 설정(`globals.css`, 기본 색상/폰트 토큰)과 ThemeProvider 자리 마련.
5. npm 스크립트 정리 (`dev`, `build`, `start`, `lint`, `format` 등).
6. VSCode용 `settings.json`, `.editorconfig` 등 팀 공통 설정이 필요하면 추가.

### Non-functional
- Node 20.x 기준, npm 사용.
- ESLint/Prettier(필요 시) 규칙은 기존 백엔드/문서와 충돌하지 않도록 최소 설정.
- Husky or lint-staged는 추후 단계로 미루고, 기본 lint만 구성.

## 3. Directory & Config Plan

```
frontend/
├─ app/
│  └─ page.tsx
├─ src/
│  ├─ components/
│  └─ lib/
├─ public/
├─ package.json
├─ tsconfig.json
├─ next.config.mjs
├─ postcss.config.js
├─ tailwind.config.ts
└─ .eslintrc.json
```

- `app/`에 라우팅, `src/` 아래에 컴포넌트/유틸 배치.
- Tailwind `tailwind.config.ts`에서 추후 사용할 커스텀 컬러/폰트 토큰 확장.
- `next.config.mjs`는 `typedRoutes: true`, `reactStrictMode: true` 등 기본 옵션 유지.

## 4. Implementation Steps
1. `rm -rf frontend && npx create-next-app@16 frontend --ts --tailwind --eslint --app --src-dir --import-alias "@/*" --use-npm`.
2. 생성된 README/Unused 파일 정리.
3. Tailwind 기본 색상/타이포 토큰을 정의 (혹은 이후 단계에서 정의할 placeholder).
4. 간단한 Landing Component 작성 (추후 Auth UI와 연결 예정).
5. `npm run lint`, `npm run dev` 로 정상 작동 확인.

## 5. TDD/Verification Plan
- `npm run lint`로 ESLint 통과 확인.
- `npm run dev` 실행 후 기본 페이지가 뜨는지 수동 확인.
- 초기에는 테스트 코드 없음; CI 파이프라인은 이후 구성.

