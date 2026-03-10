# ──────────────────────────────────────────────────────────────────────────────
# Staging Environment – Root Outputs
# ──────────────────────────────────────────────────────────────────────────────

# ── Networking ───────────────────────────────────────────────────────────────

output "vpc_id" {
  description = "The ID of the VPC."
  value       = module.networking.vpc_id
}

output "public_subnet_ids" {
  description = "List of public subnet IDs."
  value       = module.networking.public_subnet_ids
}

output "private_subnet_ids" {
  description = "List of private subnet IDs."
  value       = module.networking.private_subnet_ids
}

# ── Database ─────────────────────────────────────────────────────────────────

output "rds_endpoint" {
  description = "The connection endpoint for the RDS PostgreSQL instance."
  value       = module.database.db_endpoint
}

output "rds_address" {
  description = "The hostname of the RDS instance."
  value       = module.database.db_address
}

# ── App Runner ───────────────────────────────────────────────────────────────

output "app_runner_service_url" {
  description = "The URL of the App Runner backend service."
  value       = module.app_runner.service_url
}

output "app_runner_service_arn" {
  description = "The ARN of the App Runner service."
  value       = module.app_runner.service_arn
}

# ── ECR ──────────────────────────────────────────────────────────────────────

output "ecr_repository_url" {
  description = "The URL of the ECR repository."
  value       = module.ecr.repository_url
}

# ── Airflow Compute ──────────────────────────────────────────────────────────

output "airflow_instance_id" {
  description = "The ID of the Airflow EC2 instance."
  value       = module.airflow_compute.instance_id
}

output "airflow_private_ip" {
  description = "The private IP of the Airflow EC2 instance."
  value       = module.airflow_compute.private_ip
}

# ── CI/CD ────────────────────────────────────────────────────────────────────

output "github_actions_role_arn" {
  description = "The ARN of the IAM role for GitHub Actions OIDC."
  value       = module.ci_cd.github_actions_role_arn
}

output "s3_dags_bucket_name" {
  description = "The name of the S3 bucket storing Airflow DAGs."
  value       = module.ci_cd.s3_dags_bucket_name
}

# ── Secrets Manager ──────────────────────────────────────────────────────────

output "secrets_manager_secret_arn" {
  description = "The ARN of the Secrets Manager secret containing RDS credentials."
  value       = module.secrets_manager.secret_arn
}
