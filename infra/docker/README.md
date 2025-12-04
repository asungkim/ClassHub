# Docker Compose HTTPS 환경 가이드

## 1. 사전 준비
1. `backend/.env` 파일을 `.env.example` 기반으로 최신 값으로 채워둡니다.
2. `infra/docker/.env.example`을 복사해 같은 디렉터리에 `.env`를 생성하고, MySQL 비밀번호/포트/도메인 값을 수정합니다.
3. 로컬 개발에서 `LOCAL_DOMAIN`을 사용하려면 `/etc/hosts` 에 `127.0.0.1 local.classhub.dev` 를 추가하고, Nginx Proxy Manager에서 해당 도메인으로 프록시를 생성합니다.

## 2. 빌드 & 실행
```bash
cd infra/docker
docker compose build backend
docker compose up -d
```

초기 실행 후 Nginx Proxy Manager는 `http://localhost:81` 에서 접속 가능합니다. 기본 로그인(`admin@example.com` / `changeme`)으로 들어가 비밀번호와 이메일을 변경한 뒤, `local.classhub.dev` → `backend:8080` Reverse Proxy를 생성하고 SSL 인증서를 추가합니다(로컬 테스트라면 Self-signed 또는 HTTP Validation 사용).

## 3. 종료 / 로그
```bash
docker compose down          # 컨테이너만 중지
docker compose down -v       # 데이터 볼륨까지 삭제
docker compose logs backend  # 백엔드 로그 확인
```

## 4. 프런트 연동
프런트 dev 환경에서 API Base URL을 `https://local.classhub.dev/api/v1` 로 지정하고 `fetch` 요청에 `credentials: "include"` 를 설정하면 RefreshToken 쿠키가 저장됩니다. Vercel 등 배포 환경에서도 동일한 HTTPS 도메인으로 Reverse Proxy를 맞춰주면 됩니다.
