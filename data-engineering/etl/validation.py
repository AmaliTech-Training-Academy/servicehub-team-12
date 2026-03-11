from typing import Iterable, List, Tuple

import pandas as pd

from exceptions import DataValidationError
from logging_config import get_logger


logger = get_logger(__name__)


REQUIRED_REQUEST_COLUMNS: List[str] = [
    "id",
    "title",
    "category",
    "priority",
    "status",
    "requester_id",
    "sla_deadline",
    "created_at",
    "updated_at",
    "is_sla_breached",
]
OPTIONAL_REQUEST_COLUMNS: List[str] = [
    "first_response_at",
    "resolved_at",
    "closed_at",
    "assigned_to_id",
    "department_id",
    "department_name",
    "agent_name",
]
REQUIRED_SLA_COLUMNS: List[str] = [
    "id",
    "category",
    "priority",
    "response_time_hours",
    "resolution_time_hours",
]

ALLOWED_CATEGORIES: Iterable[str] = {"IT_SUPPORT", "FACILITIES", "HR_REQUEST"}
ALLOWED_PRIORITIES: Iterable[str] = {"CRITICAL", "HIGH", "MEDIUM", "LOW"}
ALLOWED_STATUSES: Iterable[str] = {
    "OPEN",
    "ASSIGNED",
    "IN_PROGRESS",
    "RESOLVED",
    "CLOSED",
}
EXPECTED_SLA_COMBINATIONS = {
    (category, priority)
    for category in ALLOWED_CATEGORIES
    for priority in ALLOWED_PRIORITIES
}
RESOLVED_STATUSES = {"RESOLVED", "CLOSED"}
NON_OPEN_STATUSES = {"ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"}


def _ensure_required_columns(
    df: pd.DataFrame,
    required: Iterable[str],
    frame_name: str,
) -> None:
    missing = [column for column in required if column not in df.columns]
    if missing:
        raise DataValidationError(
            f"{frame_name} is missing required columns: {', '.join(missing)}"
        )


def _append_quarantine(
    quarantine_rows: List[pd.DataFrame],
    rows: pd.DataFrame,
    reason: str,
) -> None:
    quarantined = rows.copy()
    quarantined["quarantine_reason"] = reason
    quarantine_rows.append(quarantined)


def _empty_invalid_frame(df: pd.DataFrame) -> pd.DataFrame:
    return pd.DataFrame(columns=[*df.columns.tolist(), "quarantine_reason"])


