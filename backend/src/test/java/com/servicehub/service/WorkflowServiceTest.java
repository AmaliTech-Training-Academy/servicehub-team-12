package com.servicehub.service;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.service.impl.WorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private ServiceRequest testRequest;

    @BeforeEach
    void setUp() {
        UUID testRequestId = UUID.randomUUID();

        testRequest = new ServiceRequest();
        testRequest.setId(testRequestId);
        testRequest.setStatus(RequestStatus.OPEN);
        testRequest.setCreatedAt(OffsetDateTime.now());
        testRequest.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should transition from OPEN to ASSIGNED")
    void testTransitionStatus_OpenToAssigned() {
        testRequest.setStatus(RequestStatus.OPEN);

        workflowService.transitionStatus(testRequest);

        assertEquals(RequestStatus.ASSIGNED, testRequest.getStatus());
        assertNotNull(testRequest.getFirstResponseAt());
        assertNotNull(testRequest.getUpdatedAt());
    }

    @Test
    @DisplayName("Should transition from ASSIGNED to IN_PROGRESS")
    void testTransitionStatus_AssignedToInProgress() {
        testRequest.setStatus(RequestStatus.ASSIGNED);
        OffsetDateTime firstResponse = OffsetDateTime.now().minusHours(1);
        testRequest.setFirstResponseAt(firstResponse);

        workflowService.transitionStatus(testRequest);

        assertEquals(RequestStatus.IN_PROGRESS, testRequest.getStatus());
        assertEquals(firstResponse, testRequest.getFirstResponseAt()); // Should not change
        assertNotNull(testRequest.getUpdatedAt());
    }

    @Test
    @DisplayName("Should transition from IN_PROGRESS to RESOLVED")
    void testTransitionStatus_InProgressToResolved() {
        testRequest.setStatus(RequestStatus.IN_PROGRESS);

        workflowService.transitionStatus(testRequest);

        assertEquals(RequestStatus.RESOLVED, testRequest.getStatus());
        assertNotNull(testRequest.getResolvedAt());
        assertNotNull(testRequest.getUpdatedAt());
    }

    @Test
    @DisplayName("Should transition from RESOLVED to CLOSED")
    void testTransitionStatus_ResolvedToClosed() {
        testRequest.setStatus(RequestStatus.RESOLVED);
        OffsetDateTime resolvedTime = OffsetDateTime.now().minusHours(1);
        testRequest.setResolvedAt(resolvedTime);

        workflowService.transitionStatus(testRequest);

        assertEquals(RequestStatus.CLOSED, testRequest.getStatus());
        assertNotNull(testRequest.getClosedAt());
        assertEquals(resolvedTime, testRequest.getResolvedAt()); // Should not change
        assertNotNull(testRequest.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw exception when transitioning from CLOSED (terminal state)")
    void testTransitionStatus_FromClosed_ThrowsException() {
        testRequest.setStatus(RequestStatus.CLOSED);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> workflowService.transitionStatus(testRequest)
        );

        assertTrue(exception.getMessage().contains("No valid transition from status: CLOSED"));
    }

    @Test
    @DisplayName("Should set first_response_at only once when transitioning to ASSIGNED")
    void testTransitionStatus_FirstResponseAtSetOnlyOnce() {
        testRequest.setStatus(RequestStatus.OPEN);

        workflowService.transitionStatus(testRequest);
        OffsetDateTime firstResponseAt = testRequest.getFirstResponseAt();

        assertNotNull(firstResponseAt);
        assertEquals(RequestStatus.ASSIGNED, testRequest.getStatus());

        testRequest.setStatus(RequestStatus.OPEN);
        workflowService.transitionStatus(testRequest);

        assertEquals(firstResponseAt, testRequest.getFirstResponseAt());
    }

    @Test
    @DisplayName("Should update updated_at timestamp on every transition")
    void testTransitionStatus_UpdatesUpdatedAt() {
        testRequest.setStatus(RequestStatus.OPEN);
        OffsetDateTime initialUpdatedAt = testRequest.getUpdatedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        workflowService.transitionStatus(testRequest);

        assertNotEquals(initialUpdatedAt, testRequest.getUpdatedAt());
        assertTrue(testRequest.getUpdatedAt().isAfter(initialUpdatedAt));
    }

    @Test
    @DisplayName("Should set resolved_at when transitioning to RESOLVED")
    void testTransitionStatus_SetsResolvedAt() {
        testRequest.setStatus(RequestStatus.IN_PROGRESS);
        assertNull(testRequest.getResolvedAt());

        workflowService.transitionStatus(testRequest);

        assertEquals(RequestStatus.RESOLVED, testRequest.getStatus());
        assertNotNull(testRequest.getResolvedAt());
    }

    @Test
    @DisplayName("Should set closed_at when transitioning to CLOSED")
    void testTransitionStatus_SetsClosedAt() {
        testRequest.setStatus(RequestStatus.RESOLVED);
        assertNull(testRequest.getClosedAt());

        workflowService.transitionStatus(testRequest);

        assertEquals(RequestStatus.CLOSED, testRequest.getStatus());
        assertNotNull(testRequest.getClosedAt());
    }

    @Test
    @DisplayName("Should complete full workflow from OPEN to CLOSED")
    void testTransitionStatus_CompleteWorkflow() {
        testRequest.setStatus(RequestStatus.OPEN);

        workflowService.transitionStatus(testRequest);
        assertEquals(RequestStatus.ASSIGNED, testRequest.getStatus());
        assertNotNull(testRequest.getFirstResponseAt());

        workflowService.transitionStatus(testRequest);
        assertEquals(RequestStatus.IN_PROGRESS, testRequest.getStatus());

        workflowService.transitionStatus(testRequest);
        assertEquals(RequestStatus.RESOLVED, testRequest.getStatus());
        assertNotNull(testRequest.getResolvedAt());

         workflowService.transitionStatus(testRequest);
        assertEquals(RequestStatus.CLOSED, testRequest.getStatus());
        assertNotNull(testRequest.getClosedAt());

         assertNotNull(testRequest.getFirstResponseAt());
        assertNotNull(testRequest.getResolvedAt());
        assertNotNull(testRequest.getClosedAt());
        assertNotNull(testRequest.getUpdatedAt());
    }

    @Test
    @DisplayName("Should maintain timestamp order in workflow")
    void testTransitionStatus_TimestampOrder() {
         testRequest.setStatus(RequestStatus.OPEN);
        OffsetDateTime createdAt = testRequest.getCreatedAt();

         workflowService.transitionStatus(testRequest);
        OffsetDateTime firstResponseAt = testRequest.getFirstResponseAt();

        workflowService.transitionStatus(testRequest);

        workflowService.transitionStatus(testRequest);
        OffsetDateTime resolvedAt = testRequest.getResolvedAt();

        workflowService.transitionStatus(testRequest);
        OffsetDateTime closedAt = testRequest.getClosedAt();

         assertTrue(firstResponseAt.isAfter(createdAt) || firstResponseAt.isEqual(createdAt));
        assertTrue(resolvedAt.isAfter(firstResponseAt) || resolvedAt.isEqual(firstResponseAt));
        assertTrue(closedAt.isAfter(resolvedAt) || closedAt.isEqual(resolvedAt));
    }

    @Test
    @DisplayName("Should not set first_response_at when not transitioning to ASSIGNED")
    void testTransitionStatus_NoFirstResponseAtForOtherTransitions() {
        testRequest.setStatus(RequestStatus.ASSIGNED);
        assertNull(testRequest.getFirstResponseAt());

        workflowService.transitionStatus(testRequest);

        assertEquals(RequestStatus.IN_PROGRESS, testRequest.getStatus());
        assertNull(testRequest.getFirstResponseAt());
    }

    @Test
    @DisplayName("Should not modify resolved_at after it's set")
    void testTransitionStatus_ResolvedAtNotModified() {
        testRequest.setStatus(RequestStatus.IN_PROGRESS);

        workflowService.transitionStatus(testRequest);
        OffsetDateTime resolvedAt = testRequest.getResolvedAt();

        workflowService.transitionStatus(testRequest);

        assertEquals(resolvedAt, testRequest.getResolvedAt());
    }
}
