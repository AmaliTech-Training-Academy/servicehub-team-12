# ──────────────────────────────────────────────────────────────────────────────
# CI/CD Module – Main
# Creates GitHub Actions OIDC federation and S3 DAGs bucket.
# ──────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

data "aws_region" "current" {}

data "aws_caller_identity" "current" {}

# ──────────────────────────────────────────────────────────────────────────────
# GitHub Actions OIDC Provider (already exists in this AWS account)
# ──────────────────────────────────────────────────────────────────────────────

data "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"
}

# ──────────────────────────────────────────────────────────────────────────────
# GitHub Actions IAM Role
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_iam_role" "github_actions" {
  name = "${local.name_prefix}-github-actions-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "GitHubActionsOIDC"
        Effect = "Allow"
        Principal = {
          Federated = data.aws_iam_openid_connect_provider.github.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          }
          StringLike = {
            "token.actions.githubusercontent.com:sub" = "repo:${var.github_org}/${var.github_repo}:*"
          }
        }
      }
    ]
  })

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-github-actions-role"
    Module = "ci-cd"
  })
}

# ── ECR Push Policy ──
resource "aws_iam_policy" "github_ecr_push" {
  name        = "${local.name_prefix}-github-ecr-push-policy"
  description = "Allow GitHub Actions to push images to ECR"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ECRAuthToken"
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken"
        ]
        Resource = "*"
      },
      {
        Sid    = "ECRPushAccess"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:PutImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:DescribeImages"
        ]
        Resource = var.ecr_repository_arn
      }
    ]
  })

  tags = merge(var.tags, {
    Module = "ci-cd"
  })
}

resource "aws_iam_role_policy_attachment" "github_ecr_push" {
  role       = aws_iam_role.github_actions.name
  policy_arn = aws_iam_policy.github_ecr_push.arn
}

# ── S3 Data Engineering Artifact Upload Policy ──
resource "aws_iam_policy" "github_s3_data_engineering_upload" {
  name        = "${local.name_prefix}-github-s3-data-engineering-upload-policy"
  description = "Allow GitHub Actions to upload data engineering artifacts to S3"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3DataEngineeringUploadAccess"
        Effect = "Allow"
        Action = [
          "s3:PutObject"
        ]
        Resource = [
          "${aws_s3_bucket.data_engineering.arn}/*"
        ]
      }
    ]
  })

  tags = merge(var.tags, {
    Module = "ci-cd"
  })
}

resource "aws_iam_role_policy_attachment" "github_s3_sync" {
  role       = aws_iam_role.github_actions.name
  policy_arn = aws_iam_policy.github_s3_data_engineering_upload.arn
}

# ── SSM Deploy Policy (least privilege via instance tag condition) ──
resource "aws_iam_policy" "github_ssm_deploy" {
  name        = "${local.name_prefix}-github-ssm-deploy-policy"
  description = "Allow GitHub Actions to run SSM deployment commands on Airflow host"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "SSMSendCommandToTaggedAirflowHost"
        Effect = "Allow"
        Action = [
          "ssm:SendCommand"
        ]
        Resource = [
          "arn:aws:ssm:${data.aws_region.current.name}::document/AWS-RunShellScript",
          "arn:aws:ec2:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:instance/*"
        ]
        Condition = {
          StringEquals = {
            "ssm:resourceTag/Role" = "AirflowDockerHost"
          }
        }
      },
      {
        Sid    = "SSMReadCommandStatus"
        Effect = "Allow"
        Action = [
          "ssm:GetCommandInvocation",
          "ssm:ListCommandInvocations",
          "ssm:ListCommands"
        ]
        Resource = "*"
      }
    ]
  })

  tags = merge(var.tags, {
    Module = "ci-cd"
  })
}

resource "aws_iam_role_policy_attachment" "github_ssm_deploy" {
  role       = aws_iam_role.github_actions.name
  policy_arn = aws_iam_policy.github_ssm_deploy.arn
}

# ── Secrets Manager Read Policy for deployment-time DB password fetch ──
resource "aws_iam_policy" "github_secretsmanager_rds_read" {
  name        = "${local.name_prefix}-github-secretsmanager-rds-read-policy"
  description = "Allow GitHub Actions to read the RDS credentials secret"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadRDSCredentialsSecret"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = "arn:aws:secretsmanager:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:secret:${local.name_prefix}/rds/credentials*"
      }
    ]
  })

  tags = merge(var.tags, {
    Module = "ci-cd"
  })
}

resource "aws_iam_role_policy_attachment" "github_secretsmanager_rds_read" {
  role       = aws_iam_role.github_actions.name
  policy_arn = aws_iam_policy.github_secretsmanager_rds_read.arn
}

# ──────────────────────────────────────────────────────────────────────────────
# S3 Bucket for Data Engineering Artifacts
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_s3_bucket" "data_engineering" {
  bucket        = "${local.name_prefix}-data-engineering"
  force_destroy = false

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-data-engineering"
    Module = "ci-cd"
  })
}

# ── Secure Default: Versioning Enabled ──
resource "aws_s3_bucket_versioning" "data_engineering" {
  bucket = aws_s3_bucket.data_engineering.id

  versioning_configuration {
    status = "Enabled"
  }
}

# ── Secure Default: Encryption at Rest ──
resource "aws_s3_bucket_server_side_encryption_configuration" "data_engineering" {
  bucket = aws_s3_bucket.data_engineering.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

# ── Secure Default: Block All Public Access ──
resource "aws_s3_bucket_public_access_block" "data_engineering" {
  bucket = aws_s3_bucket.data_engineering.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
