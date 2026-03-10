package com.servicehub.controller.view;

import com.servicehub.dto.ServiceRequestForm;
import com.servicehub.mapper.ServiceRequestMapper;
import com.servicehub.model.User;
import com.servicehub.service.ServiceRequestService;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {

    private final ServiceRequestService serviceRequestService;
    private final ServiceRequestMapper serviceRequestMapper;

    // ── USER: My Requests ────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'USER')")
    @GetMapping
    public String myRequests(Model model,
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        String role = principal != null ? principal.getRole().name() : "USER";
        model.addAttribute("userRole", role);
        if (principal != null) {
            model.addAttribute("currentUserName", principal.getFullName());
            model.addAttribute("tickets",
                    serviceRequestService.findAllByRequesterId(principal.getId()));
        } else {
            model.addAttribute("tickets", Collections.emptyList());
        }
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
            @RequestParam(required = false) String priority) {
        model.addAttribute("userRole", principal != null ? principal.getRole().name() : "AGENT");
        model.addAttribute("assignedTickets", Collections.emptyList());
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
}
