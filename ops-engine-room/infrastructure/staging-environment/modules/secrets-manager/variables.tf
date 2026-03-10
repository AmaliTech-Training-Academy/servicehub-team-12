# ──────────────────────────────────────────────────────────────────────────────
# Secrets Manager Module – Variables
# ──────────────────────────────────────────────────────────────────────────────

variable "project_name" {
  description = "The name of the project, used for resource naming."
  type        = string
}

variable "environment" {
  description = "The deployment environment (e.g., staging, production)."
  type        = string
}

variable "db_username" {
  description = "The master username for the RDS database."
  type        = string
}

variable "db_name" {
  description = "The name of the default database."
  type        = string
}

variable "db_host" {
  description = "The hostname/endpoint of the RDS database."
  type        = string
  default     = "pending"
}

variable "db_port" {
  description = "The port of the RDS database."
  type        = number
  default     = 5432
}

variable "tags" {
  description = "A map of tags to apply to all resources in this module."
  type        = map(string)
  default     = {}
}
