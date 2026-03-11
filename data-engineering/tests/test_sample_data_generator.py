import pandas as pd

from sample_data.generator import (
    SERVICE_CATEGORIES,
    PRIORITIES,
    SampleConfig,
    generate_sample_requests,
)


def test_generate_sample_requests_respects_configuration():
    config = SampleConfig(num_requests=50, days_back=7)
    df = generate_sample_requests(config)

    assert len(df) == 50
    assert set(df["category"].unique()).issubset(set(SERVICE_CATEGORIES))
    assert set(df["priority"].unique()).issubset(set(PRIORITIES))
    assert df["requester_id"].notna().all()
    assert df["sla_deadline"].notna().all()
    assert df["updated_at"].notna().all()

    # Ensure timestamps look sensible
    created_at = pd.to_datetime(df["created_at"])
    assert (created_at.max() - created_at.min()).days <= 7

    resolved_mask = df["status"].isin(["RESOLVED", "CLOSED"])
    assert df.loc[resolved_mask, "resolved_at"].notna().all()

    closed_mask = df["status"].eq("CLOSED")
    assert df.loc[closed_mask, "closed_at"].notna().all()

    open_mask = df["status"].eq("OPEN")
    assert df.loc[open_mask, "department_id"].isna().all()
    assert df.loc[open_mask, "assigned_to_id"].isna().all()
