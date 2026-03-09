"""
Airflow DAG to orchestrate the nightly ServiceHub SLA analytics pipeline.
"""

from __future__ import annotations

import os
import sys
from datetime import datetime, timedelta
from pathlib import Path

from airflow import DAG
from airflow.operators.python import PythonOperator

# Ensure the project root is on sys.path so that etl modules can be imported
PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.append(str(PROJECT_ROOT))

from etl_pipeline import run_pipeline  # noqa: E402
from logging_config import get_logger  # noqa: E402


logger = get_logger(__name__)

default_args = {
    "owner": "data-engineering",
    "depends_on_past": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}


def _run_servicehub_etl() -> None:
    """
    Task callable that triggers the main ServiceHub ETL pipeline.
    """
    logger.info("Starting Airflow-triggered ServiceHub ETL pipeline run.")
    run_pipeline()
    logger.info("Completed Airflow-triggered ServiceHub ETL pipeline run.")


with DAG(
    dag_id="servicehub_sla_analytics",
    description="Nightly ETL for ServiceHub SLA analytics and request volumes.",
    default_args=default_args,
    schedule_interval="0 0 * * *",  # Midnight every day
    start_date=datetime(2024, 1, 1),
    catchup=False,
    max_active_runs=1,
    tags=["servicehub", "analytics", "sla"],
) as dag:
    run_etl = PythonOperator(
        task_id="run_servicehub_sla_analytics_pipeline",
        python_callable=_run_servicehub_etl,
    )

