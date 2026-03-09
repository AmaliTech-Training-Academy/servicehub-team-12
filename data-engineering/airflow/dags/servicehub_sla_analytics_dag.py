"""
Airflow DAG to orchestrate the nightly ServiceHub SLA analytics pipeline.

Each logical ETL step is represented as a separate task to make failures and
debugging more transparent.
"""

from __future__ import annotations

import sys
from datetime import datetime, timedelta
from pathlib import Path

import pandas as pd
from airflow import DAG
from airflow.operators.python import PythonOperator

# Ensure the project root is on sys.path so that ETL modules can be imported
PROJECT_ROOT = Path(__file__).resolve().parents[2]
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


def _extract_requests(**_: object) -> str:
    """
    Extract service requests and return them as a JSON string for downstream tasks.
    """
    engine = get_engine()
    df = extract_requests(engine)
    logger.info("Airflow extract_requests produced %d rows", len(df))
    return df.to_json(orient="records", date_format="iso")


def _extract_sla_policies(**_: object) -> str:
    """
    Extract SLA policies and return them as a JSON string for downstream tasks.
    """
    engine = get_engine()
    df = extract_sla_policies(engine)
    logger.info("Airflow extract_sla_policies produced %d rows", len(df))
    return df.to_json(orient="records", date_format="iso")


def _validate_and_quarantine(**context: object) -> None:
    """
    Validate request and SLA data, quarantine invalid rows, and push valid
    datasets to XCom for downstream transformations.
    """
    ti = context["ti"]

    requests_json = ti.xcom_pull(task_ids="extract_requests")
    sla_json = ti.xcom_pull(task_ids="extract_sla_policies")

    requests_df = pd.read_json(requests_json, orient="records") if requests_json else pd.DataFrame()
    sla_df = pd.read_json(sla_json, orient="records") if sla_json else pd.DataFrame()

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

    ti.xcom_push(
        key="valid_requests",
        value=valid_requests.to_json(orient="records", date_format="iso"),
    )
    ti.xcom_push(
        key="valid_sla",
        value=valid_sla.to_json(orient="records", date_format="iso"),
    )


def _compute_and_load_sla_metrics(**context: object) -> None:
    """
    Compute SLA metrics from validated data and load them into the analytics table.
    """
    ti = context["ti"]
    valid_requests_json = ti.xcom_pull(task_ids="validate_and_quarantine", key="valid_requests")
    valid_sla_json = ti.xcom_pull(task_ids="validate_and_quarantine", key="valid_sla")

    requests_df = (
        pd.read_json(valid_requests_json, orient="records") if valid_requests_json else pd.DataFrame()
    )
    sla_df = pd.read_json(valid_sla_json, orient="records") if valid_sla_json else pd.DataFrame()

    engine = get_engine()
    sla_metrics = transform_sla_metrics(requests_df, sla_df)
    load_analytics(sla_metrics, "analytics_sla_metrics", engine)


def _compute_and_load_daily_volume(**context: object) -> None:
    """
    Compute daily request volumes from validated data and load them into the analytics table.
    """
    ti = context["ti"]
    valid_requests_json = ti.xcom_pull(task_ids="validate_and_quarantine", key="valid_requests")
    requests_df = (
        pd.read_json(valid_requests_json, orient="records") if valid_requests_json else pd.DataFrame()
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

