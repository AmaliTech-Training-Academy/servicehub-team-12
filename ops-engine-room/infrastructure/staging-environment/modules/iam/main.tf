# ──────────────────────────────────────────────────────────────────────────────
# IAM Module – Main
# Creates IAM roles and policies for EC2, App Runner.
# ──────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# ──────────────────────────────────────────────────────────────────────────────
# EC2 Instance Role (Airflow Docker Host)
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_iam_role" "ec2_airflow" {
  name = "${local.name_prefix}-ec2-airflow-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-ec2-airflow-role"
    Module = "iam"
  })
}

# SSM access for remote management (no bastion needed)
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_airflow.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# S3 read access for pulling deployment artifacts
resource "aws_iam_policy" "ec2_s3_data_engineering" {
  name        = "${local.name_prefix}-ec2-s3-data-engineering-policy"
  description = "Allow EC2 to read data engineering deployment artifacts from S3"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3DataEngineeringReadAccess"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          var.s3_data_engineering_bucket_arn,
          "${var.s3_data_engineering_bucket_arn}/*"
        ]
      }
    ]
  })

  tags = merge(var.tags, {
    Module = "iam"
  })
}

resource "aws_iam_role_policy_attachment" "ec2_s3_data_engineering" {
  role       = aws_iam_role.ec2_airflow.name
  policy_arn = aws_iam_policy.ec2_s3_data_engineering.arn
}

# CloudWatch Logs access
resource "aws_iam_policy" "ec2_cloudwatch" {
  name        = "${local.name_prefix}-ec2-cloudwatch-policy"
  description = "Allow EC2 to push logs and metrics to CloudWatch"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "CloudWatchLogsAccess"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Sid    = "CloudWatchMetricsAccess"
        Effect = "Allow"
        Action = [
          "cloudwatch:PutMetricData"
        ]
        Resource = "*"
      }
    ]
  })

  tags = merge(var.tags, {
    Module = "iam"
  })
}

resource "aws_iam_role_policy_attachment" "ec2_cloudwatch" {
  role       = aws_iam_role.ec2_airflow.name
  policy_arn = aws_iam_policy.ec2_cloudwatch.arn
}

# Instance Profile
resource "aws_iam_instance_profile" "ec2_airflow" {
  name = "${local.name_prefix}-ec2-airflow-profile"
  role = aws_iam_role.ec2_airflow.name

  tags = merge(var.tags, {
    Module = "iam"
  })
}

# ──────────────────────────────────────────────────────────────────────────────
# App Runner Instance Role
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_iam_role" "app_runner_instance" {
  name = "${local.name_prefix}-apprunner-instance-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "tasks.apprunner.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-apprunner-instance-role"
    Module = "iam"
  })
}

# App Runner CloudWatch policy
resource "aws_iam_policy" "app_runner_cloudwatch" {
  name        = "${local.name_prefix}-apprunner-cloudwatch-policy"
  description = "Allow App Runner to push logs to CloudWatch"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "CloudWatchLogsAccess"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })

  tags = merge(var.tags, {
    Module = "iam"
  })
}

resource "aws_iam_role_policy_attachment" "app_runner_cloudwatch" {
  role       = aws_iam_role.app_runner_instance.name
  policy_arn = aws_iam_policy.app_runner_cloudwatch.arn
}

# App Runner Secrets Manager policy
resource "aws_iam_policy" "app_runner_secrets" {
  name        = "${local.name_prefix}-apprunner-secrets-policy"
  description = "Allow App Runner to read RDS credentials from Secrets Manager"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "SecretsManagerAccess"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = var.secret_arn
      }
    ]
  })

  tags = merge(var.tags, {
    Module = "iam"
  })
}

resource "aws_iam_role_policy_attachment" "app_runner_secrets" {
  role       = aws_iam_role.app_runner_instance.name
  policy_arn = aws_iam_policy.app_runner_secrets.arn
}


# ──────────────────────────────────────────────────────────────────────────────
# App Runner ECR Access Role
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_iam_role" "app_runner_ecr_access" {
  name = "${local.name_prefix}-apprunner-ecr-access-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "build.apprunner.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-apprunner-ecr-access-role"
    Module = "iam"
  })
}

resource "aws_iam_policy" "app_runner_ecr_pull" {
  name        = "${local.name_prefix}-apprunner-ecr-pull-policy"
  description = "Allow App Runner to pull images from ECR"

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
        Sid    = "ECRPullAccess"
        Effect = "Allow"
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:DescribeImages"
        ]
        Resource = var.ecr_repository_arn
      }
    ]
  })

  tags = merge(var.tags, {
    Module = "iam"
  })
}

resource "aws_iam_role_policy_attachment" "app_runner_ecr_pull" {
  role       = aws_iam_role.app_runner_ecr_access.name
  policy_arn = aws_iam_policy.app_runner_ecr_pull.arn
}
