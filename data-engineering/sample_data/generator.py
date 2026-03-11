"""
Sample data generator for ServiceHub source tables.

The generator seeds the transactional tables needed by the ETL:
- departments
- users
- sla_policies
- service_requests

It follows the data contract distributions while remaining tolerant of minor
schema drift between contract revisions and the current database tables.
"""

from __future__ import annotations

import random
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Iterable, List
from uuid import uuid4

import pandas as pd
from sqlalchemy import inspect, text
from sqlalchemy.engine import Engine

from logging_config import get_logger


logger = get_logger(__name__)


SERVICE_CATEGORIES = ["IT_SUPPORT", "FACILITIES", "HR_REQUEST"]
PRIORITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"]
STATUSES = ["OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"]

DEPARTMENT_SPECS = {
    "IT_SUPPORT": {
        "name": "IT Support",
        "description": "Handles incidents related to software, hardware, and access.",
        "contact_email": "it-support@servicehub.local",
    },
    "FACILITIES": {
        "name": "Facilities",
        "description": "Coordinates office facilities, security, and maintenance.",
        "contact_email": "facilities@servicehub.local",
    },
    "HR_REQUEST": {
        "name": "Human Resources",
        "description": "Manages payroll, leave, onboarding, and people operations queries.",
        "contact_email": "hr@servicehub.local",
    },
}

SLA_POLICY_RULES = {
    ("IT_SUPPORT", "CRITICAL"): {"response_time_hours": 1, "resolution_time_hours": 4},
    ("IT_SUPPORT", "HIGH"): {"response_time_hours": 2, "resolution_time_hours": 8},
    ("IT_SUPPORT", "MEDIUM"): {"response_time_hours": 4, "resolution_time_hours": 24},
    ("IT_SUPPORT", "LOW"): {"response_time_hours": 8, "resolution_time_hours": 72},
    ("FACILITIES", "CRITICAL"): {"response_time_hours": 1, "resolution_time_hours": 8},
    ("FACILITIES", "HIGH"): {"response_time_hours": 2, "resolution_time_hours": 16},
    ("FACILITIES", "MEDIUM"): {"response_time_hours": 4, "resolution_time_hours": 48},
    ("FACILITIES", "LOW"): {"response_time_hours": 8, "resolution_time_hours": 96},
    ("HR_REQUEST", "CRITICAL"): {"response_time_hours": 2, "resolution_time_hours": 8},
    ("HR_REQUEST", "HIGH"): {"response_time_hours": 4, "resolution_time_hours": 24},
    ("HR_REQUEST", "MEDIUM"): {"response_time_hours": 8, "resolution_time_hours": 72},
    ("HR_REQUEST", "LOW"): {"response_time_hours": 24, "resolution_time_hours": 168},
}

AGENT_NAMES = [
    "Ama Mensah",
    "Kojo Asante",
    "Efua Boateng",
    "Nana Owusu",
    "Abena Ofori",
]
REQUESTER_NAMES = [
    "Kofi Addo",
    "Akosua Nyarko",
    "Yaw Appiah",
    "Mabel Tetteh",
    "Prince Frimpong",
    "Doreen Asiedu",
    "Emmanuel Sarpong",
    "Esi Quaye",
    "Michael Agyeman",
    "Ruth Antwi",
    "Daniel Lamptey",
    "Esther Osei",
    "Isaac Boadu",
    "Naa Dedei",
    "Patrick Kumi",
    "Vida Opoku",
    "Samuel Acheampong",
    "Josephine Arthur",
    "Bernice Lartey",
    "Kwame Agyei",
]
SAMPLE_PASSWORD_HASH = "$2b$12$gM2M6vHvxCcX/oHp1oD7UuHf0xP4Kx7Y8jQxvP7bNn2cN5tQnO8VO"

TITLE_TEMPLATES = {
    "IT_SUPPORT": [
        "Unable to access shared drive",
        "Laptop keeps restarting during meetings",
        "VPN connection fails on startup",
        "Email sync issue on mobile device",
    ],
    "FACILITIES": [
        "Air conditioning not working in meeting room",
        "Broken office chair needs replacement",
        "Cleaning request for pantry spill",
        "Access badge not opening side entrance",
    ],
    "HR_REQUEST": [
        "Payroll deduction clarification",
        "Leave balance appears incorrect",
        "Request for employment verification letter",
        "Question about onboarding documents",
    ],
}


@dataclass
class SampleConfig:
    """
    Configuration for generated sample data.
    """

    num_requests: int = 200
    days_back: int = 60
    num_agents: int = 5
    num_requesters: int = 20


def _utcnow() -> datetime:
    return datetime.now(timezone.utc).replace(microsecond=0)


def _table_columns(engine: Engine, table_name: str) -> Dict[str, Any]:
    return {column["name"]: column["type"] for column in inspect(engine).get_columns(table_name)}


