package com.servicehub.service.impl;

import com.servicehub.model.Department;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.service.Notification;
import com.servicehub.service.ServiceRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceImplTest {

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private ServiceRequestService serviceRequestService;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private Notification emailService;

    private ServiceRequest testRequest;

    private User assignedAgent;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();

        Department itDepartment = new Department();
        itDepartment.setId(UUID.randomUUID());
        itDepartment.setName("IT");
        itDepartment.setCategory(RequestCategory.IT_SUPPORT);

        assignedAgent = new User();
        assignedAgent.setId(UUID.randomUUID());
        assignedAgent.setEmail("agent@example.com");
        assignedAgent.setFullName("Assigned Agent");
        assignedAgent.setRole(Role.AGENT);
        assignedAgent.setDepartment("IT");

        testRequest = new ServiceRequest();
        testRequest.setId(id);
        testRequest.setStatus(RequestStatus.OPEN);
        testRequest.setDepartment(itDepartment);
        testRequest.setCreatedAt(OffsetDateTime.now());
        testRequest.setUpdatedAt(OffsetDateTime.now());

        when(serviceRequestRepository.findById(id))
                .thenReturn(Optional.of(testRequest));
    }

    private void stubAutoAssignSuccess() {
        doAnswer(ignored -> {
            testRequest.setAssignedTo(assignedAgent);
            testRequest.setStatus(RequestStatus.ASSIGNED);
            return null;
        }).when(serviceRequestService).autoAssign(testRequest.getId());
    }

    @Test
    @DisplayName("Should transition from OPEN to ASSIGNED")
    void testTransitionStatus_OpenToAssigned() {

        stubAutoAssignSuccess();
        workflowService.transitionStatus(testRequest.getId());

        assertEquals(RequestStatus.ASSIGNED, testRequest.getStatus());
        assertNotNull(testRequest.getAssignedTo());
        assertNotNull(testRequest.getUpdatedAt());
        verify(serviceRequestService).autoAssign(testRequest.getId());
    }

    @Test
    @DisplayName("Should transition from ASSIGNED to IN_PROGRESS")
    void testTransitionStatus_AssignedToInProgress() {

        testRequest.setStatus(RequestStatus.ASSIGNED);
        OffsetDateTime firstResponse = OffsetDateTime.now().minusHours(1);
        testRequest.setFirstResponseAt(firstResponse);

        workflowService.transitionStatus(testRequest.getId());

        assertEquals(RequestStatus.IN_PROGRESS, testRequest.getStatus());
        assertEquals(firstResponse, testRequest.getFirstResponseAt());
        verify(serviceRequestService, never()).autoAssign(any());
    }

    @Test
    @DisplayName("Should transition from IN_PROGRESS to RESOLVED")
    void testTransitionStatus_InProgressToResolved() {

        testRequest.setStatus(RequestStatus.IN_PROGRESS);

        workflowService.transitionStatus(testRequest.getId());

        assertEquals(RequestStatus.RESOLVED, testRequest.getStatus());
        assertNotNull(testRequest.getResolvedAt());
    }

    @Test
    @DisplayName("Should transition from RESOLVED to CLOSED")
    void testTransitionStatus_ResolvedToClosed() {

        testRequest.setStatus(RequestStatus.RESOLVED);

        workflowService.transitionStatus(testRequest.getId());

        assertEquals(RequestStatus.CLOSED, testRequest.getStatus());
        assertNotNull(testRequest.getClosedAt());
    }

    @Test
    @DisplayName("Should throw exception when transitioning from CLOSED")
    void testTransitionStatus_FromClosed_ThrowsException() {

        testRequest.setStatus(RequestStatus.CLOSED);

        assertThrows(RuntimeException.class,
                () -> workflowService.transitionStatus(testRequest.getId()));
    }

    @Test
    @DisplayName("Should complete full workflow")
    void testTransitionStatus_CompleteWorkflow() {

        stubAutoAssignSuccess();
        workflowService.transitionStatus(testRequest.getId());
        assertEquals(RequestStatus.ASSIGNED, testRequest.getStatus());

        workflowService.transitionStatus(testRequest.getId());
        assertEquals(RequestStatus.IN_PROGRESS, testRequest.getStatus());

        workflowService.transitionStatus(testRequest.getId());
        assertEquals(RequestStatus.RESOLVED, testRequest.getStatus());

        workflowService.transitionStatus(testRequest.getId());
        assertEquals(RequestStatus.CLOSED, testRequest.getStatus());

        verify(serviceRequestService, times(1)).autoAssign(testRequest.getId());
    }

}
