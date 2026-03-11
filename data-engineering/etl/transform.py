"""
Transformation layer for ServiceHub analytics ETL.

Inputs to this layer are expected to have been validated already. Row-level
validation and quarantining are handled in the validation module.
"""

from datetime import datetime, timezone
from typing import Iterable

import pandas as pd

from logging_config import get_logger


logger = get_logger(__name__)

RESOLVED_STATUSES = {"RESOLVED", "CLOSED"}
OPEN_STATUSES = {"OPEN", "ASSIGNED", "IN_PROGRESS"}

SLA_METRICS_COLUMNS = [
    "category",
    "priority",
    "total_tickets",
    "resolved_tickets",
    "breached_tickets",
    "compliance_rate_pct",
    "avg_resolution_hours",
    "avg_response_hours",
    "last_updated_at",
]
DAILY_VOLUME_COLUMNS = [
    "report_date",
    "category",
    "priority",
    "status",
    "ticket_count",
    "last_updated_at",
]
AGENT_PERFORMANCE_COLUMNS = [
    "agent_id",
    "agent_name",
    "week_start",
    "tickets_assigned",
    "tickets_resolved",
    "avg_resolution_hours",
    "sla_compliance_rate_pct",
    "last_updated_at",
]
DEPARTMENT_WORKLOAD_COLUMNS = [
    "department_id",
    "department_name",
    "week_start",
    "open_tickets",
    "resolved_tickets",
    "breached_tickets",
    "avg_resolution_hours",
    "last_updated_at",
]


def _empty_result(columns: Iterable[str]) -> pd.DataFrame:
    return pd.DataFrame(columns=list(columns))


def _prepare_datetime_columns(df: pd.DataFrame, columns: Iterable[str]) -> pd.DataFrame:
    for column in columns:
        if column in df.columns:
            df[column] = pd.to_datetime(df[column], utc=True, errors="coerce")
    return df


def _duration_hours(start_times: pd.Series, end_times: pd.Series) -> pd.Series:
    return (end_times - start_times).dt.total_seconds() / 3600


def _week_start(series: pd.Series) -> pd.Series:
    normalized = pd.to_datetime(series, utc=True, errors="coerce")
    normalized = normalized.dt.tz_convert("UTC").dt.tz_localize(None)
    return normalized.dt.to_period("W").dt.start_time.dt.date


