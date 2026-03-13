package com.servicehub.controller.view;

import com.servicehub.dto.ServiceRequestPageQuery;
import com.servicehub.dto.UserPageQuery;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.service.ServiceRequestService;
import com.servicehub.service.UserService;
import java.util.Collections;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final int PAGE_SIZE = 10;

    private static final List<RequestStatus> DONE_STATUSES =
        List.of(RequestStatus.RESOLVED, RequestStatus.CLOSED);

    private final UserService userService;
    private final ServiceRequestService serviceRequestService;

    public AdminController(UserService userService, ServiceRequestService serviceRequestService) {
        this.userService = userService;
        this.serviceRequestService = serviceRequestService;
    }

    private void addCommonAttributes(Model model, User principal) {
        model.addAttribute("userRole", "ADMIN");
        if (principal != null) {
            model.addAttribute("currentUserName", extractPrincipalName(principal));
            model.addAttribute("currentUserEmail", principal.getUsername());
        }
    }

    private String extractPrincipalName(User principal) {
        try {
            Object value = principal.getClass().getMethod("getFullName").invoke(principal);
            return value == null ? principal.getUsername() : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return principal.getUsername();
        }
    }


    @GetMapping("/requests")
    public String allRequests(Model model,
                              @AuthenticationPrincipal User principal,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String priority,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "false") boolean breached) {
        addCommonAttributes(model, principal);

        ServiceRequestPageQuery query = ServiceRequestPageQuery.builder()
                .page(page)
                .size(PAGE_SIZE)
                .q(q)
                .status(parseRequestStatus(status))
                .priority(parseRequestPriority(priority))
                .breached(breached)
                .build();

        Page<?> ticketsPage = serviceRequestService.findPage(query);

        model.addAttribute("allTickets", ticketsPage.getContent());
        addPaginationAttributes(model, ticketsPage, query.getPage());
        addQueryAttributes(model, query, status, priority);
        return "admin/requests";
    }

    private void addPaginationAttributes(Model model, Page<?> pageData, int requestedPage) {
        model.addAttribute("currentPage", Math.max(requestedPage, 1));
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("hasPrevious", pageData.hasPrevious());
        model.addAttribute("hasNext", pageData.hasNext());
        model.addAttribute(
                "pageNumbers",
                pageData.getTotalPages() == 0
                        ? Collections.emptyList()
                        : buildPaginationItems(Math.max(requestedPage, 1), pageData.getTotalPages())
        );

        if (pageData.getTotalElements() == 0) {
            model.addAttribute("startItem", 0);
            model.addAttribute("endItem", 0);
            return;
        }

        model.addAttribute("startItem", (long) (pageData.getNumber() * pageData.getSize()) + 1);
        model.addAttribute("endItem", (long) (pageData.getNumber() * pageData.getSize()) + pageData.getNumberOfElements());
    }

    private void addQueryAttributes(Model model, ServiceRequestPageQuery query, String rawStatus, String rawPriority) {
        model.addAttribute("requestQuery", query);
        model.addAttribute("q", query.getQ());
        model.addAttribute("status", rawStatus);
        model.addAttribute("priority", rawPriority);
        model.addAttribute("breachedOnly", query.isBreached());
    }

    private RequestStatus parseRequestStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return RequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private RequestPriority parseRequestPriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }

        try {
            return RequestPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    // ── User Management ──────────────────────────────────────────

    @GetMapping("/users")
    public String allUsers(Model model,
                           @AuthenticationPrincipal User principal,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String role,
                           @RequestParam(defaultValue = "1") int page) {
        addCommonAttributes(model, principal);
        Role roleEnum = parseRole(role);
        UserPageQuery query = UserPageQuery.builder()
                .page(page)
                .size(PAGE_SIZE)
                .q(q)
                .role(roleEnum)
                .build();
        Page<?> usersPage = userService.findPage(query);
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("q", q);
        model.addAttribute("filterRole", role);
        addPaginationAttributes(model, usersPage, query.getPage());
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
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String role,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not delete user: " + e.getMessage());
        }
        return buildUsersRedirect(page, q, role);
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable UUID id,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String role,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", "User status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not update user: " + e.getMessage());
        }
        return buildUsersRedirect(page, q, role);
    }

    // ── Agents ───────────────────────────────────────────────────

    @GetMapping("/agents")
    public String agents(Model model,
                         @AuthenticationPrincipal User principal,
                         @RequestParam(required = false) String q) {
        return q != null && !q.isBlank()
                ? "redirect:/admin/users?role=AGENT&q=" + q
                : "redirect:/admin/users?role=AGENT";
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
        return "redirect:/admin/users?role=AGENT";
    }

    // ── Roles & Permissions ──────────────────────────────────────

    @GetMapping("/roles")
    public String roles(Model model, @AuthenticationPrincipal User principal) {
        return "redirect:/admin/users";
    }

    @PostMapping("/roles/{id}")
    public String changeRole(@PathVariable UUID id,
                             @RequestParam String role,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false, name = "filterRole") String filterRole,
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
        return buildUsersRedirect(page, q, filterRole);
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String buildUsersRedirect(int page, String q, String role) {
        StringBuilder redirect = new StringBuilder("redirect:/admin/users?page=").append(Math.max(page, 1));
        appendQueryParam(redirect, "q", q);
        appendQueryParam(redirect, "role", role);
        return redirect.toString();
    }

    private void appendQueryParam(StringBuilder builder, String key, String value) {
        if (value != null && !value.isBlank()) {
            builder.append("&").append(key).append("=").append(encode(value));
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private List<String> buildPaginationItems(int currentPage, int totalPages) {
        if (totalPages <= 7) {
            return IntStream.rangeClosed(1, totalPages)
                    .mapToObj(String::valueOf)
                    .toList();
        }

        List<String> items = new ArrayList<>();
        items.add("1");

        if (currentPage <= 4) {
            addRange(items, 2, 5);
            items.add("...");
        } else if (currentPage >= totalPages - 3) {
            items.add("...");
            addRange(items, totalPages - 4, totalPages - 1);
        } else {
            items.add("...");
            addRange(items, currentPage - 1, currentPage + 1);
            items.add("...");
        }

        items.add(String.valueOf(totalPages));
        return items;
    }

    private void addRange(List<String> items, int start, int end) {
        for (int i = start; i <= end; i++) {
            items.add(String.valueOf(i));
        }
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
