# MCP + GitHub 연동 가이드

에이전트가 Conventional Commits 규칙을 지킨 상태로 브랜치를 푸시하고 PR을 열 수 있도록 설정하는 절차를 설명한다.

## 1) 인증 방식 선택

- Personal Access Token(PAT)
  - 가장 간단하며 사용자 권한 범위 내에서 동작
  - https://github.com/settings/tokens (classic)에서 repo 스코프로 발급
- GitHub App(팀 사용 추천)
  - 세밀한 권한 제어 가능, 대상 저장소/조직에 설치
  - https://github.com/settings/apps 에서 앱을 만들고 private key 발급

## 2) 에이전트 실행 환경 변수 설정

MCP 클라이언트 또는 CLI 하니스에서 아래 환경 변수를 세팅한다.

- PAT 사용 시
  - `GH_TOKEN`: Personal Access Token 값
- GitHub App 사용 시
  - `GITHUB_APP_ID`: 숫자 App ID
  - `GITHUB_INSTALLATION_ID`: 저장소/조직 설치 ID
  - `GITHUB_APP_PRIVATE_KEY`: PEM 본문(시크릿 매니저에 보관)

공통

- `GH_OWNER`: 저장소 소유자(`your-org` 또는 `your-user`)
- `GH_REPO`: 저장소 이름(예: `classhub`)

## 3) MCP GitHub 서버 설정

MCP 클라이언트 설정에 GitHub 서버/툴을 등록한다(예시):

```jsonc
{
  "mcpServers": {
    "github": {
      "command": "mcp-github",
      "env": {
        "GH_TOKEN": "${GH_TOKEN}",
        "GH_OWNER": "${GH_OWNER}",
        "GH_REPO": "${GH_REPO}"
      }
    }
  }
}
```

제공 기능 예시
- 브랜치 생성/조회
- 파일 변경 커밋
- PR 생성/갱신
- PR 댓글 작성

## 4) 에이전트 작업 흐름

1. 브랜치 생성: `feature/<scope>-<desc>` 등(`docs/plan/commit-standards_plan.md` 참고)
2. 변경 사항 스테이징
3. 커밋 메시지: `<type>(<scope>): <subject>`
4. 브랜치 푸시
5. 동일한 제목으로 PR 생성, 본문에 배경과 체크리스트 기재

## 5) CI 강제 조건

- `.github/workflows/commitlint.yml`이 PR 커밋과 제목을 검증한다.
- `commitlint.config.cjs`가 허용 타입/스코프 및 제한을 정의한다.

## 6) 운영 팁

- 저장소 설정에서 `main`을 보호(필수 상태 체크, 강제 푸시 금지).
- PR은 작게 유지하고 Squash merge로 이력을 깔끔하게 관리한다.
