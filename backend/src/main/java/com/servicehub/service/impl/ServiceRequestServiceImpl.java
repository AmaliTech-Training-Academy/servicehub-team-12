package com.servicehub.service.impl;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import com.servicehub.model.Department;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.SlaPolicy;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.DepartmentRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.SlaPolicyRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.ServiceRequestService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ServiceRequestServiceImpl implements ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final SlaPolicyRepository slaPolicyRepository;

    @Override
    @Transactional
    public ServiceRequestResponse create(ServiceRequestUpsertRequest request) {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setTitle(request.getTitle().trim());
        serviceRequest.setDescription(request.getDescription());
        serviceRequest.setCategory(request.getCategory());
        serviceRequest.setPriority(request.getPriority());
        serviceRequest.setStatus(request.getStatus() == null ? RequestStatus.OPEN : request.getStatus());
        serviceRequest.setRequester(getUserOrThrow(request.getRequesterId(), "Requester not found"));
        serviceRequest.setAssignedTo(getOptionalUser(request.getAssignedToId(), "Assigned user not found"));

        Department department = resolveDepartment(request.getCategory(), request.getDepartmentId());
        serviceRequest.setDepartment(department);
        serviceRequest.setSlaDeadline(resolveSlaDeadline(request.getCategory(), request.getPriority()));
        handleStatusChangeMetadata(serviceRequest, null, serviceRequest.getStatus());
        updateSlaBreachedFlag(serviceRequest);

        return toResponse(serviceRequestRepository.save(serviceRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> findAll() {
        return serviceRequestRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestResponse findById(UUID id) {
        return toResponse(getRequestOrThrow(id));
    }

    @Override
    @Transactional
    public ServiceRequestResponse update(UUID id, ServiceRequestUpsertRequest request) {
        ServiceRequest existing = getRequestOrThrow(id);
        RequestStatus oldStatus = existing.getStatus();

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title must not be blank");
            }
            existing.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }

        boolean categoryChanged = false;
        boolean priorityChanged = false;

        if (request.getCategory() != null) {
            existing.setCategory(request.getCategory());
            categoryChanged = true;
        }

        if (request.getPriority() != null) {
            existing.setPriority(request.getPriority());
            priorityChanged = true;
        }

        if (request.getRequesterId() != null) {
            existing.setRequester(getUserOrThrow(request.getRequesterId(), "Requester not found"));
        }

        if (request.getAssignedToId() != null) {
            existing.setAssignedTo(getUserOrThrow(request.getAssignedToId(), "Assigned user not found"));
        }

        if (categoryChanged || request.getDepartmentId() != null) {
            Department department = resolveDepartment(existing.getCategory(), request.getDepartmentId());
            existing.setDepartment(department);
        }

        if (categoryChanged || priorityChanged) {
            existing.setSlaDeadline(resolveSlaDeadline(existing.getCategory(), existing.getPriority()));
        }

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
            handleStatusChangeMetadata(existing, oldStatus, request.getStatus());
        }

        updateSlaBreachedFlag(existing);
        return toResponse(serviceRequestRepository.save(existing));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ServiceRequest serviceRequest = getRequestOrThrow(id);
        serviceRequestRepository.delete(serviceRequest);
    }

    private ServiceRequest getRequestOrThrow(UUID id) {
        return serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service request not found"));
    }

    private User getUserOrThrow(UUID userId, String message) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
    }

    private User getOptionalUser(UUID userId, String message) {
        if (userId == null) {
            return null;
        }
        return getUserOrThrow(userId, message);
    }

    private Department resolveDepartment(com.servicehub.model.enums.RequestCategory category, UUID departmentId) {
        if (departmentId != null) {
            return departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
        }
        return departmentRepository.findByCategory(category).orElse(null);
    }

    private OffsetDateTime resolveSlaDeadline(
            com.servicehub.model.enums.RequestCategory category,
            com.servicehub.model.enums.RequestPriority priority) {
        return slaPolicyRepository.findByCategoryAndPriority(category, priority)
                .map(SlaPolicy::getResolutionTimeHours)
                .map(hours -> OffsetDateTime.now(ZoneOffset.UTC).plusHours(hours))
                .orElse(null);
    }

    private void handleStatusChangeMetadata(
            ServiceRequest serviceRequest, RequestStatus oldStatus, RequestStatus newStatus) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (serviceRequest.getFirstResponseAt() == null
                && newStatus != null
                && newStatus != RequestStatus.OPEN
                && oldStatus != newStatus) {
            serviceRequest.setFirstResponseAt(now);
        }

        if (newStatus == RequestStatus.RESOLVED && serviceRequest.getResolvedAt() == null) {
            serviceRequest.setResolvedAt(now);
        }

        if (newStatus == RequestStatus.CLOSED && serviceRequest.getClosedAt() == null) {
            serviceRequest.setClosedAt(now);
        }
    }

    private void updateSlaBreachedFlag(ServiceRequest serviceRequest) {
        boolean isResolvedOrClosed = serviceRequest.getStatus() == RequestStatus.RESOLVED
                || serviceRequest.getStatus() == RequestStatus.CLOSED;
        boolean breached = serviceRequest.getSlaDeadline() != null
                && OffsetDateTime.now(ZoneOffset.UTC).isAfter(serviceRequest.getSlaDeadline())
                && !isResolvedOrClosed;
        serviceRequest.setIsSlaBreached(breached);
    }

    private ServiceRequestResponse toResponse(ServiceRequest serviceRequest) {
        return ServiceRequestResponse.builder()
                .id(serviceRequest.getId())
                .title(serviceRequest.getTitle())
                .description(serviceRequest.getDescription())
                .category(serviceRequest.getCategory())
                .priority(serviceRequest.getPriority())
                .status(serviceRequest.getStatus())
                .departmentId(serviceRequest.getDepartment() == null ? null : serviceRequest.getDepartment().getId())
                .assignedToId(serviceRequest.getAssignedTo() == null ? null : serviceRequest.getAssignedTo().getId())
                .requesterId(serviceRequest.getRequester() == null ? null : serviceRequest.getRequester().getId())
                .slaDeadline(serviceRequest.getSlaDeadline())
                .firstResponseAt(serviceRequest.getFirstResponseAt())
                .resolvedAt(serviceRequest.getResolvedAt())
                .closedAt(serviceRequest.getClosedAt())
                .isSlaBreached(serviceRequest.getIsSlaBreached())
                .createdAt(serviceRequest.getCreatedAt())
                .updatedAt(serviceRequest.getUpdatedAt())
                .build();
    }
}
