"""ETL Pipeline for ServiceHub - SLA Analytics and Resolution Metrics."""

from typing import Optional

import pandas as pd
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

from config import DATABASE_URL
from exceptions import (
    DataValidationError,
    ETLBaseError,
    ExtractionError,
    LoadError,
    TransformationError,
)
from logging_config import get_logger
from validation import validate_requests_df, validate_sla_policies_df


logger = get_logger(__name__)


def get_engine(database_url: Optional[str] = None) -> Engine:
    """
    Create and return a SQLAlchemy engine.

    The database URL can be overridden for testing.
    """
    url = database_url or DATABASE_URL
    return create_engine(url)


def extract_requests(engine: Engine) -> pd.DataFrame:
    """
    Extract service request records from the source database.
    """
    query = text(
        """
        SELECT sr.id,
               sr.title,
               sr.category,
               sr.priority,
               sr.status,
               sr.created_at,
               sr.updated_at,
               sr.resolved_at,
               u.name AS requester_name,
               d.name AS department_name
        FROM service_requests sr
        JOIN users u ON sr.requester_id = u.id
        LEFT JOIN departments d ON sr.department_id = d.id
        """
    )
    try:
        with engine.connect() as conn:
            df = pd.read_sql(query, conn)
            logger.info("Extracted %d service requests", len(df))
            return df
    except Exception as exc:
        logger.error("Failed to extract service requests: %s", exc)
        raise ExtractionError("Failed to extract service requests") from exc


def extract_sla_policies(engine: Engine) -> pd.DataFrame:
    """
    Extract SLA policies from the source database.
    """
    query = text("SELECT * FROM sla_policies")
    try:
        with engine.connect() as conn:
            df = pd.read_sql(query, conn)
            logger.info("Extracted %d SLA policies", len(df))
            return df
    except Exception as exc:
        logger.error("Failed to extract SLA policies: %s", exc)
        raise ExtractionError("Failed to extract SLA policies") from exc


def transform_sla_metrics(requests_df: pd.DataFrame, sla_df: pd.DataFrame) -> pd.DataFrame:
    """
    Calculate SLA compliance metrics per category and priority.
    """
    try:
        validated_requests = validate_requests_df(requests_df)
        validate_sla_policies_df(sla_df)
    except DataValidationError as exc:
        logger.error("Validation failed before SLA metrics transformation: %s", exc)
        raise TransformationError("Validation failed for SLA metrics transformation") from exc

    if validated_requests.empty:
        logger.info("No service requests available for SLA metrics. Skipping.")
        return pd.DataFrame()

    validated_requests["created_at"] = pd.to_datetime(validated_requests["created_at"])
    validated_requests["resolved_at"] = pd.to_datetime(validated_requests["resolved_at"])
    resolved = validated_requests[validated_requests["resolved_at"].notna()].copy()

    if resolved.empty:
        logger.info("No resolved requests available for SLA metrics. Skipping.")
        return pd.DataFrame()

    resolved["resolution_hours"] = (
        resolved["resolved_at"] - resolved["created_at"]
    ).dt.total_seconds() / 3600

    summary = (
        resolved.groupby(["category", "priority"])
        .agg(
            total_resolved=("id", "count"),
            avg_resolution_hours=("resolution_hours", "mean"),
            max_resolution_hours=("resolution_hours", "max"),
        )
        .reset_index()
    )

    logger.info("Computed SLA metrics for %d category/priority groups", len(summary))
    return summary


def transform_daily_volume(requests_df: pd.DataFrame) -> pd.DataFrame:
    """
    Compute daily request volumes by category.
    """
    try:
        validated_requests = validate_requests_df(requests_df)
    except DataValidationError as exc:
        logger.error("Validation failed before daily volume transformation: %s", exc)
        raise TransformationError("Validation failed for daily volume transformation") from exc

    if validated_requests.empty:
        logger.info("No service requests available for daily volume. Skipping.")
        return pd.DataFrame()

    validated_requests["date"] = pd.to_datetime(validated_requests["created_at"]).dt.date
    result = (
        validated_requests.groupby(["date", "category"])
        .size()
        .reset_index(name="request_count")
    )

    logger.info("Computed daily volume for %d date/category groups", len(result))
    return result


def load_analytics(df: pd.DataFrame, table_name: str, engine: Engine) -> None:
    """
    Load a dataframe into the analytics schema.
    """
    if df.empty:
        logger.info("No data to load into table '%s'. Skipping.", table_name)
        return

    try:
        df.to_sql(table_name, engine, if_exists="replace", index=False)
        logger.info("Loaded %d rows into table '%s'", len(df), table_name)
    except Exception as exc:
        logger.error("Failed to load analytics table '%s': %s", table_name, exc)
        raise LoadError(f"Failed to load analytics table '{table_name}'") from exc


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
