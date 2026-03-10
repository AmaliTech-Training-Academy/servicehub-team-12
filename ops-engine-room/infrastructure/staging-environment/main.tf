# ──────────────────────────────────────────────────────────────────────────────
# Staging Environment – Root Main
# Orchestrates all modules in dependency order.
# ──────────────────────────────────────────────────────────────────────────────

locals {
  common_tags = merge(var.tags, {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  })
}

# ──────────────────────────────────────────────────────────────────────────────
# 1. Networking (VPC, Subnets, NAT, VPC Endpoints)
# ──────────────────────────────────────────────────────────────────────────────

module "networking" {
  source = "./modules/networking"

  project_name         = var.project_name
  environment          = var.environment
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  tags                 = local.common_tags
}

# ──────────────────────────────────────────────────────────────────────────────
# 2. Security (Security Groups)
# ──────────────────────────────────────────────────────────────────────────────

module "security" {
  source = "./modules/security"

  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.networking.vpc_id
  vpc_cidr     = var.vpc_cidr
  tags         = local.common_tags
}

# ──────────────────────────────────────────────────────────────────────────────
# 3. CI/CD (GitHub OIDC + S3 DAGs Bucket)
#    Placed early because IAM needs the S3 bucket ARN.
# ──────────────────────────────────────────────────────────────────────────────

module "ci_cd" {
  source = "./modules/ci-cd"

  project_name       = var.project_name
  environment        = var.environment
  github_org         = var.github_org
  github_repo        = var.github_repo
  ecr_repository_arn = module.ecr.repository_arn
  tags               = local.common_tags
}

# ──────────────────────────────────────────────────────────────────────────────
# 4. ECR (Container Registry)
# ──────────────────────────────────────────────────────────────────────────────

module "ecr" {
  source = "./modules/ecr"

  repository_name = var.ecr_repository_name
  environment     = var.environment
  tags            = local.common_tags
}

# ──────────────────────────────────────────────────────────────────────────────
# 5. IAM (Roles, Policies, Instance Profiles)
# ──────────────────────────────────────────────────────────────────────────────

module "iam" {
  source = "./modules/iam"

  project_name       = var.project_name
  environment        = var.environment
  s3_dags_bucket_arn = module.ci_cd.s3_dags_bucket_arn
  ecr_repository_arn = module.ecr.repository_arn
  tags               = local.common_tags
}

# ──────────────────────────────────────────────────────────────────────────────
# 6. Secrets Manager (RDS Credentials)
# ──────────────────────────────────────────────────────────────────────────────

module "secrets_manager" {
  source = "./modules/secrets-manager"

  project_name = var.project_name
  environment  = var.environment
  db_username  = var.db_username
  db_name      = var.db_name
  db_host      = module.database.db_address
  db_port      = 5432
  tags         = local.common_tags
}

# ──────────────────────────────────────────────────────────────────────────────
# 7. Database (RDS PostgreSQL)
# ──────────────────────────────────────────────────────────────────────────────

module "database" {
  source = "./modules/database"

  project_name         = var.project_name
  environment          = var.environment
  db_name              = var.db_name
  db_username          = var.db_username
  db_password          = module.secrets_manager.db_password
  db_instance_class    = var.db_instance_class
  db_allocated_storage = var.db_allocated_storage
  subnet_ids           = module.networking.private_subnet_ids
  security_group_ids   = [module.security.rds_security_group_id]
  tags                 = local.common_tags
}

# ──────────────────────────────────────────────────────────────────────────────
# 8. Airflow Instance Compute (EC2 Docker Host)
# ──────────────────────────────────────────────────────────────────────────────

module "airflow_compute" {
  source = "./modules/airflow-instance-compute"

  project_name          = var.project_name
  environment           = var.environment
  instance_type         = var.ec2_instance_type
  subnet_id             = module.networking.private_subnet_ids[0]
  security_group_ids    = [module.security.ec2_airflow_security_group_id]
  instance_profile_name = module.iam.ec2_airflow_instance_profile_name
  ssh_public_key        = file(var.ssh_public_key_path)
  s3_dags_bucket        = module.ci_cd.s3_dags_bucket_name
  db_host               = module.database.db_address
  db_name               = var.db_name
  db_username           = var.db_username
  secret_arn            = module.secrets_manager.secret_arn
  tags                  = local.common_tags
}

# ──────────────────────────────────────────────────────────────────────────────
# 9. App Runner (Java Backend Service)
# ──────────────────────────────────────────────────────────────────────────────

module "app_runner" {
  source = "./modules/app-runner"

  project_name       = var.project_name
  environment        = var.environment
  ecr_repository_url = module.ecr.repository_url
  cpu                = var.app_runner_cpu
  memory             = var.app_runner_memory
  port               = var.app_runner_port
  subnet_ids         = module.networking.private_subnet_ids
  security_group_ids = [module.security.app_runner_connector_security_group_id]
  instance_role_arn  = module.iam.app_runner_instance_role_arn
  access_role_arn    = module.iam.app_runner_ecr_access_role_arn
  tags               = local.common_tags

  environment_variables = {
    SPRING_PROFILES_ACTIVE = var.environment
    DB_HOST                = module.database.db_address
    DB_PORT                = tostring(module.database.db_port)
    DB_NAME                = var.db_name
    DB_USERNAME            = var.db_username
    ENVIRONMENT            = var.environment
  }
}

# ──────────────────────────────────────────────────────────────────────────────
# CloudWatch Log Group (centralized observability)
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_cloudwatch_log_group" "main" {
  name              = "/${var.project_name}/${var.environment}"
  retention_in_days = var.log_retention_days

  tags = local.common_tags
}
