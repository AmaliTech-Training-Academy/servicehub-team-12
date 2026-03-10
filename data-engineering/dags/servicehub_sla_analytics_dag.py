"""
Airflow DAG to orchestrate the nightly ServiceHub SLA analytics pipeline.

Each logical ETL step is represented as a separate task to make failures and
debugging more transparent.
"""

import os
import sys
import uuid
from datetime import datetime, timedelta
from pathlib import Path

import pandas as pd
from airflow import DAG
from airflow.providers.standard.operators.python import PythonOperator

# Ensure the project root is on sys.path so that ETL modules can be imported
PROJECT_ROOT = Path(os.getenv("PYTHONPATH", Path(__file__).resolve().parents[1]))
if str(PROJECT_ROOT) not in sys.path:
    sys.path.append(str(PROJECT_ROOT))

from etl import (  # noqa: E402
    extract_requests,
    extract_sla_policies,
    load_analytics,
    transform_agent_performance,
    transform_daily_volume,
    transform_department_workload,
    transform_sla_metrics,
)
from exceptions import DataValidationError  # noqa: E402
from etl.validation import (  # noqa: E402
    validate_and_split_requests,
    validate_and_split_sla_policies,
)
from etl_pipeline import get_engine  # noqa: E402
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


def _annotate_validation_failure(df: pd.DataFrame, error_message: str) -> pd.DataFrame:
    invalid_df = df.copy()
    invalid_df["quarantine_reason"] = "dataset_validation_failed"
    invalid_df["validation_error"] = error_message
    return invalid_df


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
    Validate request and SLA data, quarantine invalid rows, and push file paths
    for downstream tasks.
    """
    ti = context["ti"]
    requests_path = ti.xcom_pull(task_ids="extract_requests", key="requests_path")
    sla_policies_path = ti.xcom_pull(
        task_ids="extract_sla_policies", key="sla_policies_path"
    )

    requests_df = pd.read_parquet(requests_path) if requests_path else pd.DataFrame()
    sla_df = pd.read_parquet(sla_policies_path) if sla_policies_path else pd.DataFrame()

    try:
        valid_requests, invalid_requests = validate_and_split_requests(requests_df)
    except DataValidationError as exc:
        logger.error("Request validation failed in DAG task: %s", exc)
        valid_requests = pd.DataFrame()
        invalid_requests = _annotate_validation_failure(requests_df, str(exc))

    try:
        valid_sla, invalid_sla = validate_and_split_sla_policies(sla_df)
    except DataValidationError as exc:
        logger.error("SLA policy validation failed in DAG task: %s", exc)
        valid_sla = pd.DataFrame()
        invalid_sla = _annotate_validation_failure(sla_df, str(exc))

    temp_dir = _ensure_temp_dir()
    valid_requests_path = temp_dir / f"valid_requests_{uuid.uuid4().hex}.parquet"
    invalid_requests_path = temp_dir / f"invalid_requests_{uuid.uuid4().hex}.parquet"
    valid_sla_path = temp_dir / f"valid_sla_{uuid.uuid4().hex}.parquet"
    invalid_sla_path = temp_dir / f"invalid_sla_{uuid.uuid4().hex}.parquet"

    valid_requests.to_parquet(valid_requests_path)
    invalid_requests.to_parquet(invalid_requests_path)
    valid_sla.to_parquet(valid_sla_path)
    invalid_sla.to_parquet(invalid_sla_path)

    ti.xcom_push(key="valid_requests_path", value=str(valid_requests_path))
    ti.xcom_push(key="invalid_requests_path", value=str(invalid_requests_path))
    ti.xcom_push(key="valid_sla_path", value=str(valid_sla_path))
    ti.xcom_push(key="invalid_sla_path", value=str(invalid_sla_path))


def _compute_and_load_sla_metrics(**context: object) -> None:
    """
    Compute SLA metrics from validated data and load them into the analytics table.
    """
    ti = context["ti"]
    valid_requests_path = ti.xcom_pull(
        task_ids="validate_and_quarantine", key="valid_requests_path"
    )
    valid_sla_path = ti.xcom_pull(
        task_ids="validate_and_quarantine", key="valid_sla_path"
    )

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
    valid_requests_path = ti.xcom_pull(
        task_ids="validate_and_quarantine", key="valid_requests_path"
    )
    requests_df = (
        pd.read_parquet(valid_requests_path)
        if valid_requests_path and Path(valid_requests_path).exists()
        else pd.DataFrame()
    )

    engine = get_engine()
    daily_volume = transform_daily_volume(requests_df)
    load_analytics(daily_volume, "analytics_daily_volume", engine)


def _compute_and_load_agent_performance(**context: object) -> None:
    """
    Compute weekly agent performance from validated request data and load it.
    """
    ti = context["ti"]
    valid_requests_path = ti.xcom_pull(
        task_ids="validate_and_quarantine", key="valid_requests_path"
    )
    requests_df = (
        pd.read_parquet(valid_requests_path)
        if valid_requests_path and Path(valid_requests_path).exists()
        else pd.DataFrame()
    )

    engine = get_engine()
    agent_performance = transform_agent_performance(requests_df)
    load_analytics(agent_performance, "analytics_agent_performance", engine)


def _compute_and_load_department_workload(**context: object) -> None:
    """
    Compute weekly department workload from validated request data and load it.
    """
    ti = context["ti"]
    valid_requests_path = ti.xcom_pull(
        task_ids="validate_and_quarantine", key="valid_requests_path"
    )
    requests_df = (
        pd.read_parquet(valid_requests_path)
        if valid_requests_path and Path(valid_requests_path).exists()
        else pd.DataFrame()
    )

    engine = get_engine()
    department_workload = transform_department_workload(requests_df)
    load_analytics(
        department_workload,
        "analytics_department_workload",
        engine,
    )


with DAG(
    dag_id="servicehub_sla_analytics",
    description="Nightly ETL for ServiceHub SLA, request volume, agent, and department analytics.",
    default_args=default_args,
    schedule="0 * * * *",  # hourly
    start_date=datetime(2026, 3, 10),
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
    )

    sla_metrics_task = PythonOperator(
        task_id="compute_and_load_sla_metrics",
        python_callable=_compute_and_load_sla_metrics,
    )

    daily_volume_task = PythonOperator(
        task_id="compute_and_load_daily_volume",
        python_callable=_compute_and_load_daily_volume,
    )

    agent_performance_task = PythonOperator(
        task_id="compute_and_load_agent_performance",
        python_callable=_compute_and_load_agent_performance,
    )

    department_workload_task = PythonOperator(
        task_id="compute_and_load_department_workload",
        python_callable=_compute_and_load_department_workload,
    )

    [extract_requests_task, extract_sla_policies_task] >> validate_and_quarantine_task
    validate_and_quarantine_task >> [
        sla_metrics_task,
        daily_volume_task,
        agent_performance_task,
        department_workload_task,
    ]