def _filter_row_for_columns(row: Dict[str, Any], columns: Iterable[str]) -> Dict[str, Any]:
    return {key: value for key, value in row.items() if key in columns}


def _insert_row(conn: Any, table_name: str, row: Dict[str, Any]) -> Any:
    column_names = list(row.keys())
    columns_sql = ", ".join(column_names)
    values_sql = ", ".join(f":{name}" for name in column_names)
    query = text(f"INSERT INTO {table_name} ({columns_sql}) VALUES ({values_sql}) RETURNING id")
    return conn.execute(query, row).scalar_one()


def _build_local_departments() -> pd.DataFrame:
    now = _utcnow()
    rows = []
    for category, spec in DEPARTMENT_SPECS.items():
        rows.append(
            {
                "id": uuid4(),
                "category": category,
                "name": spec["name"],
                "description": spec["description"],
                "contact_email": spec["contact_email"],
                "is_active": True,
                "created_at": now,
            }
        )
    return pd.DataFrame(rows)


def _build_local_users(config: SampleConfig, departments_df: pd.DataFrame) -> pd.DataFrame:
    now = _utcnow()
    department_lookup = departments_df.set_index("category").to_dict("index")
    rows: List[Dict[str, Any]] = []

    agent_categories = [SERVICE_CATEGORIES[index % len(SERVICE_CATEGORIES)] for index in range(config.num_agents)]
    for index, category in enumerate(agent_categories):
        department = department_lookup[category]
        full_name = AGENT_NAMES[index % len(AGENT_NAMES)]
        rows.append(
            {
                "id": uuid4(),
                "email": f"agent{index + 1}@servicehub.local",
                "password": SAMPLE_PASSWORD_HASH,
                "full_name": full_name,
                "name": full_name,
                "role": "AGENT",
                "department_id": department["id"],
                "department": department["name"],
                "department_category": category,
                "is_active": True,
                "created_at": now,
                "updated_at": now,
            }
        )

    for index in range(config.num_requesters):
        full_name = REQUESTER_NAMES[index % len(REQUESTER_NAMES)]
        department = departments_df.iloc[index % len(departments_df)]
        rows.append(
            {
                "id": uuid4(),
                "email": f"user{index + 1}@servicehub.local",
                "password": SAMPLE_PASSWORD_HASH,
                "full_name": full_name,
                "name": full_name,
                "role": "USER",
                "department_id": department["id"],
                "department": department["name"],
                "department_category": department["category"],
                "is_active": True,
                "created_at": now,
                "updated_at": now,
            }
        )

    return pd.DataFrame(rows)


def _build_local_sla_policies() -> pd.DataFrame:
    now = _utcnow()
    rows = []
    for (category, priority), policy in SLA_POLICY_RULES.items():
        rows.append(
            {
                "id": uuid4(),
                "category": category,
                "priority": priority,
                "response_time_hours": policy["response_time_hours"],
                "resolution_time_hours": policy["resolution_time_hours"],
                "created_at": now,
            }
        )
    return pd.DataFrame(rows)


def _choose_title(category: str) -> str:
    return random.choice(TITLE_TEMPLATES[category])


def _choose_description(category: str, priority: str) -> str:
    if category == "IT_SUPPORT":
        return f"{priority.title()} ticket: employee reported a technology disruption requiring assistance."
    if category == "FACILITIES":
        return f"{priority.title()} facilities issue affecting the workplace and requiring follow-up."
    return f"{priority.title()} HR request raised by an employee and awaiting processing."


def _ensure_departments(engine: Engine) -> pd.DataFrame:
    columns = _table_columns(engine, "departments")
    now = _utcnow()
    rows = []
    lookup_column = "category" if "category" in columns else "name"

    with engine.begin() as conn:
        for category, spec in DEPARTMENT_SPECS.items():
            lookup_value = category if lookup_column == "category" else spec["name"]
            existing = conn.execute(
                text(f"SELECT id FROM departments WHERE {lookup_column} = :lookup LIMIT 1"),
                {"lookup": lookup_value},
            ).mappings().first()

            if existing is None:
                department_row = _filter_row_for_columns(
                    {
                        "name": spec["name"],
                        "description": spec["description"],
                        "category": category,
                        "contact_email": spec["contact_email"],
                        "is_active": True,
                        "created_at": now,
                    },
                    columns,
                )
                department_id = _insert_row(conn, "departments", department_row)
            else:
                department_id = existing["id"]

            rows.append(
                {
                    "id": department_id,
                    "category": category,
                    "name": spec["name"],
                    "contact_email": spec["contact_email"],
                }
            )

    return pd.DataFrame(rows)


