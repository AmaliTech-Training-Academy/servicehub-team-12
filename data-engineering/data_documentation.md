# ServiceHub Analytics – Data & Schema Documentation

This document captures the key data objects produced and consumed by the
ServiceHub analytics pipeline, along with the main architecture decisions.

---

## 1. Source Table Documentation

### 1.1 `service_requests` – Data Documentation

#### Overview

- **Type**: table
- **Domain**: IT service management / internal ticketing
- **Primary purpose**:
  - Capture the lifecycle of internal service requests across departments.
  - Provide the fact table for SLA, volume, and performance analytics.
- **Primary keys**: `id` (UUID)
- **Update cadence**: real-time writes from the Java backend; ETL reads snapshot at run time.
- **Data source(s)**: Spring Boot backend → PostgreSQL.

#### Fields

| Name              | Type        | Nullable | Source   | Description                                                                 |
| ----------------- | ----------- | -------- | -------- | --------------------------------------------------------------------------- |
| id                | UUID        | no       | upstream | Unique identifier for the service request.                                  |
| title             | varchar     | no       | upstream | Short summary/title shown in UI and analytics.                              |
| description       | text        | yes      | upstream | Detailed problem description provided by the requester.                     |
| category          | varchar(20) | no       | upstream | Enum: `IT_SUPPORT`, `FACILITIES`, `HR_REQUEST`.                             |
| priority          | varchar(10) | no       | upstream | Enum: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`.                                  |
| status            | varchar(15) | no       | upstream | Enum: `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`.              |
| department_id     | UUID        | yes      | upstream | Department responsible for the ticket; null while status is `OPEN`.         |
| assigned_to_id    | UUID        | yes      | upstream | Agent handling the ticket; null while status is `OPEN`.                     |
| requester_id      | UUID        | no       | upstream | Employee who created the ticket.                                            |
| sla_deadline      | timestamptz | yes      | upstream | When the resolution SLA expires, based on SLA policy at creation time.      |
| first_response_at | timestamptz | yes      | upstream | First time status moved away from `OPEN` (response SLA anchor).             |
| resolved_at       | timestamptz | yes      | upstream | When the agent resolved the ticket (`RESOLVED`).                            |
| closed_at         | timestamptz | yes      | upstream | When the ticket was finally closed (`CLOSED`).                              |
| is_sla_breached   | boolean     | no       | upstream | True if ticket missed its resolution SLA (`NOW() > sla_deadline` and open). |
| created_at        | timestamptz | no       | upstream | Creation timestamp in UTC.                                                  |
| updated_at        | timestamptz | no       | upstream | Last update timestamp in UTC.                                               |

#### Business Rules & Semantics

- **Granularity**: one row = one ticket.
- **Inclusion criteria**: all service requests created in the system.
- **Important constraints**:
  - `category`, `priority`, `status` must match the canonical enums.
  - `resolved_at >= created_at` when `resolved_at` is not null.
  - `closed_at >= resolved_at` when `closed_at` is not null.
  - `sla_deadline = created_at + resolution_time_hours` based on `sla_policies`.
  - `is_sla_breached = true` when `NOW() > sla_deadline` and `status` is not `RESOLVED` or `CLOSED`.
- **Time handling**:
  - All timestamps are stored as UTC (`timestamptz`).
  - ETL uses `created_at`, `first_response_at`, `resolved_at`, `closed_at`, and `sla_deadline` for metrics.

#### Usage Examples

- **Recent open critical tickets**:

```sql
SELECT id, title, category, priority, status, created_at, sla_deadline
FROM service_requests
WHERE status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS')
  AND priority = 'CRITICAL'
ORDER BY created_at DESC;
```

- **Tickets resolved after SLA deadline**:

```sql
SELECT id, category, priority, created_at, sla_deadline, resolved_at
FROM service_requests
WHERE status IN ('RESOLVED', 'CLOSED')
  AND is_sla_breached = TRUE;
