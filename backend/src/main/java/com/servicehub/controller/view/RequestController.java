package com.servicehub.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

@Controller
@RequestMapping("/requests")
public class RequestController {

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
        model.addAttribute("formData", null);
        return "requests/new";
    }

    @PostMapping("/new")
    @SuppressWarnings("unused")
    public String submitRequest(@RequestParam String title,
                                @RequestParam String category,
                                @RequestParam String priority,
                                @RequestParam String description,
                                RedirectAttributes redirectAttributes) {
        // TODO: persist ticket via service layer
        redirectAttributes.addFlashAttribute("success",
                "Your request has been submitted successfully.");
        return "redirect:/requests/new";
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
