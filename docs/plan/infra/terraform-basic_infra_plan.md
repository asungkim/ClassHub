# Feature: Terraform 기반 AWS 네트워크/EC2 기본 리소스

## 1. Problem Definition

- Docker Compose prod/CD 파이프라인을 실제 서버에 적용할 AWS 인프라가 없다.
- 수동으로 EC2/VPC를 생성하면 설정이 일관되지 않아 재현성이 떨어지고, SSH/보안그룹/도메인 등의 구성이 팀 내에서 공유되지 않는다.
- Terraform으로 최소 구성(VPC, 서브넷, 보안 그룹, EC2, 필요한 IAM/키)을 선언해 인프라를 코드로 관리해야 한다.

## 2. Requirements

### Functional

1. **VPC & Subnet**
   - `/16` VPC(예: `10.20.0.0/16`)와 공용 서브넷 `/24` 1개를 생성한다.
   - 인터넷 게이트웨이 + 라우팅으로 공용 서브넷의 인스턴스가 인터넷에 접근할 수 있어야 한다.
2. **Security Group**
   - SSH(22), HTTP(80), HTTPS(443), NPM 대시보드(81), 백엔드 포트(8080), MySQL(3306 내부), Docker Compose 필요 포트만 허용.
   - 기본은 0.0.0.0/0로 두되, Terraform 변수로 허용 CIDR을 조정할 수 있게 한다.
3. **EC2**
   - Amazon Linux 2023, t3.small (변수화), 루트 디스크 40GB.
   - User Data로 Docker/Docker Compose 설치 + `/opt/classhub` 디렉터리 생성.
   - SSH Key Pair를 Terraform에서 생성하거나 기존 키 이름을 변수화.
4. **IAM**
   - EC2용 최소 권한 역할 (SSM, CloudWatch Logs 정도) 생성. 필요 시 S3 접근 정책 추가.
5. **Outputs / Integration**
   - `PROD_SSH_HOST`, `PROD_SSH_PORT`, `PROD_SSH_USER`, 배포 경로(`/opt/classhub/infra/docker`) 등이 Terraform output으로 제공되어 GitHub Secrets에 그대로 입력할 수 있게 한다.
   - EC2 퍼블릭 IP 혹은 Elastic IP 선택 가능(변수).

### Non-functional

- Terraform state는 로컬에서 시작하되, 추후 S3 remote backend로 이전할 수 있도록 구조화(folders, `backend.tf` placeholder).
- 리소스 이름은 `var.prefix` 기반(`classhub-prod-...`)으로 통일.
- `terraform fmt`/`validate`가 깨지지 않도록 모듈화(단일 main.tf 첫 단계).
- User Data 스크립트는 idempotent하게 작성(존재하면 skip).

## 3. API Design (Draft)

- `terraform init` → `terraform plan -var-file=env/prod.tfvars` → `terraform apply`.
- Outputs:
  - `ec2_public_ip`
  - `ssh_username`
  - `ssh_private_key_path` (옵션)
  - `docker_compose_path`
  - `security_group_id`

## 4. Domain Model (Draft)

- **Modules**
  - `network`: vpc, subnet, igw, route table.
  - `security`: security group.
  - `compute`: ec2 instance + IAM role/profile.
  - (초기 버전에서는 하나의 main.tf에서 resource grouping만, 이후 모듈화 가능)
- **Variables**
  - `prefix`, `region`, `public_key_path`, `allowed_cidrs`, `instance_type`, `assign_eip`.
- **Outputs**
  - Compose 배포에 필요한 주소와 SSH 정보.

## 5. TDD Plan

1. `terraform validate`/`terraform fmt` CI hook으로 확인.
2. Terraform `plan`을 로컬에서 실행해 예상 리소스 확인.
3. test AWS account에 `apply`, EC2에 SSH 접속, user-data로 Docker/Compose 설치됐는지 검증.
4. `terraform destroy`로 리소스 정리 가능 여부 확인.
5. Outputs 값을 GitHub Secrets에 적용해 Backend CD를 수동 실행, `/actuator/health` 응답까지 확인.
