"""
Airflow DAG to support manual loading of sample data for development and testing.

The implementation of the actual sample data generator will be added in a
separate feature. For now, this DAG provides a safe, no-op task that can be
extended without changing the orchestration structure.
"""

from __future__ import annotations

import sys
from datetime import datetime
from pathlib import Path

from airflow import DAG
from airflow.operators.python import PythonOperator

# Ensure the project root is on sys.path so that shared modules can be imported
PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.append(str(PROJECT_ROOT))

from etl_pipeline import get_engine  # noqa: E402
from logging_config import get_logger  # noqa: E402
from sample_data.generator import SampleConfig, load_sample_requests  # noqa: E402


logger = get_logger(__name__)


def _load_sample_data() -> None:
    """
    Load synthetic sample data for development and testing.
    """
    engine = get_engine()
    config = SampleConfig()
    logger.info("Starting sample data generation using config: %s", config)
    load_sample_requests(engine, config=config)
    logger.info("Completed sample data generation and load.")


with DAG(
    dag_id="servicehub_sample_data_loader",
    description="Manual DAG for loading or refreshing sample ServiceHub data.",
    schedule_interval=None,  # Manual trigger only
    start_date=datetime(2024, 1, 1),
    catchup=False,
    max_active_runs=1,
    tags=["servicehub", "sample-data"],
) as dag:
    load_sample_data = PythonOperator(
        task_id="load_sample_data",
        python_callable=_load_sample_data,
    )

