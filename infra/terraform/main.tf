terraform {
  # 필요한 Provider 정의 (현재 AWS만 사용)
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  # AWS Provider 기본 설정
  region = var.region

  default_tags {
    tags = {
      Project = var.prefix
    }
  }
}

data "aws_ami" "amazon_linux" {
  # Amazon Linux 2023 최신 AMI 조회
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_vpc" "main" {
  # 서비스 전용 VPC
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.prefix}-vpc"
  }
}

resource "aws_subnet" "public" {
  # 공용 서브넷 (EC2 + ELB 용)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidr
  availability_zone       = var.availability_zone
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-public-subnet"
  }
}

resource "aws_internet_gateway" "main" {
  # VPC에 연결되는 IGW
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.prefix}-igw"
  }
}

resource "aws_route_table" "public" {
  # 공용 라우팅 테이블
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.prefix}-public-rt"
  }
}

resource "aws_route_table_association" "public_assoc" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "app" {
  # 애플리케이션 기본 보안 그룹
  name        = "${var.prefix}-sg"
  description = "ClassHub application security group"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = var.allowed_ssh_cidrs
  }

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = var.allowed_http_cidrs
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = var.allowed_http_cidrs
  }

  ingress {
    description = "Nginx Proxy Manager"
    from_port   = 81
    to_port     = 81
    protocol    = "tcp"
    cidr_blocks = var.allowed_admin_cidrs
  }

  ingress {
    description = "Backend App"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = var.allowed_http_cidrs
  }

  ingress {
    description = "MySQL internal"
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  ingress {
    description = "Redis internal"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.prefix}-sg"
  }
}

resource "aws_iam_role" "ec2" {
  # EC2가 SSM 등에 접근할 수 있는 역할
  name = "${var.prefix}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

resource "aws_iam_instance_profile" "ec2" {
  # EC2 인스턴스 프로파일
  name = "${var.prefix}-instance-profile"
  role = aws_iam_role.ec2.name
}

locals {
  # EC2 User Data
  # 1) 시스템 업데이트
  # 2) Docker 설치 및 서비스 등록
  # 3) Docker Compose 바이너리 설치
  # 4) Git 설치
  # 5) Compose 배포용 디렉터리 생성
  user_data = <<-EOF
    #!/bin/bash
    set -e
    yum update -y

    if ! command -v docker &>/dev/null; then
      amazon-linux-extras install docker -y || yum install docker -y
    fi

    systemctl enable docker
    systemctl start docker

    curl -L "https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

    yum install -y git

    mkdir -p ${var.remote_app_path}
    chown ec2-user:ec2-user ${var.remote_app_path}
  EOF
}

resource "aws_instance" "app" {
  # 실제 애플리케이션이 배포될 EC2
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.app.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.ec2.name
  key_name                    = var.key_pair_name
  user_data                   = local.user_data

  root_block_device {
    volume_type = "gp3"
    volume_size = 40
  }

  tags = {
    Name = "${var.prefix}-app"
  }
}

resource "aws_eip" "app" {
  # 고정 공인 IP (Elastic IP)
  domain   = "vpc"
  instance = aws_instance.app.id

  tags = {
    Name = "${var.prefix}-eip"
  }
}

output "ec2_public_ip" {
  value = aws_eip.app.public_ip
}

output "ssh_username" {
  value = var.ssh_username
}

output "ssh_port" {
  value = var.ssh_port
}

output "remote_app_path" {
  value = var.remote_app_path
}

output "security_group_id" {
  value = aws_security_group.app.id
}

output "instance_id" {
  value = aws_instance.app.id
}
