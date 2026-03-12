package com.servicehub.controller.view;

import com.servicehub.dto.AgentLeaderboardEntry;
import com.servicehub.dto.DailyVolumeEntry;
import com.servicehub.dto.DeptWorkloadEntry;
import com.servicehub.dto.SlaMetricEntry;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.service.DashboardAnalyticsService;
import com.servicehub.service.ServiceRequestService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final int RECENT_REQUEST_LIMIT = 5;

    /** Statuses that are considered "done" — excluded from live breach queries. */
    private static final List<RequestStatus> DONE_STATUSES =
        List.of(RequestStatus.RESOLVED, RequestStatus.CLOSED);

    private final ServiceRequestService     serviceRequestService;
    private final DashboardAnalyticsService analyticsService;
    private final ServiceRequestRepository  serviceRequestRepository;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal Object principal) {
        if (principal instanceof User user) {
            return switch (user.getRole()) {
                case ADMIN -> "redirect:/dashboard/admin";
                case AGENT -> "redirect:/dashboard/agent";
                default    -> "redirect:/dashboard/user";
            };
        }
        return "redirect:/dashboard/user";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @GetMapping("/user")
    public String userDashboard(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "USER");
        if (principal != null) {
            model.addAttribute("currentUserName", principal.getFullName());
            List<ServiceRequestResponse> userRequests =
                    serviceRequestService.findAllByRequesterId(principal.getId());

            long openCount     = userRequests.stream().filter(r -> r.getStatus() == RequestStatus.OPEN).count();
            long resolvedCount = userRequests.stream().filter(r -> r.getStatus() == RequestStatus.RESOLVED).count();

            model.addAttribute("myOpenCount",     openCount);
            model.addAttribute("myResolvedCount", resolvedCount);
            model.addAttribute("myTotalCount",    userRequests.size());
            model.addAttribute("myRecentTickets", userRequests.stream().limit(RECENT_REQUEST_LIMIT).toList());
            return "dashboard/user";
        }

        model.addAttribute("myOpenCount",     0);
        model.addAttribute("myResolvedCount", 0);
        model.addAttribute("myTotalCount",    0);
        model.addAttribute("myRecentTickets", Collections.emptyList());
        return "dashboard/user";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @GetMapping("/agent")
    public String agentDashboard(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "AGENT");

        if (principal == null) {
            model.addAttribute("agentName",        "Agent");
            model.addAttribute("currentWeek",      Collections.emptyList());
            model.addAttribute("weekTrend",        Collections.emptyList());
            model.addAttribute("atRiskTickets",    Collections.emptyList());
            // Scalar KPI defaults so Thymeleaf never encounters missing variables
            model.addAttribute("etlDataAvailable", false);
            model.addAttribute("weekAssigned",     0);
            model.addAttribute("weekResolved",     0);
            model.addAttribute("weekCompliance",   0.0);
            model.addAttribute("weekAvgHours",     0.0);
            model.addAttribute("activeBreaches",   0L);
            return "dashboard/agent";
        }

        model.addAttribute("agentName", principal.getFullName());

        // ── ETL-sourced analytics for this specific agent ────────────────────
        // Filtered by the agent's UUID which the Airflow pipeline stores as
        // agent_id in analytics_agent_performance (matches User.id).
        UUID agentId = principal.getId();

        List<AgentLeaderboardEntry> currentWeek =
                analyticsService.getCurrentWeekForAgent(agentId);
        model.addAttribute("currentWeek", currentWeek);

        // Last 4 weeks for the trend charts
        List<AgentLeaderboardEntry> weekTrend =
                analyticsService.getAgentWeekTrend(agentId, 4);
        model.addAttribute("weekTrend", weekTrend);

        // ── Derived KPI scalars — same pattern as adminDashboard ─────────────
        // Admin derives totalTickets / overallCompliance from slaMetrics (an
        // empty list yields 0). We do the same here so the template always has
        // concrete values to render and never needs to guard with th:if.
        boolean etlDataAvailable = !currentWeek.isEmpty();
        int     weekAssigned     = etlDataAvailable ? currentWeek.getFirst().getTicketsAssigned()       : 0;
        int     weekResolved     = etlDataAvailable ? currentWeek.getFirst().getTicketsResolved()       : 0;
        double  weekCompliance   = etlDataAvailable ? currentWeek.getFirst().getSlaComplianceRatePct()  : 0.0;
        double  weekAvgHours     = etlDataAvailable ? currentWeek.getFirst().getAvgResolutionHours()    : 0.0;

        model.addAttribute("etlDataAvailable", etlDataAvailable);
        model.addAttribute("weekAssigned",     weekAssigned);
        model.addAttribute("weekResolved",     weekResolved);
        model.addAttribute("weekCompliance",   weekCompliance);
        model.addAttribute("weekAvgHours",     weekAvgHours);

        // ── Live at-risk tickets and active breaches for this agent ──────────
        // Queried directly from service_requests so the panel reflects the
        // current state between ETL runs (mirrors activeBreaches on admin dash).
        OffsetDateTime now       = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime threshold = now.plusHours(2);

        List<ServiceRequest> atRiskTickets =
                serviceRequestRepository.findAtRiskTicketsForAgent(
                        DONE_STATUSES, agentId, now, threshold);
        model.addAttribute("atRiskTickets", atRiskTickets);

        long activeBreaches = serviceRequestRepository.countActiveBreachesForAgent(agentId, DONE_STATUSES);
        model.addAttribute("activeBreaches", activeBreaches);

        return "dashboard/agent";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminDashboard(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", "ADMIN");
        if (principal != null) {
            model.addAttribute("currentUserName",  principal.getFullName());
            model.addAttribute("currentUserEmail", principal.getEmail());
        }

        // ── Analytics tables — sourced from the Airflow ETL pipeline ─────────
        // Each call returns an empty list when the ETL has not run yet, so the
        // template's "no data" states render automatically without null-checks here.
        List<SlaMetricEntry>        slaMetrics       = analyticsService.getSlaMetrics();
        List<DailyVolumeEntry>      dailyVolume      = analyticsService.getDailyVolume();
        List<AgentLeaderboardEntry> leaderboard      = analyticsService.getLeaderboard();
        List<AgentLeaderboardEntry> agentPerfHistory = analyticsService.getAgentPerfHistory();
        List<DeptWorkloadEntry>     deptWorkload     = analyticsService.getDeptWorkload();
        List<DeptWorkloadEntry>     deptPerfHistory  = analyticsService.getDeptPerfHistory();

        model.addAttribute("slaMetrics",       slaMetrics);
        model.addAttribute("dailyVolume",      dailyVolume);
        model.addAttribute("leaderboard",      leaderboard);
        model.addAttribute("agentPerfHistory", agentPerfHistory);
        model.addAttribute("deptWorkload",     deptWorkload);
        model.addAttribute("deptPerfHistory",  deptPerfHistory);
        model.addAttribute("lastEtlRun",       analyticsService.getLastEtlRunTime());

        // ── KPI headline numbers — derived from slaMetrics ───────────────────
        int    totalTickets       = slaMetrics.stream().mapToInt(SlaMetricEntry::getTotalTickets).sum();
        int    totalBreached      = slaMetrics.stream().mapToInt(SlaMetricEntry::getBreachedTickets).sum();
        double weightedCompliance = totalTickets > 0
            ? slaMetrics.stream()
                  .mapToDouble(m -> m.getComplianceRatePct() * m.getTotalTickets())
                  .sum() / totalTickets
            : 0.0;

        model.addAttribute("totalTickets",      totalTickets);
        model.addAttribute("totalBreached",     totalBreached);
        model.addAttribute("overallCompliance", Math.round(weightedCompliance * 10.0) / 10.0);

        // ── Live breach data — queried directly from service_requests ─────────
        // These values change in real time and are intentionally NOT sourced from
        // the analytics tables (which are only refreshed on the ETL schedule).
        OffsetDateTime now      = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime twoHours = now.plusHours(2);

        long                 activeBreaches = serviceRequestRepository.countActiveBreaches(DONE_STATUSES);
        List<ServiceRequest> atRiskTickets  = serviceRequestRepository.findAtRiskTickets(DONE_STATUSES, now, twoHours);

        model.addAttribute("activeBreaches", activeBreaches);
        model.addAttribute("atRiskTickets",  atRiskTickets);

        return "dashboard/admin";
    }
}

