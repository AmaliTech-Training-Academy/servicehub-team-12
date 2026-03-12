# ServiceHub Analytics - Data & Schema Documentation

This document describes the source data consumed by the ServiceHub analyticsbpipeline, the analytics tables produced by the ETL, and the current validation and orchestration behavior.

---

## 1. Source Data

### 1.1 `service_requests`

#### Overview

- **Type**: transactional source table
- **Granularity**: one row per ticket
- **Primary purpose**:
  - store the lifecycle of internal service requests
  - provide the main fact dataset for analytics
- **Primary key**: `id` (UUID)
- **Update cadence**: real-time writes from the backend; ETL reads a snapshot on each run
- **System of record**: Spring Boot backend -> PostgreSQL

#### Source fields

| Name              | Type        | Nullable | Description                                                        |
| ----------------- | ----------- | -------- | ------------------------------------------------------------------ |
| id                | UUID        | no       | Unique ticket identifier.                                          |
| title             | varchar     | no       | Ticket title shown in the UI and reports.                          |
| description       | text        | yes      | Detailed request description.                                      |
| category          | varchar(20) | no       | Enum: `IT_SUPPORT`, `FACILITIES`, `HR_REQUEST`.                    |
| priority          | varchar(10) | no       | Enum: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`.                         |
| status            | varchar(15) | no       | Enum: `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`.     |
| requester_id      | UUID        | no       | User who created the request.                                      |
| assigned_to_id    | UUID        | yes      | Agent currently assigned to the request.                           |
| department_id     | UUID        | yes      | Department currently responsible for the request.                  |
| sla_deadline      | timestamptz | yes      | Resolution SLA deadline captured on the request.                   |
| first_response_at | timestamptz | yes      | Timestamp of first response / first movement away from `OPEN`.     |
| resolved_at       | timestamptz | yes      | Timestamp when the request was resolved.                           |
| closed_at         | timestamptz | yes      | Timestamp when the request was closed.                             |
| is_sla_breached   | boolean     | no       | Upstream breach flag stored on the request.                        |
| created_at        | timestamptz | no       | Request creation timestamp in UTC.                                 |
| updated_at        | timestamptz | no       | Most recent update timestamp in UTC.                               |

#### ETL-enriched fields extracted alongside requests

The extraction query joins reference tables to make analytics transforms simpler.
These values are not stored directly on `service_requests`.

| Name            | Source table | Description                                 |
| --------------- | ------------ | ------------------------------------------- |
| requester_name  | `users`      | Requester display name.                     |
| agent_name      | `users`      | Assigned agent display name, when present.  |
| department_name | `departments`| Department name, when present.              |

#### Business rules

- `category`, `priority`, and `status` must stay within the canonical enums.
- `resolved_at >= created_at` when `resolved_at` is present.
- `first_response_at >= created_at` when `first_response_at` is present.
- `closed_at >= resolved_at` when both values are present.
- `OPEN` requests should not already have `resolved_at`.
- `RESOLVED` and `CLOSED` requests are expected to have `resolved_at`.
- Non-`OPEN` requests are expected to have assignment and department information, but missing values are treated as warnings rather than quarantined failures.
- The ETL uses both the upstream `is_sla_breached` flag and a derived `resolved_within_sla` calculation based on `resolved_at <= sla_deadline`.

#### Example queries

```sql
SELECT id, title, category, priority, status, created_at, sla_deadline
FROM service_requests
WHERE status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS')
  AND priority = 'CRITICAL'
ORDER BY created_at DESC;
```

```sql
SELECT id, category, priority, created_at, sla_deadline, resolved_at
FROM service_requests
WHERE status IN ('RESOLVED', 'CLOSED')
  AND resolved_at > sla_deadline;
