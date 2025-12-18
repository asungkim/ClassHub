# Feature: Season2 프론트엔드 재구성

## 1. Problem Definition

백엔드가 Season2 엔티티 구조로 완전히 재설계되면서, 기존 프론트엔드 코드의 대부분이 더 이상 사용되지 않는 API와 도메인 모델에 의존하고 있다. v1.9 TODO에 정의된 최소 범위(인증/인가, 회원가입, 초대 관리, 대시보드)만 구현하기 위해 프론트엔드를 정리하고 재구성해야 한다.

### 현재 상황

- **백엔드**: Season2 엔티티 (Company, Branch, Course, Member+역할별Info, Invitation, StudentEnrollmentRequest 등)
- **프론트엔드**: Season1 구조 기반 페이지/컴포넌트/Hooks (Course CRUD, Clinic 관리, Lesson 작성, StudentProfile 등)
- **불일치**: API 경로, Request/Response 스키마, 도메인 개념이 전면 변경됨

### 해결해야 할 문제

1. **사용되지 않는 페이지 제거**: Clinic, StudentProfile, Course 관리 등 Season2에서 아직 구현되지 않은 기능 페이지
2. **사용되지 않는 컴포넌트/Hooks 제거**: 구 API 기반 데이터 페칭 로직 및 도메인 컴포넌트
3. **재사용 가능한 인프라 보존**: UI 컴포넌트 라이브러리, API 클라이언트, SessionProvider, 타입 유틸리티
4. **인증 페이지 리팩터링**: Teacher 회원가입, Invitation 검증, Invited 회원가입 페이지를 Season2 API에 맞춰 수정

---

## 2. User Flows & Use Cases

### UC-1: 코드 정리 (Agent)

1. 현재 프론트엔드 디렉토리 구조 파악
2. 보존/삭제/리팩터링 대상 분류
3. 사용되지 않는 파일 백업 후 삭제
4. 빌드 검증 (`npm run build -- --webpack`)
5. AGENT_LOG에 변경 내역 기록

### UC-2: 인증 페이지 리팩터링 (Agent)

1. OpenAPI 스키마 확인 (`/api/v1/auth/*`)
2. Teacher 회원가입 페이지 수정 (Company/Branch 입력 추가)
3. Invitation 검증 페이지 수정 (Assistant/Student 분기)
4. Invited 회원가입 페이지 분리 (Assistant/Student)
5. 각 단계마다 빌드 + 수동 테스트

---

## 3. Page & Layout Structure

### 보존 대상

```
app/
├── layout.tsx              # 루트 레이아웃 (유지)
├── providers.tsx           # QueryClient, SessionProvider (유지)
├── page.tsx                # 홈페이지 (유지)
├── components/page.tsx     # UI 쇼케이스 (유지)
└── auth/                   # 인증 페이지 (리팩터링)
    ├── register/
    │   ├── teacher/page.tsx       # Season2 API 맞춤
    │   ├── assistant/page.tsx     # 신규 (invited에서 분리)
    │   └── student/page.tsx       # 신규 (invited에서 분리)
    └── invitation/
        └── verify/page.tsx        # Season2 API 맞춤
```

### 삭제 대상

```
app/dashboard/
├── clinic-slots/           # ClinicSlot 미구현
├── students/               # StudentProfile → StudentEnrollmentRequest로 변경
├── teacher/
│   ├── courses/           # Course API 변경
│   └── student-calendar/  # 미구현
├── assistants/            # 조교 관리 미구현
├── invitations/           # API 변경됨 (재구현 필요)
├── assistant/page.tsx     # 대시보드 재구현
├── student/page.tsx       # 대시보드 재구현
├── teacher/page.tsx       # 대시보드 재구현
└── superadmin/page.tsx    # 대시보드 재구현
```

---

## 4. Component Breakdown

### 4.1 보존 컴포넌트

#### UI 라이브러리 (전부 유지)

