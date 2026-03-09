"""
Airflow DAG to orchestrate the nightly ServiceHub SLA analytics pipeline.

Each logical ETL step is represented as a separate task to make failures and
debugging more transparent.
"""

from __future__ import annotations

import os
import sys
import uuid
from datetime import datetime, timedelta
from pathlib import Path

import pandas as pd
from airflow import DAG
from airflow.operators.python import PythonOperator

# Ensure the project root is on sys.path so that ETL modules can be imported
PROJECT_ROOT = Path(os.getenv("SERVICEHUB_DE_ROOT", Path(__file__).resolve().parents[2]))
if str(PROJECT_ROOT) not in sys.path:
    sys.path.append(str(PROJECT_ROOT))

from etl import (  # noqa: E402
    extract_requests,
    extract_sla_policies,
    load_analytics,
    transform_daily_volume,
    transform_sla_metrics,
)
from etl.validation import (  # noqa: E402
    validate_and_split_requests,
    validate_and_split_sla_policies,
)
from etl_pipeline import get_engine  # noqa: E402
from exceptions import ETLBaseError  # noqa: E402
from logging_config import get_logger  # noqa: E402


logger = get_logger(__name__)

default_args = {
    "owner": "data-engineering",
    "depends_on_past": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

TEMP_DIR = Path(os.getenv("ETL_TEMP_DIR", "/tmp/servicehub_etl"))


def _ensure_temp_dir() -> Path:
    TEMP_DIR.mkdir(parents=True, exist_ok=True)
    return TEMP_DIR


def _extract_requests(**context: object) -> None:
    """
    Extract service requests and persist them to a temporary file for
    downstream tasks to consume.
    """
    ti = context["ti"]
    logical_date_str = context.get("ds_nodash") or datetime.utcnow().strftime("%Y%m%d")

    engine = get_engine()
    df = extract_requests(engine)
    logger.info("Airflow extract_requests produced %d rows", len(df))

    temp_dir = _ensure_temp_dir()
    path = temp_dir / f"requests_{logical_date_str}_{uuid.uuid4().hex}.parquet"
    df.to_parquet(path)

    ti.xcom_push(key="requests_path", value=str(path))


def _extract_sla_policies(**context: object) -> None:
    """
    Extract SLA policies and persist them to a temporary file for
    downstream tasks to consume.
    """
    ti = context["ti"]
    logical_date_str = context.get("ds_nodash") or datetime.utcnow().strftime("%Y%m%d")

    engine = get_engine()
    df = extract_sla_policies(engine)
    logger.info("Airflow extract_sla_policies produced %d rows", len(df))

    temp_dir = _ensure_temp_dir()
    path = temp_dir / f"sla_policies_{logical_date_str}_{uuid.uuid4().hex}.parquet"
    df.to_parquet(path)

    ti.xcom_push(key="sla_policies_path", value=str(path))


def _validate_and_quarantine(**context: object) -> None:
    """
    Validate request and SLA data, quarantine invalid rows, and push valid
    datasets to XCom for downstream transformations.
    """
    ti = context["ti"]

    requests_path = ti.xcom_pull(task_ids="extract_requests", key="requests_path")
    sla_path = ti.xcom_pull(task_ids="extract_sla_policies", key="sla_policies_path")

    requests_df = (
        pd.read_parquet(requests_path) if requests_path and Path(requests_path).exists() else pd.DataFrame()
    )
    sla_df = (
        pd.read_parquet(sla_path) if sla_path and Path(sla_path).exists() else pd.DataFrame()
    )

    engine = get_engine()

    try:
        valid_requests, invalid_requests = validate_and_split_requests(requests_df)
        valid_sla, invalid_sla = validate_and_split_sla_policies(sla_df)
    except Exception as exc:
        # Treat this as an ETL error at the task level
        logger.exception("Validation step failed in Airflow DAG: %s", exc)
        raise ETLBaseError("Validation step failed in Airflow DAG") from exc

    # Quarantine invalid data but do not fail the DAG because of it
    try:
        if not invalid_requests.empty:
            load_analytics(invalid_requests, "analytics_invalid_requests", engine)
        if not invalid_sla.empty:
            load_analytics(invalid_sla, "analytics_invalid_sla_policies", engine)
    except ETLBaseError:
        logger.exception("Failed to load quarantined data in Airflow DAG; continuing.")

    temp_dir = _ensure_temp_dir()

    valid_requests_path = temp_dir / f"valid_requests_{uuid.uuid4().hex}.parquet"
    valid_sla_path = temp_dir / f"valid_sla_{uuid.uuid4().hex}.parquet"

    valid_requests.to_parquet(valid_requests_path)
    valid_sla.to_parquet(valid_sla_path)

    ti.xcom_push(key="valid_requests_path", value=str(valid_requests_path))
    ti.xcom_push(key="valid_sla_path", value=str(valid_sla_path))


def _compute_and_load_sla_metrics(**context: object) -> None:
    """
    Compute SLA metrics from validated data and load them into the analytics table.
    """
    ti = context["ti"]
    valid_requests_path = ti.xcom_pull(task_ids="validate_and_quarantine", key="valid_requests_path")
    valid_sla_path = ti.xcom_pull(task_ids="validate_and_quarantine", key="valid_sla_path")

    requests_df = (
        pd.read_parquet(valid_requests_path)
        if valid_requests_path and Path(valid_requests_path).exists()
        else pd.DataFrame()
    )
    sla_df = (
        pd.read_parquet(valid_sla_path)
        if valid_sla_path and Path(valid_sla_path).exists()
        else pd.DataFrame()
    )

    engine = get_engine()
    sla_metrics = transform_sla_metrics(requests_df, sla_df)
    load_analytics(sla_metrics, "analytics_sla_metrics", engine)


def _compute_and_load_daily_volume(**context: object) -> None:
    """
    Compute daily request volumes from validated data and load them into the analytics table.
    """
    ti = context["ti"]
    valid_requests_path = ti.xcom_pull(task_ids="validate_and_quarantine", key="valid_requests_path")
    requests_df = (
        pd.read_parquet(valid_requests_path)
        if valid_requests_path and Path(valid_requests_path).exists()
        else pd.DataFrame()
    )

    engine = get_engine()
    daily_volume = transform_daily_volume(requests_df)
    load_analytics(daily_volume, "analytics_daily_volume", engine)


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
    extract_requests_task = PythonOperator(
        task_id="extract_requests",
        python_callable=_extract_requests,
    )

    extract_sla_policies_task = PythonOperator(
        task_id="extract_sla_policies",
        python_callable=_extract_sla_policies,
    )

    validate_and_quarantine_task = PythonOperator(
        task_id="validate_and_quarantine",
        python_callable=_validate_and_quarantine,
        provide_context=True,
    )

    sla_metrics_task = PythonOperator(
        task_id="compute_and_load_sla_metrics",
        python_callable=_compute_and_load_sla_metrics,
        provide_context=True,
    )

    daily_volume_task = PythonOperator(
        task_id="compute_and_load_daily_volume",
        python_callable=_compute_and_load_daily_volume,
        provide_context=True,
    )

    [extract_requests_task, extract_sla_policies_task] >> validate_and_quarantine_task
    validate_and_quarantine_task >> [sla_metrics_task, daily_volume_task]

