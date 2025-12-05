variable "prefix" {
  description = "Prefix for tagging AWS resources"
  type        = string
  default     = "classhub"
}

variable "region" {
  description = "AWS region to deploy infrastructure"
  type        = string
  default     = "ap-northeast-2"
}

variable "availability_zone" {
  description = "Availability zone for the public subnet"
  type        = string
  default     = "ap-northeast-2a"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.20.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet"
  type        = string
  default     = "10.20.10.0/24"
}

variable "instance_type" {
  description = "EC2 instance type (use t2.micro for dev, t3.small+ for prod)"
  type        = string
  default     = "t3.small"
}

variable "key_pair_name" {
  description = "Name of an existing AWS key pair for SSH access"
  type        = string
  default     = "classhub-prod-key"
}

variable "remote_app_path" {
  description = "Directory on the EC2 instance where Docker Compose files will live"
  type        = string
  default     = "/opt/classhub"
}

variable "allowed_ssh_cidrs" {
  description = "CIDR blocks allowed to SSH"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "allowed_http_cidrs" {
  description = "CIDR blocks allowed to hit HTTP/HTTPS/backend ports"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "allowed_admin_cidrs" {
  description = "CIDR blocks allowed to access admin-only ports (e.g., NPM UI)"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "ssh_username" {
  description = "Default SSH username for the EC2 AMI"
  type        = string
  default     = "ec2-user"
}

variable "ssh_port" {
  description = "SSH port"
  type        = number
  default     = 22
}
