# Feature: GitHub PR 템플릿

## 1. Problem Definition
- 현재 PR 템플릿은 간단한 체크리스트만 제공해 AGENT 프로세스(PLAN → TODO → LOG → TEST) 준수를 검증하기 어렵다.
- 구조/행동 변경 분리, 관련 스펙/이슈 링크, 테스트 증빙 등 필수 정보를 누락한 채 PR이 열려 리뷰 효율이 떨어진다.
- Conventional Commits, AGENT_LOG 기록 여부, TODO 상태 언급 등을 자동으로 확인할 수 있는 항목이 필요하다.

## 2. Requirements
### Functional
- GitHub에서 렌더링되는 단일 Markdown PR 템플릿을 제공한다 (`.github/PULL_REQUEST_TEMPLATE.md`).
- 섹션 구성: 요약, 관련 문서/이슈 링크, 변경 타입, 상세 설명(STRUCTURAL/BEHAVIORAL 분리), 테스트 증빙, TODO/LOG 업데이트 여부.
- 체크리스트: PLAN 승인 여부, TODO 우선순위 준수, AGENT_LOG 기록 여부, 테스트 결과, 배포 영향 등.
- reviewers가 한눈에 diff 범위/리스크를 파악할 수 있도록 브레이킹 체인지, 검증 방법을 별도 블록으로 포함한다.
- PR 자동 라벨링은 commitlint workflow와 충돌하지 않도록 템플릿에서 직접 설정하지 않는다.

### Non-functional
- 한국어 설명을 기본으로 하되, 템플릿 구조는 GitHub 기본 Markdown과 호환되어야 한다.
- 총 길이는 스크롤 피로를 줄이기 위해 200라인 이하로 유지한다.
- 필수 정보(예: 관련 TODO, AGENT 로그 여부)는 체크박스로 강제하고, 작성자는 자유 텍스트로 근거를 기입한다.
- AGENT 지침 변화 시 쉽게 업데이트할 수 있도록 섹션/체크리스트를 모듈화(주석 없이 깔끔한 Markdown)한다.

## 3. API Design (Draft)
- `## 요약` : 핵심 변경 2~3줄, 사용자 영향 포함.
- `## 관련 문서` : TODO/PLAN/이슈/스펙 링크 bullet.
- `## 변경 타입` : 체크박스(Feat/Fix/Docs/Test/Chore/Refactor/Style/Perf + Structural/Behavioral 구분 안내).
- `## 상세 내용` : 행동 변화, 구조 변경, 검증 포인트를 소제목으로 구분.
- `## 테스트` : 실행한 테스트 명령과 결과 표기(예: `npm test`, `./gradlew test`).
- `## 체크리스트` :
  - [ ] TODO 순서 준수 확인
  - [ ] PLAN 승인 여부
  - [ ] AGENT_LOG 업데이트 완료
  - [ ] 신규/수정 문서, 코드 리뷰 노트 기재
  - [ ] 배포 영향 및 롤백 전략 작성
- `## 추가 참고` : 스크린샷, 로그, 특이사항 첨부 섹션.

## 4. Domain Model (Draft)
- `PullRequestSummary`: {title, description, linkedDocs[]}
- `ChangeTypeChecklist`: {feat, fix, refactor, docs, chore, test, perf, style, structural, behavioral}
- `ValidationInfo`: {tests[], screenshots[], rolloutPlan}
- `ProcessCompliance`: {todoLink, planLink, agentLogUpdated:boolean, risk:boolean}

## 5. TDD Plan
- 템플릿 적용 전용 브랜치에서 PR 작성 시 각 섹션/체크박스가 GitHub UI에 제대로 표시되는지 확인한다.
- 필수 체크 항목을 모두 비활성 상태로 두고 제출하려고 할 때 GitHub가 기본 체크박스를 허용하는지 확인하고, 리뷰 단계에서 Reviewer가 빠진 항목을 쉽게 찾을 수 있는지 검수한다.
- 샘플 PR을 열어 PLAN/TODO/LOG 링크를 실제로 기입해 링크 렌더링과 마크다운 서식이 정상인지 검증한다.
