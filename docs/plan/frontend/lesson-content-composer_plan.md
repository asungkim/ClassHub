# Feature: Lesson Content Composer Modal

## 1. Problem Definition
- SharedLesson/PersonalLesson 작성 API는 이미 마련되어 있지만, Teacher가 여러 화면에서 동일한 플로우로 두 종류의 진도를 한 번에 등록할 수 있는 UI가 존재하지 않는다.
- 현재는 공통 진도와 학생별 진도를 각각 다른 페이지로 이동해 작성해야 하므로, 동일 날짜/수업을 기록하면서 중복 입력과 맥락 손실이 발생한다.
- Teacher 대시보드 어디서든 동일한 CTA로 접근 가능한 전역 모달을 제공해, 반 선택 → 공통 진도 작성 → 선택 학생별 개인 진도 작성 플로우를 단일 UX에서 처리할 필요가 있다.

## 2. Requirements
### Functional
1. **전역 CTA**: DashboardShell 우측 상단에 `+ 수업 내용 작성` 버튼을 추가하고, 로그인한 사용자의 역할이 TEACHER일 때만 노출한다. 클릭 시 어디서든 동일한 모달이 열린다.
2. **모달 구조**: 모달은 세 구역(반 선택, 공통 진도 입력, 학생 선택/개별 진도 입력)으로 구성되며, 하단에 단계 요약/제출 버튼을 둔다. 모달 바깥 클릭 또는 ESC로 닫을 수 있다.
3. **반 선택 단계**:
   - `GET /api/v1/courses`(Teacher 본인 소유) 응답을 드롭다운으로 노출한다.
   - 반을 선택하면 이후 단계가 활성화되고, `GET /api/v1/courses/{courseId}/students`로 학생 목록을 불러온다.
   - 반을 변경하면 공유/개인 폼은 초기화된다.
4. **공통 진도(SharedLesson) 입력**:
   - 필수 필드: 날짜(default=오늘), 제목(max 100자), 내용(max 4000자).
   - 제출 시 SharedLesson은 반드시 생성되어야 하므로 유효성 검증이 통과하지 않으면 다음 단계로 진행할 수 없다.
5. **학생 선택 & 개인 진도(PersonalLesson)**:
   - 반 학생 목록을 체크박스 리스트로 렌더링한다. 선택된 학생마다 별도의 개인 진도 카드가 생성되며, 날짜(default=공통 진도 날짜)와 내용(max 2000자) 필드를 가진다.
   - 학생을 선택 해제하면 해당 카드/데이터도 제거된다. 학생을 전혀 선택하지 않고 제출해도 된다.
6. **제출 플로우**:
   - `POST /api/v1/shared-lessons`로 공통 진도를 먼저 생성한다(백엔드 구현 참고 시 SharedLessonController가 이 경로를 담당함. 사용자 요청에 적힌 엔드포인트 순서는 실제 구현과 반대이므로 코드 기준을 따른다).
   - SharedLesson 생성이 성공하면 선택된 학생 수만큼 `POST /api/v1/personal-lessons`를 병렬 호출한다.
   - 하나라도 실패할 경우 이미 생성된 PersonalLesson/SharedLesson에 대한 보정 로직은 제공하지 않으므로, 실패 항목에 대한 상태 메시지를 표시하고 사용자가 재시도할 수 있게 한다.
7. **결과 처리**:
   - 모든 호출이 성공하면 토스트를 띄우고 모달을 닫은 뒤 폼 및 선택 상태를 초기화한다.
   - 성공 후 `useQueryClient().invalidateQueries`로 SharedLesson/PersonalLesson/StudentCalendar 관련 캐시 키를 무효화한다.
8. **로딩/오류 상태**:
   - 반 목록/학생 목록 로딩 시 Skeleton 또는 Spinner를 표시한다.
   - 제출 중에는 CTA 버튼을 비활성화하고 Progress indicator를 보여준다.
   - 401/403 응답 시 세션 만료 안내와 함께 `/auth/login`으로 이동하는 옵션을 제공한다.

