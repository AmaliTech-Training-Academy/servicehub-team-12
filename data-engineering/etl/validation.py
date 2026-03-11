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
    "sla_deadline",
    "first_response_at",
    "is_sla_breached",
    "created_at",
    "resolved_at",
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


def _ensure_required_columns(
    df: pd.DataFrame,
    required: Iterable[str],
    frame_name: str,
) -> None:
    missing = [col for col in required if col not in df.columns]
    if missing:
        raise DataValidationError(
            f"{frame_name} is missing required columns: {', '.join(missing)}"
        )


def validate_and_split_requests(
    df: pd.DataFrame,
) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Validate the service requests dataframe and split into valid and
    invalid rows.

    Structural problems (e.g. missing required columns, non-parseable
    timestamps) raise DataValidationError. Row-level issues (invalid
    categories, priorities, statuses, timestamp ordering) do not break
    the pipeline. Such rows are quarantined and returned in the invalid
    dataframe.
    """
    if df is None:
        raise DataValidationError("requests dataframe is None")

    _ensure_required_columns(df, REQUIRED_REQUEST_COLUMNS, "requests_df")

    working = df.copy()

    invalid_mask = pd.Series(False, index=working.index)

    # Categorical domains
    invalid_categories_mask = ~working["category"].isin(ALLOWED_CATEGORIES)
    if invalid_categories_mask.any():
        invalid_mask |= invalid_categories_mask
        logger.warning(
            "Quarantining %d rows with invalid categories",
            invalid_categories_mask.sum(),
        )

    invalid_priorities_mask = ~working["priority"].isin(ALLOWED_PRIORITIES)
    if invalid_priorities_mask.any():
        invalid_mask |= invalid_priorities_mask
        logger.warning(
            "Quarantining %d rows with invalid priorities",
            invalid_priorities_mask.sum(),
        )

    invalid_statuses_mask = ~working["status"].isin(ALLOWED_STATUSES)
    if invalid_statuses_mask.any():
        invalid_mask |= invalid_statuses_mask
        logger.warning(
            "Quarantining %d rows with invalid statuses",
            invalid_statuses_mask.sum(),
        )

    # Timestamp sanity checks: hard failures if timestamps are not parseable
    # at all.
    timestamps = ["created_at", "resolved_at"]
    for ts_col in timestamps:
        if ts_col in working.columns:
            try:
                pd.to_datetime(working[ts_col], errors="raise")
            except (TypeError, ValueError) as exc:
                raise DataValidationError(
                    f"Column '{ts_col}' contains invalid datetime values"
                ) from exc

    created = pd.to_datetime(working["created_at"], errors="coerce")
    resolved = pd.to_datetime(working["resolved_at"], errors="coerce")
    invalid_order_mask = (
        resolved.notna()
        & created.notna()
        & (resolved < created)
    )
    if invalid_order_mask.any():
        invalid_mask |= invalid_order_mask
        logger.warning(
            "Quarantining %d rows where resolved_at is earlier than "
            "created_at",
            invalid_order_mask.sum(),
        )

    valid_df = working[~invalid_mask].copy()
    invalid_df = working[invalid_mask].copy()

    return valid_df, invalid_df


def validate_and_split_sla_policies(
    df: pd.DataFrame,
) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """
    Validate SLA policies and split into valid and invalid rows.

    Structural problems (e.g. missing required columns) raise
    DataValidationError. Row-level issues are quarantined and returned
    in the invalid dataframe.
    """
    if df is None:
        raise DataValidationError("sla_policies dataframe is None")

    required_columns = ["id", "category", "priority"]
    _ensure_required_columns(df, required_columns, "sla_policies_df")

    working = df.copy()
    invalid_mask = pd.Series(False, index=working.index)

    invalid_categories_mask = ~working["category"].isin(ALLOWED_CATEGORIES)
    if invalid_categories_mask.any():
        invalid_mask |= invalid_categories_mask
        logger.warning(
            "Quarantining %d SLA policy rows with invalid categories",
            invalid_categories_mask.sum(),
        )

    invalid_priorities_mask = ~working["priority"].isin(ALLOWED_PRIORITIES)
    if invalid_priorities_mask.any():
        invalid_mask |= invalid_priorities_mask
        logger.warning(
            "Quarantining %d SLA policy rows with invalid priorities",
            invalid_priorities_mask.sum(),
        )

    valid_df = working[~invalid_mask].copy()
    invalid_df = working[invalid_mask].copy()

    return valid_df, invalid_df
