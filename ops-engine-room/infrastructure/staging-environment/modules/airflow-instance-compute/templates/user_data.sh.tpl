#!/bin/bash
set -euxo pipefail

# ──────────────────────────────────────────────────────────────────────────────
# ServiceHub Airflow Docker Host – Bootstrap Script (Amazon Linux 2)
# Environment: ${environment}
# ──────────────────────────────────────────────────────────────────────────────

exec > >(tee /var/log/user-data.log) 2>&1

echo ">>> Updating system packages..."
yum update -y

echo ">>> Installing Docker..."
yum install -y yum-utils curl ca-certificates unzip
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user

# Ensure `docker compose` is available for SSM deploy commands.
if ! docker compose version >/dev/null 2>&1; then
  mkdir -p /usr/local/lib/docker/cli-plugins
  curl -fsSL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

echo ">>> Installing AWS CLI v2..."
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "/tmp/awscliv2.zip"
cd /tmp && unzip -q awscliv2.zip && ./aws/install
cd /

echo ">>> Installing and starting Amazon SSM Agent..."
yum install -y amazon-ssm-agent || true
systemctl enable amazon-ssm-agent
systemctl start amazon-ssm-agent

echo ">>> Setting up data-engineering sync directory..."
mkdir -p /opt/data-engineering
chown -R ec2-user:ec2-user /opt/data-engineering

echo ">>> Setting up DAGs sync directory..."
mkdir -p /opt/airflow/dags
chown -R ec2-user:ec2-user /opt/airflow

echo ">>> Creating initialization script..."
# Place any initial configurations here if needed.

echo ">>> Installing CloudWatch agent..."
curl -o /tmp/amazon-cloudwatch-agent.rpm https://amazoncloudwatch-agent.s3.amazonaws.com/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
rpm -U /tmp/amazon-cloudwatch-agent.rpm

cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json << 'CW_CONFIG'
{
  "agent": {
    "metrics_collection_interval": 60,
    "run_as_user": "root"
  },
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/user-data.log",
            "log_group_name": "/${project_name}/${environment}/airflow/user-data",
            "log_stream_name": "{instance_id}"
          },
          {
            "file_path": "/var/log/dag-sync.log",
            "log_group_name": "/${project_name}/${environment}/airflow/dag-sync",
            "log_stream_name": "{instance_id}"
          },
          {
            "file_path": "/opt/airflow/logs/**",
            "log_group_name": "/${project_name}/${environment}/airflow/task-logs",
            "log_stream_name": "{instance_id}"
          }
        ]
      }
    }
  }
}
CW_CONFIG

/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config -m ec2 \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json -s

echo ">>> Bootstrap complete!"
