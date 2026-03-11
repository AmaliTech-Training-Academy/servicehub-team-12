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
                "id": "1",
                "title": "Reset password",
                "category": "IT_SUPPORT",
                "priority": "HIGH",
                "status": "OPEN",
                "requester_id": "requester-1",
                "assigned_to_id": None,
                "department_id": None,
                "sla_deadline": "2024-01-01T14:00:00Z",
                "first_response_at": None,
                "is_sla_breached": False,
                "created_at": "2024-01-01T10:00:00Z",
                "updated_at": "2024-01-01T10:15:00Z",
                "resolved_at": None,
                "closed_at": None,
            }
        ]
    )


def _valid_sla_policies_frame() -> pd.DataFrame:
    rows = []
    for category in sorted(ALLOWED_CATEGORIES):
        for priority in sorted(ALLOWED_PRIORITIES):
            rows.append(
                {
                    "id": f"{category}-{priority}",
                    "category": category,
                    "priority": priority,
                    "response_time_hours": 4,
                    "resolution_time_hours": 24,
                }
            )
    return pd.DataFrame(rows)


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
    assert invalid.iloc[0]["quarantine_reason"] == "invalid_category"


def test_validate_and_split_requests_invalid_priority_quarantines():
    df = _base_requests_frame()
    df.loc[0, "priority"] = "P1"

    valid, invalid = validate_and_split_requests(df)

    assert valid.empty
    assert len(invalid) == 1
    assert invalid.iloc[0]["quarantine_reason"] == "invalid_priority"


def test_validate_and_split_requests_invalid_status_quarantines():
    df = _base_requests_frame()
    df.loc[0, "status"] = "WAITING"

    valid, invalid = validate_and_split_requests(df)

    assert valid.empty
    assert len(invalid) == 1
    assert invalid.iloc[0]["quarantine_reason"] == "invalid_status"


def test_validate_and_split_requests_missing_requester_id_quarantines():
    df = _base_requests_frame()
    df.loc[0, "requester_id"] = None

    valid, invalid = validate_and_split_requests(df)

    assert valid.empty
    assert len(invalid) == 1
    assert invalid.iloc[0]["quarantine_reason"] == "null_in_required_field"


def test_validate_and_split_requests_invalid_timestamp_order_quarantines():
    df = _base_requests_frame()
    df.loc[0, "status"] = "RESOLVED"
    df.loc[0, "resolved_at"] = "2023-12-31T12:00:00Z"

    valid, invalid = validate_and_split_requests(df)

    assert valid.empty
    assert len(invalid) == 1
    assert invalid.iloc[0]["quarantine_reason"] == "resolved_at_before_created_at"


def test_validate_and_split_requests_first_response_before_created_quarantines():
    df = _base_requests_frame()
    df.loc[0, "first_response_at"] = "2024-01-01T09:00:00Z"

    valid, invalid = validate_and_split_requests(df)

    assert valid.empty
    assert len(invalid) == 1
    assert (
        invalid.iloc[0]["quarantine_reason"]
        == "first_response_at_before_created_at"
    )


def test_validate_and_split_requests_closed_before_resolved_quarantines():
    df = _base_requests_frame()
    df.loc[0, "status"] = "CLOSED"
    df.loc[0, "assigned_to_id"] = "agent-1"
    df.loc[0, "department_id"] = "dept-1"
    df.loc[0, "resolved_at"] = "2024-01-01T12:00:00Z"
    df.loc[0, "closed_at"] = "2024-01-01T11:00:00Z"

    valid, invalid = validate_and_split_requests(df)

    assert valid.empty
    assert len(invalid) == 1
    assert invalid.iloc[0]["quarantine_reason"] == "closed_at_before_resolved_at"


def test_validate_and_split_requests_resolved_status_missing_resolved_at_quarantines():
    df = _base_requests_frame()
    df.loc[0, "status"] = "RESOLVED"
    df.loc[0, "assigned_to_id"] = "agent-1"
    df.loc[0, "department_id"] = "dept-1"

    valid, invalid = validate_and_split_requests(df)

    assert valid.empty
    assert len(invalid) == 1
    assert (
        invalid.iloc[0]["quarantine_reason"]
        == "resolved_status_missing_resolved_at"
    )


def test_validate_and_split_requests_open_status_with_resolved_at_quarantines():
    df = _base_requests_frame()
    df.loc[0, "resolved_at"] = "2024-01-01T12:00:00Z"

    valid, invalid = validate_and_split_requests(df)

    assert valid.empty
    assert len(invalid) == 1
    assert invalid.iloc[0]["quarantine_reason"] == "open_status_has_resolved_at"


def test_validate_and_split_sla_policies_happy_path():
    df = _valid_sla_policies_frame()

    valid, invalid = validate_and_split_sla_policies(df)

    assert len(valid) == len(df)
    assert invalid.empty


def test_validate_and_split_sla_policies_invalid_priority_quarantines():
    df = _valid_sla_policies_frame()
    duplicate = df.iloc[[0]].copy()
    df = pd.concat([df, duplicate], ignore_index=True)
    df.loc[len(df) - 1, "priority"] = "P1"

    valid, invalid = validate_and_split_sla_policies(df)

    assert len(valid) == len(_valid_sla_policies_frame())
    assert len(invalid) == 1
    assert invalid.iloc[0]["quarantine_reason"] == "invalid_priority"


def test_validate_and_split_sla_policies_invalid_response_hours_quarantines():
    df = _valid_sla_policies_frame()
    duplicate = df.iloc[[0]].copy()
    df = pd.concat([df, duplicate], ignore_index=True)
    df.loc[len(df) - 1, "response_time_hours"] = 0

    valid, invalid = validate_and_split_sla_policies(df)

    assert len(valid) == len(_valid_sla_policies_frame())
    assert len(invalid) == 1
    assert invalid.iloc[0]["quarantine_reason"] == "invalid_response_time_hours"


def test_validate_and_split_sla_policies_invalid_resolution_hours_quarantines():
    df = _valid_sla_policies_frame()
    duplicate = df.iloc[[0]].copy()
    df = pd.concat([df, duplicate], ignore_index=True)
    df.loc[len(df) - 1, "resolution_time_hours"] = 0

    valid, invalid = validate_and_split_sla_policies(df)

    assert len(valid) == len(_valid_sla_policies_frame())
    assert len(invalid) == 1
    assert invalid.iloc[0]["quarantine_reason"] == "invalid_resolution_time_hours"


def test_validate_and_split_sla_policies_missing_required_combinations_raises():
    df = _valid_sla_policies_frame().iloc[:-1].copy()

    with pytest.raises(DataValidationError) as exc:
        validate_and_split_sla_policies(df)

    assert "missing required category/priority combinations" in str(exc.value)
