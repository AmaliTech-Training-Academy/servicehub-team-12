package com.servicehub.controller.view;

import com.servicehub.dto.AgentLeaderboardEntry;
import com.servicehub.dto.DailyVolumeEntry;
import com.servicehub.dto.DeptWorkloadEntry;
import com.servicehub.dto.SlaMetricEntry;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.service.ServiceRequestService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    private final ServiceRequestService serviceRequestService;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal Object principal) {
        if (principal instanceof User user) {
            return switch (user.getRole()) {
                case ADMIN -> "redirect:/dashboard/admin";
                case AGENT -> "redirect:/dashboard/agent";
                default    -> "redirect:/dashboard/user";
            };
        }
        // OAuth2 principal — default to user dashboard
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

            long openCount = userRequests.stream()
                    .filter(request -> request.getStatus() == RequestStatus.OPEN)
                    .count();

            long resolvedCount = userRequests.stream()
                    .filter(request -> request.getStatus() == RequestStatus.RESOLVED)
                    .count();

            model.addAttribute("myOpenCount", openCount);
            model.addAttribute("myResolvedCount", resolvedCount);
            model.addAttribute("myTotalCount", userRequests.size());
            model.addAttribute("myRecentTickets", userRequests.stream()
                    .limit(RECENT_REQUEST_LIMIT)
                    .toList());
            return "dashboard/user";
        }

        model.addAttribute("myOpenCount", 0);
        model.addAttribute("myResolvedCount", 0);
        model.addAttribute("myTotalCount", 0);
        model.addAttribute("myRecentTickets", Collections.emptyList());
        return "dashboard/user";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @GetMapping("/agent")
    public String agentDashboard(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "AGENT");
        if (principal != null) {
            model.addAttribute("currentUserName", principal.getFullName());
        }
        model.addAttribute("currentWeek", Collections.emptyList());
        return "dashboard/agent";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminDashboard(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", "ADMIN");
        if (principal != null) {
            model.addAttribute("currentUserName", principal.getFullName());
            model.addAttribute("currentUserEmail", principal.getEmail());
        }

        // KPI headline numbers — derived from slaMetrics sample data below

        // ── Sample SLA metrics data (March 11 2026) ─────────────────────────
        // TODO: replace with a real analytics repository query once the ETL
        //       pipeline is wired to the backend.
        List<SlaMetricEntry> slaMetrics = Arrays.asList(
            new SlaMetricEntry("IT_SUPPORT",  "CRITICAL", 120, 110, 18, 83.64, 3.2,  0.6,  "2026-03-11T22:30:00Z"),
            new SlaMetricEntry("IT_SUPPORT",  "HIGH",     260, 250, 30, 88.0,  6.4,  1.2,  "2026-03-11T22:30:00Z"),
            new SlaMetricEntry("FACILITIES",  "MEDIUM",   180, 170, 12, 92.9,  10.5, 2.4,  "2026-03-11T22:30:00Z"),
            new SlaMetricEntry("FACILITIES",  "LOW",      140, 135,  5, 96.3,  14.1, 3.1,  "2026-03-11T22:30:00Z"),
            new SlaMetricEntry("HR_REQUEST",  "HIGH",      95,  92,  6, 93.5,  8.3,  1.5,  "2026-03-11T22:30:00Z"),
            new SlaMetricEntry("HR_REQUEST",  "MEDIUM",   160, 155,  9, 94.2,  11.2, 2.2,  "2026-03-11T22:30:00Z")
        );
        model.addAttribute("slaMetrics", slaMetrics);

        // Derive KPI headline numbers from the sample data
        int    totalTickets      = slaMetrics.stream().mapToInt(SlaMetricEntry::getTotalTickets).sum();
        int    totalBreached     = slaMetrics.stream().mapToInt(SlaMetricEntry::getBreachedTickets).sum();
        double weightedCompliance = slaMetrics.stream()
                .mapToDouble(m -> m.getComplianceRatePct() * m.getTotalTickets()).sum()
                / totalTickets;

        model.addAttribute("totalTickets",      totalTickets);
        model.addAttribute("overallCompliance", Math.round(weightedCompliance * 10.0) / 10.0);
        model.addAttribute("totalBreached",     totalBreached);
        model.addAttribute("activeBreaches",    0);
        model.addAttribute("atRiskTickets",     Collections.emptyList());

        // ── Agent leaderboard dummy data (week of 2026-03-02) ────────────────
        // Ranked by SLA compliance descending.
        // TODO: replace with a real analytics repository query once the ETL
        //       pipeline is wired to the backend.
        List<AgentLeaderboardEntry> leaderboard = Arrays.asList(
            new AgentLeaderboardEntry("Kwame Boateng", 60, 55, 4.5, 95.1, "2026-03-02"),
            new AgentLeaderboardEntry("Alice Johnson",  52, 47, 5.1, 92.5, "2026-03-02"),
            new AgentLeaderboardEntry("Bob Mensah",     43, 39, 6.2, 90.3, "2026-03-02")
        );
        model.addAttribute("leaderboard", leaderboard);

        // ── Agent 2-week performance history (trend chart) ───────────────────
        // Covers the two most-recent weeks so the area chart has a meaningful
        // trend line.  Ordered week-ascending so the chart renders left→right.
        // TODO: replace with a real analytics repository query.
        List<AgentLeaderboardEntry> agentPerfHistory = Arrays.asList(
            // Week of 2026-02-23
            new AgentLeaderboardEntry("Alice Johnson",  48, 43, 5.4, 90.2, "2026-02-23"),
            new AgentLeaderboardEntry("Bob Mensah",     40, 36, 6.5, 87.8, "2026-02-23"),
            new AgentLeaderboardEntry("Kwame Boateng",  55, 51, 4.8, 93.4, "2026-02-23"),
            // Week of 2026-03-02
            new AgentLeaderboardEntry("Alice Johnson",  52, 47, 5.1, 92.5, "2026-03-02"),
            new AgentLeaderboardEntry("Bob Mensah",     43, 39, 6.2, 90.3, "2026-03-02"),
            new AgentLeaderboardEntry("Kwame Boateng",  60, 55, 4.5, 95.1, "2026-03-02")
        );
        model.addAttribute("agentPerfHistory", agentPerfHistory);

        // ── Department workload dummy data (week of 2026-03-02) ─────────────
        // TODO: replace with a real analytics repository query once the ETL
        //       pipeline is wired to the backend.
        List<DeptWorkloadEntry> deptWorkload = Arrays.asList(
            new DeptWorkloadEntry("IT Support", 41, 57, 7,  6.9,  "2026-03-02"),
            new DeptWorkloadEntry("Facilities", 20, 26, 2,  10.7, "2026-03-02"),
            new DeptWorkloadEntry("HR",         17, 24, 1,  8.8,  "2026-03-02")
        );
        model.addAttribute("deptWorkload", deptWorkload);

        // ── Department 2-week performance history (trend chart) ──────────────
        // TODO: replace with a real analytics repository query.
        List<DeptWorkloadEntry> deptPerfHistory = Arrays.asList(
            // Week of 2026-02-23
            new DeptWorkloadEntry("IT Support", 38, 52, 6,  7.1,  "2026-02-23"),
            new DeptWorkloadEntry("Facilities", 18, 23, 3,  11.2, "2026-02-23"),
            new DeptWorkloadEntry("HR",         15, 21, 2,  9.1,  "2026-02-23"),
            // Week of 2026-03-02
            new DeptWorkloadEntry("IT Support", 41, 57, 7,  6.9,  "2026-03-02"),
            new DeptWorkloadEntry("Facilities", 20, 26, 2,  10.7, "2026-03-02"),
            new DeptWorkloadEntry("HR",         17, 24, 1,  8.8,  "2026-03-02")
        );
        model.addAttribute("deptPerfHistory", deptPerfHistory);

        // ── Sample daily-volume data (March 1–11 2026) ───────────────────────
        // TODO: replace with a real analytics repository query once the ETL
        //       pipeline is wired to the backend.
        List<DailyVolumeEntry> dailyVolume = Arrays.asList(
            // Mar 01 — provided
            new DailyVolumeEntry("2026-03-01", "billing",   "high",   "open",        18, "2026-03-01T23:55:12Z"),
            new DailyVolumeEntry("2026-03-01", "billing",   "medium", "closed",      11, "2026-03-01T23:55:12Z"),
            new DailyVolumeEntry("2026-03-01", "technical", "high",   "in_progress", 24, "2026-03-01T23:55:12Z"),
            new DailyVolumeEntry("2026-03-01", "technical", "low",    "open",         9, "2026-03-01T23:55:12Z"),
            // Mar 02 — provided
            new DailyVolumeEntry("2026-03-02", "billing",   "high",   "open",        15, "2026-03-02T23:56:03Z"),
            new DailyVolumeEntry("2026-03-02", "billing",   "medium", "closed",      13, "2026-03-02T23:56:03Z"),
            new DailyVolumeEntry("2026-03-02", "technical", "high",   "in_progress", 27, "2026-03-02T23:56:03Z"),
            new DailyVolumeEntry("2026-03-02", "account",   "low",    "open",         7, "2026-03-02T23:56:03Z"),
            // Mar 03 — provided
            new DailyVolumeEntry("2026-03-03", "billing",   "urgent", "open",        20, "2026-03-03T23:54:41Z"),
            new DailyVolumeEntry("2026-03-03", "technical", "high",   "resolved",    19, "2026-03-03T23:54:41Z"),
            new DailyVolumeEntry("2026-03-03", "account",   "medium", "closed",      10, "2026-03-03T23:54:41Z"),
            new DailyVolumeEntry("2026-03-03", "technical", "low",    "open",         8, "2026-03-03T23:54:41Z"),
            // Mar 04 — generated
            new DailyVolumeEntry("2026-03-04", "billing",   "high",   "open",        17, "2026-03-04T23:55:00Z"),
            new DailyVolumeEntry("2026-03-04", "billing",   "medium", "closed",      12, "2026-03-04T23:55:00Z"),
            new DailyVolumeEntry("2026-03-04", "technical", "high",   "in_progress", 22, "2026-03-04T23:55:00Z"),
            new DailyVolumeEntry("2026-03-04", "account",   "low",    "open",         8, "2026-03-04T23:55:00Z"),
            // Mar 05 — generated
            new DailyVolumeEntry("2026-03-05", "billing",   "high",   "open",        14, "2026-03-05T23:55:00Z"),
            new DailyVolumeEntry("2026-03-05", "technical", "high",   "resolved",    21, "2026-03-05T23:55:00Z"),
            new DailyVolumeEntry("2026-03-05", "account",   "medium", "closed",       9, "2026-03-05T23:55:00Z"),
            // Mar 06 — generated
            new DailyVolumeEntry("2026-03-06", "billing",   "urgent", "open",        19, "2026-03-06T23:55:00Z"),
            new DailyVolumeEntry("2026-03-06", "technical", "high",   "in_progress", 25, "2026-03-06T23:55:00Z"),
            new DailyVolumeEntry("2026-03-06", "account",   "low",    "open",         6, "2026-03-06T23:55:00Z"),
            // Mar 07 — generated
            new DailyVolumeEntry("2026-03-07", "billing",   "high",   "open",        16, "2026-03-07T23:55:00Z"),
            new DailyVolumeEntry("2026-03-07", "billing",   "medium", "closed",      14, "2026-03-07T23:55:00Z"),
            new DailyVolumeEntry("2026-03-07", "technical", "high",   "resolved",    20, "2026-03-07T23:55:00Z"),
            // Mar 08 — generated
            new DailyVolumeEntry("2026-03-08", "billing",   "high",   "open",        13, "2026-03-08T23:55:00Z"),
            new DailyVolumeEntry("2026-03-08", "technical", "medium", "open",        11, "2026-03-08T23:55:00Z"),
            new DailyVolumeEntry("2026-03-08", "account",   "high",   "closed",       7, "2026-03-08T23:55:00Z"),
            // Mar 09 — generated
            new DailyVolumeEntry("2026-03-09", "billing",   "urgent", "open",        22, "2026-03-09T23:55:00Z"),
            new DailyVolumeEntry("2026-03-09", "technical", "high",   "in_progress", 26, "2026-03-09T23:55:00Z"),
            new DailyVolumeEntry("2026-03-09", "account",   "medium", "resolved",     9, "2026-03-09T23:55:00Z"),
            // Mar 10 — generated
            new DailyVolumeEntry("2026-03-10", "billing",   "high",   "open",        18, "2026-03-10T23:55:00Z"),
            new DailyVolumeEntry("2026-03-10", "technical", "high",   "resolved",    23, "2026-03-10T23:55:00Z"),
            new DailyVolumeEntry("2026-03-10", "account",   "low",    "open",         5, "2026-03-10T23:55:00Z"),
            // Mar 11 — generated (today)
            new DailyVolumeEntry("2026-03-11", "billing",   "high",   "open",        15, "2026-03-11T23:55:00Z"),
            new DailyVolumeEntry("2026-03-11", "technical", "high",   "in_progress", 20, "2026-03-11T23:55:00Z"),
            new DailyVolumeEntry("2026-03-11", "account",   "medium", "closed",       8, "2026-03-11T23:55:00Z")
        );
        model.addAttribute("dailyVolume", dailyVolume);

        // ETL timestamp — null means "not run yet" (handled in template)
        model.addAttribute("lastEtlRun", null);

        return "dashboard/admin";
    }
}
