package com.servicehub.repository;

import com.servicehub.dto.AgentLeaderboardEntry;
import com.servicehub.dto.DailyVolumeEntry;
import com.servicehub.dto.DeptWorkloadEntry;
import com.servicehub.dto.SlaMetricEntry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Read-only repository for the four analytics tables produced by the Airflow
 * ETL pipeline.
 *
 * <p>Uses plain {@link JdbcTemplate} rather than JPA entities because the ETL
 * uses {@code pandas.to_sql(if_exists="replace")}, which drops and recreates
 * each table on every run — no primary-key constraints survive between runs.
 * JdbcTemplate reads any table shape without requiring a PK mapping.
 *
 * <p>Every public method catches {@link DataAccessException} and returns an
 * empty result, so the dashboard gracefully shows "ETL not run yet" when the
 * tables are absent.
 */
@Repository
public class AnalyticsDashboardRepository {

    private final JdbcTemplate jdbc;

    public AnalyticsDashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Row mappers ───────────────────────────────────────────────────────────

    private static final RowMapper<SlaMetricEntry> SLA_ROW_MAPPER = (rs, rowNum) ->
        new SlaMetricEntry(
            rs.getString("category"),
            rs.getString("priority"),
            rs.getInt("total_tickets"),
            rs.getInt("resolved_tickets"),
            rs.getInt("breached_tickets"),
            rs.getDouble("compliance_rate_pct"),   // DB column name differs from DTO field
            rs.getDouble("avg_resolution_hours"),
            rs.getDouble("avg_response_hours"),
            asString(rs, "last_updated_at")
        );

    private static final RowMapper<DailyVolumeEntry> VOLUME_ROW_MAPPER = (rs, rowNum) ->
        new DailyVolumeEntry(
            asString(rs, "report_date"),
            rs.getString("category"),
            rs.getString("priority"),
            rs.getString("status"),
            rs.getInt("ticket_count"),
            asString(rs, "last_updated_at")
        );

    private static final RowMapper<AgentLeaderboardEntry> AGENT_ROW_MAPPER = (rs, rowNum) ->
        new AgentLeaderboardEntry(
            rs.getString("agent_name"),
            rs.getInt("tickets_assigned"),
            rs.getInt("tickets_resolved"),
            rs.getDouble("avg_resolution_hours"),
            rs.getDouble("sla_compliance_rate_pct"),
            asString(rs, "week_start")
        );

    private static final RowMapper<DeptWorkloadEntry> DEPT_ROW_MAPPER = (rs, rowNum) ->
        new DeptWorkloadEntry(
            rs.getString("department_name"),
            rs.getInt("open_tickets"),
            rs.getInt("resolved_tickets"),
            rs.getInt("breached_tickets"),
            rs.getDouble("avg_resolution_hours"),
            asString(rs, "week_start")
        );

    /** Null-safe: converts any JDBC column value to its {@code toString()} form. */
    private static String asString(ResultSet rs, String col) throws SQLException {
        Object v = rs.getObject(col);
        return v == null ? null : v.toString();
    }

    // ── Public queries ────────────────────────────────────────────────────────

