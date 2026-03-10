package com.servicehub.service;

import java.util.List;

import com.servicehub.exception.InvalidTransitionException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;

public interface WorkflowService {

    
    /**
     * Transitions a service request to a new status
     * Only AGENT and ADMIN roles can perform transitions
     *
     * @param serviceRequest the ID of the service request
     * @return the updated ServiceRequest
     * @throws InvalidTransitionException if transition is not allowed
     */
    void transitionStatus(ServiceRequest serviceRequest);
}
