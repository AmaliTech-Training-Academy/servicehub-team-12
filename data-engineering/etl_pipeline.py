"""ETL Pipeline for ServiceHub - SLA Analytics and Resolution Metrics.

This module provides the orchestration layer. Extraction, transformation,
and loading logic live in the dedicated `etl` package.
"""

from typing import Optional

import pandas as pd
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine

from config import DATABASE_URL
from etl import (
    extract_requests,
    extract_sla_policies,
    load_analytics,
    transform_agent_performance,
    transform_daily_volume,
    transform_department_workload,
    transform_sla_metrics,
)
from etl.validation import validate_and_split_requests, validate_and_split_sla_policies
from exceptions import DataValidationError, ETLBaseError
from logging_config import get_logger


logger = get_logger(__name__)


def _annotate_validation_failure(
    df: pd.DataFrame,
    error_message: str,
) -> pd.DataFrame:
    if df is None:
        return pd.DataFrame(columns=["quarantine_reason", "validation_error"])

    invalid_df = df.copy()
    invalid_df["quarantine_reason"] = "dataset_validation_failed"
    invalid_df["validation_error"] = error_message
    return invalid_df


def get_engine(database_url: Optional[str] = None) -> Engine:
    """
    Create and return a SQLAlchemy engine.

    The database URL can be overridden for testing.
    """
    url = database_url or DATABASE_URL
    return create_engine(url)


def run_pipeline(database_url: Optional[str] = None) -> None:
    """
    Run the end-to-end ETL pipeline for ServiceHub analytics.
    """
    logger.info("Starting ServiceHub ETL pipeline")

    engine = get_engine(database_url=database_url)

    try:
        requests_df = extract_requests(engine)
        sla_df = extract_sla_policies(engine)

        try:
            valid_requests, invalid_requests = validate_and_split_requests(requests_df)
        except DataValidationError as exc:
            logger.error("Request validation failed: %s", exc)
            valid_requests = pd.DataFrame()
            invalid_requests = _annotate_validation_failure(requests_df, str(exc))

        try:
            valid_sla, invalid_sla = validate_and_split_sla_policies(sla_df)
        except DataValidationError as exc:
            logger.error("SLA policy validation failed: %s", exc)
            valid_sla = pd.DataFrame()
            invalid_sla = _annotate_validation_failure(sla_df, str(exc))

        # Quarantine invalid rows so that bad data does not block analytics
        try:
            if not invalid_requests.empty:
                load_analytics(invalid_requests, "analytics_invalid_requests", engine)
            if not invalid_sla.empty:
                load_analytics(invalid_sla, "analytics_invalid_sla_policies", engine)
        except ETLBaseError:
            # If quarantine loading fails, log and continue with the main pipeline
            logger.exception("Failed to load quarantined data; continuing with valid data only.")

        sla_metrics = transform_sla_metrics(valid_requests, valid_sla)
        load_analytics(sla_metrics, "analytics_sla_metrics", engine)

        daily_volume = transform_daily_volume(valid_requests)
        load_analytics(daily_volume, "analytics_daily_volume", engine)

        agent_performance = transform_agent_performance(valid_requests)
        load_analytics(agent_performance, "analytics_agent_performance", engine)

        department_workload = transform_department_workload(valid_requests)
        load_analytics(
            department_workload,
            "analytics_department_workload",
            engine,
        )

        logger.info("ServiceHub ETL pipeline completed successfully")
    except ETLBaseError:
        logger.exception("ServiceHub ETL pipeline failed due to an ETL error")
        raise
    except Exception as exc:
        logger.exception("ServiceHub ETL pipeline failed due to an unexpected error: %s", exc)
        raise


if __name__ == "__main__":
    run_pipeline()
