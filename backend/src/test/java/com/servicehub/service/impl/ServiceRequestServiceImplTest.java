package com.servicehub.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import com.servicehub.model.Department;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.SlaPolicy;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.DepartmentRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.SlaPolicyRepository;
import com.servicehub.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private SlaPolicyRepository slaPolicyRepository;

    private ServiceRequestServiceImpl serviceRequestService;

    @BeforeEach
    void setUp() {
        serviceRequestService = new ServiceRequestServiceImpl(
                serviceRequestRepository, departmentRepository, userRepository, slaPolicyRepository);
    }

    @Test
    @DisplayName("create: builds request, auto-routes department, and computes SLA deadline")
    void createShouldBuildAndPersistWithAutoRoutedDepartmentAndSlaDeadline() {
        UUID requesterId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        User requester = user(requesterId);
        Department department = department(departmentId, RequestCategory.IT_SUPPORT);
        SlaPolicy slaPolicy = slaPolicy(RequestCategory.IT_SUPPORT, RequestPriority.HIGH, 8);

        ServiceRequestUpsertRequest request = new ServiceRequestUpsertRequest();
        request.setTitle("  Laptop issue  ");
        request.setDescription("Screen is blank");
        request.setCategory(RequestCategory.IT_SUPPORT);
        request.setPriority(RequestPriority.HIGH);
        request.setRequesterId(requesterId);

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(departmentRepository.findByCategory(RequestCategory.IT_SUPPORT)).thenReturn(Optional.of(department));
        when(slaPolicyRepository.findByCategoryAndPriority(RequestCategory.IT_SUPPORT, RequestPriority.HIGH))
                .thenReturn(Optional.of(slaPolicy));
        when(serviceRequestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.create(request);

        assertEquals("Laptop issue", response.getTitle());
        assertEquals(RequestStatus.OPEN, response.getStatus());
        assertEquals(departmentId, response.getDepartmentId());
        assertEquals(requesterId, response.getRequesterId());
        assertNotNull(response.getSlaDeadline());
        assertFalse(response.getIsSlaBreached());
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
        existing.setSlaDeadline(OffsetDateTime.now(ZoneOffset.UTC).plusHours(4));

        ServiceRequestUpsertRequest updateRequest = new ServiceRequestUpsertRequest();
        updateRequest.setTitle("New title");
        updateRequest.setPriority(RequestPriority.CRITICAL);
        updateRequest.setCategory(RequestCategory.FACILITIES);
        updateRequest.setAssignedToId(assigneeId);
        updateRequest.setStatus(RequestStatus.RESOLVED);

        Department routedDepartment = department(departmentId, RequestCategory.FACILITIES);
        SlaPolicy slaPolicy = slaPolicy(RequestCategory.FACILITIES, RequestPriority.CRITICAL, 8);

        when(serviceRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(user(assigneeId)));
        when(departmentRepository.findByCategory(RequestCategory.FACILITIES)).thenReturn(Optional.of(routedDepartment));
        when(slaPolicyRepository.findByCategoryAndPriority(RequestCategory.FACILITIES, RequestPriority.CRITICAL))
                .thenReturn(Optional.of(slaPolicy));
        when(serviceRequestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.update(requestId, updateRequest);

        assertEquals("New title", response.getTitle());
        assertEquals(RequestCategory.FACILITIES, response.getCategory());
        assertEquals(RequestPriority.CRITICAL, response.getPriority());
        assertEquals(RequestStatus.RESOLVED, response.getStatus());
        assertNotNull(response.getFirstResponseAt());
        assertNotNull(response.getResolvedAt());
        assertEquals(departmentId, response.getDepartmentId());
        assertEquals(assigneeId, response.getAssignedToId());
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

        ServiceRequestResponse response = serviceRequestService.create(request);

        assertNull(response.getFirstResponseAt());
    }

    private User user(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@amalitech.com");
        user.setName("User");
        return user;
    }

    private Department department(UUID id, RequestCategory category) {
        Department department = new Department();
        department.setId(id);
        department.setName("IT");
        department.setCategory(category);
        return department;
    }

    private SlaPolicy slaPolicy(RequestCategory category, RequestPriority priority, int resolutionHours) {
        SlaPolicy policy = new SlaPolicy();
        policy.setId(UUID.randomUUID());
        policy.setCategory(category);
        policy.setPriority(priority);
        policy.setResolutionTimeHours(resolutionHours);
        policy.setResponseTimeHours(1);
        return policy;
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
}
