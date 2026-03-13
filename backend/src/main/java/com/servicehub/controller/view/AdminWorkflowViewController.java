package com.servicehub.controller.view;

import com.servicehub.service.WorkflowService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/requests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminWorkflowViewController {

    private final WorkflowService workflowService;

    @PostMapping("/{requestId}/transition")
    public String transitionStatus(@PathVariable UUID requestId,
                                   @RequestParam(defaultValue = "false") boolean redirectToBreaches,
                                   RedirectAttributes redirectAttributes) {
        try {
            workflowService.transitionStatus(requestId);
            redirectAttributes.addFlashAttribute("success", "Request status transitioned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not transition request status: " + e.getMessage());
        }
        return redirectToBreaches
                ? "redirect:/admin/requests?breached=true"
                : "redirect:/admin/requests";
    }
}
