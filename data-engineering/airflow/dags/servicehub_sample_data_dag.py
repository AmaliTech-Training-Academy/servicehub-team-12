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

from logging_config import get_logger  # noqa: E402


logger = get_logger(__name__)


def _load_sample_data() -> None:
    """
    Placeholder callable for loading sample data.

    This will be replaced with a real sample data generator and loader in a
    dedicated feature. Keeping this as a no-op ensures the DAG is deployable
    without failing tasks.
    """
    logger.info("Sample data loader task executed. No-op implementation in place.")


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

