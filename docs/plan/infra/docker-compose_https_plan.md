# Feature: Docker Compose 기반 HTTPS Reverse Proxy 환경

## 1. Problem Definition

- 개발자들이 `./gradlew bootRun` 대신 Docker Compose로 백엔드/DB/Redis/Nginx Proxy Manager를 한 번에 띄우고, HTTPS 환경을 테스트할 수 있어야 한다.
- 프런트는 Vercel 배포 전까지도 HTTPS 기반 API 호출이 필요하므로, 로컬에서 Reverse Proxy + 인증서를 갖춘 환경을 구성해야 한다.

## 2. Requirements

### Functional

1. **서비스 구성**
   - `backend` Spring Boot 컨테이너 (포트 8080 내부) → JAR 빌드 후 `openjdk:21` 이미지 사용.
   - `mysql` 컨테이너: 개발용 데이터베이스 설정 (volume, env, 버전은 백엔드 코드와 동일한 8.4.5 사용).
   - `nginx-proxy-manager` 컨테이너: HTTPS 인증서(로컬 self-signed 또는 Let’s Encrypt) + Reverse Proxy 설정.
2. **네트워크/도메인**
   - 모든 컨테이너를 `classhub-net` 같은 custom network에 연결.
   - Nginx Proxy Manager가 `https://local.classhub.dev` → backend 컨테이너로 프록시.
3. **환경 변수**
   - 백엔드 `.env` (DB, JWT, refresh cookie 설정)와 Docker Compose `.env` 분리.
   - MySQL/Redis 비밀번호는 `.env` 파일에 정의하고 Compose에서 참조.
4. **빌드/실행**
   - `docker-compose --profile local up -d`로 전체 환경을 구동.
   - 백엔드 이미지는 Compose 실행 시 자동 빌드(`depends_on: mysql, redis` 후 healthy check).

### Non-functional

- HTTPS 인증서는 로컬 self-signed라도 가능하지만, Proxy Manager 설정 방법을 문서에 명시.
- MySQL/Redis 데이터는 volume으로 persist.
- 로그 및 포트 사용 정보를 README에 정리.

## 3. Deployment Plan (Draft)

1. `Dockerfile` 작성: `./gradlew bootJar` → `openjdk:21-jdk` base image.
2. `docker-compose.yml`에 4개의 서비스 정의. `backend`는 build context, `mysql`은 3306, `redis`는 6379, `nginx-proxy-manager`는 81/443.
3. Nginx Proxy Manager UI에서 `local.classhub.dev` → `backend:8080` HTTPS 프록시 설정 (혹은 자동 설정을 위한 init script).
4. 프런트 dev 환경에서 API Base URL을 `https://local.classhub.dev/api/v1`로 맞추고 `credentials: include` 유지.

## 4. Test Plan

1. **Docker Compose Smoke Test**
   - `docker-compose up -d` 후 `docker ps`로 4개 컨테이너가 모두 healthy인지 확인.
   - `curl https://local.classhub.dev/api/v1/actuator/health` 실행해 HTTPS 응답 확인.
2. **Database & Redis Check**
   - MySQL 컨테이너에 접속해 schema와 초기 계정이 생성됐는지 확인.
   - Redis CLI로 ping test.
3. **Reverse Proxy Validation**
   - 브라우저에서 HTTPS URL로 백엔드 API가 정상 응답하고, Set-Cookie (refreshToken)가 저장되는지 확인.
4. **Documentation**
   - README(또는 docs/infra/guide.md)에 실행/정지/로그 확인/초기 사용자 설정 방법을 기록.
