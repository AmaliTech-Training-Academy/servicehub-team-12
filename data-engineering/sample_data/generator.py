"""
Sample data generator for ServiceHub source tables.

The generator seeds the transactional tables needed by the ETL:
- departments
- users
- service_requests

It follows the data contract distributions while remaining tolerant of minor
schema drift between contract revisions and the current database tables.
"""

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

# Default SLA policy timings (hours) aligned with backend seed data.
# Keys are (category, priority) -> (response_time_hours,
# resolution_time_hours).
SLA_POLICY_DEFAULTS: dict[tuple[str, str], tuple[int, int]] = {
    # IT
    ("IT_SUPPORT", "CRITICAL"): (1, 4),
    ("IT_SUPPORT", "HIGH"): (2, 8),
    ("IT_SUPPORT", "MEDIUM"): (4, 24),
    ("IT_SUPPORT", "LOW"): (8, 48),
    # Facilities
    ("FACILITIES", "CRITICAL"): (1, 8),
    ("FACILITIES", "HIGH"): (2, 16),
    ("FACILITIES", "MEDIUM"): (4, 48),
    ("FACILITIES", "LOW"): (8, 96),
    # HR
    ("HR_REQUEST", "CRITICAL"): (2, 8),
    ("HR_REQUEST", "HIGH"): (4, 24),
    ("HR_REQUEST", "MEDIUM"): (8, 72),
    ("HR_REQUEST", "LOW"): (24, 168),
}

