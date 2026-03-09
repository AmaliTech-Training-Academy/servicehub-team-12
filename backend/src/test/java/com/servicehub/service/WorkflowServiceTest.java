package com.servicehub.service;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.exception.InvalidTransitionException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.mapper.ServiceRequestMapper;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.UserRole;
import com.servicehub.service.impl.WorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private ServiceRequestService serviceRequestService;

    @Mock
    private ServiceRequestMapper serviceRequestMapper;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private ServiceRequest testRequest;
    private UUID testRequestId;

    @BeforeEach
    void setUp() {
        testRequestId = UUID.randomUUID();
        testRequest = new ServiceRequest();
        testRequest.setId(testRequestId);
        testRequest.setStatus(RequestStatus.OPEN);
        testRequest.setCreatedAt(OffsetDateTime.now());
        testRequest.setUpdatedAt(OffsetDateTime.now());
    }

    // ---- VALID TRANSITIONS ----

    @Test
    @DisplayName("OPEN -> ASSIGNED")
    void testTransition_OpenToAssigned() {
        mockFindRequest();

        workflowService.transitionStatus(testRequestId, RequestStatus.ASSIGNED);

        assertEquals(RequestStatus.ASSIGNED, testRequest.getStatus());
        assertNotNull(testRequest.getFirstResponseAt());
        verify(serviceRequestMapper).toEntity(any());
    }

    @Test
    @DisplayName("ASSIGNED -> IN_PROGRESS")
    void testTransition_AssignedToInProgress() {
        testRequest.setStatus(RequestStatus.ASSIGNED);

        User assignedAgent = new User();
        assignedAgent.setId(UUID.randomUUID());
        assignedAgent.setEmail("agent@example.com");
        assignedAgent.setRole(UserRole.AGENT);
        testRequest.setAssignedTo(assignedAgent);

        mockFindRequest();

        workflowService.transitionStatus(testRequestId, RequestStatus.IN_PROGRESS);

        assertEquals(RequestStatus.IN_PROGRESS, testRequest.getStatus());
    }

    @Test
    @DisplayName("IN_PROGRESS -> RESOLVED")
    void testTransition_InProgressToResolved() {
        testRequest.setStatus(RequestStatus.IN_PROGRESS);
        User assignedAgent = new User();
        assignedAgent.setId(UUID.randomUUID());
        assignedAgent.setEmail("agent@example.com");
        assignedAgent.setRole(UserRole.AGENT);
        testRequest.setAssignedTo(assignedAgent);
        mockFindRequest();

        workflowService.transitionStatus(testRequestId, RequestStatus.RESOLVED);

        assertEquals(RequestStatus.RESOLVED, testRequest.getStatus());
        assertNotNull(testRequest.getResolvedAt());
    }

    @Test
    @DisplayName("RESOLVED -> CLOSED")
    void testTransition_ResolvedToClosed() {
        testRequest.setStatus(RequestStatus.RESOLVED);
        mockFindRequest();

        workflowService.transitionStatus(testRequestId, RequestStatus.CLOSED);

        assertEquals(RequestStatus.CLOSED, testRequest.getStatus());
        assertNotNull(testRequest.getClosedAt());
    }

    @Test
    @DisplayName("Reject backward transition")
    void testInvalidTransition_Backward() {
        testRequest.setStatus(RequestStatus.ASSIGNED);
        mockFindRequest();

        InvalidTransitionException ex = assertThrows(InvalidTransitionException.class,
                () -> workflowService.transitionStatus(testRequestId, RequestStatus.OPEN));

        assertTrue(ex.getMessage().contains("Invalid status transition"));
    }

    @Test
    @DisplayName("Reject skipping status")
    void testInvalidTransition_Skip() {
        testRequest.setStatus(RequestStatus.OPEN);
        mockFindRequest();

        InvalidTransitionException ex = assertThrows(InvalidTransitionException.class,
                () -> workflowService.transitionStatus(testRequestId, RequestStatus.IN_PROGRESS));

        assertTrue(ex.getMessage().contains("Invalid status transition"));
    }

    @Test
    @DisplayName("Cannot move to IN_PROGRESS without assigned agent")
    void testTransition_InProgressWithoutAssignment() {
        testRequest.setStatus(RequestStatus.ASSIGNED);
        testRequest.setAssignedTo(null);
        mockFindRequest();

        InvalidTransitionException ex = assertThrows(InvalidTransitionException.class,
                () -> workflowService.transitionStatus(testRequestId, RequestStatus.IN_PROGRESS));

        assertTrue(ex.getMessage().contains("without assigning"));
    }


    @Test
    @DisplayName("isValidTransition works for all statuses")
    void testIsValidTransition() {
        assertTrue(workflowService.isValidTransition(RequestStatus.OPEN, RequestStatus.ASSIGNED));
        assertTrue(workflowService.isValidTransition(RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS));
        assertTrue(workflowService.isValidTransition(RequestStatus.IN_PROGRESS, RequestStatus.RESOLVED));
        assertTrue(workflowService.isValidTransition(RequestStatus.RESOLVED, RequestStatus.CLOSED));

        assertFalse(workflowService.isValidTransition(RequestStatus.OPEN, RequestStatus.IN_PROGRESS));
        assertFalse(workflowService.isValidTransition(RequestStatus.RESOLVED, RequestStatus.OPEN));
        assertFalse(workflowService.isValidTransition(null, RequestStatus.OPEN));
        assertFalse(workflowService.isValidTransition(RequestStatus.OPEN, null));
        assertFalse(workflowService.isValidTransition(null, null));
    }

    @Test
    @DisplayName("getValidNextStatuses returns correct lists")
    void testGetValidNextStatuses() {
        List<RequestStatus> openNext = workflowService.getValidNextStatuses(RequestStatus.OPEN);
        assertEquals(1, openNext.size());
        assertTrue(openNext.contains(RequestStatus.ASSIGNED));

        List<RequestStatus> closedNext = workflowService.getValidNextStatuses(RequestStatus.CLOSED);
        assertTrue(closedNext.isEmpty());
    }


    private void mockFindRequest() {
        ServiceRequestResponse response = ServiceRequestResponse.builder()
                .id(testRequestId)
                .status(RequestStatus.OPEN)
                .build();
        response.setId(testRequestId);
        response.setStatus(testRequest.getStatus());
        when(serviceRequestService.findById(testRequestId)).thenReturn(response);
        when(serviceRequestMapper.toEntity(response)).thenReturn(testRequest);
    }
}