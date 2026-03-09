import pandas as pd

from etl_pipeline import transform_daily_volume, transform_sla_metrics


def _requests_for_transformations() -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "id": 1,
                "title": "Reset password",
                "category": "IT_SUPPORT",
                "priority": "HIGH",
                "status": "RESOLVED",
                "created_at": "2024-01-01T10:00:00Z",
                "resolved_at": "2024-01-01T11:00:00Z",
            },
            {
                "id": 2,
                "title": "Office light not working",
                "category": "FACILITIES",
                "priority": "LOW",
                "status": "RESOLVED",
                "created_at": "2024-01-01T09:00:00Z",
                "resolved_at": "2024-01-01T18:00:00Z",
            },
        ]
    )


def _sla_policies() -> pd.DataFrame:
    return pd.DataFrame(
        [
            {"id": 1, "category": "IT_SUPPORT", "priority": "HIGH"},
            {"id": 2, "category": "FACILITIES", "priority": "LOW"},
        ]
    )


def test_transform_sla_metrics_computes_expected_columns():
    requests_df = _requests_for_transformations()
    sla_df = _sla_policies()

    result = transform_sla_metrics(requests_df, sla_df)

    assert not result.empty
    assert {"category", "priority", "total_resolved", "avg_resolution_hours", "max_resolution_hours"}.issubset(
        set(result.columns)
    )


def test_transform_daily_volume_computes_request_counts():
    requests_df = _requests_for_transformations()

    result = transform_daily_volume(requests_df)

    assert not result.empty
    assert {"date", "category", "request_count"}.issubset(set(result.columns))
    assert result["request_count"].sum() == len(requests_df)