| 컴포넌트           | 역할                           | 위치                    |
| ------------------ | ------------------------------ | ----------------------- |
| Button             | 공통 버튼                      | `components/ui/`        |
| Card               | 카드 레이아웃                  | `components/ui/`        |
| TextField          | 입력 필드                      | `components/ui/`        |
| Modal              | 모달 다이얼로그                | `components/ui/`        |
| Table              | 테이블                         | `components/ui/`        |
| Badge              | 뱃지/태그                      | `components/ui/`        |
| Select             | 드롭다운                       | `components/ui/`        |
| Checkbox           | 체크박스                       | `components/ui/`        |
| Skeleton           | 로딩 스켈레톤                  | `components/ui/`        |
| ErrorState         | 에러 상태 표시                 | `components/ui/`        |
| NavigationBar      | 네비게이션 바                  | `components/ui/`        |
| Footer             | 푸터                           | `components/ui/`        |
| Hero               | 히어로 섹션                    | `components/ui/`        |
| AppChrome          | 앱 크롬 레이아웃               | `components/ui/`        |
| ConfirmDialog      | 확인 다이얼로그                | `components/ui/`        |
| Toast              | 토스트 알림                    | `components/ui/`        |
| TimeSelect         | 시간 선택 (미래 Clinic 사용)   | `components/ui/`        |
| EmptyState         | 빈 상태 표시                   | `components/shared/`    |
| LoadingSkeleton    | 로딩 스켈레톤 래퍼             | `components/shared/`    |
| InvitationStatusBadge | 초대 상태 뱃지              | `components/shared/`    |

#### 세션/인프라

| 컴포넌트         | 역할                               | 위치                       | 비고                     |
| ---------------- | ---------------------------------- | -------------------------- | ------------------------ |
| SessionProvider  | 세션 상태 관리                     | `components/session/`      | Season2 API 호환 확인    |
| AppErrorBoundary | 전역 에러 바운더리                 | `components/ui/`           | 유지                     |

### 4.2 삭제 컴포넌트

#### 도메인 컴포넌트 (API 불일치)

| 컴포넌트             | 사유                           | 위치                    |
| -------------------- | ------------------------------ | ----------------------- |
| ClinicSlotGrid       | Clinic 미구현                  | `components/clinic/`    |
| CreateSlotModal      | Clinic 미구현                  | `components/clinic/`    |
| EditSlotModal        | Clinic 미구현                  | `components/clinic/`    |
| CourseFormModal      | Course API 변경                | `components/course/`    |
| CoursePicker         | Course API 변경                | `components/course/`    |
| LessonComposerModal  | Lesson 미구현                  | `components/lesson/`    |
| EditLessonModal      | Lesson 미구현                  | `components/lesson/`    |
| DashboardSections    | 대시보드 재구현 필요           | `components/dashboard/` |
| DashboardShell       | 대시보드 재구현 필요           | `components/dashboard/` |
| ComponentsShowcase   | 쇼케이스 페이지에 통합됨       | `components/showcase/`  |

### 4.3 삭제 Hooks

#### API Hooks (구 API 기반)

| Hook                 | 사유                           | 위치              |
| -------------------- | ------------------------------ | ----------------- |
| useCourseStudents    | StudentProfile → EnrollmentRequest | `hooks/api/`      |
| useTeacherCourses    | Course API 변경                | `hooks/api/`      |
| lesson-composer-keys | Lesson 미구현                  | `hooks/api/`      |
| use-clinic-slots     | Clinic 미구현                  | `hooks/clinic/`   |
| use-assistants       | API 변경                       | `hooks/`          |
| use-courses          | API 변경                       | `hooks/`          |
| use-lesson-mutations | Lesson 미구현                  | `hooks/`          |
| use-student-profiles | StudentProfile 미구현          | `hooks/`          |
| use-student-calendar | 미구현                         | `hooks/`          |

#### 보존 Hooks

| Hook            | 역할                  | 위치            | 비고                     |
| --------------- | --------------------- | --------------- | ------------------------ |
| use-debounce    | 디바운스 유틸리티     | `hooks/`        | 공통 유틸리티            |
| use-role-guard  | 역할 기반 접근 제어   | `hooks/`        | Season2 역할 체계 확인   |

