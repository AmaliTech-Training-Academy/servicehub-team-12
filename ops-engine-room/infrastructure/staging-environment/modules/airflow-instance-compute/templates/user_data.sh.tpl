#!/bin/bash
set -euxo pipefail

# ──────────────────────────────────────────────────────────────────────────────
# ServiceHub Airflow Docker Host – Bootstrap Script (Ubuntu 24.04)
# Environment: ${environment}
# ──────────────────────────────────────────────────────────────────────────────

exec > >(tee /var/log/user-data.log) 2>&1

echo ">>> Updating system packages..."
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get upgrade -y

echo ">>> Installing Docker..."
apt-get install -y ca-certificates curl gnupg lsb-release
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$${VERSION_CODENAME}") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable docker
systemctl start docker
usermod -aG docker ubuntu

echo ">>> Installing AWS CLI v2..."
apt-get install -y unzip
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "/tmp/awscliv2.zip"
cd /tmp && unzip -q awscliv2.zip && ./aws/install
cd /

echo ">>> Setting up DAGs sync directory..."
mkdir -p /opt/airflow/dags
chown -R ubuntu:ubuntu /opt/airflow

echo ">>> Creating S3 DAG sync cron job (every 2 minutes)..."
cat > /etc/cron.d/dag-sync << 'CRON'
*/2 * * * * ubuntu /usr/local/bin/aws s3 sync s3://${s3_dags_bucket}/dags/ /opt/airflow/dags/ --region ${aws_region} --delete >> /var/log/dag-sync.log 2>&1
CRON
chmod 0644 /etc/cron.d/dag-sync

echo ">>> Initial DAG sync..."
su - ubuntu -c "/usr/local/bin/aws s3 sync s3://${s3_dags_bucket}/dags/ /opt/airflow/dags/ --region ${aws_region} --delete" || true

echo ">>> Installing CloudWatch agent..."
curl -o /tmp/amazon-cloudwatch-agent.deb https://amazoncloudwatch-agent.s3.amazonaws.com/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
dpkg -i /tmp/amazon-cloudwatch-agent.deb

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