### Non-functional
- 공통 UI 컴포넌트(Button, Modal, Checkbox, TextField, Textarea, Command/Combobox)를 우선 재사용한다.
- 폼 상태 관리는 `react-hook-form` + `zodResolver`를 사용해 입력 제한(최대 길이, 필수값)을 컴파일 타임/런타임으로 모두 보장한다.
- API 타입은 `frontend/src/types/openapi.d.ts`에서 `paths["/api/v1/shared-lessons"]["post"]` 등으로 alias를 정의해 사용한다.
- 다중 PersonalLesson 요청 시 Promise.allSettled로 개별 성공/실패를 기록하며, 실패 항목은 카드 상단에 Inline Error를 노출한다.
- 모달은 키보드 포커스 트랩을 준수하고, 학생 목록은 10명 이상일 때 가상 스크롤 대신 단순 scrollable panel로 제공한다.
- Teacher 외 역할에서 버튼이나 API 호출이 일어나지 않도록 DashboardShell 수준에서 role guard를 둔다.
- 제출 완료 후에도 마지막으로 선택된 반과 날짜를 모멘토리 캐시에 보관해, 모달을 다시 열었을 때 직전 맥락을 복구할 수 있도록 한다(사용자가 원하는 경우 Reset 가능).

## 3. API Design (Draft)
### 3.1 공통 진도 생성
- **Endpoint**: `POST /api/v1/shared-lessons`
- **Auth**: TEACHER (Access Token)
- **Request Body** (`SharedLessonCreateRequest`):
  ```json
  {
    "courseId": "UUID",
    "date": "2025-01-05",
    "title": "12월 누적 복습",
    "content": "모의고사 풀이 & 오답 정리"
  }
  ```
- **Response**: `RsData<SharedLessonResponse>` (id, courseId, date, title, content 등)

### 3.2 개인 진도 생성
- **Endpoint**: `POST /api/v1/personal-lessons`
- **Auth**: TEACHER/ASSISTANT (Teacher 모달이 호출)
- **Request Body** (`PersonalLessonCreateRequest`):
  ```json
  {
    "studentProfileId": "UUID",
    "date": "2025-01-05",
    "content": "대수 집중 보완"
  }
  ```
- **Response**: `RsData<PersonalLessonResponse>`

### 3.3 보조 조회 API
- `GET /api/v1/courses?isActive=true` → Teacher의 활성 반 목록
- `GET /api/v1/courses/{courseId}/students` → 선택 반에 속한 학생 요약

### 호출 순서
1. 모달 열림 → `useQuery`로 Course 목록을 선로드.
2. Course 선택 → 해당 Course 학생 목록을 fetch.
3. 제출 → SharedLesson `POST` 성공 시 PersonalLesson들을 `Promise.allSettled`로 호출.
4. 실패 시 실패 학생 리스트와 메시지를 사용자에게 표시하고, 성공 항목은 그대로 둔다.

## 4. Domain Model (Draft)
- `LessonComposerState`
  ```ts
  type LessonComposerState = {
    isOpen: boolean;
    selectedCourseId?: string;
    selectedCourseName?: string;
    sharedLessonForm: SharedLessonFormValues;
    personalEntries: Record<string, PersonalLessonFormValues>; // key = studentProfileId
    selectedStudentIds: string[];
    submission: {
      status: 'idle' | 'creatingShared' | 'creatingPersonal' | 'error' | 'success';
      failures: Array<{ studentId: string; message: string }>;
    };
  };
  ```
- `SharedLessonFormValues`: `{ courseId: string; date: string; title: string; content: string; }`
- `PersonalLessonFormValues`: `{ studentProfileId: string; date: string; content: string; }`
- `CourseOption`: `{ id: string; name: string; isActive: boolean; }`
- `StudentOption`: `{ id: string; name: string; phoneNumber?: string; tags?: string[]; }`
- React Context `LessonComposerContext`로 `openComposer`, `closeComposer`, `prefillSharedLesson(date, courseId)` 등을 제공해 헤더 버튼과 모달 본문이 느슨하게 결합된다.
- React Query 키 예시: `['courses','teacher']`, `['courseStudents', courseId]`, `['sharedLessons','course',courseId]`, `['personalLessons','student',studentId]`.

