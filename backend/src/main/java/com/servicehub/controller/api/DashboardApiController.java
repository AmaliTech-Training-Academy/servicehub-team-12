package com.servicehub.controller.api;

import com.servicehub.dto.AdminKpiResponse;
import com.servicehub.dto.AgentLeaderboardEntry;
import com.servicehub.dto.DailyVolumeEntry;
import com.servicehub.dto.DeptWorkloadEntry;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.SlaMetricEntry;
import com.servicehub.dto.UserDashboardStats;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.service.DashboardAnalyticsService;
import com.servicehub.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API surface for the admin and user dashboard data.
 *
 * <p>All analytics endpoints ({@code /admin/**}) delegate to
 * {@link DashboardAnalyticsService}, which reads from the four
 * {@code analytics_*} tables populated by the Airflow ETL pipeline.
 * The KPI summary endpoint additionally queries
 * {@link ServiceRequestRepository} for live breach counts that change
 * in real time between ETL runs.
 *
 * <p>The {@code /me} endpoint returns the current user's own ticket
 * activity and is available to every authenticated role.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Admin analytics and per-user dashboard statistics")
public class DashboardApiController {

    /** Statuses that are considered terminal — excluded from live breach queries. */
    private static final List<RequestStatus> DONE_STATUSES =
            List.of(RequestStatus.RESOLVED, RequestStatus.CLOSED);

    /** Number of hours ahead used for the at-risk SLA window. */
    private static final long AT_RISK_WINDOW_HOURS = 2L;

    /** Maximum number of recent tickets returned in the user stats endpoint. */
    private static final int RECENT_TICKET_LIMIT = 5;

    private final DashboardAnalyticsService  analyticsService;
    private final ServiceRequestService      serviceRequestService;
    private final ServiceRequestRepository   serviceRequestRepository;

    // ── User endpoint ─────────────────────────────────────────────────────────

    /**
     * Returns the currently authenticated user's own ticket activity:
     * open/resolved counts, total count and five most-recent requests.
     */
    @Operation(
        summary = "Get the current user's dashboard statistics",
        description = "Returns open/resolved/total ticket counts and the five most-recent "
                    + "requests for the authenticated principal. Available to all roles."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stats returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserDashboardStats> myStats(@AuthenticationPrincipal User principal) {
        List<ServiceRequestResponse> requests =
                serviceRequestService.findAllByRequesterId(principal.getId());

        long openCount     = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.OPEN).count();
        long resolvedCount = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.RESOLVED).count();

        UserDashboardStats stats = UserDashboardStats.builder()
                .openCount(openCount)
                .resolvedCount(resolvedCount)
                .totalCount(requests.size())
                .recentTickets(requests.stream().limit(RECENT_TICKET_LIMIT).toList())
                .build();

        return ResponseEntity.ok(stats);
    }

    // ── Admin — KPI summary ───────────────────────────────────────────────────

    /**
     * Returns headline KPI numbers for the admin dashboard.
     * ETL-sourced metrics (totals, compliance) are combined with live
     * breach counts queried directly from the service-requests table.
     */
    @Operation(
        summary = "Get admin KPI headline numbers",
        description = "Combines ETL-sourced totals/compliance with live breach counts. "
                    + "ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "KPIs returned"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/kpis")
    public ResponseEntity<AdminKpiResponse> adminKpis() {
        List<SlaMetricEntry> slaMetrics = analyticsService.getSlaMetrics();

        int    totalTickets = slaMetrics.stream().mapToInt(SlaMetricEntry::getTotalTickets).sum();
        int    totalBreached = slaMetrics.stream().mapToInt(SlaMetricEntry::getBreachedTickets).sum();
        double weightedCompliance = totalTickets > 0
                ? slaMetrics.stream()
                        .mapToDouble(m -> m.getComplianceRatePct() * m.getTotalTickets())
                        .sum() / totalTickets
                : 0.0;

        OffsetDateTime now      = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime threshold = now.plusHours(AT_RISK_WINDOW_HOURS);

        long activeBreaches = serviceRequestRepository.countActiveBreaches(DONE_STATUSES);
        int  atRiskCount    = serviceRequestRepository
                .findAtRiskTickets(DONE_STATUSES, now, threshold).size();

        AdminKpiResponse response = AdminKpiResponse.builder()
                .totalTickets(totalTickets)
                .totalBreached(totalBreached)
                .overallCompliance(Math.round(weightedCompliance * 10.0) / 10.0)
                .activeBreaches(activeBreaches)
                .atRiskCount(atRiskCount)
                .lastEtlRun(analyticsService.getLastEtlRunTime())
                .build();

        return ResponseEntity.ok(response);
    }

    // ── Admin — SLA metrics ───────────────────────────────────────────────────

    /**
     * Returns all rows from the {@code analytics_sla_metrics} table,
     * one row per {@code (category, priority)} combination.
     */
    @Operation(
        summary = "Get SLA metrics (all category/priority combinations)",
        description = "Sourced from the Airflow ETL analytics_sla_metrics table. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "SLA metrics returned — empty list when ETL has not run"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/sla-metrics")
    public ResponseEntity<List<SlaMetricEntry>> slaMetrics() {
        return ResponseEntity.ok(analyticsService.getSlaMetrics());
    }

    // ── Admin — Daily volume ──────────────────────────────────────────────────

    /**
     * Returns daily ticket-volume rows ordered by date ascending.
     */
    @Operation(
        summary = "Get daily ticket volume",
        description = "Sourced from the Airflow ETL analytics_daily_volume table. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Daily volume rows returned — empty list when ETL has not run"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/daily-volume")
    public ResponseEntity<List<DailyVolumeEntry>> dailyVolume() {
        return ResponseEntity.ok(analyticsService.getDailyVolume());
    }

    // ── Admin — Agent endpoints ───────────────────────────────────────────────

    /**
     * Returns current-week agent performance rows ranked by SLA compliance descending.
     */
    @Operation(
        summary = "Get agent leaderboard (current week)",
        description = "Sourced from the Airflow ETL analytics_agent_performance table. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Leaderboard rows returned — empty list when ETL has not run"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/agent-leaderboard")
    public ResponseEntity<List<AgentLeaderboardEntry>> agentLeaderboard() {
        return ResponseEntity.ok(analyticsService.getLeaderboard());
    }

    /**
     * Returns the two most-recent weeks of agent performance data across all agents.
     */
    @Operation(
        summary = "Get agent performance history (two most-recent weeks)",
        description = "Sourced from the Airflow ETL analytics_agent_performance table. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History rows returned — empty list when ETL has not run"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/agent-history")
    public ResponseEntity<List<AgentLeaderboardEntry>> agentHistory() {
        return ResponseEntity.ok(analyticsService.getAgentPerfHistory());
    }

    // ── Admin — Department endpoints ──────────────────────────────────────────

    /**
     * Returns current-week department workload rows.
     */
    @Operation(
        summary = "Get department workload (current week)",
        description = "Sourced from the Airflow ETL analytics_dept_workload table. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workload rows returned — empty list when ETL has not run"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dept-workload")
    public ResponseEntity<List<DeptWorkloadEntry>> deptWorkload() {
        return ResponseEntity.ok(analyticsService.getDeptWorkload());
    }

    /**
     * Returns two most-recent weeks of department workload data across all departments.
     */
    @Operation(
        summary = "Get department performance history (two most-recent weeks)",
        description = "Sourced from the Airflow ETL analytics_dept_workload table. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History rows returned — empty list when ETL has not run"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dept-history")
    public ResponseEntity<List<DeptWorkloadEntry>> deptHistory() {
        return ResponseEntity.ok(analyticsService.getDeptPerfHistory());
    }
}

