"""
Transformation layer for ServiceHub analytics ETL.
"""

import pandas as pd

from exceptions import DataValidationError, TransformationError
from logging_config import get_logger
from validation import validate_requests_df, validate_sla_policies_df


logger = get_logger(__name__)


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

