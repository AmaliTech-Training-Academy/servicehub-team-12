# ──────────────────────────────────────────────────────────────────────────────
# Database Module – Main
# Wraps: terraform-aws-modules/rds/aws
# ──────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# ──────────────────────────────────────────────────────────────────────────────
# RDS PostgreSQL Instance
# ──────────────────────────────────────────────────────────────────────────────

module "rds" {
  source  = "terraform-aws-modules/rds/aws"
  version = "~> 6.0"

  identifier = "${local.name_prefix}-postgres"

  # ── Engine Configuration ──
  engine               = "postgres"
  engine_version       = "16"
  family               = "postgres16"
  major_engine_version = "16"
  instance_class       = var.db_instance_class

  # ── Storage ──
  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_allocated_storage * 2
  storage_type          = "gp3"
  storage_encrypted     = true # Secure default: encryption at rest

  # ── Database ──
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  port     = 5432

  # ── Network & Security ──
  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = var.security_group_ids
  publicly_accessible    = false # Secure default: no public access
  multi_az               = false # Cost-optimized for staging

  # ── Backup & Maintenance ──
  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "Sun:04:00-Sun:05:00"
  skip_final_snapshot     = true
  deletion_protection     = false # Staging only; set true for production

  # ── Monitoring ──
  performance_insights_enabled    = true
  create_cloudwatch_log_group     = true
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  # ── Parameter Group Overrides ──
  parameters = [
    {
      name  = "log_connections"
      value = "1"
    },
    {
      name  = "log_disconnections"
      value = "1"
    },
    {
      name  = "log_min_duration_statement"
      value = "1000"
    }
  ]

  # ── Managed Master User Password disabled (we manage it via Secrets Manager) ──
  manage_master_user_password = false

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-postgres"
    Module = "database"
  })
}

# ──────────────────────────────────────────────────────────────────────────────
# DB Subnet Group
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_db_subnet_group" "this" {
  name        = "${local.name_prefix}-db-subnet-group"
  description = "Database subnet group for ${local.name_prefix}"
  subnet_ids  = var.subnet_ids

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-db-subnet-group"
    Module = "database"
  })
}
