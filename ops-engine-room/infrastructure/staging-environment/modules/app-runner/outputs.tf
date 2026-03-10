# ──────────────────────────────────────────────────────────────────────────────
# App Runner Module – Outputs
# ──────────────────────────────────────────────────────────────────────────────

output "service_url" {
  description = "The URL of the App Runner service."
  value       = aws_apprunner_service.this.service_url
}

output "service_arn" {
  description = "The ARN of the App Runner service."
  value       = aws_apprunner_service.this.arn
}

output "service_id" {
  description = "The ID of the App Runner service."
  value       = aws_apprunner_service.this.service_id
}

output "vpc_connector_arn" {
  description = "The ARN of the VPC connector."
  value       = aws_apprunner_vpc_connector.this.arn
}
