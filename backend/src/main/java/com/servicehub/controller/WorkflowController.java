package com.servicehub.controller;

import com.servicehub.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
/**
 * Handles workflow transition endpoints for service requests.
 */

@Slf4j
@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * Transition a service request to a new status
     * POST /api/workflow/requests/{requestId}/transition
     */
    @PostMapping("/requests/{requestId}/transition")
    public ResponseEntity<Void> transitionStatus(
            @PathVariable UUID requestId) {

        workflowService.transitionStatus(requestId);
        return ResponseEntity.ok().build();
    }
}
