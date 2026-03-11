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
@RequestMapping("/agent")
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public class AgentController {

    @GetMapping("/performance")
    public String performance(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "AGENT");
        model.addAttribute("weekHistory", Collections.emptyList());
        return "agent/performance";
    }

    @GetMapping("/schedule")
    public String schedule(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "AGENT");
        model.addAttribute("dueThisWeek", 0);
        model.addAttribute("overdueCount", 0);
        model.addAttribute("onTrackCount", 0);
        model.addAttribute("scheduledTickets", Collections.emptyList());
        return "agent/schedule";
    }
}
