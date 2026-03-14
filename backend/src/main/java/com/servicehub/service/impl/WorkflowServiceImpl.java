package com.servicehub.service.impl;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.servicehub.event.StatusTransitionEvent;
import com.servicehub.exception.InvalidTransitionException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.service.Notification;
import com.servicehub.service.ServiceRequestService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.servicehub.service.WorkflowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ResponseStatusException;

/**
 * Default {@link WorkflowService} implementation for advancing service requests
 * through the supported status lifecycle.
 *
 * <p>This service enforces the forward-only workflow defined by the application,
 * moving requests from {@link RequestStatus#OPEN} through to
 * {@link RequestStatus#CLOSED} while rejecting invalid transitions.
 *
 * <p>During transitions, it coordinates related side effects such as auto-assignment
 * when a request first becomes assigned, timestamp updates for first
 * response, resolution, and closure milestones, and publication of
 * {@link StatusTransitionEvent} notifications for asynchronous email delivery.
 *
 * <p>The implementation centralizes workflow state changes so business rules,
 * audit-relevant timestamps, and outbound notifications stay consistent no matter
 * which controller or caller initiates the transition.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final ApplicationEventPublisher publisher;

    private static final Map<RequestStatus, RequestStatus> NEXT_STATUS = Map.of(
            RequestStatus.OPEN, RequestStatus.ASSIGNED,
            RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS,
            RequestStatus.IN_PROGRESS, RequestStatus.RESOLVED,
            RequestStatus.RESOLVED, RequestStatus.CLOSED
    );

    private final ServiceRequestRepository serviceRequestRepository;
    private final Notification emailService;
    private final ServiceRequestService serviceRequestService;

    @Async
    @EventListener
    void handleStatusTransitionEvent(StatusTransitionEvent event) {
        emailService.sendStatusUpdate(
                event.requesterEmail(),
                event.requestTitle(),
                event.status()
        );
    }

    @Override
    @Transactional
    public void transitionStatus(UUID requestId) {

        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId).orElseThrow(
                () -> new ResourceNotFoundException("Service request not found: " + requestId + " was not found")
        );

        RequestStatus currentStatus = serviceRequest.getStatus();
        RequestStatus nextStatus = NEXT_STATUS.get(currentStatus);


        if (nextStatus == null) {
            throw new InvalidTransitionException("Invalid transition from " + currentStatus);
        }

        if (nextStatus == RequestStatus.ASSIGNED) {
            serviceRequestService.autoAssign(requestId);

            if (serviceRequest.getStatus() != RequestStatus.ASSIGNED) {
                throw new InvalidTransitionException("Could not auto-assign request " + requestId);
            }
        } else {
            serviceRequest.setStatus(nextStatus);
        }

        updateTimestamps(serviceRequest, nextStatus);

        publisher.publishEvent(new StatusTransitionEvent(
                serviceRequest.getRequester().getEmail(),
                serviceRequest.getTitle(),
                serviceRequest.getStatus()
        ));

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
            case IN_PROGRESS:
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