    /**
     * All rows from {@code analytics_sla_metrics}, ordered by category then
     * priority. Returns an empty list when the table does not yet exist.
     */
    public List<SlaMetricEntry> findAllSlaMetrics() {
        try {
            return jdbc.query(
                "SELECT * FROM analytics_sla_metrics ORDER BY category, priority",
                SLA_ROW_MAPPER
            );
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * All rows from {@code analytics_daily_volume}, ordered by date ascending.
     */
    public List<DailyVolumeEntry> findAllDailyVolume() {
        try {
            return jdbc.query(
                "SELECT * FROM analytics_daily_volume ORDER BY report_date",
                VOLUME_ROW_MAPPER
            );
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Current-week performance row for a single agent, identified by their
     * {@code agent_id} UUID.  Returns a one-element list when data exists, or
     * an empty list when the ETL has not run yet or the agent has no row.
     *
     * @param agentId the {@link java.util.UUID} of the authenticated agent
     */
    public List<AgentLeaderboardEntry> findCurrentWeekForAgent(java.util.UUID agentId) {
        try {
            return jdbc.query(
                """
                SELECT *
                FROM   analytics_agent_performance
                WHERE  agent_id = ?
                AND    week_start = (
                    SELECT MAX(week_start)
                    FROM   analytics_agent_performance
                    WHERE  agent_id = ?
                )
                """,
                AGENT_ROW_MAPPER,
                agentId.toString(), agentId.toString()
            );
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Last {@code weeks} weeks of performance history for a single agent,
     * ordered oldest-to-newest — used for the trend charts on the agent
     * dashboard.
     *
     * @param agentId the {@link java.util.UUID} of the authenticated agent
     * @param weeks   number of distinct week_start values to include
     */
    public List<AgentLeaderboardEntry> findAgentWeekTrend(java.util.UUID agentId, int weeks) {
        try {
            return jdbc.query(
                """
                SELECT *
                FROM   analytics_agent_performance
                WHERE  agent_id = ?
                AND    week_start IN (
                    SELECT DISTINCT week_start
                    FROM   analytics_agent_performance
                    WHERE  agent_id = ?
                    ORDER  BY week_start DESC
                    LIMIT  ?
                )
                ORDER  BY week_start
                """,
                AGENT_ROW_MAPPER,
                agentId.toString(), agentId.toString(), weeks
            );
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Current-week agent rows ranked by SLA compliance descending — used for
     * the leaderboard card on the Agents tab.
     */
    public List<AgentLeaderboardEntry> findCurrentWeekLeaderboard() {
        try {
            return jdbc.query(
                """
                SELECT *
                FROM   analytics_agent_performance
                WHERE  week_start = (SELECT MAX(week_start) FROM analytics_agent_performance)
                ORDER  BY sla_compliance_rate_pct DESC
                """,
                AGENT_ROW_MAPPER
            );
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Two most-recent weeks of agent data (all agents) — used for the Weekly
     * Performance Trend area chart on the Agents tab.
     */
    public List<AgentLeaderboardEntry> findRecentAgentPerfHistory() {
        try {
            return jdbc.query(
                """
                SELECT *
                FROM   analytics_agent_performance
                WHERE  week_start IN (
                    SELECT DISTINCT week_start
                    FROM   analytics_agent_performance
                    ORDER  BY week_start DESC
                    LIMIT  2
                )
                ORDER  BY week_start, agent_name
                """,
                AGENT_ROW_MAPPER
            );
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Current-week department rows — used for the summary cards on the
     * Departments tab.
     */
    public List<DeptWorkloadEntry> findCurrentWeekDeptWorkload() {
        try {
            return jdbc.query(
                """
                SELECT *
                FROM   analytics_department_workload
                WHERE  week_start = (SELECT MAX(week_start) FROM analytics_department_workload)
                ORDER  BY department_name
                """,
                DEPT_ROW_MAPPER
            );
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Two most-recent weeks of department workload — used for the Weekly
     * Resolved Tickets trend line chart on the Departments tab.
     */
    public List<DeptWorkloadEntry> findRecentDeptPerfHistory() {
        try {
            return jdbc.query(
                """
                SELECT *
                FROM   analytics_department_workload
                WHERE  week_start IN (
                    SELECT DISTINCT week_start
                    FROM   analytics_department_workload
                    ORDER  BY week_start DESC
                    LIMIT  2
                )
                ORDER  BY week_start, department_name
                """,
                DEPT_ROW_MAPPER
            );
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the most-recent {@code last_updated_at} timestamp across all four
     * analytics tables, or {@code null} if no ETL run has occurred yet.
     * Each table is queried independently so a missing table does not prevent
     * the others from being checked.
     */
    public OffsetDateTime findLastEtlRunTime() {
        String[] tables = {
            "analytics_sla_metrics",
            "analytics_daily_volume",
            "analytics_agent_performance",
            "analytics_department_workload"
        };
        OffsetDateTime latest = null;
        for (String table : tables) {
            try {
                OffsetDateTime t = jdbc.queryForObject(
                    "SELECT MAX(last_updated_at) FROM " + table,
                    OffsetDateTime.class
                );
                if (t != null && (latest == null || t.isAfter(latest))) {
                    latest = t;
                }
            } catch (DataAccessException ignored) {
                // Table not yet created by ETL — skip silently
            }
        }
        return latest;
    }
}

