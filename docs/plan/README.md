# docs/plan 구조 가이드

## 디렉터리 구조

- `backend/season1`: 설계 실패로 중단한 PLAN 문서를 보존한다.
- `backend/season2`: 현재 진행 중인 Season2 리팩터링/기능 PLAN을 작성한다. (Season1 문서와 구분)
- `frontend/season1`, `frontend/season2`: 프런트엔드도 동일하게 시즌별 디렉터리를 유지한다.
- 루트(`docs/plan`): 커밋/개발 규칙, GitHub 템플릿 등 전역 공통 PLAN 문서를 유지한다.

## 사용 방법

1. 새 기능을 시작할 때 담당 영역(backend 또는 frontend)에 해당하는 디렉터리를 선택한다.
2. `<feature>_plan.md` 네이밍을 유지하며 PLAN 문서를 작성/수정한다.
3. 모든 PLAN 문서는 AGENTS.md 5.3에 정의된 템플릿(백엔드: 5.3.1, 프런트엔드: 5.3.2)을 반드시 따르고, 필요 항목을 빠짐없이 채운다.
4. 전역 규칙/문서화와 같이 특정 영역에 속하지 않는 PLAN은 루트에 추가한다.

## 작성 규칙

1. 각 PLAN은 Problem Definition → Requirements → API Design → Domain Model → Test Plan 순서를 따른다.
2. 계획 내용을 한국어로 요약해 리뷰어가 맥락을 쉽게 파악하도록 한다.
3. **프런트엔드 PLAN**에서는 5번 섹션을 `Test Plan`으로 작성해 React Testing Library, Storybook, Playwright 등 어떤 테스트를 수행할지 구체적으로 적는다 (기존 TDD Plan 명칭 대신 Test Plan 고정).
4. 사용자의 승인을 받은 후에만 테스트/구현을 진행하며, PLAN과 실제 작업이 어긋나면 즉시 문서를 업데이트한다.
