package com.servicehub.controller.view;

import com.servicehub.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private void addCommonAttributes(Model model, User principal) {
        model.addAttribute("userRole", "ADMIN");
        if (principal != null) {
            model.addAttribute("currentUserName", principal.getFullName());
            model.addAttribute("currentUserEmail", principal.getEmail());
        }
    }

    // ── All Requests ─────────────────────────────────────────────

    @GetMapping("/requests")
    public String allRequests(Model model,
                              @AuthenticationPrincipal User principal,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String category) {
        addCommonAttributes(model, principal);
        model.addAttribute("allTickets", Collections.emptyList());
        return "admin/requests";
    }

    // ── User Management ──────────────────────────────────────────

    @GetMapping("/users")
    public String allUsers(Model model,
                           @AuthenticationPrincipal User principal,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String role) {
        addCommonAttributes(model, principal);
        model.addAttribute("users", Collections.emptyList());
        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String editUser(@PathVariable Long id,
                           @AuthenticationPrincipal User principal,
                           Model model) {
        addCommonAttributes(model, principal);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
        return "redirect:/admin/users";
    }

    // ── Agents ───────────────────────────────────────────────────

    @GetMapping("/agents")
    public String agents(Model model, @AuthenticationPrincipal User principal) {
        addCommonAttributes(model, principal);
        model.addAttribute("agents", Collections.emptyList());
        return "admin/agents";
    }

    @GetMapping("/agents/{id}")
    public String editAgent(@PathVariable Long id,
                            @AuthenticationPrincipal User principal,
                            Model model) {
        addCommonAttributes(model, principal);
        return "redirect:/admin/agents";
    }

    @PostMapping("/agents/{id}/toggle")
    public String toggleAgent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "Agent status updated.");
        return "redirect:/admin/agents";
    }

    // ── Roles & Permissions ──────────────────────────────────────

    @GetMapping("/roles")
    public String roles(Model model, @AuthenticationPrincipal User principal) {
        addCommonAttributes(model, principal);
        model.addAttribute("users", Collections.emptyList());
        return "admin/roles";
    }

    @PostMapping("/roles/{id}")
    public String changeRole(@PathVariable Long id,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "Role updated successfully.");
        return "redirect:/admin/roles";
    }

    // ── Reports ──────────────────────────────────────────────────

    @GetMapping("/reports/sla")
    public String reportsSla(Model model, @AuthenticationPrincipal User principal) {
        addCommonAttributes(model, principal);
        model.addAttribute("slaMetrics", Collections.emptyList());
        return "admin/reports-sla";
    }

    @GetMapping("/reports/performance")
    public String reportsPerformance(Model model, @AuthenticationPrincipal User principal) {
        addCommonAttributes(model, principal);
        model.addAttribute("leaderboard", Collections.emptyList());
        return "admin/reports-performance";
    }

    // ── Settings ─────────────────────────────────────────────────

    @GetMapping("/settings")
    public String settings(Model model, @AuthenticationPrincipal User principal) {
        addCommonAttributes(model, principal);
        model.addAttribute("settings", null);
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam String systemName,
                               @RequestParam(required = false) String supportEmail,
                               RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "General settings saved.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/sla")
    public String saveSlaSettings(@RequestParam int slaCritical,
                                  @RequestParam int slaHigh,
                                  @RequestParam int slaMedium,
                                  @RequestParam int slaLow,
                                  RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "SLA thresholds saved.");
        return "redirect:/admin/settings";
    }

    @PostMapping("/settings/clear-analytics")
    public String clearAnalytics(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "Analytics data cleared.");
        return "redirect:/admin/settings";
    }
}
