# ──────────────────────────────────────────────────────────────────────────────
# Airflow Instance Compute Module – Outputs
# ──────────────────────────────────────────────────────────────────────────────

output "instance_id" {
  description = "The ID of the Airflow EC2 instance."
  value       = aws_instance.airflow.id
}

output "private_ip" {
  description = "The private IP address of the Airflow EC2 instance."
  value       = aws_instance.airflow.private_ip
}

output "key_pair_name" {
  description = "The name of the SSH key pair."
  value       = aws_key_pair.airflow.key_name
}
