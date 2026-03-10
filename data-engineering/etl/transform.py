"""
Transformation layer for ServiceHub analytics ETL.

Inputs to this layer are expected to have been validated already. Row-level
validation and quarantining are handled in the validation module.
"""

import pandas as pd

from logging_config import get_logger


logger = get_logger(__name__)


def transform_sla_metrics(requests_df: pd.DataFrame, sla_df: pd.DataFrame) -> pd.DataFrame:
    """
    Calculate SLA compliance metrics per category and priority based on
    already-validated inputs.
    """
    if requests_df.empty or sla_df.empty:
        logger.info("No data available for SLA metrics. Skipping.")
        return pd.DataFrame()

    requests_df = requests_df.copy()
    requests_df["created_at"] = pd.to_datetime(requests_df["created_at"])
    requests_df["resolved_at"] = pd.to_datetime(requests_df["resolved_at"])
    resolved = requests_df[requests_df["resolved_at"].notna()].copy()

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
    Compute daily request volumes by category based on already-validated inputs.
    """
    if requests_df.empty:
        logger.info("No service requests available for daily volume. Skipping.")
        return pd.DataFrame()

    working = requests_df.copy()
    working["date"] = pd.to_datetime(working["created_at"]).dt.date
    result = (
        working.groupby(["date", "category"])
        .size()
        .reset_index(name="request_count")
    )

    logger.info("Computed daily volume for %d date/category groups", len(result))
    return result

