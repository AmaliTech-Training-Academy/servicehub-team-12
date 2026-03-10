# ──────────────────────────────────────────────────────────────────────────────
# App Runner Module – Main
# Provisions AWS App Runner service with VPC connector for RDS access.
# ──────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# ──────────────────────────────────────────────────────────────────────────────
# VPC Connector (enables App Runner to reach RDS in private subnets)
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_apprunner_vpc_connector" "this" {
  vpc_connector_name = "${local.name_prefix}-vpc-connector"
  subnets            = var.subnet_ids
  security_groups    = var.security_group_ids

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-vpc-connector"
    Module = "app-runner"
  })
}

# ──────────────────────────────────────────────────────────────────────────────
# App Runner Auto Scaling Configuration
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_apprunner_auto_scaling_configuration_version" "this" {
  auto_scaling_configuration_name = "${local.name_prefix}-autoscaling"

  max_concurrency = 100
  max_size        = 3 # Cost-conscious for staging
  min_size        = 1

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-autoscaling"
    Module = "app-runner"
  })
}

# ──────────────────────────────────────────────────────────────────────────────
# App Runner Service
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_apprunner_service" "this" {
  service_name = "${local.name_prefix}-backend"

  source_configuration {
    # Auto-deployments will be enabled once the ECR image is available.
    # Initially using a public nginx image as a bootstrap placeholder.
    auto_deployments_enabled = false

    image_repository {
      image_identifier      = "public.ecr.aws/bitnami/nginx:latest"
      image_repository_type = "ECR_PUBLIC"

      image_configuration {
        port = var.port
        runtime_environment_variables = merge(var.environment_variables, {
          NGINX_HTTP_PORT_NUMBER = var.port
        })
      }
    }
  }

  instance_configuration {
    cpu               = var.cpu
    memory            = var.memory
    instance_role_arn = var.instance_role_arn
  }

  network_configuration {
    egress_configuration {
      egress_type       = "VPC"
      vpc_connector_arn = aws_apprunner_vpc_connector.this.arn
    }
  }

  auto_scaling_configuration_arn = aws_apprunner_auto_scaling_configuration_version.this.arn

  health_check_configuration {
    protocol            = "TCP"
    interval            = 10
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-backend"
    Module = "app-runner"
  })
}
