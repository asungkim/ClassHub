# Feature: GitHub Issue 템플릿

## 1. Problem Definition
- 현재 이슈는 자유 형식으로 작성되어 버그/기능/잡무 맥락을 정확히 담기 어렵다.
- 심각도/영향/재현 절차 등 우선순위 판단에 필요한 정보가 일관되지 않아 triage와 TODO 진행이 늦어진다.

## 2. Requirements

### Functional
- 버그, 기능 제안, 작업/잡무 3가지 흐름에 대한 GitHub Issue Form(YAML)을 제공한다.
- 각 템플릿은 자동으로 라벨(`bug`, `enhancement`, `chore`)을 적용하고, 담당자/프로젝트 메타데이터를 위한 placeholder를 둔다.
- Bug 템플릿: 요약, 영향/심각도, 환경, 재현 단계, 기대/실제 행동, 첨부/로그를 수집한다.
- Feature 템플릿: 사용자 스토리, 문제 정의, 요구 결과, 승인 기준, 의존성, 롤아웃 고려 사항을 수집한다.
- Task 템플릿: 운영/문서 잡무에 초점을 맞추고 범위, DOD, 관련 스펙·TODO 링크를 수집한다.
- 모든 템플릿은 필수 입력 검증과 “기존 이슈 검색/필수 정보 기재” 확인 체크리스트를 포함한다.

### Non-functional
- 각 템플릿은 5개 이하 주요 질문으로 짧고 가독성 있게 구성한다.
- GitHub Issue Form(YAML)만 사용해 웹/모바일 렌더링 일관성을 보장하고 Markdown 템플릿은 사용하지 않는다.
- 자동화를 위해 라벨/체크리스트 등은 영어 기반으로 유지하되, 설명은 자유롭게 작성 가능하게 한다.
- 향후 CI/TODO 연동과 호환되도록 조직 특화 시크릿을 피한다.

## 3. API Design (Draft)
- **버그 리포트(`.github/ISSUE_TEMPLATE/bug-report.yml`)**
  - name/about/title 기본값: `bug: <component> ...`
  - labels: `bug`
  - Body: 심각도 드롭다운, 재현 절차 텍스트, 로그 텍스트, 환경(OS/브라우저/백엔드 버전), 기대/실제 행동 입력.
- **기능 제안(`.github/ISSUE_TEMPLATE/feature-request.yml`)**
  - name/about/title 기본값: `feat: ...`
  - labels: `enhancement`
  - Body: 문제 정의, 제안 솔루션, `Acceptance Criteria` 체크리스트, 의존성/노트 텍스트.
- **작업/잡무(`.github/ISSUE_TEMPLATE/task.yml`)**
  - name/about/title 기본값: `chore: ...`
  - labels: `chore`
  - Body: 범위 요약, DOD 체크리스트, 참고 링크(스펙/TODO/PR) 입력.
- 모든 템플릿에 “기존 이슈 검색 완료” 확인 체크박스를 필수로 둔다.

## 4. Domain Model (Draft)
- `BugReport`: {title, severity, environment, reproductionSteps, expectedBehavior, actualBehavior, impact, attachments}
- `FeatureRequest`: {title, persona/userStory, problem, proposedSolution, acceptanceCriteria[], dependencies, rolloutNotes}
- `Task`: {title, scope, deliverables, references[], dueDate(optional)}
- 라벨(`bug`, `enhancement`, `chore`)은 향후 보드 자동화와 연동된다.

## 5. TDD Plan
- 템플릿 작성 후 `gh issue create --template <name>` 명령으로 필수 필드/기본 라벨이 적용되는지 수동 검증한다.
- GitHub Issue Form 스키마에 맞춰 YAML을 검증(CI 또는 IDE)한다.
- 테스트용 브랜치/저장소에서 GitHub UI 미리보기를 확인해 질문 길이와 필수 입력 동작을 검증한다.