### 4.4 삭제 Context

| Context              | 사유              | 위치          |
| -------------------- | ----------------- | ------------- |
| LessonComposerContext | Lesson 미구현     | `contexts/`   |

---

## 5. State & Data Flow

### 5.1 보존 인프라

#### API 클라이언트

- **위치**: `lib/api.ts`
- **역할**: `openapi-fetch` 기반 타입 안전 API 클라이언트
- **기능**:
  - Request Interceptor: `Authorization: Bearer {token}` 자동 주입
  - Response Interceptor: 401 에러 시 자동 토큰 갱신 후 재시도
  - `setAuthToken`, `getAuthToken`, `clearAuthToken`, `tryRefreshToken`, `forceLogout`
- **변경 불필요**: Season2 API도 동일한 인증 메커니즘 사용

#### 에러 처리

- **위치**: `lib/api-error.ts`
- **헬퍼**: `getApiErrorMessage`, `getFetchError`
- **변경 불필요**: RsData 응답 포맷 동일

#### 역할 & 라우팅

- **위치**: `lib/role.ts`, `lib/role-route.ts`
- **역할**: Season2 역할 체계 (`TEACHER`, `ASSISTANT`, `STUDENT`, `ADMIN`, `SUPER_ADMIN`) 타입 정의
- **변경 필요**: Season2 단일 역할 시스템 반영 (Member.role은 단일 값)

#### 환경 변수

- **위치**: `lib/env.ts`
- **변경 불필요**: `apiBaseUrl`, `mockToken` 등 그대로 사용

### 5.2 세션 관리

#### SessionProvider

- **위치**: `components/session/session-provider.tsx`
- **현재 동작**:
  1. 초기화 시 `tryRefreshToken()`으로 access 토큰 발급
  2. `/api/v1/auth/me`로 사용자 정보 조회
  3. `MemberSummary` 타입 (`memberId`, `email`, `name`, `role`) 반환
- **Season2 확인 사항**:
  - `/api/v1/auth/me` 응답 스키마 확인 (백엔드 구현 여부)
  - `role` 필드가 단일 값인지 확인
- **리팩터링 필요**: `/api/v1/auth/me` API가 없으면 토큰 디코딩 또는 로그인 응답에서 세션 정보 추출

### 5.3 OpenAPI 타입

- **위치**: `types/openapi.d.ts`, `types/openapi.json`
- **생성**: `openapi-typescript`로 자동 생성
- **사용 패턴**:
  ```typescript
  type LoginRequestBody = components["schemas"]["LoginRequest"];
  type LoginResponseData = components["schemas"]["LoginResponse"];
  ```
- **업데이트 필요**: Season2 백엔드 OpenAPI 스펙 재생성 후 반영

---

## 6. Interaction & UX Details

### 6.1 인증 페이지 리팩터링

#### Teacher 회원가입 (`/auth/register/teacher`)

**현재 구조**:
- 입력 필드: email, password, confirmPassword, name, termsAccepted
- API: `POST /api/v1/auth/register/teacher`
- Request: `TeacherRegisterRequest { email, password, name }`

**Season2 변경 사항**:
- Company/Branch 입력 추가 (Company.type: INDIVIDUAL/ACADEMY)
- Request 스키마 확인 필요 (백엔드 `TeacherRegisterRequest` 확인)
- 성공 시 로그인 페이지로 이동

**리팩터링 계획**:
1. OpenAPI 스키마에서 `TeacherRegisterRequest` 확인
2. Company/Branch 입력 필드 추가 (type 선택, name, description)
3. 폼 검증 로직 업데이트
4. 빌드 + 수동 테스트 (회원가입 → 로그인 → 대시보드)

#### Invitation 검증 (`/auth/invitation/verify`)

**현재 구조**:
- 쿼리 파라미터: `code`
- API: `GET /api/v1/invitations/verify/{code}` (추정)
- 검증 성공 시 회원가입 페이지로 이동

