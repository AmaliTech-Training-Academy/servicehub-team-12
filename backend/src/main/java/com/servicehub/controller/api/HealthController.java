package com.servicehub.controller.api;

import com.servicehub.dto.HealthResponse;
import com.servicehub.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Application health check — no authentication required")
public class HealthController {

    private final HealthService healthService;

    @Operation(summary = "Check application health")
    @ApiResponse(responseCode = "200", description = "Application is up")
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(healthService.getHealth());
    }
}
