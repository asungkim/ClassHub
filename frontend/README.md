# ClassHub Frontend

Next.js 16 + React 19 + Tailwind CSS 4 + React Query 기반의 내부 데모/검증용 앱입니다. 백엔드 API를 호출해 Auth, Invitation, StudentProfile 흐름을 빠르게 검증할 수 있도록 설계되었습니다.

## 1. 요구 사항

- Node.js 20 LTS
- npm 10+

## 2. 설치

```bash
cd frontend
npm install
```

## 3. 환경 변수

`.env.local` 파일이 자동 생성되어 있습니다. 필요시 아래 값을 수정하세요.

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_MOCK_TOKEN=
```

- `NEXT_PUBLIC_API_BASE_URL`: 백엔드 API 베이스 URL
- `NEXT_PUBLIC_MOCK_TOKEN`: 개발 편의를 위한 임시 JWT 토큰 (선택)

## 4. 스크립트

| 명령                     | 설명                                            |
| ------------------------ | ----------------------------------------------- |
| `npm run dev`            | 개발 서버 실행 (http://localhost:3000)         |
| `npm run build`          | 프로덕션 빌드                                   |
| `npm run start`          | 프로덕션 빌드 실행                              |
| `npm run lint`           | Next.js 기본 ESLint 검사                        |
| `npm run openapi`        | OpenAPI 스펙 다운로드 및 타입 생성              |
| `npm run openapi:download` | OpenAPI JSON 스펙만 다운로드                  |
| `npm run openapi:generate` | 다운로드된 스펙에서 TypeScript 타입 생성      |

## 5. 기술 스택

- **프레임워크**: Next.js 16 (App Router)
- **UI 라이브러리**: React 19
- **스타일링**: Tailwind CSS 4 (CSS 기반 설정)
- **상태 관리**: @tanstack/react-query 5
- **타입 시스템**: TypeScript 5
- **HTTP 클라이언트**: Fetch API (타입 안전한 래퍼 제공)

## 6. 프로젝트 구조

```
frontend/
 ├─ app/
 │   ├─ layout.tsx           # 루트 레이아웃 (서버 컴포넌트)
 │   ├─ providers.tsx        # React Query Provider (클라이언트)
 │   ├─ page.tsx             # 홈 페이지
 │   └─ globals.css          # Tailwind 4 CSS 설정
 ├─ src/
 │   ├─ lib/
 │   │   └─ api.ts           # OpenAPI 기반 타입 안전 API 클라이언트
 │   └─ types/
 │       ├─ openapi.json     # OpenAPI 스펙 (자동 생성, gitignore)
 │       └─ openapi.d.ts     # TypeScript 타입 (자동 생성, gitignore)
 ├─ public/                  # 정적 자산
 ├─ .env.local               # 환경 변수 (gitignore)
 ├─ .env.local.example       # 환경 변수 예시
 ├─ package.json
 ├─ tsconfig.json
 └─ next.config.mjs
```

## 7. API 클라이언트 사용법

백엔드의 OpenAPI 스펙에서 타입을 자동 생성하여 사용합니다.

### 1단계: 백엔드 실행
```bash
# 터미널 1: 백엔드 실행
cd backend
./gradlew bootRun
```

### 2단계: OpenAPI 타입 생성
```bash
# 터미널 2: 프론트엔드에서 타입 생성
cd frontend
npm run openapi
```

이 명령은 다음을 수행합니다:
1. `http://localhost:8080/v3/api-docs`에서 OpenAPI JSON 다운로드
2. `src/types/openapi.d.ts` 타입 정의 파일 생성

### 3단계: API 호출

```typescript
import { api } from "@/src/lib/api";

// GET 요청 (타입 자동 완성)
const { data, error } = await api.GET("/api/v1/auth/me", {
  headers: { Authorization: `Bearer ${token}` }
});

// POST 요청 (body 타입 자동 검증)
const { data, error } = await api.POST("/api/v1/auth/login", {
  body: {
    email: "test@test.com",
    password: "1234"
  }
});

// 에러 처리
if (error) {
  console.error("API Error:", error);
  return;
}

// data 타입이 자동으로 추론됨
console.log("Success:", data);
```

**특징:**
- ✅ 백엔드 스펙과 100% 동기화 (자동 생성)
- ✅ 타입 안전성 보장 (컴파일 타임 에러 체크)
- ✅ IDE 자동완성 지원 (경로, 메서드, body, response)
- ✅ 에러 타입도 포함
- ✅ 제로 런타임 오버헤드

## 8. 타입 정의

타입은 OpenAPI 스펙에서 자동 생성됩니다:

- `src/types/openapi.d.ts`: 백엔드 API의 모든 타입 정의 (자동 생성)
- `src/types/openapi.json`: OpenAPI JSON 스펙 (자동 다운로드)

백엔드 API가 변경되면 `npm run openapi`로 타입을 재생성하세요.

## 9. Tailwind CSS 4

Tailwind CSS 4는 **CSS 기반 설정**을 사용합니다:

- `app/globals.css`에서 `@theme` 블록으로 커스텀 색상 정의
- `tailwind.config.ts` 파일 없음 (v3과 다름)
- `postcss.config.cjs` 파일 불필요

```css
@theme {
  --color-brand-500: #6366f1;
}
```

## 10. 다음 단계

1. **Auth 화면 추가** (`app/auth/login`, `app/auth/signup`)
2. **Invitation 화면 추가** (`app/invitations`)
3. **StudentProfile 화면 추가** (`app/students`)
4. **React Query hooks 작성** (useMutation, useQuery)
5. **Playwright E2E 테스트 추가**

## 11. 개발 가이드

### React Query 사용 예시

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/src/lib/api";

export function ProfilePage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["auth", "me"],
    queryFn: async () => {
      const { data, error } = await api.GET("/api/v1/auth/me", {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("accessToken")}`
        }
      });
      if (error) throw error;
      return data;
    }
  });

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  // data 타입이 자동으로 Member로 추론됨
  return <div>Hello, {data?.name}</div>;
}
```

### 서버 컴포넌트 vs 클라이언트 컴포넌트

- **서버 컴포넌트** (기본): `layout.tsx`, 정적 페이지
- **클라이언트 컴포넌트** (`"use client"`): 상태, 이벤트, React Query 사용 시

## 12. 문제 해결

### 빌드 에러 발생 시

```bash
rm -rf .next node_modules
npm install
npm run dev
```

### 타입 에러

`tsconfig.json`의 `paths` 설정을 확인하세요:

```json
{
  "paths": {
    "@/*": ["./*"]
  }
}
```
