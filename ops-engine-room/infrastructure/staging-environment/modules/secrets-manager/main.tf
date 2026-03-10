# ──────────────────────────────────────────────────────────────────────────────
# Secrets Manager Module – Main
# Generates a random password and stores RDS credentials securely.
# ──────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# ── Generate a Secure Random Password ──
resource "random_password" "db_password" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

# ── Store Credentials in AWS Secrets Manager ──
resource "aws_secretsmanager_secret" "rds_credentials" {
  name                    = "${local.name_prefix}/rds/credentials"
  description             = "RDS PostgreSQL credentials for ${local.name_prefix}"
  recovery_window_in_days = 7

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-rds-credentials"
    Module = "secrets-manager"
  })
}

resource "aws_secretsmanager_secret_version" "rds_credentials" {
  secret_id = aws_secretsmanager_secret.rds_credentials.id

  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db_password.result
    engine   = "postgres"
    host     = var.db_host
    port     = var.db_port
    dbname   = var.db_name
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}
