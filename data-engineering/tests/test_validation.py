import pandas as pd
import pytest

from exceptions import DataValidationError
from etl.validation import (
    ALLOWED_CATEGORIES,
    ALLOWED_PRIORITIES,
    ALLOWED_STATUSES,
    validate_and_split_requests,
    validate_and_split_sla_policies,
)


def _base_requests_frame() -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "id": 1,
                "title": "Reset password",
                "category": "IT",
                "priority": "HIGH",
                "status": "OPEN",
                "sla_deadline": "2024-01-01T14:00:00Z",
                "first_response_at": None,
                "is_sla_breached": False,
                "created_at": "2024-01-01T10:00:00Z",
                "resolved_at": "2024-01-01T12:00:00Z",
            }
        ]
    )


def test_validate_and_split_requests_happy_path():
    df = _base_requests_frame()
    valid, invalid = validate_and_split_requests(df)
    assert not valid.empty
    assert invalid.empty
    assert set(valid["category"].unique()).issubset(ALLOWED_CATEGORIES)
    assert set(valid["priority"].unique()).issubset(ALLOWED_PRIORITIES)
    assert set(valid["status"].unique()).issubset(ALLOWED_STATUSES)


def test_validate_and_split_requests_missing_required_column_raises():
    df = _base_requests_frame().drop(columns=["title"])
    with pytest.raises(DataValidationError) as exc:
        validate_and_split_requests(df)
    assert "missing required columns" in str(exc.value)


def test_validate_and_split_requests_invalid_category_quarantines():
    df = _base_requests_frame()
    df.loc[0, "category"] = "UNKNOWN_CAT"
    valid, invalid = validate_and_split_requests(df)
    assert valid.empty
    assert len(invalid) == 1


def test_validate_and_split_requests_invalid_priority_quarantines():
    df = _base_requests_frame()
    df.loc[0, "priority"] = "P1"
    valid, invalid = validate_and_split_requests(df)
    assert valid.empty
    assert len(invalid) == 1


def test_validate_and_split_requests_invalid_status_quarantines():
    df = _base_requests_frame()
    df.loc[0, "status"] = "WAITING"
    valid, invalid = validate_and_split_requests(df)
    assert valid.empty
    assert len(invalid) == 1


def test_validate_and_split_requests_invalid_timestamp_order_quarantines():
    df = _base_requests_frame()
    df.loc[0, "resolved_at"] = "2023-12-31T12:00:00Z"
    valid, invalid = validate_and_split_requests(df)
    assert valid.empty
    assert len(invalid) == 1


def test_validate_and_split_sla_policies_happy_path():
    df = pd.DataFrame(
        [
            {
                "id": 1,
                "category": "IT",
                "priority": "LOW",
            }
        ]
    )
    valid, invalid = validate_and_split_sla_policies(df)
    assert not valid.empty
    assert invalid.empty


def test_validate_and_split_sla_policies_invalid_priority_quarantines():
    df = pd.DataFrame(
        [
            {
                "id": 1,
                "category": "IT",
                "priority": "P1",
            }
        ]
    )
    valid, invalid = validate_and_split_sla_policies(df)
    assert valid.empty
    assert len(invalid) == 1
