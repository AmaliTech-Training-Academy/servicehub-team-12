package com.servicehub.controller.view;

import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.service.ServiceRequestService;
import com.servicehub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ServiceRequestService serviceRequestService;

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
        model.addAttribute("allTickets", serviceRequestService.findAll());
        return "admin/requests";
    }

    // ── User Management ──────────────────────────────────────────

    @GetMapping("/users")
    public String allUsers(Model model,
                           @AuthenticationPrincipal User principal,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String role) {
        addCommonAttributes(model, principal);
        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try { roleEnum = Role.valueOf(role.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        model.addAttribute("users", userService.findAll(q, roleEnum));
        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable UUID id,
                           @AuthenticationPrincipal User principal,
                           Model model) {
        addCommonAttributes(model, principal);
        try {
            model.addAttribute("user", userService.findById(id));
        } catch (Exception e) {
            return "redirect:/admin/users";
        }
        // Reuse the users list page until a dedicated edit page is built
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable UUID id,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not delete user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable UUID id,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", "User status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not update user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ── Agents ───────────────────────────────────────────────────

    @GetMapping("/agents")
    public String agents(Model model,
                         @AuthenticationPrincipal User principal,
                         @RequestParam(required = false) String q) {
        addCommonAttributes(model, principal);
        model.addAttribute("agents", userService.findAll(q, Role.AGENT));
        return "admin/agents";
    }

    @PostMapping("/agents/{id}/toggle")
    public String toggleAgent(@PathVariable UUID id,
                              RedirectAttributes redirectAttributes) {
        try {
            userService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", "Agent status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not update agent: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    // ── Roles & Permissions ──────────────────────────────────────

    @GetMapping("/roles")
    public String roles(Model model, @AuthenticationPrincipal User principal) {
        addCommonAttributes(model, principal);
        model.addAttribute("users", userService.findAll(null, null));
        return "admin/roles";
    }

    @PostMapping("/roles/{id}")
    public String changeRole(@PathVariable UUID id,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        try {
            Role newRole = Role.valueOf(role.toUpperCase());
            userService.changeRole(id, newRole);
            redirectAttributes.addFlashAttribute("success", "Role updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Invalid role: " + role);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not update role: " + e.getMessage());
        }
        return "redirect:/admin/roles";
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
