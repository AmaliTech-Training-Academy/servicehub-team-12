package com.servicehub.service.impl;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.servicehub.mapper.ServiceRequestMapper;
import com.servicehub.model.ServiceRequest;
import com.servicehub.service.ServiceRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.servicehub.exception.InvalidTransitionException;

import com.servicehub.model.enums.RequestStatus;
import com.servicehub.service.WorkflowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final ServiceRequestService serviceRequestService;
    private final ServiceRequestMapper serviceRequestMapper;

    private static final Map<RequestStatus, List<RequestStatus>> VALID_TRANSITIONS = Map.of(
        RequestStatus.OPEN, List.of(RequestStatus.ASSIGNED),
        RequestStatus.ASSIGNED, List.of(RequestStatus.IN_PROGRESS),
        RequestStatus.IN_PROGRESS, List.of(RequestStatus.RESOLVED),
        RequestStatus.RESOLVED, List.of(RequestStatus.CLOSED)
    );

    @Override
    public boolean isValidTransition(RequestStatus currentStatus, RequestStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        List<RequestStatus> validNextStatuses = VALID_TRANSITIONS.get(currentStatus);
        return validNextStatuses != null && validNextStatuses.contains(newStatus);
    }

    @Override
    @Transactional
    public void transitionStatus(UUID requestId, RequestStatus newStatus) {
        log.info("Attempting to transition request {} to status {}", requestId, newStatus);

        ServiceRequest serviceRequest = serviceRequestMapper.toEntity(serviceRequestService.findById(requestId));

        validateTransition(serviceRequest, newStatus);

        RequestStatus oldStatus = serviceRequest.getStatus();

        serviceRequest.setStatus(newStatus);

        updateTimestamps(serviceRequest, newStatus);

        serviceRequest.setUpdatedAt(OffsetDateTime.now());

        log.info("Successfully transitioned request {} from {} to {}",
            requestId, oldStatus, newStatus);
    }

    @Override
    public List<RequestStatus> getValidNextStatuses(RequestStatus currentStatus) {
        return VALID_TRANSITIONS.getOrDefault(currentStatus, Collections.emptyList());
    }

    @Override
    public void validateTransition(ServiceRequest serviceRequest, RequestStatus newStatus) {
        RequestStatus currentStatus = serviceRequest.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidTransitionException(
                String.format(
                    "Invalid status transition from %s to %s. Valid transitions from %s are: %s",
                    currentStatus,
                    newStatus,
                    currentStatus,
                    VALID_TRANSITIONS.getOrDefault(currentStatus, Collections.emptyList())
                )
            );
        }

        if (newStatus == RequestStatus.IN_PROGRESS && serviceRequest.getAssignedTo() == null) {
            throw new InvalidTransitionException(
                "Cannot move request to IN_PROGRESS without assigning it to an agent"
            );
        }
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
