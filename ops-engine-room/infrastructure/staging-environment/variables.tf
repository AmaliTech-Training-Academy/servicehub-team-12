# ──────────────────────────────────────────────────────────────────────────────
# Staging Environment – Root Variables
# ──────────────────────────────────────────────────────────────────────────────

# ── General ──────────────────────────────────────────────────────────────────

variable "aws_region" {
  description = "The AWS region to deploy resources into."
  type        = string

  validation {
    condition     = can(regex("^[a-z]{2}-[a-z]+-[0-9]+$", var.aws_region))
    error_message = "aws_region must be a valid AWS region identifier (e.g., eu-west-1)."
  }
}

variable "project_name" {
  description = "The name of the project, used across all resources for naming and tagging."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.project_name))
    error_message = "project_name must contain only lowercase alphanumeric characters and hyphens."
  }
}

variable "environment" {
  description = "The deployment environment (e.g., dev, staging, production)."
  type        = string

  validation {
    condition     = contains(["dev", "staging", "production"], var.environment)
    error_message = "environment must be one of: dev, staging, production."
  }
}

# ── Networking ───────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "The CIDR block for the VPC."
  type        = string

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "vpc_cidr must be a valid CIDR block."
  }
}

variable "availability_zones" {
  description = "List of availability zones for multi-AZ deployment."
  type        = list(string)

  validation {
    condition     = length(var.availability_zones) >= 2
    error_message = "At least two availability zones are required for high availability."
  }
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (ingress/egress tier)."
  type        = list(string)

  validation {
    condition     = length(var.public_subnet_cidrs) >= 2
    error_message = "At least two public subnet CIDRs must be specified."
  }
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (compute/data tier)."
  type        = list(string)

  validation {
    condition     = length(var.private_subnet_cidrs) >= 2
    error_message = "At least two private subnet CIDRs must be specified."
  }
}

# ── Database ─────────────────────────────────────────────────────────────────

variable "db_name" {
  description = "The name of the PostgreSQL database to create."
  type        = string

  validation {
    condition     = can(regex("^[a-zA-Z_][a-zA-Z0-9_]*$", var.db_name))
    error_message = "db_name must be a valid PostgreSQL identifier."
  }
}

variable "db_username" {
  description = "The master username for the RDS PostgreSQL instance."
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "The RDS instance class (e.g., db.t3.micro)."
  type        = string

  validation {
    condition     = can(regex("^db\\.", var.db_instance_class))
    error_message = "db_instance_class must start with 'db.' prefix."
  }
}

variable "db_allocated_storage" {
  description = "The allocated storage in GB for the RDS instance."
  type        = number

  validation {
    condition     = var.db_allocated_storage >= 20 && var.db_allocated_storage <= 1000
    error_message = "db_allocated_storage must be between 20 and 1000 GB."
  }
}

# ── EC2 / Airflow ────────────────────────────────────────────────────────────

variable "ec2_instance_type" {
  description = "The EC2 instance type for the Airflow Docker host."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9]*\\.[a-z0-9]+$", var.ec2_instance_type))
    error_message = "ec2_instance_type must be a valid EC2 instance type."
  }
}

variable "ssh_public_key_path" {
  description = "Path to the SSH public key file for the Airflow EC2 key pair."
  type        = string
}

# ── App Runner ───────────────────────────────────────────────────────────────

variable "app_runner_cpu" {
  description = "CPU units for the App Runner service."
  type        = string

  validation {
    condition     = contains(["256", "512", "1024", "2048", "4096"], var.app_runner_cpu)
    error_message = "app_runner_cpu must be one of: 256, 512, 1024, 2048, 4096."
  }
}

variable "app_runner_memory" {
  description = "Memory in MB for the App Runner service."
  type        = string

  validation {
    condition     = contains(["512", "1024", "2048", "3072", "4096", "6144", "8192", "10240", "12288"], var.app_runner_memory)
    error_message = "app_runner_memory must be a valid App Runner memory setting."
  }
}

variable "app_runner_port" {
  description = "The port the Java backend listens on."
  type        = string
  default     = "8080"
}

# ── ECR ──────────────────────────────────────────────────────────────────────

variable "ecr_repository_name" {
  description = "The name of the ECR repository for the backend Docker image."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.ecr_repository_name))
    error_message = "ecr_repository_name must contain only lowercase alphanumeric characters and hyphens."
  }
}

# ── Observability ────────────────────────────────────────────────────────────

variable "log_retention_days" {
  description = "Number of days to retain CloudWatch logs."
  type        = number
  default     = 30

  validation {
    condition     = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1827, 3653], var.log_retention_days)
    error_message = "log_retention_days must be a valid CloudWatch Logs retention period."
  }
}

# ── CI/CD ────────────────────────────────────────────────────────────────────

variable "github_org" {
  description = "The GitHub organization for OIDC federation."
  type        = string
  default     = "AmaliTech-Training-Academy"
}

variable "github_repo" {
  description = "The GitHub repository name for OIDC federation."
  type        = string
  default     = "servicehub-team-12"
}

# ── Tags ─────────────────────────────────────────────────────────────────────

variable "tags" {
  description = "Additional tags to apply to all resources (merged with default tags)."
  type        = map(string)
  default     = {}
}
