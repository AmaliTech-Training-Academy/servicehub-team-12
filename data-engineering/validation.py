from typing import Iterable, List

import pandas as pd

from exceptions import DataValidationError


REQUIRED_REQUEST_COLUMNS: List[str] = [
    "id",
    "title",
    "category",
    "priority",
    "status",
    "created_at",
    "resolved_at",
]

ALLOWED_CATEGORIES: Iterable[str] = {"IT_SUPPORT", "FACILITIES", "HR_REQUEST"}
ALLOWED_PRIORITIES: Iterable[str] = {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
ALLOWED_STATUSES: Iterable[str] = {
    "OPEN",
    "ASSIGNED",
    "IN_PROGRESS",
    "RESOLVED",
    "CLOSED",
}


def _ensure_required_columns(df: pd.DataFrame, required: Iterable[str], frame_name: str) -> None:
    missing = [col for col in required if col not in df.columns]
    if missing:
        raise DataValidationError(
            f"{frame_name} is missing required columns: {', '.join(missing)}"
        )


def validate_requests_df(df: pd.DataFrame) -> pd.DataFrame:
    """
    Validate the service requests dataframe.

    Ensures required columns are present, categorical values are within expected
    domains, and timestamps are well formed where applicable.
    """
    if df is None:
        raise DataValidationError("requests dataframe is None")

    _ensure_required_columns(df, REQUIRED_REQUEST_COLUMNS, "requests_df")

    invalid_categories = set(df["category"].dropna().unique()) - set(ALLOWED_CATEGORIES)
    if invalid_categories:
        raise DataValidationError(
            f"Found invalid categories: {', '.join(sorted(invalid_categories))}"
        )

    invalid_priorities = set(df["priority"].dropna().unique()) - set(ALLOWED_PRIORITIES)
    if invalid_priorities:
        raise DataValidationError(
            f"Found invalid priorities: {', '.join(sorted(invalid_priorities))}"
        )

    invalid_statuses = set(df["status"].dropna().unique()) - set(ALLOWED_STATUSES)
    if invalid_statuses:
        raise DataValidationError(
            f"Found invalid statuses: {', '.join(sorted(invalid_statuses))}"
        )

    # Timestamp sanity checks
    timestamps = ["created_at", "resolved_at"]
    for ts_col in timestamps:
        if ts_col in df.columns:
            try:
                pd.to_datetime(df[ts_col], errors="raise")
            except (TypeError, ValueError) as exc:
                raise DataValidationError(
                    f"Column '{ts_col}' contains invalid datetime values"
                ) from exc

    if "resolved_at" in df.columns:
        created = pd.to_datetime(df["created_at"], errors="coerce")
        resolved = pd.to_datetime(df["resolved_at"], errors="coerce")
        invalid_order_mask = (resolved.notna()) & (created.notna()) & (resolved < created)
        if invalid_order_mask.any():
            raise DataValidationError(
                "Found records where resolved_at is earlier than created_at"
            )

    return df


def validate_sla_policies_df(df: pd.DataFrame) -> pd.DataFrame:
    """
    Basic validation for SLA policies dataframe.

    Assumes the table contains category and priority definitions that match the
    allowed domains used for requests.
    """
    if df is None:
        raise DataValidationError("sla_policies dataframe is None")

    required_columns = ["id", "category", "priority"]
    _ensure_required_columns(df, required_columns, "sla_policies_df")

    invalid_categories = set(df["category"].dropna().unique()) - set(ALLOWED_CATEGORIES)
    if invalid_categories:
        raise DataValidationError(
            f"SLA policies contain invalid categories: {', '.join(sorted(invalid_categories))}"
        )

    invalid_priorities = set(df["priority"].dropna().unique()) - set(ALLOWED_PRIORITIES)
    if invalid_priorities:
        raise DataValidationError(
            f"SLA policies contain invalid priorities: {', '.join(sorted(invalid_priorities))}"
        )

    return df

