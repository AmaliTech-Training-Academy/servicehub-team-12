import pandas as pd

from etl.transform import transform_daily_volume, transform_sla_metrics


def _requests_for_transformations() -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "id": 1,
                "title": "Reset password",
                "category": "IT",
                "priority": "HIGH",
                "status": "RESOLVED",
                "sla_deadline": "2024-01-01T14:00:00Z",
                "first_response_at": "2024-01-01T10:30:00Z",
                "is_sla_breached": False,
                "created_at": "2024-01-01T10:00:00Z",
                "resolved_at": "2024-01-01T11:00:00Z",
            },
            {
                "id": 2,
                "title": "Office light not working",
                "category": "FACILITIES",
                "priority": "LOW",
                "status": "CLOSED",
                "sla_deadline": "2024-01-02T09:00:00Z",
                "first_response_at": "2024-01-01T10:00:00Z",
                "is_sla_breached": True,
                "created_at": "2024-01-01T09:00:00Z",
                "resolved_at": "2024-01-01T18:00:00Z",
            },
        ]
    )


def _sla_policies() -> pd.DataFrame:
    return pd.DataFrame(
        [
            {"id": 1, "category": "IT", "priority": "HIGH"},
            {"id": 2, "category": "FACILITIES", "priority": "LOW"},
        ]
    )


def test_transform_sla_metrics_computes_expected_columns():
    requests_df = _requests_for_transformations()
    sla_df = _sla_policies()

    result = transform_sla_metrics(requests_df, sla_df)

    assert not result.empty
    expected_columns = {
        "category",
        "priority",
        "total_tickets",
        "resolved_tickets",
        "breached_tickets",
        "compliance_rate_pct",
        "avg_resolution_hours",
        "avg_response_hours",
        "last_updated_at",
    }
    assert expected_columns.issubset(set(result.columns))


def test_transform_daily_volume_computes_request_counts():
    requests_df = _requests_for_transformations()

    result = transform_daily_volume(requests_df)

    assert not result.empty
    expected_columns = {
        "report_date",
        "category",
        "priority",
        "status",
        "ticket_count",
        "last_updated_at",
    }
    assert expected_columns.issubset(set(result.columns))
    assert result["ticket_count"].sum() == len(requests_df)

