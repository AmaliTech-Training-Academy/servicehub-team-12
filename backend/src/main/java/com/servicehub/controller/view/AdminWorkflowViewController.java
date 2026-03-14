package com.servicehub.controller.view;

import com.servicehub.service.WorkflowService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
/**
 * Serves administrator workflow views for managing requests.
 */

@Controller
@RequestMapping("/admin/requests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminWorkflowViewController {

    private final WorkflowService workflowService;

    @PostMapping("/{requestId}/transition")
    public String transitionStatus(@PathVariable UUID requestId,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String priority,
                                   @RequestParam(defaultValue = "false") boolean redirectToBreaches,
                                   RedirectAttributes redirectAttributes) {
        try {
            workflowService.transitionStatus(requestId);
            redirectAttributes.addFlashAttribute("success", "Request status transitioned successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not transition request status: " + e.getMessage());
        }
        StringBuilder redirect = new StringBuilder("redirect:/admin/requests?page=")
                .append(Math.max(page, 1));

        if (redirectToBreaches) {
            redirect.append("&breached=true");
        }
        if (q != null && !q.isBlank()) {
            redirect.append("&q=").append(encode(q));
        }
        if (status != null && !status.isBlank()) {
            redirect.append("&status=").append(encode(status));
        }
        if (priority != null && !priority.isBlank()) {
            redirect.append("&priority=").append(encode(priority));
        }
        return redirect.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
