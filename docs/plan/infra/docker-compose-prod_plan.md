# Feature: Docker Compose Prod Override & Env Templates

## 1. Problem Definition
- 현재 Compose 설정은 로컬 개발 중심이며, 운영 환경(EC2)에서 바로 사용할 수 있는 override 구조와 비밀값 템플릿이 없어 배포 과정이 반복적으로 깨진다.
- 운영에서는 HTTPS Reverse Proxy, 백엔드 이미지 태그, DB 자격증명 등을 분리 관리해야 하므로, base Compose를 확장하는 prod 전용 정의와 `.env.prod` 표준이 필요하다.
- 목표는 `docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d` 명령 하나로 운영 구성을 재현할 수 있게 만드는 것이다.

## 2. Requirements
### Functional
1. **파일 구조**
   - `infra/docker/docker-compose.prod.yml`을 추가해 base Compose와 Overlay 방식으로 사용한다.
   - prod override에는 `backend`, `mysql`, `nginx-proxy-manager` 서비스에 `profiles: ["prod"]`를 지정하고, 로컬과 다른 포트/볼륨/이미지 정책을 정의한다.
2. **백엔드 배포 방식**
   - `backend` 서비스는 `build` 대신 `image: ${BACKEND_IMAGE}:${BACKEND_TAG}`를 사용한다.
   - 환경변수 `SPRING_PROFILES_ACTIVE=prod`, `SERVER_PORT=9000` 등을 `.env.prod`에서 주입해 운영 구성을 강제한다.
3. **데이터베이스 & 네트워크**
   - 운영에서도 Compose 기반 MySQL을 사용하되, `MYSQL_DATA_DIR=/data/mysql` 같은 호스트 경로를 volume으로 고정해 재부팅 시 데이터를 유지한다.
   - 외부 노출 포트는 `MYSQL_PUBLIC_PORT` 환경 변수로 제어하며, 필요 시 3306 노출을 차단할 수 있게 한다(override에서 `ports` 삭제 허용).
4. **HTTPS Reverse Proxy**
   - `nginx-proxy-manager`는 운영 도메인(`PUBLIC_DOMAIN`)과 이메일(`PUBLIC_CONTACT_EMAIL`)을 `.env.prod`로부터 읽어 자동화된 SSL 발급을 지원한다.
   - 80/443은 고정 노출하고, 81 포트는 SSH 터널 혹은 보안 그룹으로 보호하도록 가이드를 명시한다.
5. **환경 변수 템플릿**
   - `infra/docker/.env.prod.example`을 추가해 운영에 필요한 모든 키(예: `DB_PASSWORD`, `JWT_SECRET_KEY`, `BACKEND_IMAGE`, `BACKEND_TAG`, `REFRESH_COOKIE_DOMAIN`, `NPM_ADMIN_EMAIL/PASSWORD`)를 placeholder로 나열한다.
   - `.env.prod`는 gitignore 되고, README에 복사 절차를 문서화한다.

### Non-functional
- override 파일은 base Compose와 같은 버전(3.9)과 네이밍 컨벤션을 유지해 `docker compose config` 시 경고가 없어야 한다.
- 민감한 값은 모두 `.env.prod`에서만 관리하며, 예시 파일에는 절대로 실제 비밀번호를 넣지 않는다.
- 문서/주석으로 운영자에게 필요한 단계(이미지 푸시 → Compose up → 초기 관리자 설정)를 안내한다.

## 3. API Design (Draft)
- `docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod config` : 운영 config 검증 명령, CI에서도 사용.
- `docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --remove-orphans` : EC2 배포 명령.
- `docker compose --profile prod logs -f backend` / `docker compose --profile prod ps` : 운영 로그/상태 확인 명령을 README에 포함한다.

## 4. Domain Model (Draft)
- **Compose Base vs Prod Override**: base는 공용 서비스 정의, prod override는 `image`, `profiles`, `volumes`, `ports`, `environment` 차이를 덮어쓰며 `classhub-net` 네트워크는 공유한다.
- **Env Files**: `backend/.env`는 애플리케이션 내부 설정, `infra/docker/.env.prod`는 컨테이너 오케스트레이션용 변수로 분리된다.
- **Artifacts**: 백엔드 Docker image는 `BACKEND_IMAGE/BACKEND_TAG`로 식별되며, Compose prod override는 해당 이미지를 참조해 바로 배포한다.

## 5. TDD Plan
1. `.env.prod.example` 작성 후 `cp`해도 빠진 키가 없는지 `docker compose config` 로 검증한다.
2. GitHub Actions(또는 로컬)에서 `docker compose --env-file .env.prod.example config`를 dry-run 하여 syntax를 CI에서 검사한다.
3. EC2 테스트 인스턴스에서 `docker compose ... up -d` 실행 → `curl https://<PUBLIC_DOMAIN>/api/v1/actuator/health` 로 HTTPS 응답 확인.
4. Nginx Proxy Manager UI(81 포트) 접속 후 도메인 프록시 자동화가 동작하는지 확인하고, 설정 스크린샷/설명 추가.
5. README/infra 문서를 따라 신규 운영자가 그대로 실행해 동일 결과를 얻을 수 있는지 peer review 받는다.
