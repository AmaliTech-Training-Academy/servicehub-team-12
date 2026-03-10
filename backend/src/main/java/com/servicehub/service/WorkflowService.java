package com.servicehub.service;

import java.util.List;
import java.util.UUID;

import com.servicehub.exception.InvalidTransitionException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;

public interface WorkflowService {
    
    /**
     * Validates if a status transition is allowed
     * @param currentStatus the current status
     * @param newStatus the desired new status
     * @return true if transition is valid, false otherwise
     */
    boolean isValidTransition(RequestStatus currentStatus, RequestStatus newStatus);
    
    /**
     * Transitions a service request to a new status
     * Only AGENT and ADMIN roles can perform transitions
     * @param requestId the ID of the service request
     * @param newStatus the new status to transition to
     * @return the updated ServiceRequest
     * @throws InvalidTransitionException if transition is not allowed
     */
    void transitionStatus(UUID requestId, RequestStatus newStatus);
    
    /**
     * Gets all valid next statuses for a given current status
     * @param currentStatus the current status
     * @return list of valid next statuses
     */
    List<RequestStatus> getValidNextStatuses(RequestStatus currentStatus);
    
    /**
     * Validates workflow rules before transition
     * @param serviceRequest the service request
     * @param newStatus the new status
     * @throws InvalidTransitionException if validation fails
     */
    void validateTransition(ServiceRequest serviceRequest, RequestStatus newStatus);
}
