package com.servicehub.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import com.servicehub.event.ServiceRequestCreatedEvent;
import com.servicehub.mapper.ServiceRequestMapper;
import com.servicehub.model.Department;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.DepartmentRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceRequestService")
class ServiceRequestServiceImplTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ServiceRequestMapper serviceRequestMapper;

    private ServiceRequestServiceImpl serviceRequestService;

    @BeforeEach
    void setUp() {
        serviceRequestService = new ServiceRequestServiceImpl(
                serviceRequestRepository, departmentRepository, userRepository, eventPublisher, serviceRequestMapper);
    }

    @Test
    @DisplayName("create: builds request, auto-routes department, and publishes SLA event")
    void createShouldBuildAndPersistWithAutoRoutedDepartmentAndPublishSlaEvent() {
        UUID requesterId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        User requester = user(requesterId);
        Department department = department(departmentId, RequestCategory.IT_SUPPORT);

        ServiceRequestUpsertRequest request = new ServiceRequestUpsertRequest();
        request.setTitle("  Laptop issue  ");
        request.setDescription("Screen is blank");
        request.setCategory(RequestCategory.IT_SUPPORT);
        request.setPriority(RequestPriority.HIGH);
        request.setRequesterId(requesterId);

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(departmentRepository.findByCategory(RequestCategory.IT_SUPPORT)).thenReturn(Optional.of(department));
        when(serviceRequestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceRequestMapper.toResponse(any(ServiceRequest.class)))
                .thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        ServiceRequestResponse response = serviceRequestService.create(request);

        assertEquals("Laptop issue", response.getTitle());
        assertEquals(RequestStatus.OPEN, response.getStatus());
        assertEquals(departmentId, response.getDepartmentId());
        assertEquals(requesterId, response.getRequesterId());
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof ServiceRequestCreatedEvent(ServiceRequest request1)
                        && request1.getRequester().getId().equals(requesterId)
                        && request1.getDepartment().getId().equals(departmentId)));
    }

    @Test
    @DisplayName("create: throws BAD_REQUEST when requester does not exist")
    void createShouldThrowWhenRequesterDoesNotExist() {
        UUID missingRequesterId = UUID.randomUUID();
        ServiceRequestUpsertRequest request = new ServiceRequestUpsertRequest();
        request.setTitle("Printer not working");
        request.setCategory(RequestCategory.IT_SUPPORT);
        request.setPriority(RequestPriority.MEDIUM);
        request.setRequesterId(missingRequesterId);

        when(userRepository.findById(missingRequesterId)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> serviceRequestService.create(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(serviceRequestRepository, never()).save(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("findAll: returns mapped response list")
    void findAllShouldReturnMappedResponses() {
        ServiceRequest first = serviceRequest(UUID.randomUUID(), "First");
        ServiceRequest second = serviceRequest(UUID.randomUUID(), "Second");
        when(serviceRequestRepository.findAll()).thenReturn(List.of(first, second));
        when(serviceRequestMapper.toResponse(first)).thenReturn(toResponse(first));
        when(serviceRequestMapper.toResponse(second)).thenReturn(toResponse(second));

        List<ServiceRequestResponse> responses = serviceRequestService.findAll();

        assertEquals(2, responses.size());
        assertEquals("First", responses.get(0).getTitle());
        assertEquals("Second", responses.get(1).getTitle());
    }

    @Test
    @DisplayName("findById: returns mapped response when id exists")
    void findByIdShouldReturnSingleMappedResponse() {
        UUID requestId = UUID.randomUUID();
        ServiceRequest serviceRequest = serviceRequest(requestId, "Printer issue");
        when(serviceRequestRepository.findById(requestId)).thenReturn(Optional.of(serviceRequest));
        when(serviceRequestMapper.toResponse(serviceRequest)).thenReturn(toResponse(serviceRequest));

        ServiceRequestResponse response = serviceRequestService.findById(requestId);

        assertEquals(requestId, response.getId());
        assertEquals("Printer issue", response.getTitle());
    }

    @Test
    @DisplayName("findById: throws NOT_FOUND when id does not exist")
    void findByIdShouldThrowWhenMissing() {
        UUID requestId = UUID.randomUUID();
        when(serviceRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> serviceRequestService.findById(requestId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("update: applies field changes and sets resolution metadata")
    void updateShouldApplyChangesAndSetResolvedMetadata() {
        UUID requestId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        ServiceRequest existing = serviceRequest(requestId, "Old title");
        existing.setStatus(RequestStatus.OPEN);
        existing.setRequester(user(requesterId));

        ServiceRequestUpsertRequest updateRequest = new ServiceRequestUpsertRequest();
        updateRequest.setTitle("New title");
        updateRequest.setPriority(RequestPriority.CRITICAL);
        updateRequest.setCategory(RequestCategory.FACILITIES);
        updateRequest.setAssignedToId(assigneeId);
        updateRequest.setStatus(RequestStatus.RESOLVED);

        Department routedDepartment = department(departmentId, RequestCategory.FACILITIES);
        when(serviceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(user(assigneeId)));
        when(departmentRepository.findByCategory(RequestCategory.FACILITIES)).thenReturn(Optional.of(routedDepartment));
        when(serviceRequestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceRequestMapper.toResponse(any(ServiceRequest.class)))
                .thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        ServiceRequestResponse response = serviceRequestService.update(requestId, updateRequest);

        assertEquals("New title", response.getTitle());
        assertEquals(RequestCategory.FACILITIES, response.getCategory());
        assertEquals(RequestPriority.CRITICAL, response.getPriority());
        assertEquals(RequestStatus.RESOLVED, response.getStatus());
        assertEquals(departmentId, response.getDepartmentId());
        assertEquals(assigneeId, response.getAssignedToId());
        verify(eventPublisher).publishEvent(argThat((Object event) ->
                event instanceof ServiceRequestCreatedEvent createdEvent
                        && createdEvent.request().getId().equals(requestId)
                        && createdEvent.request().getCategory() == RequestCategory.FACILITIES
                        && createdEvent.request().getPriority() == RequestPriority.CRITICAL));
    }

    @Test
    @DisplayName("update: throws BAD_REQUEST for blank title")
    void updateShouldRejectBlankTitle() {
        UUID requestId = UUID.randomUUID();
        ServiceRequest existing = serviceRequest(requestId, "Existing");
        when(serviceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));

        ServiceRequestUpsertRequest updateRequest = new ServiceRequestUpsertRequest();
        updateRequest.setTitle("   ");

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> serviceRequestService.update(requestId, updateRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("delete: removes existing request")
    void deleteShouldRemoveExistingRequest() {
        UUID requestId = UUID.randomUUID();
        ServiceRequest existing = serviceRequest(requestId, "Delete me");
        when(serviceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));

        serviceRequestService.delete(requestId);

        verify(serviceRequestRepository).delete(existing);
    }

    @Test
    @DisplayName("autoAssign: assigns first matching agent in routed department")
    void autoAssignShouldAssignFirstMatchingAgent() {
        UUID requestId = UUID.randomUUID();
        ServiceRequest request = serviceRequest(requestId, "Route me");
        request.setDepartment(department(UUID.randomUUID(), RequestCategory.IT_SUPPORT));
        request.getDepartment().setName("IT");

        User agent = user(UUID.randomUUID());
        agent.setRole(Role.AGENT);
        agent.setDepartment("IT");

        when(serviceRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findFirstByRoleAndDepartmentIgnoreCaseOrderByCreatedAtAsc(Role.AGENT, "IT"))
                .thenReturn(Optional.of(agent));

        serviceRequestService.autoAssign(requestId);

        assertEquals(agent.getId(), request.getAssignedTo().getId());
    }

    @Test
    @DisplayName("autoAssign: leaves request unchanged when already assigned")
    void autoAssignShouldNotOverrideExistingAssignment() {
        UUID requestId = UUID.randomUUID();
        ServiceRequest request = serviceRequest(requestId, "Already assigned");
        User existingAgent = user(UUID.randomUUID());
        existingAgent.setRole(Role.AGENT);
        request.setAssignedTo(existingAgent);
        request.setDepartment(department(UUID.randomUUID(), RequestCategory.IT_SUPPORT));

        when(serviceRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        serviceRequestService.autoAssign(requestId);

        assertEquals(existingAgent.getId(), request.getAssignedTo().getId());
        verify(userRepository, never()).findFirstByRoleAndDepartmentIgnoreCaseOrderByCreatedAtAsc(any(), any());
    }

    @Test
    @DisplayName("autoAssign: leaves request unassigned when no department agent exists")
    void autoAssignShouldLeaveRequestUnassignedWhenNoMatchingAgentExists() {
        UUID requestId = UUID.randomUUID();
        ServiceRequest request = serviceRequest(requestId, "No agent available");
        request.setDepartment(department(UUID.randomUUID(), RequestCategory.IT_SUPPORT));
        request.getDepartment().setName("IT");

        when(serviceRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findFirstByRoleAndDepartmentIgnoreCaseOrderByCreatedAtAsc(Role.AGENT, "IT"))
                .thenReturn(Optional.empty());

        serviceRequestService.autoAssign(requestId);

        assertNull(request.getAssignedTo());
    }

    @Test
    @DisplayName("create: leaves firstResponseAt null when initial status is OPEN")
    void createShouldNotSetFirstResponseAtWhenStatusIsOpen() {
        UUID requesterId = UUID.randomUUID();
        ServiceRequestUpsertRequest request = new ServiceRequestUpsertRequest();
        request.setTitle("Reset password");
        request.setCategory(RequestCategory.HR_REQUEST);
        request.setPriority(RequestPriority.LOW);
        request.setRequesterId(requesterId);
        request.setStatus(RequestStatus.OPEN);

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(user(requesterId)));
        when(departmentRepository.findByCategory(RequestCategory.HR_REQUEST))
                .thenReturn(Optional.of(department(UUID.randomUUID(), RequestCategory.HR_REQUEST)));
        when(serviceRequestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceRequestMapper.toResponse(any(ServiceRequest.class)))
                .thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        ServiceRequestResponse response = serviceRequestService.create(request);

        assertNull(response.getFirstResponseAt());
    }

    private User user(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@amalitech.com");
        user.setFullName("User");
        user.setRole(Role.USER);
        return user;
    }

    private Department department(UUID id, RequestCategory category) {
        Department department = new Department();
        department.setId(id);
        department.setName("IT");
        department.setCategory(category);
        return department;
    }

    private ServiceRequest serviceRequest(UUID id, String title) {
        ServiceRequest request = new ServiceRequest();
        request.setId(id);
        request.setTitle(title);
        request.setCategory(RequestCategory.IT_SUPPORT);
        request.setPriority(RequestPriority.HIGH);
        request.setStatus(RequestStatus.OPEN);
        request.setIsSlaBreached(Boolean.FALSE);
        request.setRequester(user(UUID.randomUUID()));
        return request;
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
