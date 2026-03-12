package com.servicehub.event;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;

public record StatusTransitionEvent(ServiceRequest request) {
}