def _ensure_users(engine: Engine, departments_df: pd.DataFrame, config: SampleConfig) -> pd.DataFrame:
    columns = _table_columns(engine, "users")
    local_users = _build_local_users(config, departments_df)
    rows = []

    with engine.begin() as conn:
        for user in local_users.to_dict("records"):
            existing = conn.execute(
                text("SELECT id FROM users WHERE email = :email LIMIT 1"),
                {"email": user["email"]},
            ).mappings().first()

            if existing is None:
                user_row = _filter_row_for_columns(
                    {
                        "email": user["email"],
                        "password": user["password"],
                        "full_name": user["full_name"],
                        "name": user["name"],
                        "role": user["role"],
                        "department_id": user["department_id"],
                        "department": user["department"],
                        "is_active": user["is_active"],
                        "created_at": user["created_at"],
                        "updated_at": user["updated_at"],
                    },
                    columns,
                )
                user_id = _insert_row(conn, "users", user_row)
            else:
                user_id = existing["id"]

            rows.append(
                {
                    "id": user_id,
                    "email": user["email"],
                    "role": user["role"],
                    "full_name": user["full_name"],
                    "name": user["name"],
                    "department_id": user["department_id"],
                    "department": user["department"],
                    "department_category": user["department_category"],
                }
            )

    return pd.DataFrame(rows)


def ensure_sla_policies(engine: Engine) -> pd.DataFrame:
    """
    Ensure SLA policies exist and reflect the current data contract.
    """
    columns = _table_columns(engine, "sla_policies")
    now = _utcnow()
    rows = []

    with engine.begin() as conn:
        for (category, priority), policy in SLA_POLICY_RULES.items():
            existing = conn.execute(
                text(
                    """
                    SELECT id
                    FROM sla_policies
                    WHERE category = :category AND priority = :priority
                    LIMIT 1
                    """
                ),
                {"category": category, "priority": priority},
            ).mappings().first()

            if existing is None:
                policy_row = _filter_row_for_columns(
                    {
                        "category": category,
                        "priority": priority,
                        "response_time_hours": policy["response_time_hours"],
                        "resolution_time_hours": policy["resolution_time_hours"],
                        "created_at": now,
                    },
                    columns,
                )
                policy_id = _insert_row(conn, "sla_policies", policy_row)
            else:
                policy_id = existing["id"]
                update_parts = []
                params: Dict[str, Any] = {
                    "id": policy_id,
                    "response_time_hours": policy["response_time_hours"],
                    "resolution_time_hours": policy["resolution_time_hours"],
                }
                if "response_time_hours" in columns:
                    update_parts.append("response_time_hours = :response_time_hours")
                if "resolution_time_hours" in columns:
                    update_parts.append("resolution_time_hours = :resolution_time_hours")
                if update_parts:
                    conn.execute(
                        text(
                            f"UPDATE sla_policies SET {', '.join(update_parts)} WHERE id = :id"
                        ),
                        params,
                    )

            rows.append(
                {
                    "id": policy_id,
                    "category": category,
                    "priority": priority,
                    "response_time_hours": policy["response_time_hours"],
                    "resolution_time_hours": policy["resolution_time_hours"],
                }
            )

    logger.info("Ensured SLA policies for all category/priority combinations.")
    return pd.DataFrame(rows)


def _build_local_reference_data(
    config: SampleConfig,
) -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    departments_df = _build_local_departments()
    users_df = _build_local_users(config, departments_df)
    sla_policies_df = _build_local_sla_policies()
    return departments_df, users_df, sla_policies_df


