# ──────────────────────────────────────────────────────────────────────────────
# CI/CD Module – Outputs
# ──────────────────────────────────────────────────────────────────────────────

output "github_actions_role_arn" {
  description = "The ARN of the IAM role for GitHub Actions."
  value       = aws_iam_role.github_actions.arn
}

output "s3_dags_bucket_name" {
  description = "The name of the S3 bucket storing Airflow DAGs."
  value       = aws_s3_bucket.dags.id
}

output "s3_dags_bucket_arn" {
  description = "The ARN of the S3 bucket storing Airflow DAGs."
  value       = aws_s3_bucket.dags.arn
}

output "oidc_provider_arn" {
  description = "The ARN of the GitHub Actions OIDC provider."
  value       = data.aws_iam_openid_connect_provider.github.arn
}
