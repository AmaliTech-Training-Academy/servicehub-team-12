package com.servicehub.service;

import com.servicehub.dto.AgentLeaderboardEntry;
import com.servicehub.dto.DailyVolumeEntry;
import com.servicehub.dto.DeptWorkloadEntry;
import com.servicehub.dto.SlaMetricEntry;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Contract for the admin dashboard analytics service.
 *
 * <p>All methods return data read from the four {@code analytics_*} tables that
 * the Airflow ETL pipeline writes on every run. The implementation must
 * <em>not</em> recompute metrics itself — see Data Contract § 8.2.
 *
 * <p>Methods return empty lists (never {@code null}) when the ETL has not run
 * yet, so the dashboard can render its "no data" states without null-checks in
 * the controller.
 */
public interface DashboardAnalyticsService {

    /**
     * All SLA metric rows from {@code analytics_sla_metrics}.
     * One row per {@code (category, priority)} combination.
     */
    List<SlaMetricEntry> getSlaMetrics();

    /**
     * All daily volume rows from {@code analytics_daily_volume}, ordered by
     * date ascending.
     */
    List<DailyVolumeEntry> getDailyVolume();

    /**
     * Current-week agent performance rows, ranked by SLA compliance descending.
     * Used for the leaderboard card on the Agents tab.
     */
    List<AgentLeaderboardEntry> getLeaderboard();

    /**
     * Two most-recent weeks of agent performance data across all agents.
     * Used for the Weekly Performance Trend area chart on the Agents tab.
     */
    List<AgentLeaderboardEntry> getAgentPerfHistory();

    /**
     * Current-week department workload rows.
     * Used for the summary cards on the Departments tab.
     */
    List<DeptWorkloadEntry> getDeptWorkload();

    /**
     * Two most-recent weeks of department workload data across all departments.
     * Used for the Weekly Resolved Tickets trend line chart on the Departments tab.
     */
    List<DeptWorkloadEntry> getDeptPerfHistory();

    /**
     * Returns the most-recent {@code last_updated_at} timestamp across all four
     * analytics tables, or {@code null} if no ETL run has occurred yet.
     */
    OffsetDateTime getLastEtlRunTime();
}

