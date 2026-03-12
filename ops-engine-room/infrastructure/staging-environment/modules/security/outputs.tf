# ──────────────────────────────────────────────────────────────────────────────
# Security Module – Outputs
# ──────────────────────────────────────────────────────────────────────────────

output "alb_security_group_id" {
  description = "The ID of the ALB security group."
  value       = aws_security_group.alb.id
}

output "ec2_airflow_security_group_id" {
  description = "The ID of the EC2 Airflow Docker host security group."
  value       = aws_security_group.ec2_airflow.id
}

output "rds_security_group_id" {
  description = "The ID of the RDS security group."
  value       = aws_security_group.rds.id
}

output "app_runner_connector_security_group_id" {
  description = "The ID of the App Runner VPC Connector security group."
  value       = aws_security_group.app_runner_connector.id
}
