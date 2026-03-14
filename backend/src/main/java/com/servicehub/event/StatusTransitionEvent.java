package com.servicehub.event;

import com.servicehub.model.enums.RequestStatus;
/**
 * Application event published when a service request changes workflow status.
 */

public record StatusTransitionEvent(String requesterEmail, String requestTitle, RequestStatus status) {
}
