# ──────────────────────────────────────────────────────────────────────────────
# Secrets Manager Module – Outputs
# ──────────────────────────────────────────────────────────────────────────────

output "secret_arn" {
  description = "The ARN of the Secrets Manager secret containing RDS credentials."
  value       = aws_secretsmanager_secret.rds_credentials.arn
}

output "db_password" {
  description = "The generated database password."
  value       = random_password.db_password.result
  sensitive   = true
}
