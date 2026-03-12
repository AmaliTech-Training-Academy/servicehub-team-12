package com.servicehub.service;

import com.servicehub.model.ServiceRequest;
import java.time.OffsetDateTime;

public interface SlaService {

    /**
     * Calculates and sets the SLA deadline for a service request
     * Uses category + priority to lookup SLA policy and calculate deadline
     * @param request the service request
     * @return the calculated deadline
     */
    OffsetDateTime calculateAndSetSlaDeadline(ServiceRequest request);

    /**
     * Checks if a service request has breached its SLA deadline
     * @param request the service request to check
     * @return true if breached, false otherwise
     */
    boolean checkSlaBreached(ServiceRequest request);

    /**
     * Detects and updates SLA breach status for a specific request
     * Sets is_sla_breached flag if current time exceeds sla_deadline
     */
    void detectAndUpdateBreachStatus();

    /**
     * Calculates response time in hours for a resolved request
     * Response time = first_response_at - created_at
     * @param request the service request
     * @return response time in hours, or null if not yet responded
     */
    Double calculateResponseTimeHours(ServiceRequest request);

    /**
     * Calculates resolution time in hours for a resolved request
     * Resolution time = resolved_at - created_at
     * @param request the service request
     * @return resolution time in hours, or null if not yet resolved
     */
    Double calculateResolutionTimeHours(ServiceRequest request);
}
