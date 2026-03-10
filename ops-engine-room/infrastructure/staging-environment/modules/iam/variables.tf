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

variable "s3_dags_bucket_arn" {
  description = "ARN of the S3 bucket storing Airflow DAGs."
  type        = string
}

variable "ecr_repository_arn" {
  description = "ARN of the ECR repository for the backend image."
  type        = string
}

variable "tags" {
  description = "A map of tags to apply to all resources in this module."
  type        = map(string)
  default     = {}
}
