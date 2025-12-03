# Feature: Frontend Bootstrap (Next.js + Tailwind)

## 1. Problem Definition
- 백엔드 API는 구축되었지만, 로컬에서 기능을 검증하거나 향후 데모/테스트를 진행할 최소한의 Next.js 기반 프런트 프로젝트가 없다.
- TODO Phase 2 항목 중 "간단한 프런트(Next.js)로 현재까지 만든 기능 테스트"를 진행하기 위해 우선 공용 프레임워크/환경 세팅과 API 호출 기반을 마련해야 한다.

## 2. Requirements

### Functional
1. **프로젝트 스캐폴딩**
   - Next.js 16(App Router) + React 19 + TypeScript 5 + Tailwind 4 구조를 기본으로 하는 프로젝트를 `frontend/` 에 생성한다.
   - `package.json`, `tsconfig.json`, `next.config.mjs`, `postcss.config.cjs`, `tailwind.config.ts` 등을 포함한다.
2. **스타일링/레이아웃**
   - Tailwind 초기 설정과 글로벌 스타일(`globals.css`)을 포함한 최소 레이아웃 구성.
   - `app/layout.tsx`, `app/page.tsx`에서 기본 테마와 UI 컨테이너를 정의.
3. **API 클라이언트**
   - `src/lib/apiClient.ts` 형태로 Fetch 기반 래퍼를 만들고, `NEXT_PUBLIC_API_BASE_URL` + JWT 헤더 주입을 지원.
4. **환경 변수 가이드**
   - `.env.local.example` 에 API base URL, 샘플 토큰 등 필수 키를 명시.
5. **문서화**
   - README 또는 `frontend/README.md`에 설치/실행 방법, env 설정, 구조 설명을 간단히 작성.

### Non-functional
- Yarn/NPM 중 npm 사용(기존 repo 일관성), Node 20 LTS 가정.
- Lint/format은 추후 추가 예정이므로 스캐폴딩은 최소 설정만 포함하되 ESLint 비활성화 옵션 유지.
- 테스트는 추후 작업에서 Jest/RTL/Playwright를 붙일 수 있도록 구조만 마련.

## 3. API Design (Draft)
- API 클라이언트는 `GET/POST/PATCH/DELETE` 헬퍼를 노출하고, 기본 베이스 URL + JSON 컨텐트 타입 헤더를 설정한다.
- 토큰은 추후 세션 컨텍스트에서 주입 가능하도록 선택적 매개변수 사용.

## 4. Domain Model (Draft)
- 프런트 영역이라 별도 도메인 모델은 없으며, `/src/types/api.ts` 등에 백엔드 DTO 인터페이스를 정의할 준비만 한다.

## 5. TDD Plan
1. `npm run lint` (기본 Next lint)로 초기 구조 검증.
2. 기본 smoke test는 차후 Playwright/E2E 단계에서 추가하되, 현재는 빌드/런 가능한지 확인.
