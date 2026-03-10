package com.servicehub.service;

import java.util.UUID;

import com.servicehub.exception.InvalidTransitionException;

public interface WorkflowService {

    
    /**
     * Transitions a service request to a new status
     * Only AGENT and ADMIN roles can perform transitions
     *
     * @param requestId the ID of the service request
     * @throws InvalidTransitionException if transition is not allowed
     */
    void transitionStatus(UUID requestId);
}
