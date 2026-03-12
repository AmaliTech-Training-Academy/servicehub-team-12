# ──────────────────────────────────────────────────────────────────────────────
# Airflow Instance Compute Module – Variables
# ──────────────────────────────────────────────────────────────────────────────

variable "project_name" {
  description = "The name of the project, used for resource naming."
  type        = string
}

variable "environment" {
  description = "The deployment environment (e.g., staging, production)."
  type        = string
}

variable "instance_type" {
  description = "The EC2 instance type for the Airflow Docker host."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9]*\\.[a-z0-9]+$", var.instance_type))
    error_message = "instance_type must be a valid EC2 instance type (e.g., t3.medium)."
  }
}

variable "subnet_id" {
  description = "The private subnet ID where the EC2 instance will be launched."
  type        = string
}

variable "security_group_ids" {
  description = "List of security group IDs to associate with the EC2 instance."
  type        = list(string)
}

variable "instance_profile_name" {
  description = "The name of the IAM instance profile to attach to the EC2 instance."
  type        = string
}

variable "ssh_public_key" {
  description = "The SSH public key material for the key pair."
  type        = string
}

variable "s3_data_engineering_bucket" {
  description = "The name of the S3 bucket containing data engineering deployment artifacts."
  type        = string
}

variable "db_host" {
  description = "The hostname of the RDS database for Airflow metadata."
  type        = string
}

variable "db_name" {
  description = "The database name for Airflow metadata."
  type        = string
}

variable "db_username" {
  description = "The database username for Airflow."
  type        = string
  sensitive   = true
}

variable "secret_arn" {
  description = "ARN of the Secrets Manager secret containing DB credentials."
  type        = string
}

variable "tags" {
  description = "A map of tags to apply to all resources in this module."
  type        = map(string)
  default     = {}
}
