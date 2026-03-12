"""
Extraction layer for ServiceHub analytics ETL.
"""

from typing import Optional

import pandas as pd
from sqlalchemy import text
from sqlalchemy.engine import Engine

from config import ETL_DAILY_VOLUME_SINCE, ETL_REQUESTS_SINCE
from exceptions import ExtractionError
from logging_config import get_logger


logger = get_logger(__name__)


def extract_requests(engine: Engine, since: Optional[str] = None) -> pd.DataFrame:
    """
    Extract service request records from the source database.

    The selected columns align with the ServiceHub data contract and include
    SLA-related fields required for analytics.

    A simple incremental watermark can be provided via the `since` argument
    or the `ETL_REQUESTS_SINCE` configuration. When set, only rows with
    `updated_at` greater than the watermark are returned.
    """
    watermark = since or ETL_REQUESTS_SINCE

    where_clause = ""
    params = {}
    if watermark:
        where_clause = "WHERE sr.updated_at > :since_updated_at"
        params["since_updated_at"] = watermark

    query = text(
        f"""
        SELECT sr.id,
               sr.title,
               sr.description,
               sr.category,
               sr.priority,
               sr.status,
               sr.requester_id,
               sr.assigned_to_id,
               sr.department_id,
               sr.sla_deadline,
               sr.first_response_at,
               sr.is_sla_breached,
               sr.created_at,
               sr.updated_at,
               sr.resolved_at,
               sr.closed_at,
               requester.name AS requester_name,
               assignee.name AS agent_name,
               d.name AS department_name
        FROM service_requests sr
        JOIN users requester ON sr.requester_id = requester.id
        LEFT JOIN users assignee ON sr.assigned_to_id = assignee.id
        LEFT JOIN departments d ON sr.department_id = d.id
        {where_clause}
        """
    )
    try:
        with engine.connect() as conn:
            df = pd.read_sql(query, conn, params=params or None)
            # Ensure UUID columns are parquet-friendly
            for column in ("id", "requester_id", "assigned_to_id", "department_id"):
                if column in df.columns:
                    df[column] = df[column].astype("string")
            logger.info("Extracted %d service requests", len(df))
            return df
    except Exception as exc:
        logger.error("Failed to extract service requests: %s", exc)
        raise ExtractionError("Failed to extract service requests") from exc


def extract_sla_policies(engine: Engine) -> pd.DataFrame:
    """
    Extract SLA policies from the source database.
    """
    query = text("SELECT * FROM sla_policies")
    try:
        with engine.connect() as conn:
            df = pd.read_sql(query, conn)
            # Ensure UUID columns are parquet-friendly
            if "id" in df.columns:
                df["id"] = df["id"].astype("string")
            logger.info("Extracted %d SLA policies", len(df))
            return df
    except Exception as exc:
        logger.error("Failed to extract SLA policies: %s", exc)
        raise ExtractionError("Failed to extract SLA policies") from exc


def extract_daily_volume_aggregates(
    engine: Engine,
    since: Optional[str] = None,
) -> pd.DataFrame:
    """
    Extract pre-aggregated daily request volumes directly from the database.

    This reduces the amount of data that needs to be processed in pandas by
    pushing the heavy GROUP BY work down to PostgreSQL.
    """
    watermark = since or ETL_DAILY_VOLUME_SINCE

    where_clause = ""
    params = {}
    if watermark:
        where_clause = "WHERE sr.created_at >= :since_created_at"
        params["since_created_at"] = watermark

    query = text(
        f"""
        SELECT
            date(sr.created_at AT TIME ZONE 'UTC') AS report_date,
            sr.category,
            sr.priority,
            sr.status,
            COUNT(*) AS ticket_count
        FROM service_requests sr
        {where_clause}
        GROUP BY report_date, sr.category, sr.priority, sr.status
        """
    )

    try:
        with engine.connect() as conn:
            df = pd.read_sql(query, conn, params=params or None)
            logger.info(
                "Extracted %d daily volume aggregate rows from service_requests", len(df)
            )
            return df
    except Exception as exc:
        logger.error("Failed to extract daily volume aggregates: %s", exc)
        raise ExtractionError("Failed to extract daily volume aggregates") from exc

