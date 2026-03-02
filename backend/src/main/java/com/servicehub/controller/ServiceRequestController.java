package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.model.User;
import com.servicehub.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class ServiceRequestController {
    private final ServiceRequestService requestService;

    @GetMapping
    public ResponseEntity<Page<ServiceRequestResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(requestService.getAllRequests(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(
            @Valid @RequestBody ServiceRequestDto dto,
            @AuthenticationPrincipal User requester) {
        return ResponseEntity.ok(requestService.createRequest(dto, requester));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ServiceRequestResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal User agent) {
        return ResponseEntity.ok(requestService.updateStatus(id, request, agent));
    }

    // TODO: Add assign endpoint - PUT /api/requests/{id}/assign
    // TODO: Add my-requests endpoint - GET /api/requests/my-requests
    // TODO: Add dashboard stats endpoint - GET /api/requests/dashboard
}
