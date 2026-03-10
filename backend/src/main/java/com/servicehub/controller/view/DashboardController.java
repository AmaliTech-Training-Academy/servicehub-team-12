package com.servicehub.controller.view;

import com.servicehub.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

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
        }
        model.addAttribute("myOpenCount", 0);
        model.addAttribute("myResolvedCount", 0);
        model.addAttribute("myTickets", Collections.emptyList());
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

        // KPI headline numbers
        model.addAttribute("totalTickets", 0);
        model.addAttribute("overallCompliance", 0);
        model.addAttribute("totalBreached", 0);
        model.addAttribute("activeBreaches", 0);

        // List data — always empty lists so JS never receives null
        model.addAttribute("atRiskTickets",  Collections.emptyList());
        model.addAttribute("slaMetrics",     Collections.emptyList());
        model.addAttribute("leaderboard",    Collections.emptyList());
        model.addAttribute("deptWorkload",   Collections.emptyList());
        model.addAttribute("dailyVolume",    Collections.emptyList());

        // ETL timestamp — null means "not run yet" (handled in template)
        model.addAttribute("lastEtlRun", null);

        return "dashboard/admin";
    }
}
