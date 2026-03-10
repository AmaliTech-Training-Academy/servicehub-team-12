package com.servicehub.service.impl;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.servicehub.exception.InvalidTransitionException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.mapper.ServiceRequestMapper;
import com.servicehub.model.ServiceRequest;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.service.ServiceRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.servicehub.model.enums.RequestStatus;
import com.servicehub.service.WorkflowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final ServiceRequestMapper serviceRequestMapper;

    private static final Map<RequestStatus, RequestStatus> NEXT_STATUS = Map.of(
            RequestStatus.OPEN, RequestStatus.ASSIGNED,
            RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS,
            RequestStatus.IN_PROGRESS, RequestStatus.RESOLVED,
            RequestStatus.RESOLVED, RequestStatus.CLOSED
    );

    private final ServiceRequestService serviceRequestService;
    private final ServiceRequestRepository serviceRequestRepository;

    @Override
    @Transactional
    public void transitionStatus(UUID requestId) {

        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId).orElseThrow(
                () -> new ResourceNotFoundException("Service request not found: " + requestId + " was not found")
        );

        RequestStatus currentStatus = serviceRequest.getStatus();
        System.out.println("current " + currentStatus);
        RequestStatus nextStatus = NEXT_STATUS.get(currentStatus);

        if (nextStatus == null) {
            throw new InvalidTransitionException("Invalid transition from " + currentStatus);
        }

        serviceRequest.setStatus(nextStatus);

        updateTimestamps(serviceRequest, nextStatus);

        serviceRequest.setUpdatedAt(OffsetDateTime.now());

        log.info("Successfully transitioned request {} from {} to {}",
                requestId, currentStatus, nextStatus);
    }

    /**
     * Updates timestamps based on status transitions
     * Service layer explicitly manages all timestamps
     */
    private void updateTimestamps(ServiceRequest serviceRequest, RequestStatus newStatus) {
        OffsetDateTime now = OffsetDateTime.now();

        switch (newStatus) {
            case ASSIGNED:
                if (serviceRequest.getFirstResponseAt() == null) {
                    serviceRequest.setFirstResponseAt(now);
                    log.debug("Set first_response_at for request {}", serviceRequest.getId());
                }
                break;

            case RESOLVED:
                serviceRequest.setResolvedAt(now);
                log.debug("Set resolved_at for request {}", serviceRequest.getId());
                break;

            case CLOSED:
                serviceRequest.setClosedAt(now);
                log.debug("Set closed_at for request {}", serviceRequest.getId());
                break;

            default:
                break;
        }
    }
}
