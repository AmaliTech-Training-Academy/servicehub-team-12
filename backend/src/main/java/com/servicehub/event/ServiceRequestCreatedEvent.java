package com.servicehub.event;

import com.servicehub.model.ServiceRequest;
/**
 * Application event published when a service request is created.
 */

public record ServiceRequestCreatedEvent(ServiceRequest request) {
}
