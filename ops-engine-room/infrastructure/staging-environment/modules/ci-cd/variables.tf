# ──────────────────────────────────────────────────────────────────────────────
# CI/CD Module – Variables
# ──────────────────────────────────────────────────────────────────────────────

variable "project_name" {
  description = "The name of the project, used for resource naming."
  type        = string
}

variable "environment" {
  description = "The deployment environment (e.g., staging, production)."
  type        = string
}

variable "github_org" {
  description = "The GitHub organization or user owning the repository."
  type        = string

  validation {
    condition     = length(var.github_org) > 0
    error_message = "github_org must not be empty."
  }
}

variable "github_repo" {
  description = "The GitHub repository name (without org prefix)."
  type        = string

  validation {
    condition     = length(var.github_repo) > 0
    error_message = "github_repo must not be empty."
  }
}

variable "ecr_repository_arn" {
  description = "ARN of the ECR repository that GitHub Actions can push to."
  type        = string
}

variable "tags" {
  description = "A map of tags to apply to all resources in this module."
  type        = map(string)
  default     = {}
}
