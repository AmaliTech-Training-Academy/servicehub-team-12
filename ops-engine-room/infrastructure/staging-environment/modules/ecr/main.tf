# ──────────────────────────────────────────────────────────────────────────────
# ECR Module – Main
# Creates an ECR repository with secure defaults.
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_ecr_repository" "this" {
  name                 = var.repository_name
  image_tag_mutability = var.image_tag_mutability
  force_delete         = false

  # ── Secure Default: Encryption at Rest ──
  encryption_configuration {
    encryption_type = "AES256"
  }

  # ── Secure Default: Scan on Push ──
  image_scanning_configuration {
    scan_on_push = true
  }

  tags = merge(var.tags, {
    Name        = var.repository_name
    Environment = var.environment
    Module      = "ecr"
  })
}

# ── Lifecycle Policy: Cleanup Untagged Images ──
resource "aws_ecr_lifecycle_policy" "this" {
  repository = aws_ecr_repository.this.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images older than ${var.max_untagged_image_count} count"
        selection = {
          tagStatus   = "untagged"
          countType   = "imageCountMoreThan"
          countNumber = var.max_untagged_image_count
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
