# ──────────────────────────────────────────────────────────────────────────────
# IAM Module – Variables
# ──────────────────────────────────────────────────────────────────────────────

variable "project_name" {
  description = "The name of the project, used for resource naming."
  type        = string
}

variable "environment" {
  description = "The deployment environment (e.g., staging, production)."
  type        = string
}

variable "s3_data_engineering_bucket_arn" {
  description = "ARN of the S3 bucket storing data engineering deployment artifacts."
  type        = string
}

variable "ecr_repository_arn" {
  description = "ARN of the ECR repository for the backend image."
  type        = string
}

variable "secret_arn" {
  description = "ARN of the Secrets Manager secret for App Runner access."
  type        = string
}

variable "tags" {
  description = "A map of tags to apply to all resources in this module."
  type        = map(string)
  default     = {}
}
