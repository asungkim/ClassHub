# Feature: 불필요한 코드 정리 및 기존 인증/인가 검증

## 목표

Season2 백엔드에 맞춰 프론트엔드 정리 + SessionProvider 인증 플로우 검증

---

## 작업 순서

### 1. 코드 정리

**삭제 대상**:
- `app/dashboard/*` (전체 - 다음 Task에서 재구현)
- `components/{clinic,course,lesson,dashboard}`
- `hooks/{api,clinic,queries,use-assistants,use-courses,use-lesson-mutations,use-student-profiles,use-student-calendar}`
- `contexts/`
- `types/api/`

**보존**:
- `components/ui/*`, `components/shared/*`, `components/session/*`
- `lib/*`
- `hooks/use-debounce.ts`, `hooks/use-role-guard.tsx`

**실행**:
```bash
# 백업 후 삭제
rm -rf src/app/dashboard
rm -rf src/components/{clinic,course,lesson,dashboard}
rm -rf src/hooks/{api,clinic,queries}
rm src/hooks/use-{assistants,courses,lesson-mutations,student-profiles,student-calendar}.ts
rm -rf src/contexts
rm -rf src/types/api

# 빌드 검증
cd frontend && npm run build -- --webpack
```

**예상 에러**: Import 에러 → 사용되지 않는 import 제거 후 재빌드

---

### 2. SessionProvider 타입 수정

`components/session/session-provider.tsx`:

```typescript
// 기존
type MemberSummary = {
  memberId: string;
  email: string;
  name: string;
  role: string;  // ❌ any string
};

// 수정
type MemberRole = "TEACHER" | "ASSISTANT" | "STUDENT" | "ADMIN" | "SUPER_ADMIN";

type MemberSummary = {
  memberId: string;
  email: string;
  name: string;
  role: MemberRole;  // ✅ 명시적 타입
};
```

---

### 3. 인증 플로우 수동 테스트

#### 준비
- 백엔드 실행: `./gradlew bootRun`
- 프론트 실행: `cd frontend && npm run dev`

#### 테스트 1: 세션 초기화
1. 쿠키 전체 삭제
2. `http://localhost:3000` 접속
3. **확인**: 네트워크 탭에서 `POST /api/v1/auth/refresh` → 401, `status: unauthenticated`

#### 테스트 2: 세션 복원
1. Postman으로 로그인 API 호출 → Refresh 쿠키 복사
2. 브라우저에 쿠키 수동 설정
3. 페이지 새로고침
4. **확인**: `POST /auth/refresh` → 200, `GET /auth/me` → 200, `status: authenticated`

#### 테스트 3: 역할 확인
1. Teacher 계정 로그인
2. 개발자 도구 콘솔: `window.__SESSION__` (또는 컴포넌트에서 `useSession()` 출력)
3. **확인**: `member.role === "TEACHER"`

#### 테스트 4: 로그아웃
1. 대시보드에 임시 버튼 추가: `<button onClick={() => logout()}>로그아웃</button>`
2. 버튼 클릭
3. **확인**: `POST /auth/logout` → 200, 쿠키 제거, 홈으로 리다이렉트

---

## 완료 조건

- [ ] `npm run build -- --webpack` 통과
- [ ] `tree src/` 확인 → 삭제 대상 없음
- [ ] SessionProvider 타입 수정
- [ ] 수동 테스트 4개 완료
- [ ] AGENT_LOG 기록

---

## AGENT_LOG 템플릿

```markdown
## [날짜 시간] 프론트 코드 정리 및 인증 검증

### Type: STRUCTURAL | BEHAVIORAL

### Summary
- 사용되지 않는 프론트 코드 삭제 (dashboard, clinic, course, lesson 등)
- SessionProvider 인증 플로우 검증 완료

### Details
- 삭제: app/dashboard, components/{clinic,course,lesson,dashboard}, hooks 다수
- 보존: UI 컴포넌트, SessionProvider, API 클라이언트
- 빌드 통과, 수동 테스트 완료 (세션 초기화, 복원, 역할, 로그아웃)

### 다음 단계
- 선생님 회원가입 페이지 리팩터링
```