def validate_and_split_requests(df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Validate the service requests dataframe and split it into valid and invalid rows.

    Structural issues raise DataValidationError. Row-level data quality issues are
    quarantined and returned alongside the clean subset.
    """
    if df is None:
        raise DataValidationError("requests dataframe is None")

    _ensure_required_columns(df, REQUIRED_REQUEST_COLUMNS, "requests_df")

    working = df.copy()
    for column in OPTIONAL_REQUEST_COLUMNS:
        if column not in working.columns:
            working[column] = pd.NA

    for column in (
        "created_at",
        "updated_at",
        "resolved_at",
        "first_response_at",
        "sla_deadline",
        "closed_at",
    ):
        working[column] = pd.to_datetime(working[column], utc=True, errors="coerce")

    quarantine_rows: List[pd.DataFrame] = []

    invalid_categories = working[~working["category"].isin(ALLOWED_CATEGORIES)]
    if not invalid_categories.empty:
        logger.warning(
            "Quarantining %d rows with invalid categories: %s",
            len(invalid_categories),
            invalid_categories["category"].dropna().unique().tolist(),
        )
        _append_quarantine(quarantine_rows, invalid_categories, "invalid_category")
        working = working[working["category"].isin(ALLOWED_CATEGORIES)].copy()

    invalid_priorities = working[~working["priority"].isin(ALLOWED_PRIORITIES)]
    if not invalid_priorities.empty:
        logger.warning(
            "Quarantining %d rows with invalid priorities: %s",
            len(invalid_priorities),
            invalid_priorities["priority"].dropna().unique().tolist(),
        )
        _append_quarantine(quarantine_rows, invalid_priorities, "invalid_priority")
        working = working[working["priority"].isin(ALLOWED_PRIORITIES)].copy()

    invalid_statuses = working[~working["status"].isin(ALLOWED_STATUSES)]
    if not invalid_statuses.empty:
        logger.warning(
            "Quarantining %d rows with invalid statuses: %s",
            len(invalid_statuses),
            invalid_statuses["status"].dropna().unique().tolist(),
        )
        _append_quarantine(quarantine_rows, invalid_statuses, "invalid_status")
        working = working[working["status"].isin(ALLOWED_STATUSES)].copy()

    critical_null_mask = (
        working["id"].isna()
        | working["requester_id"].isna()
        | working["created_at"].isna()
        | working["updated_at"].isna()
    )
    null_critical_fields = working[critical_null_mask]
    if not null_critical_fields.empty:
        logger.warning(
            "Quarantining %d rows with NULL or invalid id, requester_id, created_at, or updated_at",
            len(null_critical_fields),
        )
        _append_quarantine(
            quarantine_rows,
            null_critical_fields,
            "null_in_required_field",
        )
        working = working[~critical_null_mask].copy()

    invalid_resolution_time = working[
        working["resolved_at"].notna() & (working["resolved_at"] < working["created_at"])
    ]
    if not invalid_resolution_time.empty:
        logger.warning(
            "Quarantining %d rows where resolved_at is earlier than created_at",
            len(invalid_resolution_time),
        )
        _append_quarantine(
            quarantine_rows,
            invalid_resolution_time,
            "resolved_at_before_created_at",
        )
        working = working[
            ~(working["resolved_at"].notna() & (working["resolved_at"] < working["created_at"]))
        ].copy()

    invalid_response_time = working[
        working["first_response_at"].notna()
        & (working["first_response_at"] < working["created_at"])
    ]
    if not invalid_response_time.empty:
        logger.warning(
            "Quarantining %d rows where first_response_at is earlier than created_at",
            len(invalid_response_time),
        )
        _append_quarantine(
            quarantine_rows,
            invalid_response_time,
            "first_response_at_before_created_at",
        )
        working = working[
            ~(
                working["first_response_at"].notna()
                & (working["first_response_at"] < working["created_at"])
            )
        ].copy()

    invalid_close_time = working[
        working["closed_at"].notna()
        & working["resolved_at"].notna()
        & (working["closed_at"] < working["resolved_at"])
    ]
    if not invalid_close_time.empty:
        logger.warning(
            "Quarantining %d rows where closed_at is earlier than resolved_at",
            len(invalid_close_time),
        )
        _append_quarantine(
            quarantine_rows,
            invalid_close_time,
            "closed_at_before_resolved_at",
        )
        working = working[
            ~(
                working["closed_at"].notna()
                & working["resolved_at"].notna()
                & (working["closed_at"] < working["resolved_at"])
            )
        ].copy()

    resolved_without_timestamp = working[
        working["status"].isin(RESOLVED_STATUSES) & working["resolved_at"].isna()
    ]
    if not resolved_without_timestamp.empty:
        logger.warning(
            "Quarantining %d RESOLVED/CLOSED rows missing resolved_at",
            len(resolved_without_timestamp),
        )
        _append_quarantine(
            quarantine_rows,
            resolved_without_timestamp,
            "resolved_status_missing_resolved_at",
        )
        working = working[
            ~(working["status"].isin(RESOLVED_STATUSES) & working["resolved_at"].isna())
        ].copy()

    open_with_resolution = working[
        working["status"].eq("OPEN") & working["resolved_at"].notna()
    ]
    if not open_with_resolution.empty:
        logger.warning(
            "Quarantining %d OPEN rows that already have resolved_at",
            len(open_with_resolution),
        )
        _append_quarantine(
            quarantine_rows,
            open_with_resolution,
            "open_status_has_resolved_at",
        )
        working = working[
            ~(working["status"].eq("OPEN") & working["resolved_at"].notna())
        ].copy()

    missing_agent = working[
        working["status"].isin(NON_OPEN_STATUSES) & working["assigned_to_id"].isna()
    ]
    if not missing_agent.empty:
        logger.warning(
            "Data warning: %d non-OPEN rows have no assigned_to_id",
            len(missing_agent),
        )

    missing_department = working[
        working["status"].isin(NON_OPEN_STATUSES) & working["department_id"].isna()
    ]
    if not missing_department.empty:
        logger.warning(
            "Data warning: %d non-OPEN rows have no department_id",
            len(missing_department),
        )

    actual_breach = working[
        working["resolved_at"].notna()
        & working["sla_deadline"].notna()
        & (working["resolved_at"] > working["sla_deadline"])
        & (~working["is_sla_breached"].fillna(False).astype(bool))
    ]
    if not actual_breach.empty:
        logger.warning(
            "Data warning: %d rows resolved after the SLA deadline but marked as not breached",
            len(actual_breach),
        )

    invalid_df = (
        pd.concat(quarantine_rows, ignore_index=True)
        if quarantine_rows
        else _empty_invalid_frame(working)
    )

    return working, invalid_df


def validate_and_split_sla_policies(df: pd.DataFrame) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Validate SLA policies and split them into valid and invalid rows.

    Structural issues and missing required category/priority coverage raise
    DataValidationError. Row-level invalid values are quarantined.
    """
    if df is None:
        raise DataValidationError("sla_policies dataframe is None")

    _ensure_required_columns(df, REQUIRED_SLA_COLUMNS, "sla_policies_df")

    working = df.copy()
    quarantine_rows: List[pd.DataFrame] = []

    invalid_categories = working[~working["category"].isin(ALLOWED_CATEGORIES)]
    if not invalid_categories.empty:
        logger.warning(
            "Quarantining %d SLA policy rows with invalid categories",
            len(invalid_categories),
        )
        _append_quarantine(quarantine_rows, invalid_categories, "invalid_category")
        working = working[working["category"].isin(ALLOWED_CATEGORIES)].copy()

    invalid_priorities = working[~working["priority"].isin(ALLOWED_PRIORITIES)]
    if not invalid_priorities.empty:
        logger.warning(
            "Quarantining %d SLA policy rows with invalid priorities",
            len(invalid_priorities),
        )
        _append_quarantine(quarantine_rows, invalid_priorities, "invalid_priority")
        working = working[working["priority"].isin(ALLOWED_PRIORITIES)].copy()

    invalid_response_hours = working[
        working["response_time_hours"].isna() | (working["response_time_hours"] <= 0)
    ]
    if not invalid_response_hours.empty:
        logger.warning(
            "Quarantining %d SLA policy rows with invalid response_time_hours",
            len(invalid_response_hours),
        )
        _append_quarantine(
            quarantine_rows,
            invalid_response_hours,
            "invalid_response_time_hours",
        )
        working = working[
            ~(working["response_time_hours"].isna() | (working["response_time_hours"] <= 0))
        ].copy()

    invalid_resolution_hours = working[
        working["resolution_time_hours"].isna()
        | (working["resolution_time_hours"] <= 0)
    ]
    if not invalid_resolution_hours.empty:
        logger.warning(
            "Quarantining %d SLA policy rows with invalid resolution_time_hours",
            len(invalid_resolution_hours),
        )
        _append_quarantine(
            quarantine_rows,
            invalid_resolution_hours,
            "invalid_resolution_time_hours",
        )
        working = working[
            ~(
                working["resolution_time_hours"].isna()
                | (working["resolution_time_hours"] <= 0)
            )
        ].copy()

    actual_combinations = set(zip(working["category"], working["priority"]))
    missing_combinations = EXPECTED_SLA_COMBINATIONS - actual_combinations
    if missing_combinations:
        missing_pairs = ", ".join(
            f"{category}/{priority}"
            for category, priority in sorted(missing_combinations)
        )
        raise DataValidationError(
            "sla_policies is missing required category/priority combinations: "
            f"{missing_pairs}"
        )

    invalid_df = (
        pd.concat(quarantine_rows, ignore_index=True)
        if quarantine_rows
        else _empty_invalid_frame(working)
    )

    return working, invalid_df