def generate_sample_requests(
    config: SampleConfig | None = None,
    departments_df: pd.DataFrame | None = None,
    users_df: pd.DataFrame | None = None,
    sla_policies_df: pd.DataFrame | None = None,
) -> pd.DataFrame:
    """
    Generate a dataframe of synthetic service requests aligned with the data contract.
    """
    cfg = config or SampleConfig()
    if departments_df is None or users_df is None or sla_policies_df is None:
        departments_df, users_df, sla_policies_df = _build_local_reference_data(cfg)

    now = _utcnow()
    rows: List[Dict[str, Any]] = []

    category_weights = [0.5, 0.25, 0.25]
    priority_weights = [0.10, 0.25, 0.45, 0.20]
    status_weights = [0.15, 0.10, 0.20, 0.40, 0.15]

    requesters_df = users_df[users_df["role"] == "USER"].reset_index(drop=True)
    agents_df = users_df[users_df["role"] == "AGENT"].reset_index(drop=True)
    departments_by_category = departments_df.set_index("category").to_dict("index")
    sla_lookup = sla_policies_df.set_index(["category", "priority"]).to_dict("index")

    for index in range(cfg.num_requests):
        category = random.choices(SERVICE_CATEGORIES, weights=category_weights, k=1)[0]
        priority = random.choices(PRIORITIES, weights=priority_weights, k=1)[0]
        status = random.choices(STATUSES, weights=status_weights, k=1)[0]

        created_at = now - timedelta(
            days=random.randint(0, cfg.days_back),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59),
            seconds=random.randint(0, 59),
        )

        department = departments_by_category[category]
        requester = requesters_df.sample(n=1).iloc[0]
        available_agents = agents_df[agents_df["department_category"] == category]
        assigned_agent = available_agents.sample(n=1).iloc[0]
        policy = sla_lookup[(category, priority)]

        response_hours = policy["response_time_hours"]
        resolution_hours = policy["resolution_time_hours"]
        response_deadline = created_at + timedelta(hours=response_hours)
        sla_deadline = created_at + timedelta(hours=resolution_hours)

        department_id = department["id"] if status != "OPEN" else None
        assigned_to_id = assigned_agent["id"] if status != "OPEN" else None

        first_response_at = None
        if status in {"IN_PROGRESS", "RESOLVED", "CLOSED"}:
            response_breached = random.random() < 0.10
            if response_breached:
                first_response_at = response_deadline + timedelta(
                    minutes=random.randint(15, 180)
                )
            else:
                first_response_at = created_at + timedelta(
                    minutes=random.randint(10, max(15, response_hours * 45))
                )
        elif status == "ASSIGNED" and random.random() < 0.35:
            first_response_at = created_at + timedelta(
                minutes=random.randint(10, max(15, response_hours * 30))
            )

        resolved_at = None
        closed_at = None
        is_sla_breached = False

        if status in {"RESOLVED", "CLOSED"}:
            is_sla_breached = random.random() < 0.15
            if is_sla_breached:
                resolved_at = sla_deadline + timedelta(
                    minutes=random.randint(30, max(60, resolution_hours * 30))
                )
            else:
                resolution_minutes = random.randint(
                    max(30, response_hours * 30),
                    max(60, int(resolution_hours * 60 * 0.9)),
                )
                resolved_at = created_at + timedelta(minutes=resolution_minutes)
                if resolved_at > sla_deadline:
                    resolved_at = sla_deadline - timedelta(minutes=random.randint(5, 30))

            if first_response_at is not None and resolved_at <= first_response_at:
                resolved_at = first_response_at + timedelta(minutes=random.randint(30, 180))

            if status == "CLOSED":
                close_delay = timedelta(hours=random.randint(1, 72))
                closed_at = min(now, resolved_at + close_delay)
        else:
            is_sla_breached = now > sla_deadline

        if status == "OPEN":
            updated_at = created_at
        elif status == "ASSIGNED":
            updated_at = first_response_at or (created_at + timedelta(minutes=15))
        elif status == "IN_PROGRESS":
            updated_at = max(
                first_response_at or created_at,
                created_at + timedelta(hours=random.uniform(1, 24)),
            )
            updated_at = min(updated_at, now)
        elif status == "RESOLVED":
            updated_at = resolved_at
        else:
            updated_at = closed_at or resolved_at

        rows.append(
            {
                "id": uuid4(),
                "title": _choose_title(category),
                "description": _choose_description(category, priority),
                "category": category,
                "priority": priority,
                "status": status,
                "department_id": department_id,
                "assigned_to_id": assigned_to_id,
                "requester_id": requester["id"],
                "sla_deadline": sla_deadline,
                "first_response_at": first_response_at,
                "resolved_at": resolved_at,
                "closed_at": closed_at,
                "is_sla_breached": is_sla_breached,
                "created_at": created_at,
                "updated_at": updated_at,
            }
        )

    df = pd.DataFrame(rows)
    logger.info("Generated %d synthetic service requests.", len(df))
    return df


def _service_request_insert_frame(engine: Engine, df: pd.DataFrame) -> pd.DataFrame:
    columns = _table_columns(engine, "service_requests")
    prepared = df.copy()

    if "id" in prepared.columns and "UUID" not in str(columns.get("id", "")).upper():
        prepared = prepared.drop(columns=["id"])

    return prepared[[column for column in prepared.columns if column in columns]]


def load_sample_requests(engine: Engine, config: SampleConfig | None = None) -> None:
    """
    Generate and load sample source data into the ServiceHub database.
    """
    cfg = config or SampleConfig()
    departments_df = _ensure_departments(engine)
    users_df = _ensure_users(engine, departments_df, cfg)
    sla_policies_df = ensure_sla_policies(engine)
    requests_df = generate_sample_requests(
        cfg,
        departments_df=departments_df,
        users_df=users_df,
        sla_policies_df=sla_policies_df,
    )

    if requests_df.empty:
        logger.info("No sample requests generated; skipping load.")
        return

    insert_df = _service_request_insert_frame(engine, requests_df)
    insert_df.to_sql("service_requests", engine, if_exists="append", index=False)
    logger.info("Loaded %d sample service requests into 'service_requests'.", len(insert_df))