## 5. TDD Plan
1. **타입 정의**
   - `frontend/src/types/api/lesson.ts`(신규)에서 SharedLesson/PersonalLesson POST operations alias 작성.
   - `npm run build -- --webpack`으로 타입 에러 확인.
2. **데이터 훅 작성**
   - `useTeacherCourses`, `useCourseStudents` 훅을 만들고 로딩/에러 캐싱 로직을 포함.
   - Storybook/Playground가 없으므로 mock response로 단일 렌더 테스트 컴포넌트 작성 후 빌드 검증.
3. **컨텍스트 & 버튼 연결**
   - DashboardShell 상단에 `LessonComposerProvider`를 감싸고, `+ 수업 내용 작성` 버튼을 연결.
   - Provider 단위의 단위 테스트 대신 dev server로 버튼→모달 오픈 여부를 수동 확인.
4. **SharedLesson 폼 검증**
   - react-hook-form + zod schema 로직을 구현하고, 필수값 누락/길이 초과 케이스를 단위로 확인.
   - `npm run build -- --webpack`으로 schema 타입 충돌 여부 점검.
5. **학생 선택/개별 폼**
   - 학생 체크박스 토글 시 personal form card가 동적으로 추가/제거되는지 수동 확인.
   - 각 personal 폼은 독립 validation을 가지며, 빈 텍스트 허용 X.
6. **제출 로직**
   - `handleSubmit`에서 SharedLesson → PersonalLesson 시퀀스를 Promise mock으로 검증.
   - 실패 시 card 상단에 에러를 표시하고 CTA 버튼을 다시 활성화하는지 확인.
7. **전역 상태 리셋/성공 흐름**
   - 성공 후 모달 닫힘, 토스트 노출, Query invalidation 호출 여부를 로그로 확인.
8. **회귀 테스트**
   - 주요 시나리오 수동 점검: (1) 학생 미선택 제출, (2) 여러 학생 선택, (3) SharedLesson 유효성 실패, (4) PersonalLesson 일부 실패.
   - 마지막으로 `npm run build -- --webpack` 실행.

## 6. UI & Interaction Walkthrough
1. **버튼 노출**: Dashboard 상단 우측에 Primary Button `+ 수업 내용 작성` 고정. 화면 폭이 좁으면 아이콘만 노출하고 텍스트는 툴팁으로 제공.
2. **모달 헤더**: `수업 내용 작성` 제목과 단계 안내(반 선택 → 공통 진도 → 개인 진도). 닫기 아이콘 제공.
3. **반 선택 구역**: Searchable Combobox로 반을 선택. 선택 후 Info Badge(학생 수, 담당 과목)를 표시하고 아래 단계로 자동 스크롤.
4. **공통 진도 폼**: 3열 레이아웃 (날짜 DatePicker, 제목 TextField, 내용 Textarea). 모든 필수, 미기입 시 Inline Error.
5. **학생 목록**: 두 컬럼 카드 또는 리스트로 표시, 각 항목에 체크박스 + 이름 + 연락처. 선택 시 해당 학생 카드가 personal form 스택에 추가.
6. **개별 진도 폼**: 선택 순서대로 accordion 형태로 추가. 각 학생 섹션에는 날짜/내용 입력, 삭제 아이콘, 에러표시 슬롯이 있다.
7. **하단 액션 바**: 왼쪽에는 선택 현황(예: `선택 학생 2명`), 오른쪽에는 Cancel + Primary Submit 버튼. 제출 중에는 Spinner + “작성 중…” 텍스트.
8. **성공/실패 피드백**: 성공 시 “공통 진도 및 2건의 개인 진도가 등록되었습니다” 토스트. 실패 시 실패 학생 이름 리스트를 제공하고 Retry 버튼 노출.

