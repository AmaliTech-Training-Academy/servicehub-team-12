package com.servicehub.event;

import com.servicehub.model.enums.RequestStatus;

public record StatusTransitionEvent(String requesterEmail, String requestTitle, RequestStatus status) {
}
