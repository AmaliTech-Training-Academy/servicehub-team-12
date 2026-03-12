import pandas as pd

from etl.transform import (
    transform_agent_performance,
    transform_daily_volume,
    transform_department_workload,
    transform_sla_metrics,
)


def _requests_for_transformations() -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "id": "1",
                "title": "Reset password",
                "category": "IT_SUPPORT",
                "priority": "HIGH",
                "status": "RESOLVED",
                "requester_id": "requester-1",
                "assigned_to_id": "agent-1",
                "agent_name": "Ama Mensah",
                "department_id": "dept-1",
                "department_name": "IT Support",
                "sla_deadline": "2024-01-01T14:00:00Z",
                "first_response_at": "2024-01-01T10:30:00Z",
                "is_sla_breached": False,
                "created_at": "2024-01-01T10:00:00Z",
                "updated_at": "2024-01-01T11:00:00Z",
                "resolved_at": "2024-01-01T11:00:00Z",
                "closed_at": None,
            },
            {
                "id": "2",
                "title": "Office light not working",
                "category": "FACILITIES",
                "priority": "LOW",
                "status": "CLOSED",
                "requester_id": "requester-2",
                "assigned_to_id": "agent-2",
                "agent_name": "Kojo Asante",
                "department_id": "dept-2",
                "department_name": "Facilities",
                "sla_deadline": "2024-01-01T17:00:00Z",
                "first_response_at": "2024-01-01T10:00:00Z",
                "is_sla_breached": True,
                "created_at": "2024-01-01T09:00:00Z",
                "updated_at": "2024-01-01T20:00:00Z",
                "resolved_at": "2024-01-01T18:00:00Z",
                "closed_at": "2024-01-01T20:00:00Z",
            },
            {
                "id": "3",
                "title": "Need laptop charger",
                "category": "IT_SUPPORT",
                "priority": "HIGH",
                "status": "OPEN",
                "requester_id": "requester-3",
                "assigned_to_id": None,
                "agent_name": None,
                "department_id": None,
                "department_name": None,
                "sla_deadline": "2024-01-03T12:00:00Z",
                "first_response_at": None,
                "is_sla_breached": False,
                "created_at": "2024-01-02T12:00:00Z",
                "updated_at": "2024-01-02T12:00:00Z",
                "resolved_at": None,
                "closed_at": None,
            },
            {
                "id": "4",
                "title": "VPN access issue",
                "category": "IT_SUPPORT",
                "priority": "HIGH",
                "status": "IN_PROGRESS",
                "requester_id": "requester-4",
                "assigned_to_id": "agent-1",
                "agent_name": "Ama Mensah",
                "department_id": "dept-1",
                "department_name": "IT Support",
                "sla_deadline": "2024-01-02T18:00:00Z",
                "first_response_at": "2024-01-01T14:00:00Z",
                "is_sla_breached": False,
                "created_at": "2024-01-01T13:00:00Z",
                "updated_at": "2024-01-01T14:00:00Z",
                "resolved_at": None,
                "closed_at": None,
            },
        ]
    )


def _sla_policies() -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "id": "sla-1",
                "category": "IT_SUPPORT",
                "priority": "HIGH",
                "response_time_hours": 2,
                "resolution_time_hours": 8,
            },
            {
                "id": "sla-2",
                "category": "FACILITIES",
                "priority": "LOW",
                "response_time_hours": 8,
                "resolution_time_hours": 96,
            },
            {
                "id": "sla-3",
                "category": "HR_REQUEST",
                "priority": "MEDIUM",
                "response_time_hours": 8,
                "resolution_time_hours": 72,
            },
        ]
    )


def test_transform_sla_metrics_computes_expected_columns_and_values():
    requests_df = _requests_for_transformations()
    sla_df = _sla_policies()

    result = transform_sla_metrics(requests_df, sla_df)

    assert not result.empty
    expected_columns = {
        "category",
        "priority",
        "total_tickets",
        "resolved_tickets",
        "breached_tickets",
        "compliance_rate_pct",
        "avg_resolution_hours",
        "avg_response_hours",
        "last_updated_at",
    }
    assert expected_columns == set(result.columns)

    it_high = result[
        (result["category"] == "IT_SUPPORT") & (result["priority"] == "HIGH")
    ].iloc[0]
    assert it_high["total_tickets"] == 3
    assert it_high["resolved_tickets"] == 1
    assert it_high["breached_tickets"] == 0
    assert it_high["compliance_rate_pct"] == 100.0
    assert it_high["avg_resolution_hours"] == 1.0
    assert it_high["avg_response_hours"] == 0.75

    hr_medium = result[
        (result["category"] == "HR_REQUEST") & (result["priority"] == "MEDIUM")
    ].iloc[0]
    assert hr_medium["total_tickets"] == 0
    assert hr_medium["resolved_tickets"] == 0
    assert hr_medium["compliance_rate_pct"] == 0.0


def test_transform_daily_volume_computes_request_counts():
    requests_df = _requests_for_transformations()

    # Simulate pre-aggregated daily volume as returned from the database.
    working = requests_df.copy()
    working["created_at"] = pd.to_datetime(working["created_at"])
    working["report_date"] = working["created_at"].dt.date
    aggregated = (
        working.groupby(["report_date", "category", "priority", "status"], dropna=False)
        .agg(ticket_count=("id", "count"))
        .reset_index()
    )

    result = transform_daily_volume(aggregated)

    assert not result.empty
    expected_columns = {
        "report_date",
        "category",
        "priority",
        "status",
        "ticket_count",
        "last_updated_at",
    }
    assert expected_columns == set(result.columns)
    assert result["ticket_count"].sum() == len(requests_df)


def test_transform_agent_performance_computes_weekly_agent_metrics():
    requests_df = _requests_for_transformations()

    result = transform_agent_performance(requests_df)

    assert not result.empty
    expected_columns = {
        "agent_id",
        "agent_name",
        "week_start",
        "tickets_assigned",
        "tickets_resolved",
        "avg_resolution_hours",
        "sla_compliance_rate_pct",
        "last_updated_at",
    }
    assert expected_columns == set(result.columns)

    agent_one = result[result["agent_id"] == "agent-1"].iloc[0]
    assert agent_one["agent_name"] == "Ama Mensah"
    assert agent_one["tickets_assigned"] == 2
    assert agent_one["tickets_resolved"] == 1
    assert agent_one["avg_resolution_hours"] == 1.0
    assert agent_one["sla_compliance_rate_pct"] == 100.0


def test_transform_department_workload_computes_weekly_department_metrics():
    requests_df = _requests_for_transformations()

    result = transform_department_workload(requests_df)

    assert not result.empty
    expected_columns = {
        "department_id",
        "department_name",
        "week_start",
        "open_tickets",
        "resolved_tickets",
        "breached_tickets",
        "avg_resolution_hours",
        "last_updated_at",
    }
    assert expected_columns == set(result.columns)

    department_one = result[result["department_id"] == "dept-1"].iloc[0]
    assert department_one["department_name"] == "IT Support"
    assert department_one["open_tickets"] == 1
    assert department_one["resolved_tickets"] == 1
    assert department_one["breached_tickets"] == 0
    assert department_one["avg_resolution_hours"] == 1.0
