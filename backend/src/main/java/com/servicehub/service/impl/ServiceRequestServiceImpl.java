package com.servicehub.service.impl;

import com.servicehub.dto.ServiceRequestPageQuery;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import com.servicehub.event.ServiceRequestCreatedEvent;
import com.servicehub.mapper.ServiceRequestMapper;
import com.servicehub.model.Department;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.exception.AccessDeniedException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.repository.DepartmentRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.specification.ServiceRequestSpecifications;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.assignment.AutoAssignmentStrategy;
import com.servicehub.service.ServiceRequestService;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
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
    private final ApplicationEventPublisher eventPublisher;
    private final ServiceRequestMapper serviceRequestMapper;
    private final AutoAssignmentStrategy autoAssignmentStrategy;

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
        serviceRequest.setAssignedTo(getOptionalUser(request.getAssignedToId()));

        Department department = resolveDepartment(request.getCategory(), request.getDepartmentId());
        serviceRequest.setDepartment(department);
        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);

        if (savedRequest.getAssignedTo() == null) {
            autoAssign(savedRequest.getId());
        }

        eventPublisher.publishEvent(new ServiceRequestCreatedEvent(savedRequest));
        return serviceRequestMapper.toResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> findAll() {
        return serviceRequestRepository.findAll().stream()
                .map(serviceRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> findPage(ServiceRequestPageQuery query) {
        return findPageByQuery(query);
    }

    private Page<ServiceRequestResponse> findPageByQuery(ServiceRequestPageQuery query) {
        return serviceRequestRepository.findAll(
                ServiceRequestSpecifications.fromQuery(query),
                query.toPageable()
        ).map(serviceRequestMapper::toResponse);
    }

    private List<ServiceRequestResponse> mapRequesterRequests(User user) {
        return serviceRequestRepository.findAllByRequester(user).stream()
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(serviceRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceRequestResponse> findAllByRequesterId(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        return mapRequesterRequests(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> findAllByRequesterId(UUID userId, ServiceRequestPageQuery query) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        return findPageByQuery(query.forRequester(userId));
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> findAllByAssignedToId(UUID userId, ServiceRequestPageQuery query) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        return findPageByQuery(query.forAssignee(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestResponse findById(UUID id) {
        return serviceRequestMapper.toResponse(getRequestOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceRequestResponse findByIdForUser(UUID id, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        ServiceRequest request = serviceRequestRepository.findByIdAndRequester(id, user)
                .orElseThrow(AccessDeniedException::new);
        return serviceRequestMapper.toResponse(request);
    }

    @Override
    @Transactional
    public ServiceRequestResponse update(UUID id, ServiceRequestUpsertRequest request) {
        ServiceRequest existing = getRequestOrThrow(id);
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

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        ServiceRequest savedRequest = serviceRequestRepository.save(existing);
        if (categoryChanged || priorityChanged) {
            eventPublisher.publishEvent(new ServiceRequestCreatedEvent(savedRequest));
        }
        return serviceRequestMapper.toResponse(savedRequest);
    }

    @Override
    @Transactional
    public void autoAssign(UUID id) {
        ServiceRequest serviceRequest = getRequestOrThrow(id);

        if (serviceRequest.getAssignedTo() != null || serviceRequest.getDepartment() == null) {
            return;
        }

        autoAssignmentStrategy.selectAssignee(serviceRequest)
                .ifPresent(assignee -> {
                    serviceRequest.setAssignedTo(assignee);

                    if (serviceRequest.getStatus() == RequestStatus.OPEN) {
                        serviceRequest.setStatus(RequestStatus.ASSIGNED);

                        if (serviceRequest.getFirstResponseAt() == null) {
                            serviceRequest.setFirstResponseAt(OffsetDateTime.now());
                        }
                    }
                });
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

    private User getOptionalUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return getUserOrThrow(userId, "Assigned user not found");
    }

    private Department resolveDepartment(com.servicehub.model.enums.RequestCategory category, UUID departmentId) {
        if (departmentId != null) {
            return departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
        }
        return departmentRepository.findByCategory(category).orElse(null);
    }

}
