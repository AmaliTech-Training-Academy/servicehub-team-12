"""
Sample data generator for ServiceHub service requests and SLA policies.

The goal is to provide realistic-enough data for local development and testing
without introducing additional dependencies.
"""

from __future__ import annotations

import random
from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import List
from uuid import uuid4

import pandas as pd
from sqlalchemy import text
from sqlalchemy.engine import Engine

from logging_config import get_logger


logger = get_logger(__name__)


SERVICE_CATEGORIES = ["IT_SUPPORT", "FACILITIES", "HR_REQUEST"]
PRIORITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"]
STATUSES = ["OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"]


@dataclass
class SampleConfig:
    """
    Configuration for generated sample service requests.
    """

    num_requests: int = 200
    days_back: int = 60


def _resolution_hours_for(priority: str) -> float:
    """
    Return a plausible resolution time in hours for a given priority.
    """
    if priority == "CRITICAL":
        return random.uniform(1, 4)
    if priority == "HIGH":
        return random.uniform(2, 8)
    if priority == "MEDIUM":
        return random.uniform(4, 24)
    return random.uniform(8, 72)


def generate_sample_requests(config: SampleConfig | None = None) -> pd.DataFrame:
    """
    Generate a dataframe of synthetic service requests.
    """
    cfg = config or SampleConfig()
    now = datetime.utcnow()
    rows: List[dict] = []

    # Distributions derived from the data contract
    category_weights = [0.5, 0.25, 0.25]
    priority_weights = [0.10, 0.25, 0.45, 0.20]
    status_weights = [0.15, 0.10, 0.20, 0.40, 0.15]

    for i in range(cfg.num_requests):
        category = random.choices(SERVICE_CATEGORIES, weights=category_weights, k=1)[0]
        priority = random.choices(PRIORITIES, weights=priority_weights, k=1)[0]

        created_at = now - timedelta(
            days=random.randint(0, cfg.days_back),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59),
        )

        status = random.choices(STATUSES, weights=status_weights, k=1)[0]
        is_resolved = status in {"RESOLVED", "CLOSED"}

        if is_resolved:
            resolution_delta = timedelta(hours=_resolution_hours_for(priority))
            resolved_at = created_at + resolution_delta
        else:
            resolved_at = None

        # Approximate first_response_at and SLA breach flags
        if status in {"ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"}:
            # Response occurs shortly after creation for most tickets
            response_offset_hours = random.uniform(0.1, max(0.5, _resolution_hours_for(priority) / 2))
            first_response_at = created_at + timedelta(hours=response_offset_hours)
        else:
            first_response_at = None

        # Roughly 15% of resolved tickets should be marked as breached
        if is_resolved:
            is_sla_breached = random.random() < 0.15
        else:
            is_sla_breached = False

        rows.append(
            {
                # Generate UUIDs explicitly to align with the data contract.
                "id": uuid4(),
                "title": f"{category.title().replace('_', ' ')} request #{i + 1}",
                "category": category,
                "priority": priority,
                "status": status,
                "created_at": created_at,
                "updated_at": created_at if not is_resolved else resolved_at,
                "resolved_at": resolved_at,
                "first_response_at": first_response_at,
                "is_sla_breached": is_sla_breached,
            }
        )

    df = pd.DataFrame(rows)
    logger.info("Generated %d synthetic service requests.", len(df))
    return df


def ensure_sla_policies(engine: Engine) -> None:
    """
    Ensure that basic SLA policies exist for all category/priority combinations.

    This uses simple INSERT ... ON CONFLICT patterns where supported. If the
    target database does not support them, duplicate inserts may occur but are
    acceptable for development and testing environments.
    """
    with engine.begin() as conn:
        for category in SERVICE_CATEGORIES:
            for priority in PRIORITIES:
                conn.execute(
                    text(
                        """
                        INSERT INTO sla_policies (category, priority)
                        VALUES (:category, :priority)
                        ON CONFLICT (category, priority) DO NOTHING
                        """
                    ),
                    {"category": category, "priority": priority},
                )
    logger.info("Ensured SLA policies for all category/priority combinations.")


def load_sample_requests(engine: Engine, config: SampleConfig | None = None) -> None:
    """
    Generate and load sample service requests into the source database.

    The function relies on the database schema being compatible with the
    columns generated by `generate_sample_requests`. It is intended for
    development and testing only.
    """
    df = generate_sample_requests(config)
    if df.empty:
        logger.info("No sample requests generated; skipping load.")
        return

    ensure_sla_policies(engine)

    # Use append so repeated runs accumulate sample data.
    df.to_sql("service_requests", engine, if_exists="append", index=False)
    logger.info("Loaded %d sample service requests into 'service_requests'.", len(df))