DEPARTMENT_SPECS = {
    "IT_SUPPORT": {
        "name": "IT Support",
        "description": (
            "Handles incidents related to software, hardware, and access."
        ),
        "contact_email": "it-support@servicehub.local",
    },
    "FACILITIES": {
        "name": "Facilities",
        "description": (
            "Coordinates office facilities, security, and maintenance."
        ),
        "contact_email": "facilities@servicehub.local",
    },
    "HR_REQUEST": {
        "name": "Human Resources",
        "description": (
            "Manages payroll, leave, onboarding, and people operations "
            "queries."
        ),
        "contact_email": "hr@servicehub.local",
    },
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
SAMPLE_PASSWORD_HASH = (
    "$2b$12$gM2M6vHvxCcX/oHp1oD7UuHf0xP4Kx7Y8jQxvP7bNn2cN5tQnO8VO"
)

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

    - num_requests: how many service_requests rows to generate.
    - days_back: spread created_at uniformly across this range.
    - num_agents / num_requesters: only used by in-memory/local test helpers.
    """

    num_requests: int = 200
    days_back: int = 60
    num_agents: int = 5
    num_requesters: int = 20


def _utcnow() -> datetime:
    """Return the current UTC time with microseconds stripped."""
    return datetime.now(timezone.utc).replace(microsecond=0)


def _table_columns(engine: Engine, table_name: str) -> Dict[str, Any]:
    """Return a mapping of column name -> type for the given table."""
    return {
        column["name"]: column["type"]
        for column in inspect(engine).get_columns(table_name)
    }


def _build_local_departments() -> pd.DataFrame:
    """
    Build an in-memory departments dataframe for tests-only usage.
    """
    now = _utcnow()
    rows = []
    for category, spec in DEPARTMENT_SPECS.items():
        rows.append(
            {
                "id": uuid4(),
                "category": category,
                "name": spec["name"],
                "description": spec["description"],
                "created_at": now,
            }
        )
    return pd.DataFrame(rows)


def _build_local_users(
    config: SampleConfig,
    departments_df: pd.DataFrame,
) -> pd.DataFrame:
    """
    Build an in-memory users dataframe (agents + requesters) for tests-only usage.
    """
    now = _utcnow()
    department_lookup = departments_df.set_index("category").to_dict("index")
    rows: List[Dict[str, Any]] = []

    agent_categories = [
        SERVICE_CATEGORIES[index % len(SERVICE_CATEGORIES)]
        for index in range(config.num_agents)
    ]
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


def _choose_title(category: str) -> str:
    """Pick a random human-readable title for the given category."""
    return random.choice(TITLE_TEMPLATES[category])


def _choose_description(category: str, priority: str) -> str:
    """Generate a simple description text for the given category/priority."""
    if category == "IT_SUPPORT":
        return (
            f"{priority.title()} ticket: employee reported a technology "
            "disruption requiring assistance."
        )
    if category == "FACILITIES":
        return (
            f"{priority.title()} facilities issue affecting the workplace "
            "and requiring follow-up."
        )
    return (
        f"{priority.title()} HR request raised by an employee and awaiting "
        "processing."
    )


def _ensure_departments(engine: Engine) -> pd.DataFrame:
    """
    Load existing departments seeded by the backend.

    This function assumes departments have already been created by the
    application migrations/seed scripts. It does not insert any new
    departments; it simply returns the current active ones with the
    columns needed by the generator.
    """
    columns = _table_columns(engine, "departments")
    required_columns = {"id", "name"}
    missing = required_columns - set(columns.keys())
    if missing:
        raise RuntimeError(
            f"departments table is missing required columns for sample generation: {missing}"
        )

    query = text(
        """
        SELECT id,
               name,
               category
        FROM departments        
        """
    )

    with engine.connect() as conn:
        df = pd.read_sql(query, conn)

    if df.empty:
        raise RuntimeError(
            "No active departments found. Backend seeds must be applied before "
            "running the sample data generator."
        )

    logger.info("Loaded %d departments from backend seed data.", len(df))
    return df


def _ensure_users(
    engine: Engine,
    departments_df: pd.DataFrame,
    config: SampleConfig,
) -> pd.DataFrame:
    """
    Load existing users seeded by the backend and join them with departments.

    The backend is responsible for creating both AGENT and USER accounts
    with appropriate roles and department assignments. This function does
    not create users; it reads them and prepares a dataframe with the
    columns required by the generator.
    """
    columns = _table_columns(engine, "users")
    required_columns = {"id", "email", "role"}
    missing = required_columns - set(columns.keys())
    if missing:
        raise RuntimeError(
            f"users table is missing required columns for sample generation: {missing}"
        )

    query = text(
        """
        SELECT u.id,
               u.email,
               u.role,
               COALESCE(u.full_name, u.name) AS full_name,
               COALESCE(u.name, u.full_name) AS name,
               u.department_id,
               d.name AS department,
               d.category AS department_category
        FROM users u
        LEFT JOIN departments d ON u.department_id = d.id
        WHERE u.is_active = TRUE
        """
    )

    with engine.connect() as conn:
        df = pd.read_sql(query, conn)

    if df.empty:
        raise RuntimeError(
            "No active users found. Backend seeds must be applied before "
            "running the sample data generator."
        )

    logger.info("Loaded %d users from backend seed data.", len(df))
    return df


def _build_local_reference_data(
    config: SampleConfig,
) -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    """
    Build local departments and users dataframes for tests that exercise the
    generator without a running database.
    """
    departments_df = _build_local_departments()
    users_df = _build_local_users(config, departments_df)
    return departments_df, users_df


def generate_sample_requests(
    config: SampleConfig | None = None,
    departments_df: pd.DataFrame | None = None,
    users_df: pd.DataFrame | None = None,
) -> pd.DataFrame:
    """
    Generate a dataframe of synthetic service requests.

    When `departments_df` and `users_df` are provided, they are assumed to
    come from the real database (back-end seeds). Otherwise, local in-memory
    reference data is constructed for use in tests.
    """
    cfg = config or SampleConfig()
    if departments_df is None or users_df is None:
        departments_df, users_df = _build_local_reference_data(cfg)

    now = _utcnow()
    rows: List[Dict[str, Any]] = []

    category_weights = [0.5, 0.25, 0.25]
    priority_weights = [0.10, 0.25, 0.45, 0.20]
    status_weights = [0.15, 0.10, 0.20, 0.40, 0.15]

    requesters_df = users_df[users_df["role"] == "USER"].reset_index(drop=True)
    agents_df = users_df[users_df["role"] == "AGENT"].reset_index(drop=True)
    departments_by_category = departments_df.set_index("category").to_dict(
        "index"
    )

    for index in range(cfg.num_requests):
        category = random.choices(
            SERVICE_CATEGORIES,
            weights=category_weights,
            k=1,
        )[0]
        priority = random.choices(
            PRIORITIES,
            weights=priority_weights,
            k=1,
        )[0]
        status = random.choices(
            STATUSES,
            weights=status_weights,
            k=1,
        )[0]

        created_at = now - timedelta(
            days=random.randint(0, cfg.days_back),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59),
            seconds=random.randint(0, 59),
        )

        department = departments_by_category[category]
        requester = requesters_df.sample(n=1).iloc[0]
        available_agents = agents_df[
            agents_df["department_category"] == category
        ]
        assigned_agent = available_agents.sample(n=1).iloc[0]

        response_hours, resolution_hours = SLA_POLICY_DEFAULTS.get(
            (category, priority),
            SLA_POLICY_DEFAULTS.get(
                ("IT_SUPPORT", priority),
                (4, 24),
            ),
        )
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
                    minutes=random.randint(
                        10,
                        max(15, response_hours * 45),
                    )
                )
        elif status == "ASSIGNED" and random.random() < 0.35:
            first_response_at = created_at + timedelta(
                minutes=random.randint(
                    10,
                    max(15, response_hours * 30),
                )
            )

        resolved_at = None
        closed_at = None
        is_sla_breached = False

        if status in {"RESOLVED", "CLOSED"}:
            is_sla_breached = random.random() < 0.15
            if is_sla_breached:
                resolved_at = sla_deadline + timedelta(
                    minutes=random.randint(
                        30,
                        max(60, resolution_hours * 30),
                    )
                )
            else:
                resolution_minutes = random.randint(
                    max(30, response_hours * 30),
                    max(60, int(resolution_hours * 60 * 0.9)),
                )
                resolved_at = created_at + timedelta(
                    minutes=resolution_minutes
                )
                if resolved_at > sla_deadline:
                    resolved_at = sla_deadline - timedelta(
                        minutes=random.randint(5, 30)
                    )

            if (
                first_response_at is not None
                and resolved_at <= first_response_at
            ):
                resolved_at = first_response_at + timedelta(
                    minutes=random.randint(30, 180)
                )

            if status == "CLOSED":
                close_delay = timedelta(hours=random.randint(1, 72))
                closed_at = min(now, resolved_at + close_delay)
        else:
            is_sla_breached = now > sla_deadline

        if status == "OPEN":
            updated_at = created_at
        elif status == "ASSIGNED":
            updated_at = first_response_at or (
                created_at + timedelta(minutes=15)
            )
        elif status == "IN_PROGRESS":
            updated_at = max(
                first_response_at or created_at,
                created_at + timedelta(
                    hours=random.uniform(1, 24),
                ),
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
    logger.info(
        "Generated %d synthetic service requests.",
        len(df),
    )
    return df


def _service_request_insert_frame(
    engine: Engine,
    df: pd.DataFrame,
) -> pd.DataFrame:
    """
    Project the generated dataframe down to the columns that exist in the
    current `service_requests` table, dropping any extras.
    """
    columns = _table_columns(engine, "service_requests")
    prepared = df.copy()

    if "id" in prepared.columns and "UUID" not in str(
        columns.get("id", "")
    ).upper():
        prepared = prepared.drop(columns=["id"])

    return prepared[
        [column for column in prepared.columns if column in columns]
    ]


def load_sample_requests(
    engine: Engine,
    config: SampleConfig | None = None,
) -> None:
    """
    Generate and load sample `service_requests` rows into the database.

    This function:
    - Reads existing departments and users seeded by the backend.
    - Generates realistic tickets across all categories and priorities.
    - Appends the generated rows to the `service_requests` table.
    """
    cfg = config or SampleConfig()
    departments_df = _ensure_departments(engine)
    users_df = _ensure_users(engine, departments_df, cfg)
    requests_df = generate_sample_requests(
        cfg,
        departments_df=departments_df,
        users_df=users_df,
    )

    if requests_df.empty:
        logger.info("No sample requests generated; skipping load.")
        return

    insert_df = _service_request_insert_frame(engine, requests_df)
    insert_df.to_sql(
        "service_requests",
        engine,
        if_exists="append",
        index=False,
    )
    logger.info(
        "Loaded %d sample service requests into 'service_requests'.",
        len(insert_df),
    )
