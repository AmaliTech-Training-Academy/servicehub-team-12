package com.servicehub.event;

import com.servicehub.model.ServiceRequest;

public record ServiceRequestCreatedEvent(ServiceRequest request) {
}