**Season2 변경 사항**:
- Invitation.inviteeRole로 ASSISTANT/STUDENT 구분
- 검증 후 역할별 회원가입 페이지로 분기
  - ASSISTANT → `/auth/register/assistant`
  - STUDENT → `/auth/register/student`

**리팩터링 계획**:
1. OpenAPI 스키마에서 Invitation 검증 API 확인
2. 응답에서 `inviteeRole` 추출
3. 역할별 라우팅 로직 구현
4. 빌드 + 수동 테스트 (초대 코드 입력 → 검증 → 회원가입)

#### Invited 회원가입 분리

**현재 구조**:
- 단일 페이지: `/auth/register/invited`
- API: `POST /api/v1/auth/register/invited` (추정)

**Season2 변경 사항**:
- ASSISTANT와 STUDENT 회원가입 분리
- Assistant: Teacher와의 연결 (`TeacherAssistantAssignment`)
- Student: Course 등록 신청 없이 회원가입만 (초대 코드 기반)

**리팩터링 계획**:
1. `/auth/register/assistant/page.tsx` 생성
   - 입력: invitationCode, email, password, name
   - API: `POST /api/v1/auth/register/assistant`
2. `/auth/register/student/page.tsx` 생성
   - 입력: invitationCode, email, password, name, schoolName, grade, birthDate, parentPhone
   - API: `POST /api/v1/auth/register/student`
3. 기존 `/auth/register/invited/page.tsx` 삭제
4. 빌드 + 수동 테스트 (각 역할별 회원가입 → 로그인)

### 6.2 로딩/에러/빈 상태

모든 인증 페이지에서 다음 상태 처리:
- **로딩**: `isSubmitting` 상태 + 버튼 비활성화
- **에러**: `InlineError` 컴포넌트로 에러 메시지 표시
- **성공**: 성공 메시지 + 1.5초 후 자동 리다이렉트

---

## 7. Test & Verification Plan

### 7.1 정리 작업 검증

#### 1단계: 파일 삭제 후 빌드 검증

```bash
cd frontend
npm run build -- --webpack
```

**통과 조건**: TypeScript 컴파일 에러 0개

#### 2단계: 남은 파일 목록 확인

```bash
tree src/app src/components src/hooks src/lib -I 'node_modules|.next'
```

**예상 결과**:
- `app/`: layout, providers, page, components, auth (리팩터링 전)
- `components/ui/`: 전체 유지
- `components/shared/`: EmptyState, LoadingSkeleton, InvitationStatusBadge
- `components/session/`: SessionProvider
- `hooks/`: use-debounce, use-role-guard
- `lib/`: api, api-error, env, role, role-route

### 7.2 인증 페이지 리팩터링 검증

#### Teacher 회원가입 테스트

1. **시나리오**: Teacher 신규 회원가입 → 로그인 → 대시보드
2. **테스트 경로**: `/auth/register/teacher`
3. **입력 데이터**:
   - Email: `teacher1@example.com`
   - Password: `password123`
   - Name: `홍길동`
   - Company Type: INDIVIDUAL
   - Company Name: `개인학원`
4. **확인 사항**:
   - 폼 검증 (필수 필드, 비밀번호 일치)
   - API 호출 성공 → 성공 메시지 표시
   - 1.5초 후 홈 페이지로 리다이렉트
5. **에러 케이스**:
   - 이미 존재하는 이메일 → 409 에러 메시지 표시
   - 네트워크 에러 → 에러 메시지 표시

#### Invitation 검증 테스트

1. **시나리오**: Teacher가 생성한 초대 코드로 검증 → Assistant 회원가입
2. **테스트 경로**: `/auth/invitation/verify?code=ABC123`
3. **확인 사항**:
   - 초대 코드 검증 API 호출
   - 응답에서 `inviteeRole` 추출 → ASSISTANT
   - `/auth/register/assistant?code=ABC123`로 리다이렉트
