package com.servicehub.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // ── All Requests ─────────────────────────────────────────────

    @GetMapping("/requests")
    @SuppressWarnings("unused")
    public String allRequests(Model model,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String category) {
        model.addAttribute("userRole", "ADMIN");
        model.addAttribute("allTickets", Collections.emptyList());
        return "admin/requests";
    }

    // ── User Management ──────────────────────────────────────────

    @GetMapping("/users")
    @SuppressWarnings("unused")
    public String allUsers(Model model,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String role) {
        model.addAttribute("userRole", "ADMIN");
        model.addAttribute("users", Collections.emptyList());
        return "admin/users";
    }

    @GetMapping("/users/{id}")
    @SuppressWarnings("unused")
    public String editUser(@PathVariable Long id, Model model) {
        model.addAttribute("userRole", "ADMIN");
        // TODO: load user by id from service layer
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    @SuppressWarnings("unused")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // TODO: delete user via service layer
        redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
        return "redirect:/admin/users";
    }

    // ── Agents ───────────────────────────────────────────────────

    @GetMapping("/agents")
    public String agents(Model model) {
        model.addAttribute("userRole", "ADMIN");
        model.addAttribute("agents", Collections.emptyList());
        return "admin/agents";
    }

    @GetMapping("/agents/{id}")
    @SuppressWarnings("unused")
    public String editAgent(@PathVariable Long id, Model model) {
        model.addAttribute("userRole", "ADMIN");
        // TODO: load agent by id from service layer
        return "redirect:/admin/agents";
    }

    @PostMapping("/agents/{id}/toggle")
    @SuppressWarnings("unused")
    public String toggleAgent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // TODO: toggle agent active status via service layer
        redirectAttributes.addFlashAttribute("success", "Agent status updated.");
        return "redirect:/admin/agents";
    }

    // ── Roles & Permissions ──────────────────────────────────────

    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("userRole", "ADMIN");
        model.addAttribute("users", Collections.emptyList());
        return "admin/roles";
    }

    @PostMapping("/roles/{id}")
    @SuppressWarnings("unused")
    public String changeRole(@PathVariable Long id,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        // TODO: update user role via service layer
        redirectAttributes.addFlashAttribute("success", "Role updated successfully.");
        return "redirect:/admin/roles";
    }

    // ── Reports ──────────────────────────────────────────────────

    @GetMapping("/reports/sla")
    public String reportsSla(Model model) {
        model.addAttribute("userRole", "ADMIN");
        model.addAttribute("slaMetrics", Collections.emptyList());
        return "admin/reports-sla";
    }

    @GetMapping("/reports/performance")
    public String reportsPerformance(Model model) {
        model.addAttribute("userRole", "ADMIN");
        model.addAttribute("leaderboard", Collections.emptyList());
        return "admin/reports-performance";
    }

    // ── Settings ─────────────────────────────────────────────────

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("userRole", "ADMIN");
        model.addAttribute("settings", null);
        return "admin/settings";
    }

    @PostMapping("/settings")
    @SuppressWarnings("unused")
    public String saveSettings(@RequestParam String systemName,
                               @RequestParam(required = false) String supportEmail,
                               RedirectAttributes redirectAttributes) {
        // TODO: persist settings via service layer
        redirectAttributes.addFlashAttribute("success", "General settings saved.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/sla")
    @SuppressWarnings("unused")
    public String saveSlaSettings(@RequestParam int slaCritical,
                                  @RequestParam int slaHigh,
                                  @RequestParam int slaMedium,
                                  @RequestParam int slaLow,
                                  RedirectAttributes redirectAttributes) {
        // TODO: persist SLA thresholds via service layer
        redirectAttributes.addFlashAttribute("success", "SLA thresholds saved.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/clear-analytics")
    public String clearAnalytics(RedirectAttributes redirectAttributes) {
        // TODO: clear analytics data via service layer
        redirectAttributes.addFlashAttribute("success", "Analytics data cleared.");
        return "redirect:/admin/settings";
    }
}