## 7. Risks & Open Questions
1. **학생 수 대량일 때 렌더링 부담**: 현재 예상 규모(반당 <40명)라면 가상 스크롤 없이도 무방하지만, 필요 시 Intersection Observer로 Lazy mount 고려.
2. **PersonalLesson 실패 롤백**: 일부 학생만 실패했을 때 수동 재시작 외 자동 롤백 요구가 없는지 사용자에게 재확인 필요.
3. **SharedLesson 날짜**: PersonalLesson 날짜를 SharedLesson 날짜와 다르게 입력할 수 있도록 허용할지? 기본은 동기화하되 사용자가 덮어쓸 수 있게 설계.
4. **Access control**: 조교가 동일 모달을 쓰지 않는다는 전제. 향후 조교도 작성해야 한다면 버튼 노출/권한 정의 재검토 필요.

## 8. 개발 순서

### Phase 1: 타입 정의 및 데이터 훅 준비

**참고**: 섹션 2 (Requirements), 섹션 3 (API Design), 섹션 5.1-5.2 (TDD Plan)

1. **OpenAPI 타입 alias 작성**

   - `frontend/src/types/api/lesson.ts` 파일을 생성하고 `paths["/api/v1/shared-lessons"]["post"]`, `paths["/api/v1/personal-lessons"]["post"]`, `paths["/api/v1/courses"]["get"]`, `paths["/api/v1/courses/{courseId}/students"]["get"]` alias를 선언한다.
   - Shared/Personal Lesson form value 타입과 API 요청 바디 타입을 구분해 import/export 구조를 정리한다.
   - `cd frontend && npm run build -- --webpack`으로 타입 선언이 잘 동작하는지 확인한다.

2. **데이터 훅 구성**

   - `useTeacherCourses`, `useCourseStudents` React Query 훅을 만든다(`frontend/src/hooks/api` 경로).
   - 훅 내부에 로딩/에러 상태를 노출하고, Course 변경 시 학생 목록 캐시를 초기화하는 invalidate 로직을 포함한다.
   - 훅을 테스트하기 위한 간단한 Playground 컴포넌트를 만들고 빌드로 타입 검증을 반복한다.

### Phase 2: Composer 컨텍스트 & CTA 연결

**참고**: 섹션 2.1-2.2 (전역 CTA/모달 구조), 섹션 4 (Domain Model), 섹션 6.1-6.2 (UI Walkthrough)

3. **LessonComposerProvider 구현**

   - `LessonComposerContext`와 Provider를 `frontend/src/contexts/lesson-composer.tsx`(가칭)에 정의하고, `LessonComposerState`/action을 useReducer로 관리한다.
   - `openComposer`, `closeComposer`, `resetComposer`, `prefillSharedLesson` 함수를 context value로 노출한다.

4. **DashboardShell 통합**
   - DashboardShell(및 Teacher/Assistant 전용 레이아웃)을 Provider로 감싸고, 상단 툴바 우측에 `+ 수업 내용 작성` 버튼을 추가한다.
   - 버튼은 TEACHER 권한에만 노출되도록 guard를 두고, 클릭 시 `openComposer()` 호출 여부를 수동 테스트한다.
   - 반응형 레이아웃(아이콘-only, tooltip)도 함께 구현한다.

### Phase 3: 공통 진도 폼 구축

**참고**: 섹션 2.3-2.4 (반 선택 + SharedLesson 입력), 섹션 4 (SharedLessonFormValues), 섹션 6.3-6.4 (UI Walkthrough)

5. **반 선택 영역**

   - Searchable Combobox 또는 Command 팔레트를 활용해 `GET /api/v1/courses` 응답을 렌더링한다.
   - 반 선택 시 sharedLessonForm의 `courseId`, `courseName`, `date` 기본값을 세팅하고 personalEntries/selectedStudentIds를 초기화한다.
   - 반 목록 로딩 중에는 Skeleton을, 오류 시 Alert + Retry 버튼을 표시한다.

