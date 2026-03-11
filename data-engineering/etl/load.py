"""
Loading layer for ServiceHub analytics ETL.
"""

import pandas as pd
from sqlalchemy.engine import Engine

from exceptions import LoadError
from logging_config import get_logger


logger = get_logger(__name__)


def load_analytics(df: pd.DataFrame, table_name: str, engine: Engine) -> None:
    """
    Load a dataframe into the analytics schema.
    """
    if df.empty:
        logger.info("No data to load into table '%s'. Skipping.", table_name)
        return

    try:
        df.to_sql(table_name, engine, if_exists="replace", index=False)
        logger.info("Loaded %d rows into table '%s'", len(df), table_name)
    except Exception as exc:
        logger.error("Failed to load analytics table '%s': %s", table_name, exc)
        raise LoadError(f"Failed to load analytics table '{table_name}'") from exc

