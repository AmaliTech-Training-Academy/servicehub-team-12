package com.servicehub.controller.api;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import com.servicehub.service.ServiceRequestService;
import jakarta.validation.groups.Default;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/service-requests", "/api/requests"})
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @PostMapping
    public ResponseEntity<ServiceRequestResponse> create(
            @Validated({ServiceRequestUpsertRequest.Create.class, Default.class})
            @RequestBody ServiceRequestUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceRequestService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ServiceRequestResponse>> findAll() {
        return ResponseEntity.ok(serviceRequestService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceRequestService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> update(
            @PathVariable UUID id, @RequestBody ServiceRequestUpsertRequest request) {
        return ResponseEntity.ok(serviceRequestService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        serviceRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}