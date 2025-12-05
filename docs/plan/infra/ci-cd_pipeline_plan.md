# Feature: GitHub Actions CI/CD 파이프라인

## 1. Problem Definition

- prod Compose 환경은 외부 레지스트리에 올라간 백엔드 이미지를 참조해야 하므로, 수동 빌드/배포 대신 일관된 CI/CD 파이프라인이 필요하다.
- 현재 저장소에는 테스트/빌드/이미지 푸시/배포 단계가 자동화되어 있지 않아, 배포 시마다 로컬 환경에 의존하거나 누락된 검증으로 인한 장애 위험이 존재한다.

## 2. Requirements

### Functional

1. **트리거**
   - main 브랜치 PR 생성 시 CI(Test + Build) 실행.
   - main 브랜치 머지 시 CD(이미지 빌드/푸시 + 배포 트리거) 실행.
   - 필요 시 `workflow_dispatch`로 수동 배포 허용.
2. **백엔드 파이프라인**
   - `./gradlew test`로 전체 테스트 실행(테스트 프로필 사용).
   - Docker 이미지 빌드 → GHCR `ghcr.io/<org>/classhub-backend:<git-sha>` 태그로 푸시.
   - 이미지 태그/메타데이터를 artifacts 혹은 GitHub Environments에 기록해 배포 단계에서 사용.
3. **배포 단계**
   - EC2(또는 runner)에 SSH/Session Manager로 접속해 `docker compose pull && docker compose up -d` 실행.
   - 배포 job은 main merge 후에만 실행되도록 `environment: production` + 승인자 지정.
4. **비밀 관리**
   - GHCR 토큰, AWS 자격증명, Vercel 토큰은 GitHub Actions Secrets/Environments에서 주입한다.

### Non-functional

- Workflow는 Ubuntu runner 기준으로 10분 이내 완료, 재시도 가능.
- 캐싱(`actions/setup-java`, `actions/setup-node`, Gradle cache, npm cache)으로 불필요한 빌드 시간을 줄인다.
- Infra 관련 명령은 idempotent하게 구성(배포 job에서 `docker compose pull` 실패 시 전체 workflow 실패).

## 3. API Design (Draft)

- `.github/workflows/ci.yml`: PR 기준 CI (frontend/backend test/build) → status 체크.
- `.github/workflows/cd.yml`: main merge 시 실행. backend 이미지 빌드/푸시 → `deploy` job에서 EC2 명령 실행.
- 배포 명령 예시:
  ```bash
  ssh ec2-user@${PROD_HOST} "cd /opt/classhub && docker compose pull && docker compose up -d"
  ```
  혹은 GitHub Actions OIDC + SSM `aws ssm send-command` 사용.

## 4. Domain Model (Draft)

- **Jobs**
  - `backend-test`: Java 21, Gradle 캐시, 테스트 실행.
  - `docker-build`: backend-test 성공 시 의존, Docker buildx로 GHCR 푸시.
  - `deploy`: docker-build 성공 + main push 시 실행.
- **Artifacts/registry**
  - GHCR 리포지토리: `ghcr.io/<org>/classhub-backend`.
  - 태그 전략: `sha`, `branch-latest`, `prod`.
  - Compose prod override는 `BACKEND_IMAGE`, `BACKEND_TAG` env로 이 이미지를 참조.

## 5. TDD Plan

1. 워크플로 작성 후 `act` 또는 PR로 마른 실행(Dry Run) → 각 job이 정상적으로 완료되는지 확인.
2. 테스트 job에 `SPRING_PROFILES_ACTIVE=test` 환경을 넣어 DB 의존 없이 성공하는지 확인.
3. GHCR dummy repo로 푸시 테스트(권한 최소 범위 PAT 사용) 후 이미지가 업로드되는지 검증.
4. 배포 job은 sandbox EC2에서 `workflow_dispatch`로 시험 실행해 `docker compose pull/up`이 성공하는지 확인.
5. 실패 시 Slack/Webhook 알림이 도달하는지, GitHub Checks 상태가 PR에 반영되는지 검토한다.
