package com.servicehub.controller.view;

import com.servicehub.dto.ServiceRequestForm;
import com.servicehub.dto.ServiceRequestPageQuery;
import com.servicehub.mapper.ServiceRequestMapper;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.service.ServiceRequestService;
import com.servicehub.service.WorkflowService;
import java.util.Collections;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
/**
 * Serves Thymeleaf views for submitting and browsing service requests.
 */

@Controller
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {

    private static final int PAGE_SIZE = 10;

    private final ServiceRequestService serviceRequestService;
    private final ServiceRequestMapper serviceRequestMapper;
    private final WorkflowService workflowService;

    // ── USER: My Requests ────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @GetMapping
    public String myRequests(Model model,
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "1") int page) {
        ServiceRequestPageQuery query = ServiceRequestPageQuery.builder()
                .page(page)
                .size(PAGE_SIZE)
                .q(q)
                .status(parseRequestStatus(status))
                .priority(parseRequestPriority(priority))
                .build();
        String role = principal != null ? principal.getRole().name() : "USER";
        model.addAttribute("userRole", role);
        if (principal != null) {
            model.addAttribute("currentUserName", principal.getFullName());
            Page<?> ticketPage = serviceRequestService.findAllByRequesterId(principal.getId(), query);
            model.addAttribute("tickets", ticketPage.getContent());
            addPaginationAttributes(model, ticketPage, query);
        } else {
            model.addAttribute("tickets", Collections.emptyList());
            addPaginationAttributes(model, Page.empty(), query);
        }
        addQueryAttributes(model, query, status, priority);
        return "requests/list";
    }

    // ── USER: New Request form ───────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @GetMapping("/new")
    public String newRequestForm(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "USER");
        model.addAttribute("formData", new ServiceRequestForm());
        return "requests/new";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @PostMapping("/new")
    public String submitRequest(
            @ModelAttribute("formData") @Validated ServiceRequestForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal User principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("userRole", principal != null ? principal.getRole().name() : "USER");
            return "requests/new";
        }

        UUID requesterId = principal == null ? null : principal.getId();

        if (requesterId == null) {
            model.addAttribute("error", "Could not identify the logged-in user. Please log in and try again.");
            model.addAttribute("userRole", "USER");
            return "requests/new";
        }

        try {
            serviceRequestService.create(serviceRequestMapper.toCreateRequest(form, requesterId));
            redirectAttributes.addFlashAttribute("success", "Your request has been submitted successfully.");
            return "redirect:/requests/new";
        } catch (Exception e) {
            model.addAttribute("error", "Something went wrong while submitting your request. Please try again.");
            model.addAttribute("userRole", principal.getRole().name());
            return "requests/new";
        }
    }

    // ── USER: Request History ────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @GetMapping("/history")
    public String requestHistory(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "USER");
        if (principal != null) {
            model.addAttribute("history",
                    serviceRequestService.findAllByRequesterId(principal.getId()));
        } else {
            model.addAttribute("history", Collections.emptyList());
        }
        return "requests/history";
    }

    // ── AGENT: Assigned Tickets ──────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @GetMapping("/assigned")
    public String assignedTickets(Model model,
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "1") int page) {
        ServiceRequestPageQuery query = ServiceRequestPageQuery.builder()
                .page(page)
                .size(PAGE_SIZE)
                .q(q)
                .status(parseRequestStatus(status))
                .priority(parseRequestPriority(priority))
                .build();
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "AGENT");
        if (principal == null) {
            model.addAttribute("assignedTickets", Collections.emptyList());
            addPaginationAttributes(model, Page.empty(), query);
        } else {
            Page<?> assignedPage = serviceRequestService.findAllByAssignedToId(principal.getId(), query);
            model.addAttribute("assignedTickets", assignedPage.getContent());
            addPaginationAttributes(model, assignedPage, query);
        }
        addQueryAttributes(model, query, status, priority);
        return "requests/assigned";
    }

    // ── AGENT: Open Queue ────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @GetMapping("/open")
    public String openQueue(Model model, @AuthenticationPrincipal User principal) {
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "AGENT");
        model.addAttribute("openTickets", Collections.emptyList());
        return "requests/open";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @PostMapping("/{id}/assign")
    public String pickUpTicket(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "Ticket picked up successfully.");
        return "redirect:/requests/assigned";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @PostMapping("/assigned/{id}/transition")
    public String transitionAssignedTicket(@PathVariable UUID id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            RedirectAttributes redirectAttributes) {
        try {
            workflowService.transitionStatus(id);
            redirectAttributes.addFlashAttribute("success", "Ticket status transitioned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not transition ticket status: " + e.getMessage());
        }
        return buildAssignedRedirect(page, q, status, priority);
    }

    private String buildAssignedRedirect(int page, String q, String status, String priority) {
        StringBuilder redirect = new StringBuilder("redirect:/requests/assigned?page=").append(Math.max(page, 1));
        appendQueryParam(redirect, "q", q);
        appendQueryParam(redirect, "status", status);
        appendQueryParam(redirect, "priority", priority);
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

    private void addPaginationAttributes(Model model, Page<?> pageData, ServiceRequestPageQuery query) {
        model.addAttribute("currentPage", Math.max(query.getPage(), 1));
        model.addAttribute("totalPages", pageData.getTotalPages());
        model.addAttribute("totalItems", pageData.getTotalElements());
        model.addAttribute("hasPrevious", pageData.hasPrevious());
        model.addAttribute("hasNext", pageData.hasNext());
        model.addAttribute(
                "pageNumbers",
                pageData.getTotalPages() == 0
                        ? Collections.emptyList()
                        : buildPaginationItems(Math.max(query.getPage(), 1), pageData.getTotalPages())
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
}
