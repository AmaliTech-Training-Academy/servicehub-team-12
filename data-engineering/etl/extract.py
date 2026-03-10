"""
Extraction layer for ServiceHub analytics ETL.
"""

from typing import Optional

import pandas as pd
from sqlalchemy import text
from sqlalchemy.engine import Engine

from exceptions import ExtractionError
from logging_config import get_logger


logger = get_logger(__name__)


def extract_requests(engine: Engine) -> pd.DataFrame:
    """
    Extract service request records from the source database.

    The selected columns align with the ServiceHub data contract and include
    SLA-related fields required for analytics.
    """
    query = text(
        """
        SELECT sr.id,
               sr.title,
               sr.description,
               sr.category,
               sr.priority,
               sr.status,
               sr.sla_deadline,
               sr.first_response_at,
               sr.is_sla_breached,
               sr.created_at,
               sr.updated_at,
               sr.resolved_at,
               u.full_name AS requester_name,
               d.name AS department_name
        FROM service_requests sr
        JOIN users u ON sr.requester_id = u.id
        LEFT JOIN departments d ON sr.department_id = d.id
        """
    )
    try:
        with engine.connect() as conn:
            df = pd.read_sql(query, conn)
            # Ensure UUID columns are parquet-friendly
            if "id" in df.columns:
                df["id"] = df["id"].astype(str)
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
                df["id"] = df["id"].astype(str)
            logger.info("Extracted %d SLA policies", len(df))
            return df
    except Exception as exc:
        logger.error("Failed to extract SLA policies: %s", exc)
        raise ExtractionError("Failed to extract SLA policies") from exc

