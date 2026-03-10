# ──────────────────────────────────────────────────────────────────────────────
# Database Module – Variables
# ──────────────────────────────────────────────────────────────────────────────

variable "project_name" {
  description = "The name of the project, used for resource naming."
  type        = string
}

variable "environment" {
  description = "The deployment environment (e.g., staging, production)."
  type        = string
}

variable "db_name" {
  description = "The name of the default database to create."
  type        = string

  validation {
    condition     = can(regex("^[a-zA-Z_][a-zA-Z0-9_]*$", var.db_name))
    error_message = "db_name must be a valid PostgreSQL database identifier."
  }
}

variable "db_username" {
  description = "The master username for the RDS instance."
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "The master password for the RDS instance."
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "The instance class for the RDS instance (e.g., db.t3.micro)."
  type        = string

  validation {
    condition     = can(regex("^db\\.", var.db_instance_class))
    error_message = "db_instance_class must start with 'db.' prefix."
  }
}

variable "db_allocated_storage" {
  description = "The allocated storage size in GB for the RDS instance."
  type        = number

  validation {
    condition     = var.db_allocated_storage >= 20 && var.db_allocated_storage <= 1000
    error_message = "db_allocated_storage must be between 20 and 1000 GB."
  }
}

variable "subnet_ids" {
  description = "List of private subnet IDs for the DB subnet group."
  type        = list(string)
}

variable "security_group_ids" {
  description = "List of security group IDs to associate with the RDS instance."
  type        = list(string)
}

variable "tags" {
  description = "A map of tags to apply to all resources in this module."
  type        = map(string)
  default     = {}
}