```

---

### 1.2 `sla_policies`

#### Overview

- **Type**: reference/configuration table
- **Granularity**: one row per `(category, priority)` pair
- **Primary purpose**:
  - define response and resolution SLA targets
  - provide the category/priority key set used by SLA analytics
- **Primary key**: `id` (UUID)
- **Unique key**: `(category, priority)`
- **Update cadence**: low-change administrative data

#### Fields

| Name                  | Type        | Nullable | Description                                       |
| --------------------- | ----------- | -------- | ------------------------------------------------- |
| id                    | UUID        | no       | Primary key.                                      |
| category              | varchar(20) | no       | Category enum, same domain as `service_requests`. |
| priority              | varchar(10) | no       | Priority enum.                                    |
| response_time_hours   | integer     | no       | Allowed hours to first response.                  |
| resolution_time_hours | integer     | no       | Allowed hours to resolution.                      |

#### Business rules

- The ETL expects complete coverage for all category/priority combinations.
- `response_time_hours` must be positive.
- `resolution_time_hours` must be positive.
- Missing required combinations are treated as dataset-level validation failures.

---

## 2. Analytics Outputs

All analytics tables are rebuilt with `if_exists="replace"` on each successful
load, so each table reflects the latest full snapshot available at run time.

### 2.1 `analytics_sla_metrics`

#### Overview

- **Granularity**: one row per `(category, priority)` from `sla_policies`
- **Purpose**: summarize SLA performance by category and priority

#### Schema

| Column               | Type        | Description                                                         |
| -------------------- | ----------- | ------------------------------------------------------------------- |
| category             | varchar(20) | Ticket category.                                                    |
| priority             | varchar(10) | Ticket priority.                                                    |
| total_tickets        | integer     | Total requests in this category/priority group.                     |
| resolved_tickets     | integer     | Requests with status `RESOLVED` or `CLOSED` and a `resolved_at`.    |
| breached_tickets     | integer     | Requests flagged with `is_sla_breached = TRUE`.                     |
| compliance_rate_pct  | numeric     | `resolved_within_sla / resolved_tickets * 100`.                     |
| avg_resolution_hours | numeric     | Mean hours from `created_at` to `resolved_at` for resolved tickets. |
| avg_response_hours   | numeric     | Mean hours from `created_at` to `first_response_at`.                |
| last_updated_at      | timestamptz | ETL recomputation timestamp.                                        |

#### Business rules

- Policy keys come from `sla_policies`, so combinations with no requests still appear with zero counts.
- `resolved_within_sla` is derived from `resolved_at <= sla_deadline`.
- If `resolved_tickets = 0`, `compliance_rate_pct` is set to `0.0`.
- `avg_resolution_hours` and `avg_response_hours` are filled with `0.0` when no observations exist.

---

### 2.2 `analytics_daily_volume`

#### Overview

- **Granularity**: one row per `(report_date, category, priority, status)`
- **Purpose**: track daily ticket creation volume segmented by category, priority, and current status

#### Schema

| Column          | Type        | Description                                   |
| --------------- | ----------- | --------------------------------------------- |
| report_date     | date        | UTC date derived from `created_at`.           |
| category        | varchar(20) | Ticket category.                              |
| priority        | varchar(10) | Ticket priority.                              |
| status          | varchar(15) | Current status at ETL runtime.                |
| ticket_count    | integer     | Count of requests created on `report_date`.   |
| last_updated_at | timestamptz | ETL recomputation timestamp.                  |

#### Business rules

- `report_date = date(created_at at UTC)`.
- Tickets are counted once based on creation date.
- `status` reflects the latest status in the source snapshot, not a historical status snapshot.

---

### 2.3 `analytics_agent_performance`

#### Overview

- **Granularity**: one row per `(agent_id, week_start)`
- **Purpose**: measure assigned volume, resolution throughput, and SLA performance by agent

#### Schema

| Column                  | Type        | Description                                              |
| ----------------------- | ----------- | -------------------------------------------------------- |
| agent_id                | UUID/text   | Assigned agent identifier.                               |
| agent_name              | varchar     | Assigned agent display name.                             |
| week_start              | date        | Start date of the week derived from `created_at`.        |
| tickets_assigned        | integer     | Count of requests assigned to the agent.                 |
| tickets_resolved        | integer     | Count of assigned requests resolved in that week bucket. |
| avg_resolution_hours    | numeric     | Mean resolution duration for resolved assigned tickets.  |
| sla_compliance_rate_pct | numeric     | Resolved-within-SLA rate for the agent.                  |
| last_updated_at         | timestamptz | ETL recomputation timestamp.                             |

#### Business rules

- Only requests with `assigned_to_id` are included.
- `week_start` is derived from the request `created_at` week, not `resolved_at`.
- `sla_compliance_rate_pct` is `0.0` when `tickets_resolved = 0`.
- This table is recomputed frequently (every minute by default), even though the metric grain is weekly.

---

### 2.4 `analytics_department_workload`

#### Overview

- **Granularity**: one row per `(department_id, week_start)`
- **Purpose**: show weekly workload and resolution performance by department

#### Schema

| Column               | Type        | Description                                              |
| -------------------- | ----------- | -------------------------------------------------------- |
| department_id        | UUID/text   | Department identifier.                                   |
| department_name      | varchar     | Department display name.                                 |
| week_start           | date        | Start date of the week derived from `created_at`.        |
| open_tickets         | integer     | Requests in `OPEN`, `ASSIGNED`, or `IN_PROGRESS`.        |
| resolved_tickets     | integer     | Requests in `RESOLVED` or `CLOSED` with `resolved_at`.   |
| breached_tickets     | integer     | Requests flagged as SLA-breached.                        |
| avg_resolution_hours | numeric     | Mean resolution duration for resolved department tickets.|
| last_updated_at      | timestamptz | ETL recomputation timestamp.                             |

#### Business rules

- Only requests with `department_id` are included.
- `week_start` is derived from the request `created_at` week.
- `open_tickets` uses the current request status from the ETL snapshot.
- This table is recomputed frequently (every minute by default), even though the metric grain is weekly.

---

### 2.5 Quarantine outputs

The pipeline also persists invalid datasets when validation fails or rows are
quarantined.

#### `analytics_invalid_requests`

- Stores request rows excluded from downstream analytics.
- Common `quarantine_reason` values include:
  - `invalid_category`
  - `invalid_priority`
  - `invalid_status`
  - `null_in_required_field`
  - `resolved_at_before_created_at`
  - `first_response_at_before_created_at`
  - `closed_at_before_resolved_at`
  - `resolved_status_missing_resolved_at`
  - `open_status_has_resolved_at`
  - `dataset_validation_failed`

#### `analytics_invalid_sla_policies`

- Stores SLA policy rows excluded from downstream analytics.
- Common `quarantine_reason` values include:
  - `invalid_category`
  - `invalid_priority`
  - `invalid_response_time_hours`
  - `invalid_resolution_time_hours`
  - `dataset_validation_failed`

---

## 3. Data Quality and Validation

### 3.1 Request validation

The request validator splits the extracted dataset into valid and invalid rows.

#### Hard validation failures

- missing required request columns
- `requests_df is None`

#### Row-level quarantines

- invalid category, priority, or status values
- null or invalid `id`, `requester_id`, `created_at`, or `updated_at`
- `resolved_at < created_at`
- `first_response_at < created_at`
- `closed_at < resolved_at`
- `RESOLVED` or `CLOSED` rows without `resolved_at`
- `OPEN` rows that already have `resolved_at`

#### Warnings that do not quarantine rows

- non-`OPEN` rows without `assigned_to_id`
- non-`OPEN` rows without `department_id`
- rows resolved after `sla_deadline` but flagged as `is_sla_breached = FALSE`

### 3.2 SLA policy validation

#### Hard validation failures

- missing required SLA policy columns
- missing required `(category, priority)` combinations after cleaning
- `sla_policies_df is None`

#### Row-level quarantines

- invalid category or priority values
- non-positive or null `response_time_hours`
- non-positive or null `resolution_time_hours`

---

## 4. Architecture Decision Record

### ADR 001 - Modular ETL with Airflow Orchestration


#### Context

- ServiceHub needs analytics for SLA performance, daily request volume, agent performance, and department workload.
- Operational data is written by the Java backend into PostgreSQL.
- The analytics pipeline must tolerate bad rows without losing the entire run.
- The team needs a repeatable local Airflow setup for demo and QA workflows.

#### Decision

- Use a modular Python ETL structure with separate extraction, validation, transformation, and loading modules.
- Use Apache Airflow to orchestrate extraction, validation/quarantine, and per-table load tasks.
- Recompute the analytics outputs on a frequent schedule (every minute by default) from the latest source snapshot.
- Persist invalid request and SLA policy rows to dedicated analytics quarantine tables.

#### Current pipeline behavior

- **Schedule**: every minute (`*/1 * * * *`)
- **Main DAG**: `servicehub_sla_analytics`
- **Key task flow**:
  - `extract_requests`
  - `extract_sla_policies`
  - `validate_and_quarantine`
  - `compute_and_load_sla_metrics`
  - `compute_and_load_daily_volume`
  - `compute_and_load_agent_performance`
  - `compute_and_load_department_workload`

#### Scope

- **Inputs**:
  - `service_requests`
  - `sla_policies`
  - reference joins to `users` and `departments`
- **Outputs**:
  - `analytics_sla_metrics`
  - `analytics_daily_volume`
  - `analytics_agent_performance`
  - `analytics_department_workload`
  - `analytics_invalid_requests`
  - `analytics_invalid_sla_policies`

#### Consequences

- The pipeline remains resilient to row-level data issues through quarantine tables.
- Weekly-grain analytics can still be refreshed every minute to reflect the latest operational state.
- The analytics tables are easy to reason about because each run replaces the prior snapshot rather than applying incremental merge logic.
