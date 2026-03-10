# ──────────────────────────────────────────────────────────────────────────────
# Security Module – Main
# Creates all Security Groups for the infrastructure.
# ──────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# ──────────────────────────────────────────────────────────────────────────────
# ALB Security Group
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "alb" {
  name_prefix = "${local.name_prefix}-alb-"
  description = "Security group for the Airflow Application Load Balancer"
  vpc_id      = var.vpc_id

  ingress {
    description = "HTTPS from the internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP from the internet (redirect to HTTPS)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-alb-sg"
    Module = "security"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# ──────────────────────────────────────────────────────────────────────────────
# EC2 / Airflow Docker Host Security Group
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "ec2_airflow" {
  name_prefix = "${local.name_prefix}-ec2-airflow-"
  description = "Security group for the Airflow EC2 Docker host in private subnet"
  vpc_id      = var.vpc_id

  # Airflow web UI port from ALB
  ingress {
    description     = "HTTP from ALB on port 8080"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # SSH from within VPC (bastion or SSM)
  ingress {
    description = "SSH from within VPC"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-ec2-airflow-sg"
    Module = "security"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# ──────────────────────────────────────────────────────────────────────────────
# RDS Security Group
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "rds" {
  name_prefix = "${local.name_prefix}-rds-"
  description = "Security group for the RDS PostgreSQL instance"
  vpc_id      = var.vpc_id

  # PostgreSQL from EC2/Airflow
  ingress {
    description     = "PostgreSQL from Airflow EC2"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_airflow.id]
  }

  # PostgreSQL from App Runner VPC Connector
  ingress {
    description     = "PostgreSQL from App Runner VPC Connector"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app_runner_connector.id]
  }

  egress {
    description = "Allow all outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-rds-sg"
    Module = "security"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# ──────────────────────────────────────────────────────────────────────────────
# App Runner VPC Connector Security Group
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "app_runner_connector" {
  name_prefix = "${local.name_prefix}-apprunner-vpc-"
  description = "Security group for the App Runner VPC Connector"
  vpc_id      = var.vpc_id

  # Outbound to RDS
  egress {
    description = "PostgreSQL to RDS"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  # Outbound HTTPS (for AWS APIs via VPC endpoints)
  egress {
    description = "HTTPS for AWS service access"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-apprunner-vpc-sg"
    Module = "security"
  })

  lifecycle {
    create_before_destroy = true
  }
}