4. **에러 케이스**:
   - 잘못된 초대 코드 → 404 에러 메시지
   - 만료된 초대 코드 → 410 에러 메시지

#### Assistant 회원가입 테스트

1. **시나리오**: 초대 코드로 Assistant 회원가입 → 로그인 → 대시보드
2. **테스트 경로**: `/auth/register/assistant?code=ABC123`
3. **입력 데이터**:
   - Invitation Code: `ABC123` (자동 입력)
   - Email: `assistant1@example.com`
   - Password: `password123`
   - Name: `김철수`
4. **확인 사항**:
   - 초대 코드 자동 입력 (쿼리 파라미터)
   - API 호출 성공 → 성공 메시지
   - 1.5초 후 홈 페이지로 리다이렉트
   - 로그인 후 역할이 ASSISTANT인지 확인

#### Student 회원가입 테스트

1. **시나리오**: 초대 코드로 Student 회원가입 → 로그인 → 대시보드
2. **테스트 경로**: `/auth/register/student?code=XYZ789`
3. **입력 데이터**:
   - Invitation Code: `XYZ789`
   - Email: `student1@example.com`
   - Password: `password123`
   - Name: `이영희`
   - School Name: `서울고등학교`
   - Grade: HIGH_3
   - Birth Date: `2007-03-15`
   - Parent Phone: `010-1234-5678`
4. **확인 사항**:
   - 모든 필드 검증 (필수 입력, 날짜 형식, 전화번호 형식)
   - API 호출 성공 → 성공 메시지
   - 로그인 후 역할이 STUDENT이고 StudentInfo 생성 확인

### 7.3 빌드 & 타입 검증

매 리팩터링 단계마다:

```bash
cd frontend
npm run build -- --webpack
```

**통과 조건**: 컴파일 에러 0개, 빌드 성공

---

## 8. Implementation Sequence

### Phase 1: 코드 정리 (STRUCTURAL)

#### Task 1.1: 삭제 대상 파일 백업

```bash
# Dashboard 페이지
find src/app/dashboard -type f -name "*.tsx" ! -path "*invitations*" -exec mv {} {}.bak \;

# Clinic/Course/Lesson 컴포넌트
mv src/components/clinic src/components/clinic.bak
mv src/components/course src/components/course.bak
mv src/components/lesson src/components/lesson.bak
mv src/components/dashboard src/components/dashboard.bak

# Hooks
mv src/hooks/api src/hooks/api.bak
mv src/hooks/clinic src/hooks/clinic.bak
mv src/hooks/queries src/hooks/queries.bak
mv src/hooks/use-assistants.ts src/hooks/use-assistants.ts.bak
mv src/hooks/use-courses.ts src/hooks/use-courses.ts.bak
mv src/hooks/use-lesson-mutations.ts src/hooks/use-lesson-mutations.ts.bak
mv src/hooks/use-student-profiles.ts src/hooks/use-student-profiles.ts.bak
mv src/hooks/use-student-calendar.ts src/hooks/use-student-calendar.ts.bak

# Contexts
mv src/contexts/lesson-composer-context.tsx src/contexts/lesson-composer-context.tsx.bak

# Types (lesson 타입)
mv src/types/api src/types/api.bak
```

**검증**: `npm run build -- --webpack` (컴파일 에러 확인)

#### Task 1.2: 에러 수정 후 재빌드

- Import 에러 제거 (삭제된 파일 참조 제거)
- 사용되지 않는 export 제거

**검증**: 빌드 통과

#### Task 1.3: 삭제 대상 완전 제거

백업 파일(`.bak`) 삭제:

```bash
find src -name "*.bak" -type f -delete
find src -name "*.bak" -type d -exec rm -rf {} +
```

**검증**: `tree src/` 확인, AGENT_LOG 기록

---

### Phase 2: 인증 페이지 리팩터링 (BEHAVIORAL)

#### Task 2.1: OpenAPI 스펙 확인

