# ──────────────────────────────────────────────────────────────────────────────
# IAM Module – Outputs
# ──────────────────────────────────────────────────────────────────────────────

output "ec2_airflow_role_arn" {
  description = "ARN of the EC2 Airflow instance role."
  value       = aws_iam_role.ec2_airflow.arn
}

output "ec2_airflow_instance_profile_name" {
  description = "Name of the EC2 Airflow instance profile."
  value       = aws_iam_instance_profile.ec2_airflow.name
}

output "ec2_airflow_instance_profile_arn" {
  description = "ARN of the EC2 Airflow instance profile."
  value       = aws_iam_instance_profile.ec2_airflow.arn
}

output "app_runner_instance_role_arn" {
  description = "ARN of the App Runner instance role."
  value       = aws_iam_role.app_runner_instance.arn
}

output "app_runner_ecr_access_role_arn" {
  description = "ARN of the App Runner ECR access role."
  value       = aws_iam_role.app_runner_ecr_access.arn
}
