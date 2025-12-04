# ClassHub Frontend

Next.js 16 + React 19 + Tailwind CSS 4 기반의 내부 데모/검증용 앱입니다. 백엔드 API를 빠르게 호출해 Auth, Invitation, StudentProfile 시나리오를 검증할 수 있도록 최소 스캐폴딩만을 제공합니다.

## 1. 요구 사항
- Node.js 20 LTS
- npm 10+

## 2. 설치
```bash
cd frontend
npm install
```

## 3. 환경 변수
`.env.local` 파일은 자동 생성되지 않습니다. 아래 예시를 참고해 수동으로 생성하세요.

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_MOCK_TOKEN=
```

- `NEXT_PUBLIC_API_BASE_URL`: 백엔드 API 베이스 URL (`/api/v1`까지 포함)
- `NEXT_PUBLIC_MOCK_TOKEN`: 개발 편의를 위한 임시 JWT 토큰 (선택)

## 4. 스크립트

| 명령 | 설명 |
| --- | --- |
| `npm run dev` | 개발 서버 실행 (http://localhost:3000) |
| `npm run build` | 프로덕션 빌드 |
| `npm run start` | 프로덕션 빌드 실행 |
| `npm run format` | Prettier 기반 코드 포맷 |
| `npm run openapi` | OpenAPI 스펙 다운로드 + 타입 생성 |

## 5. 기술 스택
- **프레임워크**: Next.js 16 (App Router, `src/` 디렉터리)
- **UI 라이브러리**: React 19
- **스타일링**: Tailwind CSS 4 (`@theme` 기반 토큰)
- **상태 관리**: @tanstack/react-query 5 (프로바이더만 선제 구성)
- **타입 시스템**: TypeScript 5.8
- **HTTP 클라이언트**: Fetch + `openapi-fetch`

## 6. 프로젝트 구조
```
frontend/
 ├─ src/
 │   ├─ app/
 │   │   ├─ layout.tsx           # 루트 레이아웃 + Providers 적용
 │   │   ├─ page.tsx             # 안내 페이지
 │   │   ├─ providers.tsx        # React Query Provider
 │   │   └─ globals.css          # Tailwind 4 CSS 설정
 │   ├─ components/              # (향후) UI 컴포넌트
 │   ├─ lib/
 │   │   ├─ api.ts               # 공용 API 클라이언트
 │   │   └─ env.ts               # 환경 변수 유틸리티
 │   └─ types/
 │       └─ openapi.json/.d.ts   # `npm run openapi` 결과 (gitignore)
 ├─ public/
 ├─ .env.local.example
 ├─ package.json
 ├─ tsconfig.json
 └─ next.config.ts
```

## 7. API 클라이언트 사용법
1. 백엔드 실행 후 `npm run openapi`를 수행하면 `src/types` 아래에 OpenAPI JSON과 타입 정의가 생성됩니다.
2. `src/lib/api.ts`에서 base URL과 JWT 헬퍼(`setAuthToken`, `clearAuthToken`)가 정의되어 있습니다.
3. 각 페이지 혹은 hook에서 `api.GET("/auth/me")`와 같이 사용하면 됩니다.

> 현재 `paths` 타입은 placeholder로 구성되어 있으며, `npm run openapi` 실행 시 생성된 타입으로 교체할 수 있습니다.

## 8. 다음 단계
1. Auth/Invitation/StudentProfile 페이지를 `src/app`에 추가합니다.
2. React Query hooks(`useQuery`, `useMutation`)를 정의해 API 호출을 분리합니다.
3. 공통 레이아웃, 에러 처리, 상태 표시 컴포넌트를 `src/components` 아래에 정리합니다.
4. Playwright/E2E 스크립트를 추가해 주요 플로우를 검증합니다.
