"""
Transformation layer for ServiceHub analytics ETL.

Inputs to this layer are expected to have been validated already. Row-level
validation and quarantining are handled in the validation module.
"""

from datetime import datetime, timezone

import pandas as pd

from logging_config import get_logger


logger = get_logger(__name__)


def transform_sla_metrics(requests_df: pd.DataFrame, sla_df: pd.DataFrame) -> pd.DataFrame:
    """
    Calculate SLA compliance metrics per category and priority based on
    the analytics_sla_metrics contract.
    """
    if requests_df.empty or sla_df.empty:
        logger.info("No data available for SLA metrics. Skipping.")
        return pd.DataFrame()

    working = requests_df.copy()
    working["created_at"] = pd.to_datetime(working["created_at"])
    working["resolved_at"] = pd.to_datetime(working["resolved_at"])
    working["first_response_at"] = pd.to_datetime(
        working["first_response_at"], errors="coerce"
    )

    now_ts = datetime.now(timezone.utc)

    # Total tickets per (category, priority)
    group = working.groupby(["category", "priority"], as_index=True)
    total_tickets = group["id"].count().rename("total_tickets")

    # Resolved tickets (RESOLVED or CLOSED)
    resolved_mask = working["status"].isin(["RESOLVED", "CLOSED"])
    resolved = working[resolved_mask].copy()
    if resolved.empty:
        logger.info("No resolved requests available for SLA metrics. Skipping.")
        return pd.DataFrame()

    resolved["resolution_hours"] = (
        resolved["resolved_at"] - resolved["created_at"]
    ).dt.total_seconds() / 3600

    resolved_group = resolved.groupby(["category", "priority"], as_index=True)
    resolved_tickets = resolved_group["id"].count().rename("resolved_tickets")
    breached_tickets = resolved_group["is_sla_breached"].sum().rename(
        "breached_tickets"
    )
    avg_resolution_hours = resolved_group["resolution_hours"].mean().rename(
        "avg_resolution_hours"
    )

    # Response times: consider only rows with a first_response_at timestamp
    responded = resolved[resolved["first_response_at"].notna()].copy()
    if not responded.empty:
        responded["response_hours"] = (
            responded["first_response_at"] - responded["created_at"]
        ).dt.total_seconds() / 3600
        responded_group = responded.groupby(["category", "priority"], as_index=True)
        avg_response_hours = responded_group["response_hours"].mean().rename(
            "avg_response_hours"
        )
    else:
        avg_response_hours = None

    summary = total_tickets.to_frame()
    summary = summary.join(resolved_tickets, how="left")
    summary = summary.join(breached_tickets, how="left")
    summary = summary.join(avg_resolution_hours, how="left")

    if avg_response_hours is not None:
        summary = summary.join(avg_response_hours, how="left")
    else:
        summary["avg_response_hours"] = pd.NA

    summary[["resolved_tickets", "breached_tickets"]] = summary[
        ["resolved_tickets", "breached_tickets"]
    ].fillna(0).astype(int)

    # Compliance rate = tickets resolved within SLA / resolved_tickets * 100
    mask = summary["resolved_tickets"] > 0
    summary["compliance_rate_pct"] = pd.NA
    summary.loc[mask, "compliance_rate_pct"] = (
        (summary.loc[mask, "resolved_tickets"] - summary.loc[mask, "breached_tickets"])
        / summary.loc[mask, "resolved_tickets"]
        * 100
    )

    summary["last_updated_at"] = now_ts

    summary = summary.reset_index()

    logger.info("Computed SLA metrics for %d category/priority groups", len(summary))
    return summary[
        [
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
    ]


def transform_daily_volume(requests_df: pd.DataFrame) -> pd.DataFrame:
    """
    Compute daily request volumes based on the analytics_daily_volume contract.
    """
    if requests_df.empty:
        logger.info("No service requests available for daily volume. Skipping.")
        return pd.DataFrame()

    working = requests_df.copy()
    working["created_at"] = pd.to_datetime(working["created_at"])
    working["report_date"] = working["created_at"].dt.date

    now_ts = datetime.now(timezone.utc)

    result = (
        working.groupby(
            ["report_date", "category", "priority", "status"], as_index=False
        )["id"]
        .count()
        .rename(columns={"id": "ticket_count"})
    )

    result["last_updated_at"] = now_ts

    logger.info(
        "Computed daily volume for %d report/category/priority/status groups", len(result)
    )
    return result[
        [
            "report_date",
            "category",
            "priority",
            "status",
            "ticket_count",
            "last_updated_at",
        ]
    ]


