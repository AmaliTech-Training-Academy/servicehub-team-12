import os
from datetime import datetime, timedelta, timezone

from dotenv import load_dotenv

load_dotenv()

DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "port": os.getenv("DB_PORT"),
    "database": os.getenv("DB_NAME"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
}

DATABASE_URL = (
    "postgresql://"
    f"{DB_CONFIG['user']}:{DB_CONFIG['password']}"
    f"@{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}"
)

# Rolling window configuration (in days) for incremental ETL.
ETL_WINDOW_DAYS = int(os.getenv("ETL_WINDOW_DAYS", "7"))


def _default_since(days: int) -> str:
    """
    Compute an ISO-8601 UTC timestamp representing `now - days`.
    """
    now = datetime.now(timezone.utc)
    watermark = now - timedelta(days=days)
    return watermark.isoformat()


# Optional absolute watermarks for incremental ETL.
# If unset, a rolling window based on ETL_WINDOW_DAYS is applied.
ETL_REQUESTS_SINCE = os.getenv("ETL_REQUESTS_SINCE") or _default_since(ETL_WINDOW_DAYS)
ETL_DAILY_VOLUME_SINCE = os.getenv("ETL_DAILY_VOLUME_SINCE") or _default_since(
    ETL_WINDOW_DAYS
)

