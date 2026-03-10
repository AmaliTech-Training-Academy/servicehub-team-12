package com.servicehub.service.impl;

import com.servicehub.dto.HealthResponse;
import com.servicehub.model.enums.HealthStatus;
import com.servicehub.service.HealthService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class HealthServiceImpl implements HealthService {

    @Override
    public HealthResponse getHealth() {
        return HealthResponse.builder()
                .status(HealthStatus.UP.name())
                .message("OK")
                .timestamp(LocalDateTime.now())
                .build();
    }
}

