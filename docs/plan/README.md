# docs/plan 구조 가이드

## 디렉터리 구조
- `backend/`: Spring/Auth/Invitation 등 서버 사이드 관련 PLAN 문서를 모은다.
- `frontend/`: Next.js 기반 UI/테스트 등 프런트 작업 PLAN 문서를 모은다.
- 루트(`docs/plan`): 커밋/개발 규칙, GitHub 템플릿 등 전역 공통 PLAN 문서를 유지한다.

## 사용 방법
1. 새 기능을 시작할 때 담당 영역(backend 또는 frontend)에 해당하는 디렉터리를 선택한다.
2. `<feature>_plan.md` 네이밍을 유지하며 PLAN 문서를 작성/수정한다.
3. 전역 규칙/문서화와 같이 특정 영역에 속하지 않는 PLAN은 루트에 추가한다.
