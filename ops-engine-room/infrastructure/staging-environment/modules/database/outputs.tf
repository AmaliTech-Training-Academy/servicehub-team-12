# ──────────────────────────────────────────────────────────────────────────────
# Database Module – Outputs
# ──────────────────────────────────────────────────────────────────────────────

output "db_endpoint" {
  description = "The connection endpoint for the RDS instance."
  value       = module.rds.db_instance_endpoint
}

output "db_address" {
  description = "The hostname of the RDS instance."
  value       = module.rds.db_instance_address
}

output "db_port" {
  description = "The port of the RDS instance."
  value       = module.rds.db_instance_port
}

output "db_name" {
  description = "The name of the default database."
  value       = module.rds.db_instance_name
}

output "db_instance_id" {
  description = "The identifier of the RDS instance."
  value       = module.rds.db_instance_identifier
}
