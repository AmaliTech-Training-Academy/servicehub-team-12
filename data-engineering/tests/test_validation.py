import pandas as pd
import pytest

from exceptions import DataValidationError
from validation import (
    ALLOWED_CATEGORIES,
    ALLOWED_PRIORITIES,
    ALLOWED_STATUSES,
    validate_requests_df,
    validate_sla_policies_df,
)


def _base_requests_frame() -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "id": 1,
                "title": "Reset password",
                "category": "IT_SUPPORT",
                "priority": "HIGH",
                "status": "OPEN",
                "created_at": "2024-01-01T10:00:00Z",
                "resolved_at": "2024-01-01T12:00:00Z",
            }
        ]
    )


def test_validate_requests_df_happy_path():
    df = _base_requests_frame()
    validated = validate_requests_df(df)
    assert not validated.empty
    assert set(validated["category"].unique()).issubset(ALLOWED_CATEGORIES)
    assert set(validated["priority"].unique()).issubset(ALLOWED_PRIORITIES)
    assert set(validated["status"].unique()).issubset(ALLOWED_STATUSES)


def test_validate_requests_df_missing_required_column_raises():
    df = _base_requests_frame().drop(columns=["title"])
    with pytest.raises(DataValidationError) as exc:
        validate_requests_df(df)
    assert "missing required columns" in str(exc.value)


def test_validate_requests_df_invalid_category_raises():
    df = _base_requests_frame()
    df.loc[0, "category"] = "UNKNOWN_CAT"
    with pytest.raises(DataValidationError) as exc:
        validate_requests_df(df)
    assert "invalid categories" in str(exc.value)


def test_validate_requests_df_invalid_priority_raises():
    df = _base_requests_frame()
    df.loc[0, "priority"] = "P1"
    with pytest.raises(DataValidationError) as exc:
        validate_requests_df(df)
    assert "invalid priorities" in str(exc.value)


def test_validate_requests_df_invalid_status_raises():
    df = _base_requests_frame()
    df.loc[0, "status"] = "WAITING"
    with pytest.raises(DataValidationError) as exc:
        validate_requests_df(df)
    assert "invalid statuses" in str(exc.value)


def test_validate_requests_df_invalid_timestamp_order_raises():
    df = _base_requests_frame()
    df.loc[0, "resolved_at"] = "2023-12-31T12:00:00Z"
    with pytest.raises(DataValidationError) as exc:
        validate_requests_df(df)
    assert "resolved_at is earlier than created_at" in str(exc.value)


def test_validate_sla_policies_df_happy_path():
    df = pd.DataFrame(
        [
            {
                "id": 1,
                "category": "IT_SUPPORT",
                "priority": "LOW",
            }
        ]
    )
    validated = validate_sla_policies_df(df)
    assert not validated.empty


def test_validate_sla_policies_df_invalid_priority_raises():
    df = pd.DataFrame(
        [
            {
                "id": 1,
                "category": "IT_SUPPORT",
                "priority": "P1",
            }
        ]
    )
    with pytest.raises(DataValidationError) as exc:
        validate_sla_policies_df(df)
    assert "invalid priorities" in str(exc.value)