```bash
# 백엔드 OpenAPI 스펙 다운로드 (백엔드 실행 후)
curl http://localhost:8080/v3/api-docs > frontend/src/types/openapi.json

# TypeScript 타입 생성
cd frontend
npx openapi-typescript src/types/openapi.json -o src/types/openapi.d.ts
```

**확인 사항**:
- `POST /api/v1/auth/register/teacher` Request/Response
- `GET /api/v1/invitations/verify/{code}` Response
- `POST /api/v1/auth/register/assistant` Request/Response
- `POST /api/v1/auth/register/student` Request/Response
- `GET /api/v1/auth/me` Response (세션 정보)

#### Task 2.2: Teacher 회원가입 리팩터링

1. `src/app/auth/register/teacher/page.tsx` 수정
   - Company/Branch 입력 필드 추가
   - Request 타입 업데이트
   - 폼 검증 로직 업데이트
2. 빌드 검증
3. 수동 테스트 (회원가입 → 로그인)

**테스트 경로**: `/auth/register/teacher`

#### Task 2.3: Invitation 검증 리팩터링

1. `src/app/auth/invitation/verify/page.tsx` 수정
   - API 호출 로직 업데이트
   - 역할별 라우팅 로직 추가 (ASSISTANT/STUDENT)
2. 빌드 검증
3. 수동 테스트 (초대 코드 입력 → 검증 → 리다이렉트)

**테스트 경로**: `/auth/invitation/verify?code=TEST123`

#### Task 2.4: Assistant 회원가입 신규 구현

1. `src/app/auth/register/assistant/page.tsx` 생성
   - Teacher 회원가입 페이지 복사 후 수정
   - Invitation Code 필드 추가 (쿼리 파라미터에서 자동 입력)
   - API: `POST /api/v1/auth/register/assistant`
2. 빌드 검증
3. 수동 테스트 (초대 코드 → 회원가입 → 로그인)

**테스트 경로**: `/auth/register/assistant?code=TEST123`

#### Task 2.5: Student 회원가입 신규 구현

1. `src/app/auth/register/student/page.tsx` 생성
   - StudentInfo 필드 추가 (schoolName, grade, birthDate, parentPhone)
   - Grade enum 선택 UI (Select 컴포넌트)
   - API: `POST /api/v1/auth/register/student`
2. 빌드 검증
3. 수동 테스트 (초대 코드 → 회원가입 → 로그인)

**테스트 경로**: `/auth/register/student?code=TEST456`

#### Task 2.6: Invited 회원가입 페이지 삭제

```bash
rm -rf src/app/auth/register/invited
```

**검증**: 빌드 통과, 모든 인증 플로우 재테스트

---

### Phase 3: SessionProvider 검증 (BEHAVIORAL)

#### Task 3.1: `/api/v1/auth/me` API 확인

백엔드에 `/api/v1/auth/me` 엔드포인트가 있는지 확인:

- 있으면: 응답 스키마 확인 후 SessionProvider 수정
- 없으면: 대체 방안 구현 (로그인 응답에서 세션 정보 추출 또는 JWT 디코딩)

#### Task 3.2: SessionProvider 리팩터링

1. `components/session/session-provider.tsx` 수정
   - Season2 API 호출 로직 업데이트
   - `MemberSummary` 타입 확인 (단일 역할)
2. 빌드 검증
3. 수동 테스트 (로그인 → 세션 확인 → 로그아웃)

**확인**: `useSession()` 훅에서 `member.role`이 단일 값으로 반환됨

---

### Phase 4: AGENT_LOG 기록 (DESIGN)

모든 작업 완료 후 `docs/history/AGENT_LOG.md`에 기록:

