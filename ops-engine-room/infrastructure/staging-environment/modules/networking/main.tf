# ──────────────────────────────────────────────────────────────────────────────
# Networking Module – Main
# Wraps: terraform-aws-modules/vpc/aws
# ──────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# ──────────────────────────────────────────────────────────────────────────────
# VPC (Public Registry Wrapper)
# ──────────────────────────────────────────────────────────────────────────────

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${local.name_prefix}-vpc"
  cidr = var.vpc_cidr

  azs             = var.availability_zones
  public_subnets  = var.public_subnet_cidrs
  private_subnets = var.private_subnet_cidrs

  # ── Hardcoded Organizational Defaults ──
  enable_nat_gateway     = true
  single_nat_gateway     = true # Cost-optimized for staging
  one_nat_gateway_per_az = false
  enable_dns_hostnames   = true
  enable_dns_support     = true

  # ── Subnet Tagging ──
  public_subnet_tags = {
    Tier = "Public"
  }

  private_subnet_tags = {
    Tier = "Private"
  }

  tags = merge(var.tags, {
    Module = "networking"
  })
}

# ──────────────────────────────────────────────────────────────────────────────
# VPC Endpoints
# ──────────────────────────────────────────────────────────────────────────────

# S3 Gateway Endpoint (free, routed via AWS backbone)
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = module.vpc.vpc_id
  service_name      = "com.amazonaws.${data.aws_region.current.name}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = module.vpc.private_route_table_ids

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-vpce-s3"
    Module = "networking"
  })
}

# CloudWatch Logs Interface Endpoint (avoids NAT costs for logging)
resource "aws_vpc_endpoint" "cloudwatch_logs" {
  vpc_id              = module.vpc.vpc_id
  service_name        = "com.amazonaws.${data.aws_region.current.name}.logs"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-vpce-cloudwatch-logs"
    Module = "networking"
  })
}

# ──────────────────────────────────────────────────────────────────────────────
# Security Group for VPC Interface Endpoints
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_security_group" "vpc_endpoints" {
  name_prefix = "${local.name_prefix}-vpce-"
  description = "Security group for VPC Interface Endpoints"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "HTTPS from VPC CIDR"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-vpce-sg"
    Module = "networking"
  })

  lifecycle {
    create_before_destroy = true
  }
}

# ──────────────────────────────────────────────────────────────────────────────
# Data Sources
# ──────────────────────────────────────────────────────────────────────────────

data "aws_region" "current" {}
