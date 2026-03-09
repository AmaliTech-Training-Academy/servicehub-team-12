import pandas as pd

from sample_data.generator import SAMPLE_CATEGORIES, PRIORITIES, SampleConfig, generate_sample_requests


def test_generate_sample_requests_respects_configuration():
    config = SampleConfig(num_requests=50, days_back=7)
    df = generate_sample_requests(config)

    assert len(df) == 50
    assert set(df["category"].unique()).issubset(set(SAMPLE_CATEGORIES))
    assert set(df["priority"].unique()).issubset(set(PRIORITIES))

    # Ensure timestamps look sensible
    created_at = pd.to_datetime(df["created_at"])
    assert (created_at.max() - created_at.min()).days <= 7

