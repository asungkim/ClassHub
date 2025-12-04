# docs/plan 구조 가이드

## 디렉터리 구조
- `backend/`: Spring/Auth/Invitation 등 서버 사이드 관련 PLAN 문서를 모은다.
- `frontend/`: Next.js 기반 UI/테스트 등 프런트 작업 PLAN 문서를 모은다.
- 루트(`docs/plan`): 커밋/개발 규칙, GitHub 템플릿 등 전역 공통 PLAN 문서를 유지한다.

## 사용 방법
1. 새 기능을 시작할 때 담당 영역(backend 또는 frontend)에 해당하는 디렉터리를 선택한다.
2. `<feature>_plan.md` 네이밍을 유지하며 PLAN 문서를 작성/수정한다.
3. 전역 규칙/문서화와 같이 특정 영역에 속하지 않는 PLAN은 루트에 추가한다.

## 작성 규칙
1. 각 PLAN은 Problem Definition → Requirements → API Design → Domain Model → Test Plan 순서를 따른다.
2. 계획 내용을 한국어로 요약해 리뷰어가 맥락을 쉽게 파악하도록 한다.
3. **프런트엔드 PLAN**에서는 5번 섹션을 `Test Plan`으로 작성해 React Testing Library, Storybook, Playwright 등 어떤 테스트를 수행할지 구체적으로 적는다 (기존 TDD Plan 명칭 대신 Test Plan 고정).
4. 사용자의 승인을 받은 후에만 테스트/구현을 진행하며, PLAN과 실제 작업이 어긋나면 즉시 문서를 업데이트한다.
