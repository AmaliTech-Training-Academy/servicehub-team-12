# Data Engineering Deployment (Airflow on EC2)

This document describes the production deployment flow for the data engineering stack.

## Overview

The deployment is event-driven and uses GitHub Actions, S3, and AWS Systems Manager (SSM):

1. GitHub Actions generates `data-engineering/.env`.
2. Workflow archives the `data-engineering/` directory into `data-engineering.tar.gz`.
3. Workflow uploads the archive to the data engineering S3 bucket.
4. Workflow sends an SSM Run Command to EC2 instances tagged `Role=AirflowDockerHost`.
5. EC2 pulls the archive, extracts to `/opt/`, and runs:
   - `docker compose down || true`
   - `docker compose up -d --build`

## Workflow

Deployment logic lives in:

- `.github/workflows/ci-reusable.yml` (`deploy-dags` job)
- `.github/workflows/ci-merge.yml` (enables `deploy_dags: true` on main merges)

## Required GitHub Secrets

### AWS + Deployment Routing

- `AWS_ROLE_ARN`: OIDC role assumed by GitHub Actions
- `AWS_REGION`: AWS region (for example `eu-west-1`)
- `S3_DATA_ENGINEERING_BUCKET`: target bucket for deployment tarball
- `RDS_CREDENTIALS_SECRET_ID`: Secrets Manager secret id/name/arn

### Airflow Runtime (optional, with defaults)

If not provided, defaults are applied by workflow.

- `AIRFLOW_DB` (default: `airflow`)
- `AIRFLOW_WEB_PORT` (default: `8080`)
- `AIRFLOW_UID` (default: `50000`)
- `AIRFLOW_ADMIN_USER` (default: `airflow`)
- `AIRFLOW_ADMIN_PASSWORD` (default: `airflow_pass`)
- `LOG_LEVEL` (default: `INFO`)
- `ETL_WINDOW_DAYS` (default: `7`)

## Secrets Manager Contract

`RDS_CREDENTIALS_SECRET_ID` must resolve to a JSON secret containing these keys:

- `host`
- `port`
- `dbname`
- `username`
- `password`

Example structure:

```json
{
  "engine": "postgres",
  "host": "servicehub-staging-postgres.xxxx.eu-west-1.rds.amazonaws.com",
  "port": 5432,
  "dbname": "servicehub_db",
  "username": "servicehub_admin",
  "password": "..."
}
```

## Generated .env on Deploy

During deployment, the workflow writes `/data-engineering/.env` with:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `DB_USER_URI` (URL-encoded)
- `DB_PASSWORD_URI` (URL-encoded)
- `AIRFLOW_DB`
- `AIRFLOW_WEB_PORT`
- `AIRFLOW_UID`
- `AIRFLOW_ADMIN_USER`
- `AIRFLOW_ADMIN_PASSWORD`
- `LOG_LEVEL`
- `ETL_WINDOW_DAYS`

`DB_USER_URI` and `DB_PASSWORD_URI` are required so special characters in credentials do not break SQLAlchemy/Celery connection URI parsing.

## Docker Compose Notes

The compose file uses:

- raw DB values for Postgres container auth
- URL-encoded DB values for Airflow/Celery database connection URIs

This prevents errors such as:

- `ValueError: Port could not be cast to integer value ...`

## EC2 Expectations

Target instance must:

- be tagged `Role=AirflowDockerHost`
- have `amazon-ssm-agent` installed and running
- have IAM permissions for:
  - `AmazonSSMManagedInstanceCore`
  - `s3:GetObject` on the data engineering bucket

## Troubleshooting

### No SSM command invocations found

Check:

- EC2 managed node is online in SSM
- instance has `Role=AirflowDockerHost` tag
- GitHub role has `ssm:SendCommand`, `ssm:List*`, `ssm:GetCommandInvocation`, `ssm:DescribeInstanceInformation`

### Waiter timeout / command stuck InProgress

Usually means remote command started but took too long.

Check on instance:

```bash
cd /opt/data-engineering
docker compose ps
docker compose logs --tail=200
```

### Missing environment variables in instance

Check hidden file:

```bash
cd /opt/data-engineering
ls -lah
cat .env
```

If DB keys are empty, verify `RDS_CREDENTIALS_SECRET_ID` and secret JSON keys.

## Manual Validation

After a deployment run:

```bash
cd /opt/data-engineering
cat .env
docker compose ps
```

Expected:

- `.env` contains non-empty DB values
- Airflow services healthy and running
