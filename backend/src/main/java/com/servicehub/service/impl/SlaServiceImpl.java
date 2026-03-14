package com.servicehub.service.impl;

import com.servicehub.event.ServiceRequestCreatedEvent;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import com.servicehub.service.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.servicehub.exception.SlaPolicyNotFoundException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.SlaPolicy;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.SlaPolicyRepository;
import com.servicehub.service.SlaService;
import com.servicehub.service.WorkingHoursCalculator;
import lombok.RequiredArgsConstructor;

/**
 * Default {@link SlaService} implementation for applying SLA policy rules to
 * service requests.
 *
 * <p>This service resolves the correct {@link SlaPolicy} for a request's
 * category and priority, calculates business-hour-aware deadlines with
 * {@link WorkingHoursCalculator}, and stores the resulting SLA target on the
 * request entity.
 *
 * <p>It also monitors persisted requests for deadline breaches, updates the
 * breach flag when a ticket moves past its SLA deadline, and sends notification
 * messages to the requester and assignee when a breach is detected.
 *
 * <p>In addition to deadline management, the implementation exposes helper
 * calculations for response and resolution times so dashboard and reporting
 * features can derive timing metrics from request timestamps.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaServiceImpl implements SlaService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WorkingHoursCalculator workingHoursCalculator;
    private final Notification emailService;

    @EventListener
    public void handleServiceRequestCreated(ServiceRequestCreatedEvent event) {
        calculateAndSetSlaDeadline(event.request());
    }

    @Override
    @Transactional
    public OffsetDateTime calculateAndSetSlaDeadline(ServiceRequest request) {
        log.info("Calculating SLA deadline for request {} with category {} and priority {}",
                request.getId(), request.getCategory(), request.getPriority());

        SlaPolicy policy = slaPolicyRepository
                .findByCategoryAndPriority(request.getCategory(), request.getPriority())
                .orElseThrow(() -> new SlaPolicyNotFoundException(
                        String.format("No SLA policy found for category %s and priority %s",
                                request.getCategory(), request.getPriority())
                ));

        OffsetDateTime effectiveStart =
                workingHoursCalculator.getNextWorkingHoursStart(request.getCreatedAt());

        OffsetDateTime deadline =
                workingHoursCalculator.addBusinessHours(effectiveStart, policy.getResolutionTimeHours());

        request.setSlaDeadline(deadline);

        log.info("SLA deadline set to {} for request {}", deadline, request.getId());
        return deadline;
    }

    @Override
    public boolean checkSlaBreached(ServiceRequest request) {
        if (request.getSlaDeadline() == null) {
            log.warn("Request {} has no SLA deadline set", request.getId());
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean isBreached = now.isAfter(request.getSlaDeadline());

        if (isBreached) {
            log.warn("Request {} has breached SLA. Deadline: {}, Current time: {}",
                    request.getId(), request.getSlaDeadline(), now);
        }

        return isBreached;
    }

    @Override
    @Transactional
    public void detectAndUpdateBreachStatus() {

        List<ServiceRequest> requests = serviceRequestRepository
                .findRequestsPastDeadline(OffsetDateTime.now());

        for (ServiceRequest request : requests) {
            try {
                boolean isBreached = checkSlaBreached(request);

                if (request.getIsSlaBreached() == null || !request.getIsSlaBreached()) {
                    request.setIsSlaBreached(isBreached);
                    serviceRequestRepository.save(request);
                    emailService.sendSlaBreachNotification(request.getRequester().getEmail(), request);
                    emailService.sendSlaBreachNotification(request.getAssignedTo().getEmail(), request);

                    log.warn("SLA BREACH: Request {} ({}|{}) exceeded deadline: {}",
                            request.getId(),
                            request.getCategory(),
                            request.getPriority(),
                            request.getSlaDeadline());
                }
            } catch (Exception e) {
                log.error("Failed to update SLA breach for request {}: {}",
                        request.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public Double calculateResponseTimeHours(ServiceRequest request) {
        if (request.getFirstResponseAt() == null) {
            return null;
        }

        Duration duration = Duration.between(
                request.getCreatedAt(),
                request.getFirstResponseAt()
        );

        double hours = duration.toMinutes() / 60.0;
        log.debug("Response time for request {}: {} hours", request.getId(), hours);

        return hours;
    }

    @Override
    public Double calculateResolutionTimeHours(ServiceRequest request) {
        if (request.getResolvedAt() == null) {
            return null;
        }

        Duration duration = Duration.between(
                request.getCreatedAt(),
                request.getResolvedAt()
        );

        double hours = duration.toMinutes() / 60.0;
        log.debug("Resolution time for request {}: {} hours", request.getId(), hours);

        return hours;
    }
}