```

---

### 1.2 `sla_policies` – Data Documentation

#### Overview

- **Type**: table
- **Domain**: SLA configuration
- **Primary purpose**:
  - Define response and resolution time targets per `(category, priority)` combination.
- **Primary keys**: `id` (UUID); unique constraint on `(category, priority)`.
- **Update cadence**: static / rarely changed (managed by admins).
- **Data source(s)**: Spring Boot backend seed data + potential admin UI.

#### Fields

| Name                  | Type        | Nullable | Source   | Description                                       |
| --------------------- | ----------- | -------- | -------- | ------------------------------------------------- |
| id                    | UUID        | no       | upstream | Primary key.                                      |
| category              | varchar(20) | no       | upstream | Category enum, same domain as `service_requests`. |
| priority              | varchar(10) | no       | upstream | Priority enum.                                    |
| response_time_hours   | integer     | no       | upstream | Max hours allowed for first response.             |
| resolution_time_hours | integer     | no       | upstream | Max hours allowed for full resolution.            |

#### Business Rules & Semantics

- **Granularity**: one row per `(category, priority)` pair.
- **Inclusion criteria**: all relevant category/priority combinations.
- **Usage in ETL**:
  - Used by the backend and data contract to derive `sla_deadline` in `service_requests`.
  - ETL uses resolution windows indirectly via `sla_deadline` and `is_sla_breached`.

---

## 2. Analytics Tables (ETL Outputs)

### 2.1 `analytics_sla_metrics` – Metric Definition

#### Overview

- **Type**: table
- **Granularity**: one row per `(category, priority)`.
- **Purpose**:
  - Provide aggregate SLA performance metrics grouped by category and priority.

#### Schema

| Column               | Type        | Description                                                         |
| -------------------- | ----------- | ------------------------------------------------------------------- |
| category             | varchar(20) | Ticket category.                                                    |
| priority             | varchar(10) | Ticket priority.                                                    |
| total_tickets        | integer     | Count of all tickets for this `(category, priority)`.               |
| resolved_tickets     | integer     | Count of tickets with status `RESOLVED` or `CLOSED`.                |
| breached_tickets     | integer     | Count of resolved tickets where `is_sla_breached = TRUE`.           |
| compliance_rate_pct  | numeric     | `(resolved_tickets - breached_tickets) / resolved_tickets * 100`.   |
| avg_resolution_hours | numeric     | Mean hours from `created_at` to `resolved_at` for resolved tickets. |
| avg_response_hours   | numeric     | Mean hours from `created_at` to `first_response_at` (when set).     |
| last_updated_at      | timestamptz | Timestamp of when this row was last recomputed by the ETL.          |

#### Business Rules

- Only tickets with status `RESOLVED` or `CLOSED` contribute to `resolved_tickets` and time-based averages.
- If `resolved_tickets = 0`, `compliance_rate_pct` is left null to avoid misleading 0%/100% values.
- The ETL uses the current snapshot of `service_requests` at run time and **replaces** the whole table each run.

#### Example query

```sql
SELECT category,
       priority,
       compliance_rate_pct,
       avg_resolution_hours,
       avg_response_hours
FROM analytics_sla_metrics
ORDER BY category, priority;
```

---

### 2.2 `analytics_daily_volume` – Metric Definition

#### Overview

- **Type**: table
- **Granularity**: one row per `(report_date, category, priority, status)`.
- **Purpose**:
  - Track daily ticket volumes by category, priority, and end-of-day status.

#### Schema

| Column          | Type        | Description                                         |
| --------------- | ----------- | --------------------------------------------------- |
| report_date     | date        | Calendar day (UTC) derived from `created_at`.       |
| category        | varchar(20) | Ticket category.                                    |
| priority        | varchar(10) | Ticket priority.                                    |
| status          | varchar(15) | Status at the time of ETL snapshot for that ticket. |
| ticket_count    | integer     | Number of tickets opened on `report_date`.          |
| last_updated_at | timestamptz | Timestamp of when this row was last recomputed.     |

#### Business Rules

- `report_date = date(created_at at time zone 'UTC')`.
- Tickets are counted once based on their creation date; status reflects the current state at ETL run time, not the historical status at creation time.
- Table is **replaced** on each ETL run to avoid drift.

#### Example query

```sql
SELECT report_date, category, priority, status, ticket_count
FROM analytics_daily_volume
WHERE report_date >= current_date - INTERVAL '7 days'
ORDER BY report_date, category, priority, status;
```

---

## 3. Architecture Decision Record – ETL & Airflow Design

### ADR 001 – Modular ETL with Airflow Orchestration

#### Status

- **Status**: Accepted
- **Date**: 2026-03-09
- **Owners**: Data Engineering (ServiceHub Team 12)

#### Context

- ServiceHub requires analytics on request volumes, SLA compliance, and resolution times across categories and priorities.
- The Java backend writes operational data into PostgreSQL; analytics is offloaded to Python/Pandas.
- The project must support:
  - A robust nightly pipeline that tolerates bad data.
  - A manual mechanism to load sample data for demos and QA.
  - Clear observability and debuggability for each ETL step.

#### Decision

- **I decided to**:
  - Implement a modular ETL stack in Python with distinct `extract`, `validation`, `transform`, and `load` modules.
  - Orchestrate the ETL with Apache Airflow using a DAG that separates extraction, validation/quarantine, and individual load steps.
  - Introduce a sample data generator to seed realistic source data for development and demonstrations.
- **Scope**:
  - `service_requests`, `users`, `departments`, `sla_policies` as inputs.
  - `analytics_sla_metrics` and `analytics_daily_volume` as first-class outputs (with extension points for agent and department metrics).
  - Airflow DAGs and Docker-based local environment for data engineering.

#### Implementation Notes

- Airflow image:
  - Custom image based on `apache/airflow:3.1.7`, extended with ETL dependencies via `data-engineering/Dockerfile`.
- Orchestration:
  - `servicehub_sla_analytics` DAG:
    - `extract_requests` / `extract_sla_policies` → temp Parquet files.
    - `validate_and_quarantine` → quarantines invalid rows and persists valid subsets.
    - `compute_and_load_sla_metrics` / `compute_and_load_daily_volume` → analytics tables.
  - `servicehub_sample_data_loader` DAG:
    - Calls into `sample_data.generator.load_sample_requests` for dev/test seeding.
- Error handling:
  - Row-level validation issues are quarantined, not fatal.
  - Structural or ETL runtime errors raise ETL-specific exceptions and surface as failed tasks in Airflow.
