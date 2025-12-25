# Feature: 학생 선생님 검색/요청 UI

## 1. Problem Definition
- 학생은 “선생님 목록” 자체보다 연결 요청을 보내고 상태를 확인하는 흐름이 필요하다.
- 기존 `/student/course/search`와 `/student/my-courses` 화면 패턴을 재사용해 학습 비용을 줄인다.
- 요청 중복을 막고, 요청 상태를 명확히 보여줘야 한다.

## 2. User Flows & Use Cases
### Flow A: 선생님 검색 → 요청 생성
1. 사이드바에서 “선생님 관리” 진입
2. 기본 탭: “선생님 검색”
3. 이름 검색 + 학원/지점 필터 선택
4. 검색 결과 카드에서 요청 버튼 클릭
5. 요청 성공 시 카드 비활성/성공 메시지 표시

### Flow B: 요청 내역 확인 → 취소
1. “신청 내역” 탭으로 이동
2. 기본 필터는 PENDING
3. 상태 필터 변경 시 목록 갱신
4. PENDING 요청에 한해 취소 버튼 클릭
5. 취소 후 상태 갱신

## 3. Page & Layout Structure
### /student/teachers (탭형 페이지)
- 상단: 페이지 타이틀 + 탭(선생님 검색 / 신청 내역)
- 탭 1: 선생님 검색
  - 필터 영역: 이름 검색, 학원 필터, 지점 필터
  - 목록 영역: 선생님 카드 리스트 + 페이지네이션
- 탭 2: 신청 내역
  - 상태 필터(드롭다운 또는 버튼 그룹)
  - 요청 카드 리스트 + 페이지네이션

### 반응형
- 모바일: 필터는 1열 스택, 카드 리스트는 1열
- 데스크탑: 필터는 1줄 정렬, 카드 리스트 2~3열

## 4. Component Breakdown
- **StudentTeacherTabs**
  - 왜 필요한지: 검색/신청내역 전환을 명확히 하기 위해
  - 어떻게 동작: 기존 탭 컴포넌트 또는 페이지 내 상태로 탭 렌더링
  - 어디에 붙나: `/student/teachers` 상단

- **TeacherSearchFilters**
  - 왜 필요한지: 이름/학원/지점 필터를 한 곳에서 관리
  - 어떻게 동작: 입력값 변경 시 query state 업데이트
  - 어디에 붙나: 검색 탭 상단

- **TeacherSearchCard**
  - 왜 필요한지: 선생님 + 출강 지점을 요약 보여주기 위해
  - 어떻게 동작: 이름/지점 목록 표시 + “요청” 버튼
  - 어디에 붙나: 검색 탭 리스트

- **TeacherRequestListItem**
  - 왜 필요한지: 요청 상태와 메시지를 한 눈에 확인
  - 어떻게 동작: 상태 배지 + 취소 버튼(PENDING만 활성)
  - 어디에 붙나: 신청 내역 탭 리스트

- **Pagination**
  - 왜 필요한지: 페이지 기반 조회
  - 어떻게 동작: page/size 변경 시 재조회
  - 어디에 붙나: 각 탭 리스트 하단

> 공통 컴포넌트(Button, Card, TextField, Checkbox, Badge 등)는 `/components`를 우선 사용한다.
> 신규 컴포넌트가 필요하면 사전 승인 후 추가한다.

## 5. State & Data Flow
### 공통 상태
- query: keyword, companyId, branchId, page, size
- requestStatus: PENDING/APPROVED/REJECTED/CANCELLED
- loading/error/empty state

### API 계약
- 선생님 검색
  - `GET /api/v1/teachers?keyword=&companyId=&branchId=&page=&size=`
- 요청 생성
  - `POST /api/v1/teacher-student-requests`
  - body: `teacherId`, `message?`
- 요청 내역 조회
  - `GET /api/v1/teacher-student-requests?status=&page=&size=`
- 요청 취소
  - `PATCH /api/v1/teacher-student-requests/{requestId}/cancel`

### 타입 사용
- `frontend/src/types/openapi.d.ts`의 `paths`, `components` 타입 사용
- API 호출은 `frontend/src/lib/api.ts` 클라이언트 사용
- 에러 메시지는 `getApiErrorMessage`, `getFetchError` 사용

## 6. Interaction & UX Details
- 검색 키워드는 300~500ms 디바운스 (선택)
- 요청 생성 후 카드에서 “요청됨” 상태 표시 또는 카드 제거
- 요청 내역에서 PENDING 외 상태는 취소 버튼 비활성
- 에러 발생 시 토스트 또는 인라인 에러 메시지
- 빈 상태 문구 제공 (검색 결과 없음, 요청 내역 없음)

## 7. Test & Verification Plan
- 타입 검증: `cd frontend && npm run build -- --webpack`
- 수동 QA
  - 선생님 검색: 키워드/필터 적용, 요청 버튼 동작
  - 신청 내역: 상태 필터 변경, PENDING 취소
  - 에러/빈 상태 UI 확인
  - 모바일/데스크탑 레이아웃 확인
