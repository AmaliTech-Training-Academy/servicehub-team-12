# ──────────────────────────────────────────────────────────────────────────────
# App Runner Module – Variables
# ──────────────────────────────────────────────────────────────────────────────

variable "project_name" {
  description = "The name of the project, used for resource naming."
  type        = string
}

variable "environment" {
  description = "The deployment environment (e.g., staging, production)."
  type        = string
}

variable "ecr_repository_url" {
  description = "The URL of the ECR repository containing the backend image."
  type        = string
}

variable "cpu" {
  description = "CPU units for the App Runner service (e.g., 1024 = 1 vCPU)."
  type        = string

  validation {
    condition     = contains(["256", "512", "1024", "2048", "4096"], var.cpu)
    error_message = "cpu must be one of: 256, 512, 1024, 2048, 4096."
  }
}

variable "memory" {
  description = "Memory in MB for the App Runner service."
  type        = string

  validation {
    condition     = contains(["512", "1024", "2048", "3072", "4096", "6144", "8192", "10240", "12288"], var.memory)
    error_message = "memory must be a valid App Runner memory configuration."
  }
}

variable "port" {
  description = "The port the application listens on."
  type        = string
  default     = "8080"
}

variable "subnet_ids" {
  description = "List of private subnet IDs for the VPC connector."
  type        = list(string)
}

variable "security_group_ids" {
  description = "List of security group IDs for the VPC connector."
  type        = list(string)
}

variable "instance_role_arn" {
  description = "ARN of the IAM role for the App Runner instance."
  type        = string
}

variable "access_role_arn" {
  description = "ARN of the IAM role for App Runner to access ECR."
  type        = string
}

variable "secret_arn" {
  description = "ARN of the Secrets Manager secret for DB password."
  type        = string
}

variable "environment_variables" {
  description = "Map of environment variables to set on the App Runner service."
  type        = map(string)
  default     = {}
}

variable "tags" {
  description = "A map of tags to apply to all resources in this module."
  type        = map(string)
  default     = {}
}
