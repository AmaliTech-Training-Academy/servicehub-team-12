package com.servicehub.service;

import com.servicehub.dto.HealthResponse;
/**
 * Defines health status lookup operations.
 */

public interface HealthService {

    HealthResponse getHealth();
}

