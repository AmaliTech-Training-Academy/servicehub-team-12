# ──────────────────────────────────────────────────────────────────────────────
# Networking Module – Outputs
# ──────────────────────────────────────────────────────────────────────────────

output "vpc_id" {
  description = "The ID of the VPC."
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "The CIDR block of the VPC."
  value       = module.vpc.vpc_cidr_block
}

output "public_subnet_ids" {
  description = "List of public subnet IDs."
  value       = module.vpc.public_subnets
}

output "private_subnet_ids" {
  description = "List of private subnet IDs."
  value       = module.vpc.private_subnets
}

output "nat_gateway_ids" {
  description = "List of NAT Gateway IDs."
  value       = module.vpc.natgw_ids
}

output "s3_endpoint_id" {
  description = "The ID of the S3 Gateway VPC Endpoint."
  value       = aws_vpc_endpoint.s3.id
}

output "cloudwatch_logs_endpoint_id" {
  description = "The ID of the CloudWatch Logs Interface VPC Endpoint."
  value       = aws_vpc_endpoint.cloudwatch_logs.id
}

output "vpc_endpoints_security_group_id" {
  description = "The ID of the security group attached to VPC interface endpoints."
  value       = aws_security_group.vpc_endpoints.id
}

output "private_route_table_ids" {
  description = "List of private route table IDs."
  value       = module.vpc.private_route_table_ids
}