def transform_sla_metrics(
    requests_df: pd.DataFrame,
    sla_df: pd.DataFrame,
) -> pd.DataFrame:
    """
    Calculate SLA compliance metrics per category and priority based on
    the analytics_sla_metrics contract.
    """
    if requests_df is None or requests_df.empty:
        logger.warning("SLA metrics transform skipped: requests input is empty.")
        return _empty_result(SLA_METRICS_COLUMNS)

    if sla_df is None or sla_df.empty:
        logger.warning("SLA metrics transform skipped: SLA policy input is empty.")
        return _empty_result(SLA_METRICS_COLUMNS)

    working = _prepare_datetime_columns(
        requests_df.copy(),
        ["created_at", "resolved_at", "first_response_at", "sla_deadline"],
    )
    working["is_resolved"] = working["status"].isin(RESOLVED_STATUSES) & working[
        "resolved_at"
    ].notna()
    working["resolution_hours"] = _duration_hours(
        working["created_at"], working["resolved_at"]
    ).where(working["is_resolved"])
    working["response_hours"] = _duration_hours(
        working["created_at"], working["first_response_at"]
    ).where(working["first_response_at"].notna())
    working["resolved_within_sla"] = (
        working["is_resolved"]
        & working["sla_deadline"].notna()
        & (working["resolved_at"] <= working["sla_deadline"])
    )
    if "is_sla_breached" in working.columns:
        working["is_sla_breached"] = working["is_sla_breached"].fillna(False).astype(bool)
    else:
        working["is_sla_breached"] = False

    grouped = (
        working.groupby(["category", "priority"], dropna=False)
        .agg(
            total_tickets=("id", "count"),
            resolved_tickets=("is_resolved", "sum"),
            breached_tickets=("is_sla_breached", "sum"),
            avg_resolution_hours=("resolution_hours", "mean"),
            avg_response_hours=("response_hours", "mean"),
            resolved_within_sla=("resolved_within_sla", "sum"),
        )
        .reset_index()
    )

    policy_keys = sla_df[["category", "priority"]].drop_duplicates()
    result = policy_keys.merge(grouped, on=["category", "priority"], how="left")

    for count_column in (
        "total_tickets",
        "resolved_tickets",
        "breached_tickets",
        "resolved_within_sla",
    ):
        result[count_column] = result[count_column].fillna(0).astype(int)

    result["compliance_rate_pct"] = 0.0
    resolved_mask = result["resolved_tickets"] > 0
    result.loc[resolved_mask, "compliance_rate_pct"] = (
        result.loc[resolved_mask, "resolved_within_sla"]
        / result.loc[resolved_mask, "resolved_tickets"]
        * 100
    )
    result["avg_resolution_hours"] = result["avg_resolution_hours"].fillna(0.0).round(2)
    result["avg_response_hours"] = result["avg_response_hours"].fillna(0.0).round(2)
    result["compliance_rate_pct"] = result["compliance_rate_pct"].round(2)
    result["last_updated_at"] = datetime.now(timezone.utc)

    logger.info("Computed SLA metrics for %d category/priority groups", len(result))
    return result[SLA_METRICS_COLUMNS]


def transform_daily_volume(requests_df: pd.DataFrame) -> pd.DataFrame:
    """
    Compute daily request volumes based on the analytics_daily_volume contract.
    """
    if requests_df is None or requests_df.empty:
        logger.warning("Daily volume transform skipped: requests input is empty.")
        return _empty_result(DAILY_VOLUME_COLUMNS)

    working = _prepare_datetime_columns(requests_df.copy(), ["created_at"])
    working["report_date"] = working["created_at"].dt.date

    result = (
        working.groupby(["report_date", "category", "priority", "status"], dropna=False)
        .agg(ticket_count=("id", "count"))
        .reset_index()
    )
    result["ticket_count"] = result["ticket_count"].astype(int)
    result["last_updated_at"] = datetime.now(timezone.utc)

    logger.info(
        "Computed daily volume for %d report/category/priority/status groups", len(result)
    )
    return result[DAILY_VOLUME_COLUMNS]


