# Docker Compose HTTPS 환경 가이드

## 1. 사전 준비
1. `backend/.env` 파일을 `.env.example` 기반으로 채웁니다.
2. 로컬 실행은 `infra/docker/.env.example` → `.env` 복사 후 값을 설정합니다.
3. 운영 실행은 `infra/docker/.env.prod.example` → `.env.prod` 복사 후 `BACKEND_IMAGE/BACKEND_TAG`, DB 자격증명, 쿠키 설정을 채웁니다.
4. 로컬 개발에서 `LOCAL_DOMAIN`을 사용하려면 `/etc/hosts` 에 `127.0.0.1 local.classhub.dev` 를 추가하고, Nginx Proxy Manager에서 해당 도메인으로 프록시를 생성합니다.

## 2. 빌드 & 실행

### 로컬 (모든 서비스를 단일 머신에서 테스트)
```bash
cd infra/docker
cp .env.example .env # 최초 1회
docker compose -f docker-compose-local.yml --env-file .env up -d
```

### 운영 (EC2 등에서 prod 이미지 사용)
```bash
cd infra/docker
cp .env.prod.example .env.prod # 최초 1회
docker compose -f docker-compose-prod.yml --env-file .env.prod up -d
```

초기 실행 후 Nginx Proxy Manager는 `http://localhost:81` 에서 접속 가능합니다. 기본 로그인(`admin@example.com` / `changeme`)으로 들어가 비밀번호와 이메일을 변경한 뒤, `local.classhub.dev` → `backend:8080` Reverse Proxy를 생성하고 SSL 인증서를 추가합니다(로컬 테스트라면 Self-signed 또는 HTTP Validation 사용).

## 3. 종료 / 로그
```bash
docker compose -f docker-compose-local.yml down          # 로컬 컨테이너만 중지
docker compose -f docker-compose-local.yml down -v       # 로컬 데이터 볼륨 삭제
docker compose -f docker-compose-local.yml logs backend  # 로컬 백엔드 로그

docker compose -f docker-compose-prod.yml --env-file .env.prod down    # 운영 중지
docker compose -f docker-compose-prod.yml --env-file .env.prod logs backend
```

## 4. 프런트 연동
프런트 dev 환경에서 API Base URL을 `https://local.classhub.dev/api/v1` 로 지정하고 `fetch` 요청에 `credentials: "include"` 를 설정하면 RefreshToken 쿠키가 저장됩니다. Vercel 등 배포 환경에서도 동일한 HTTPS 도메인으로 Reverse Proxy를 맞춰주면 됩니다.
