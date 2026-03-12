# ──────────────────────────────────────────────────────────────────────────────
# ECR Module – Variables
# ──────────────────────────────────────────────────────────────────────────────

variable "repository_name" {
  description = "The name of the ECR repository."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.repository_name))
    error_message = "repository_name must contain only lowercase alphanumeric characters and hyphens."
  }
}

variable "environment" {
  description = "The deployment environment (e.g., staging, production)."
  type        = string
}

variable "image_tag_mutability" {
  description = "The tag mutability setting for the repository. Must be MUTABLE or IMMUTABLE."
  type        = string
  default     = "IMMUTABLE"

  validation {
    condition     = contains(["MUTABLE", "IMMUTABLE"], var.image_tag_mutability)
    error_message = "image_tag_mutability must be either MUTABLE or IMMUTABLE."
  }
}

variable "max_untagged_image_count" {
  description = "Maximum number of untagged images to retain in the lifecycle policy."
  type        = number
  default     = 10
}

variable "tags" {
  description = "A map of tags to apply to all resources in this module."
  type        = map(string)
  default     = {}
}
