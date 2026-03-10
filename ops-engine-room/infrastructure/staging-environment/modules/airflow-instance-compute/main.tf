# ──────────────────────────────────────────────────────────────────────────────
# Airflow Instance Compute Module – Main
# Provisions an Ubuntu 24.04 EC2 instance as a Docker host for Airflow.
# Uses the existing servicehub SSH key pair from the staging environment.
# ──────────────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "${var.project_name}-${var.environment}"
}

# ──────────────────────────────────────────────────────────────────────────────
# SSH Key Pair (using existing servicehub-dev-key)
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_key_pair" "airflow" {
  key_name   = "${local.name_prefix}-airflow-key"
  public_key = var.ssh_public_key

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-airflow-key"
    Module = "airflow-instance-compute"
  })
}

# ──────────────────────────────────────────────────────────────────────────────
# EC2 Instance (Airflow Docker Host)
# ──────────────────────────────────────────────────────────────────────────────

resource "aws_instance" "airflow" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = var.security_group_ids
  iam_instance_profile   = var.instance_profile_name
  key_name               = aws_key_pair.airflow.key_name

  # ── Secure Default: No public IP in private subnet ──
  associate_public_ip_address = false

  # ── Secure Default: Encrypted root volume ──
  root_block_device {
    volume_type           = "gp3"
    volume_size           = 30
    encrypted             = true
    delete_on_termination = true

    tags = merge(var.tags, {
      Name = "${local.name_prefix}-airflow-root-vol"
    })
  }

  # ── Metadata Service v2 (IMDSv2) hardened ──
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required" # Enforce IMDSv2
    http_put_response_hop_limit = 2
  }

  # ── User Data: Bootstrap Docker + Airflow ──
  user_data = base64encode(templatefile("${path.module}/templates/user_data.sh.tpl", {
    s3_dags_bucket = var.s3_dags_bucket
    db_host        = var.db_host
    db_name        = var.db_name
    db_username    = var.db_username
    secret_arn     = var.secret_arn
    aws_region     = data.aws_region.current.name
    environment    = var.environment
    project_name   = var.project_name
  }))

  tags = merge(var.tags, {
    Name   = "${local.name_prefix}-airflow-docker-host"
    Module = "airflow-instance-compute"
    Role   = "AirflowDockerHost"
  })

  lifecycle {
    ignore_changes = [ami, user_data]
  }
}

# ──────────────────────────────────────────────────────────────────────────────
# Data Sources
# ──────────────────────────────────────────────────────────────────────────────

data "aws_region" "current" {}

# Ubuntu 24.04 LTS (Noble Numbat) – latest AMI from Canonical
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "state"
    values = ["available"]
  }
}
