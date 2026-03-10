package com.servicehub.controller.view;

import com.servicehub.dto.ServiceRequestForm;
import com.servicehub.mapper.ServiceRequestMapper;
import com.servicehub.model.User;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.ServiceRequestService;
import java.security.Principal;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
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
    private final UserRepository userRepository;
    private final ServiceRequestMapper serviceRequestMapper;

    // ── USER: My Requests ────────────────────────────────────────

    @GetMapping
    @SuppressWarnings("unused")
    public String myRequests(Model model,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        model.addAttribute("userRole", "USER");
        model.addAttribute("tickets", Collections.emptyList());
        return "requests/list";
    }

    // ── USER: New Request form ───────────────────────────────────

    @GetMapping("/new")
    public String newRequestForm(Model model) {
        model.addAttribute("userRole", "USER");
        model.addAttribute("formData", new ServiceRequestForm());
        return "requests/new";
    }

    @PostMapping("/new")
    public String submitRequest(
            @ModelAttribute("formData") @Validated ServiceRequestForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("userRole", "USER");
            return "requests/new";
        }

        var requesterId = principal == null ? null
                : userRepository.findByEmail(principal.getName()).map(User::getId).orElse(null);

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
            model.addAttribute("userRole", "USER");
            return "requests/new";
        }
    }

    // ── USER: Request History ────────────────────────────────────

    @GetMapping("/history")
    public String requestHistory(Model model) {
        model.addAttribute("userRole", "USER");
        model.addAttribute("history", Collections.emptyList());
        return "requests/history";
    }

    // ── AGENT: Assigned Tickets ──────────────────────────────────

    @GetMapping("/assigned")
    @SuppressWarnings("unused")
    public String assignedTickets(Model model,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String priority) {
        model.addAttribute("userRole", "AGENT");
        model.addAttribute("assignedTickets", Collections.emptyList());
        return "requests/assigned";
    }

    // ── AGENT: Open Queue ────────────────────────────────────────

    @GetMapping("/open")
    public String openQueue(Model model) {
        model.addAttribute("userRole", "AGENT");
        model.addAttribute("openTickets", Collections.emptyList());
        return "requests/open";
    }

    @PostMapping("/{id}/assign")
    @SuppressWarnings("unused")
    public String pickUpTicket(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        // TODO: assign ticket to current agent via service layer
        redirectAttributes.addFlashAttribute("success", "Ticket picked up successfully.");
        return "redirect:/requests/assigned";
    }
}
