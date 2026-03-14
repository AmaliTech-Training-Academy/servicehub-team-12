package com.servicehub.controller.api;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import com.servicehub.model.User;
import com.servicehub.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
/**
 * Exposes CRUD and query endpoints for service requests.
 */

@RestController
@RequestMapping({"/api/service-requests", "/api/requests"})
@RequiredArgsConstructor
@Tag(name = "Service Requests", description = "Create, view and manage service requests")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @Operation(summary = "Submit a new service request")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(
            @Validated({ServiceRequestUpsertRequest.Create.class, Default.class})
            @RequestBody ServiceRequestUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceRequestService.create(request));
    }

    @Operation(summary = "List service requests — ADMIN/AGENT see all; USER sees only their own")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<ServiceRequestResponse>> findAll(@AuthenticationPrincipal User principal) {
        if (principal != null) {
            return switch (principal.getRole()) {
                case ADMIN, AGENT -> ResponseEntity.ok(serviceRequestService.findAll());
                default -> ResponseEntity.ok(serviceRequestService.findAllByRequesterId(principal.getId()));
            };
        }
        return ResponseEntity.ok(serviceRequestService.findAll());
    }

    @Operation(summary = "Get a service request — ADMIN/AGENT unrestricted; USER restricted to own")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User principal) {
        if (principal != null) {
            return switch (principal.getRole()) {
                case ADMIN, AGENT -> ResponseEntity.ok(serviceRequestService.findById(id));
                default -> ResponseEntity.ok(serviceRequestService.findByIdForUser(id, principal.getId()));
            };
        }
        return ResponseEntity.ok(serviceRequestService.findById(id));
    }

    @Operation(summary = "Update a service request — ADMIN/AGENT only")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> update(
            @PathVariable UUID id,
            @RequestBody ServiceRequestUpsertRequest request) {
        return ResponseEntity.ok(serviceRequestService.update(id, request));
    }

    @Operation(summary = "Delete a service request — ADMIN only")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}