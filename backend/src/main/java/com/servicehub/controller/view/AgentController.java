package com.servicehub.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

@Controller
@RequestMapping("/agent")
public class AgentController {

    @GetMapping("/performance")
    public String performance(Model model) {
        model.addAttribute("userRole", "AGENT");
        model.addAttribute("weekHistory", Collections.emptyList());
        return "agent/performance";
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        model.addAttribute("userRole", "AGENT");
        model.addAttribute("dueThisWeek", 0);
        model.addAttribute("overdueCount", 0);
        model.addAttribute("onTrackCount", 0);
        model.addAttribute("scheduledTickets", Collections.emptyList());
        return "agent/schedule";
    }
}

