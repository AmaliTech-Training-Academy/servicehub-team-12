"""ETL Pipeline for ServiceHub - SLA Analytics and Resolution Metrics.

This module provides the orchestration layer. Extraction, transformation,
and loading logic live in the dedicated `etl` package.
"""

from typing import Optional

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine

from config import DATABASE_URL
from etl import (
    extract_requests,
    extract_sla_policies,
    load_analytics,
    transform_daily_volume,
    transform_sla_metrics,
)
from exceptions import ETLBaseError
from logging_config import get_logger


logger = get_logger(__name__)


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

        sla_metrics = transform_sla_metrics(requests_df, sla_df)
        load_analytics(sla_metrics, "analytics_sla_metrics", engine)

        daily_volume = transform_daily_volume(requests_df)
        load_analytics(daily_volume, "analytics_daily_volume", engine)

        # Future extensions:
        # - SLA breach detection
        # - Agent performance metrics
        # - Department workload analysis

        logger.info("ServiceHub ETL pipeline completed successfully")
    except ETLBaseError:
        logger.exception("ServiceHub ETL pipeline failed due to an ETL error")
        raise
    except Exception as exc:
        logger.exception("ServiceHub ETL pipeline failed due to an unexpected error: %s", exc)
        raise


if __name__ == "__main__":
    run_pipeline()