6. **SharedLesson form + 검증**

   - `react-hook-form`과 `zodResolver`로 날짜/제목/내용 필수 검증을 구성한다.
   - DatePicker, TextField, Textarea 공통 컴포넌트를 사용하고, 인라인 에러 메시지를 표시한다.
   - SharedLesson form 상태 변경이 Context state와 동기화되도록 Controller를 적용한다.

### Phase 4: 학생 선택 및 PersonalLesson 폼

**참고**: 섹션 2.5 (학생 선택), 섹션 4 (personalEntries 구조), 섹션 6.5-6.6 (UI Flow)

7. **학생 목록 렌더링**

   - Course 선택 후 `useCourseStudents`로 받아온 학생들을 체크박스 리스트 또는 카드로 보여준다.
   - 10명 이상일 때 scrollable panel을 제공하고, 선택 수 표시 배지를 추가한다.

8. **동적 PersonalLesson 카드**

   - 학생 체크 시 personalEntries에 기본 폼(날짜=공통 진도 날짜, 내용="")를 추가하고 카드 UI를 생성한다.
   - 체크 해제 시 해당 personal form을 제거하며, Inline validation(내용 필수, 2000자 제한)을 적용한다.
   - 다수 학생 선택 시 Accordion 형태로 펼침/접힘을 제공한다.

### Phase 5: 제출 시퀀스 및 오류 처리

**참고**: 섹션 2.6-2.8 (제출/결과/로딩), 섹션 3 (API 순서), 섹션 5.6 (TDD 제출 로직)

9. **Shared → Personal 호출 시나리오**

   - `handleSubmit`에서 SharedLesson POST를 먼저 호출하고, 성공 응답의 ID/날짜를 personal form에 전달한다.
   - PersonalLesson 요청은 `Promise.allSettled`로 병렬 실행하며, 실패 항목은 `submission.failures`에 저장한다.
   - 제출 중에는 CTA 버튼과 Close 아이콘을 비활성화하고 Progress indicator를 노출한다.

10. **결과 피드백 & 상태 리셋**

    - 모든 요청 성공 시 Toast + 모달 닫기 + Context 상태 초기화 + 관련 React Query 캐시 invalidate.
    - 일부 실패 시 실패 학생 이름과 사유를 카드 상단/Toast로 보여주고, 사용자가 내용 수정 후 재시도할 수 있도록 CTA를 재활성화한다.
    - 401/403 발생 시 세션 만료 안내와 함께 로그인 화면 이동 버튼을 제공한다.

### Phase 6: 반응형 UX & 접근성 보완

**참고**: 섹션 2 (Non-functional), 섹션 6 전체 (Walkthrough), 섹션 5.7-5.8 (TDD Plan)

11. **모바일 Bottom Sheet**

    - 모달을 viewport width에 따라 중앙 모달 ↔ bottom sheet로 전환하고, 포커스 트랩 및 swipe-close(선택 사항) 동작을 확인한다.
    - 학생 리스트와 personal 폼이 모바일에서도 스크롤 가능한 단일 컬럼으로 배치되는지 확인한다.

12. **로딩/에러/Empty 상태**

    - 반/학생 목록, personal 폼, 제출 CTA 각각에 Skeleton/Spinner/Error Banner를 적용한다.
    - personalEntries가 없을 때는 “선택된 학생이 없습니다” 안내를 표시한다.

### Phase 7: 검증 및 로그 기록

**참고**: 섹션 5 (TDD Plan), 섹션 6.8 (피드백), 전역 AGENTS 지침

13. **수동/자동 검증**

    - 주요 시나리오(학생 미선택 제출, 다수 학생 선택, SharedLesson 유효성 실패, PersonalLesson 일부 실패)를 dev 서버에서 수동 실행한다.
    - `cd frontend && npm run build -- --webpack`으로 타입/빌드를 통과시킨다.

14. **AGENT_LOG 업데이트**

    - 테스트 경로, 빌드 결과, 남은 위험 요소를 `docs/history/AGENT_LOG.md`에 DESIGN/BEHAVIORAL 이벤트로 기록한다.
    - 필요시 TODO 상태(Phase 4 → Epic 통합 수업 내용 작성)를 🔄/✅로 갱신한다.