def transform_agent_performance(requests_df: pd.DataFrame) -> pd.DataFrame:
    """
    Compute weekly ticket performance per assigned agent.
    """
    if requests_df is None or requests_df.empty:
        logger.warning("Agent performance transform skipped: requests input is empty.")
        return _empty_result(AGENT_PERFORMANCE_COLUMNS)

    if "assigned_to_id" not in requests_df.columns:
        logger.warning("Agent performance transform skipped: assigned_to_id is unavailable.")
        return _empty_result(AGENT_PERFORMANCE_COLUMNS)

    working = requests_df.copy()
    if "agent_name" not in working.columns:
        working["agent_name"] = pd.NA

    working = working[working["assigned_to_id"].notna()].copy()
    if working.empty:
        logger.warning("Agent performance transform skipped: no assigned requests found.")
        return _empty_result(AGENT_PERFORMANCE_COLUMNS)

    working = _prepare_datetime_columns(
        working,
        ["created_at", "resolved_at", "sla_deadline"],
    )
    working["week_start"] = _week_start(working["created_at"])
    working["is_resolved"] = working["status"].isin(RESOLVED_STATUSES) & working[
        "resolved_at"
    ].notna()
    working["resolved_within_sla"] = (
        working["is_resolved"]
        & working["sla_deadline"].notna()
        & (working["resolved_at"] <= working["sla_deadline"])
    )
    working["resolution_hours"] = _duration_hours(
        working["created_at"], working["resolved_at"]
    ).where(working["is_resolved"])

    result = (
        working.groupby(["assigned_to_id", "agent_name", "week_start"], dropna=False)
        .agg(
            tickets_assigned=("id", "count"),
            tickets_resolved=("is_resolved", "sum"),
            avg_resolution_hours=("resolution_hours", "mean"),
            resolved_within_sla=("resolved_within_sla", "sum"),
        )
        .reset_index()
        .rename(columns={"assigned_to_id": "agent_id"})
    )

    result["tickets_assigned"] = result["tickets_assigned"].astype(int)
    result["tickets_resolved"] = result["tickets_resolved"].astype(int)
    result["avg_resolution_hours"] = result["avg_resolution_hours"].fillna(0.0).round(2)
    result["sla_compliance_rate_pct"] = 0.0
    resolved_mask = result["tickets_resolved"] > 0
    result.loc[resolved_mask, "sla_compliance_rate_pct"] = (
        result.loc[resolved_mask, "resolved_within_sla"]
        / result.loc[resolved_mask, "tickets_resolved"]
        * 100
    )
    result["sla_compliance_rate_pct"] = result["sla_compliance_rate_pct"].round(2)
    result["last_updated_at"] = datetime.now(timezone.utc)

    logger.info("Computed agent performance for %d agent/week groups", len(result))
    return result.drop(columns=["resolved_within_sla"])[AGENT_PERFORMANCE_COLUMNS]


def transform_department_workload(requests_df: pd.DataFrame) -> pd.DataFrame:
    """
    Compute weekly workload metrics per department.
    """
    if requests_df is None or requests_df.empty:
        logger.warning("Department workload transform skipped: requests input is empty.")
        return _empty_result(DEPARTMENT_WORKLOAD_COLUMNS)

    if "department_id" not in requests_df.columns:
        logger.warning("Department workload transform skipped: department_id is unavailable.")
        return _empty_result(DEPARTMENT_WORKLOAD_COLUMNS)

    working = requests_df.copy()
    if "department_name" not in working.columns:
        working["department_name"] = pd.NA

    working = working[working["department_id"].notna()].copy()
    if working.empty:
        logger.warning("Department workload transform skipped: no departmental requests found.")
        return _empty_result(DEPARTMENT_WORKLOAD_COLUMNS)

    working = _prepare_datetime_columns(working, ["created_at", "resolved_at"])
    working["week_start"] = _week_start(working["created_at"])
    working["is_open"] = working["status"].isin(OPEN_STATUSES)
    working["is_resolved"] = working["status"].isin(RESOLVED_STATUSES) & working[
        "resolved_at"
    ].notna()
    working["resolution_hours"] = _duration_hours(
        working["created_at"], working["resolved_at"]
    ).where(working["is_resolved"])
    if "is_sla_breached" in working.columns:
        working["is_sla_breached"] = working["is_sla_breached"].fillna(False).astype(bool)
    else:
        working["is_sla_breached"] = False

    result = (
        working.groupby(["department_id", "department_name", "week_start"], dropna=False)
        .agg(
            open_tickets=("is_open", "sum"),
            resolved_tickets=("is_resolved", "sum"),
            breached_tickets=("is_sla_breached", "sum"),
            avg_resolution_hours=("resolution_hours", "mean"),
        )
        .reset_index()
    )

    result["open_tickets"] = result["open_tickets"].astype(int)
    result["resolved_tickets"] = result["resolved_tickets"].astype(int)
    result["breached_tickets"] = result["breached_tickets"].astype(int)
    result["avg_resolution_hours"] = result["avg_resolution_hours"].fillna(0.0).round(2)
    result["last_updated_at"] = datetime.now(timezone.utc)

    logger.info("Computed department workload for %d department/week groups", len(result))
    return result[DEPARTMENT_WORKLOAD_COLUMNS]
