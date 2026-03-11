# ──────────────────────────────────────────────────────────────────────────────
# CI/CD Module – Main
# Creates GitHub Actions OIDC federation and S3 DAGs bucket.
# ──────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

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

# ── S3 DAGs Sync Policy ──
resource "aws_iam_policy" "github_s3_sync" {
  name        = "${local.name_prefix}-github-s3-sync-policy"
  description = "Allow GitHub Actions to sync DAGs to S3"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3DAGsSyncAccess"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.dags.arn,
          "${aws_s3_bucket.dags.arn}/*"
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
  policy_arn = aws_iam_policy.github_s3_sync.arn
}

# ──────────────────────────────────────────────────────────────────────────────
# S3 Bucket for Airflow DAGs (Source of Truth)
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_s3_bucket" "dags" {
  bucket        = "${local.name_prefix}-airflow-dags"
  force_destroy = false

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-airflow-dags"
    Module = "ci-cd"
  })
}

# ── Secure Default: Versioning Enabled ──
resource "aws_s3_bucket_versioning" "dags" {
  bucket = aws_s3_bucket.dags.id

  versioning_configuration {
    status = "Enabled"
  }
}

# ── Secure Default: Encryption at Rest ──
resource "aws_s3_bucket_server_side_encryption_configuration" "dags" {
  bucket = aws_s3_bucket.dags.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

# ── Secure Default: Block All Public Access ──
resource "aws_s3_bucket_public_access_block" "dags" {
  bucket = aws_s3_bucket.dags.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