```markdown
## [2025-12-18 HH:mm] Season2 프론트엔드 재구성

### Type
STRUCTURAL | BEHAVIORAL | DESIGN

### Summary
- 백엔드 Season2 엔티티 구조에 맞춰 프론트엔드 정리 및 인증 페이지 리팩터링
- 사용되지 않는 페이지/컴포넌트/Hooks 삭제 (Clinic, Course, Lesson, StudentProfile 등)
- Teacher/Assistant/Student 회원가입 페이지 분리 및 Season2 API 연동

### Details
- **삭제된 파일**: app/dashboard/{clinic-slots,students,teacher,assistants,assistant,student,superadmin}, components/{clinic,course,lesson,dashboard}, hooks/{api,clinic,queries,use-*}, contexts/lesson-composer-context
- **보존된 인프라**: components/ui/*, lib/*, components/session/session-provider, hooks/use-debounce
- **리팩터링된 페이지**: auth/register/teacher, auth/invitation/verify
- **신규 페이지**: auth/register/assistant, auth/register/student
- **테스트**: 빌드 통과, 수동 기능 테스트 완료 (회원가입 → 로그인 → 대시보드)

### 다음 단계
- v1.9 TODO: 역할별 대시보드 페이지 신규 구현
- Teacher 초대 관리 페이지 (초대 생성, 목록 조회, 취소)
```

---

## 9. Risk & Mitigation

### Risk 1: OpenAPI 스펙 불일치

**위험**: 백엔드 API가 문서와 다를 경우 프론트 구현 막힘

**완화**:
- Phase 2 시작 전 백엔드 실행 후 OpenAPI 스펙 다운로드
- Postman/Thunder Client로 API 수동 테스트
- 불일치 발견 시 백엔드 수정 요청

### Risk 2: SessionProvider 대체 구현 필요

**위험**: `/api/v1/auth/me` API가 없으면 세션 관리 방식 변경 필요

**완화**:
- 로그인 API 응답에 사용자 정보 포함 (백엔드 수정)
- JWT 디코딩으로 사용자 정보 추출 (프론트 구현)
- 우선순위: 백엔드에 `/api/v1/auth/me` 추가 요청

### Risk 3: 빌드 실패 (타입 에러)

**위험**: 대량 파일 삭제 후 import 에러 발생

**완화**:
- Phase 1에서 `.bak` 백업 후 단계적 삭제
- 각 단계마다 빌드 검증
- 에러 발생 시 백업에서 복원 가능

---

## 10. Definition of Done

### Phase 1 완료 조건

- [ ] 사용되지 않는 페이지/컴포넌트/Hooks 삭제
- [ ] `npm run build -- --webpack` 통과 (컴파일 에러 0개)
- [ ] `tree src/` 결과에 삭제 대상 파일 없음
- [ ] AGENT_LOG에 STRUCTURAL 이벤트 기록

### Phase 2 완료 조건

- [ ] Teacher 회원가입 페이지 리팩터링 (Company/Branch 입력 추가)
- [ ] Invitation 검증 페이지 리팩터링 (역할별 라우팅)
- [ ] Assistant 회원가입 페이지 신규 구현
- [ ] Student 회원가입 페이지 신규 구현 (StudentInfo 필드)
- [ ] 모든 인증 페이지 빌드 통과 + 수동 테스트 완료
- [ ] AGENT_LOG에 BEHAVIORAL 이벤트 기록

### Phase 3 완료 조건

- [ ] SessionProvider Season2 API 호환 확인
- [ ] `useSession()` 훅 동작 확인 (로그인 → 세션 정보 조회)
- [ ] AGENT_LOG 기록

### 전체 완료 조건

- [ ] 모든 Phase 완료
- [ ] `npm run build -- --webpack` 최종 빌드 통과
- [ ] 수동 테스트: Teacher/Assistant/Student 회원가입 → 로그인 → 세션 확인
- [ ] AGENT_LOG에 전체 작업 요약 기록
- [ ] 사용자에게 완료 보고 (삭제된 파일 목록, 리팩터링 내역, 테스트 결과)

---

## 11. References

- **요구사항**: `docs/requirement/v1.3.md`
- **스펙**: `docs/spec/v1.3.md`
- **TODO**: `docs/todo/v1.9.md`
- **프로젝트 가이드**: `AGENTS.md`, `frontend/AGENTS.md`
- **백엔드 OpenAPI**: `http://localhost:8080/swagger-ui.html`
