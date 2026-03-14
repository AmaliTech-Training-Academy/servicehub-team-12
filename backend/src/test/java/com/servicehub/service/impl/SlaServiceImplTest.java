package com.servicehub.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.servicehub.model.enums.RequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.servicehub.exception.SlaPolicyNotFoundException;
import com.servicehub.event.ServiceRequestCreatedEvent;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.SlaPolicy;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.SlaPolicyRepository;
import com.servicehub.service.WorkingHoursCalculator;

import static org.mockito.Mockito.*;
/**
 * Tests SLA service behavior.
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SlaServiceImplTest {

    @Mock
    private SlaPolicyRepository slaPolicyRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private WorkingHoursCalculator workingHoursCalculator;

    @InjectMocks
    private SlaServiceImpl slaService;

    private ServiceRequest testRequest;
    private SlaPolicy testPolicy;

    @BeforeEach
    void setUp() {
        UUID testRequestId = UUID.randomUUID();

        testPolicy = new SlaPolicy();
        testPolicy.setId(UUID.randomUUID());
        testPolicy.setCategory(RequestCategory.IT_SUPPORT);
        testPolicy.setPriority(RequestPriority.CRITICAL);
        testPolicy.setResponseTimeHours(1);
        testPolicy.setResolutionTimeHours(4);

        testRequest = new ServiceRequest();
        testRequest.setId(testRequestId);
        testRequest.setCategory(RequestCategory.IT_SUPPORT);
        testRequest.setPriority(RequestPriority.CRITICAL);
        testRequest.setStatus(RequestStatus.OPEN);
        testRequest.setCreatedAt(OffsetDateTime.now());
        testRequest.setIsSlaBreached(false);

        // WorkingHoursCalculator stubs: effective start = createdAt, deadline delegated to mock
        when(workingHoursCalculator.getNextWorkingHoursStart(any())).thenAnswer(i -> i.getArgument(0));
        when(workingHoursCalculator.addBusinessHours(any(), anyLong()))
                .thenAnswer(i -> ((OffsetDateTime) i.getArgument(0)).plusHours(i.getArgument(1)));
    }

    @Test
    @DisplayName("Should calculate SLA deadline correctly for IT_SUPPORT CRITICAL")
    void testCalculateAndSetSlaDeadline_Success() {
        when(slaPolicyRepository.findByCategoryAndPriority(
                RequestCategory.IT_SUPPORT, RequestPriority.CRITICAL))
                .thenReturn(Optional.of(testPolicy));

        OffsetDateTime actualDeadline = slaService.calculateAndSetSlaDeadline(testRequest);

        assertNotNull(actualDeadline);
        assertEquals(actualDeadline, testRequest.getSlaDeadline());
        verify(workingHoursCalculator).getNextWorkingHoursStart(testRequest.getCreatedAt());
        verify(workingHoursCalculator).addBusinessHours(testRequest.getCreatedAt(), 4L);
    }

    @Test
    @DisplayName("Should throw exception when SLA policy not found")
    void testCalculateAndSetSlaDeadline_PolicyNotFound() {
        when(slaPolicyRepository.findByCategoryAndPriority(
                RequestCategory.IT_SUPPORT, RequestPriority.CRITICAL))
                .thenReturn(Optional.empty());

        assertThrows(SlaPolicyNotFoundException.class, () -> slaService.calculateAndSetSlaDeadline(testRequest));
    }

    @Test
    @DisplayName("Should calculate deadline for FACILITIES HIGH correctly")
    void testCalculateAndSetSlaDeadline_FacilitiesHigh() {
        testRequest.setCategory(RequestCategory.FACILITIES);
        testRequest.setPriority(RequestPriority.HIGH);

        SlaPolicy facilitiesPolicy = new SlaPolicy();
        facilitiesPolicy.setCategory(RequestCategory.FACILITIES);
        facilitiesPolicy.setPriority(RequestPriority.HIGH);
        facilitiesPolicy.setResolutionTimeHours(16);

        when(slaPolicyRepository.findByCategoryAndPriority(
                RequestCategory.FACILITIES, RequestPriority.HIGH))
                .thenReturn(Optional.of(facilitiesPolicy));

        OffsetDateTime actualDeadline = slaService.calculateAndSetSlaDeadline(testRequest);

        assertNotNull(actualDeadline);
        verify(workingHoursCalculator).addBusinessHours(testRequest.getCreatedAt(), 16L);
    }

    @Test
    @DisplayName("Should calculate deadline for HR_REQUEST LOW correctly")
    void testCalculateAndSetSlaDeadline_HrRequestLow() {
        testRequest.setCategory(RequestCategory.HR_REQUEST);
        testRequest.setPriority(RequestPriority.LOW);

        SlaPolicy hrPolicy = new SlaPolicy();
        hrPolicy.setCategory(RequestCategory.HR_REQUEST);
        hrPolicy.setPriority(RequestPriority.LOW);
        hrPolicy.setResolutionTimeHours(168);

        when(slaPolicyRepository.findByCategoryAndPriority(
                RequestCategory.HR_REQUEST, RequestPriority.LOW))
                .thenReturn(Optional.of(hrPolicy));

        OffsetDateTime actualDeadline = slaService.calculateAndSetSlaDeadline(testRequest);

        assertNotNull(actualDeadline);
        verify(workingHoursCalculator).addBusinessHours(testRequest.getCreatedAt(), 168L);
    }

    @Test
    @DisplayName("Should handle service request created event by setting SLA deadline")
    void testHandleServiceRequestCreated() {
        when(slaPolicyRepository.findByCategoryAndPriority(
                RequestCategory.IT_SUPPORT, RequestPriority.CRITICAL))
                .thenReturn(Optional.of(testPolicy));

        slaService.handleServiceRequestCreated(new ServiceRequestCreatedEvent(testRequest));

        assertNotNull(testRequest.getSlaDeadline());
        verify(workingHoursCalculator).getNextWorkingHoursStart(testRequest.getCreatedAt());
    }

    @Test
    @DisplayName("Should detect SLA breach when deadline passed")
    void testCheckSlaBreached_DeadlinePassed() {
        testRequest.setSlaDeadline(OffsetDateTime.now().minusHours(1));

        boolean isBreached = slaService.checkSlaBreached(testRequest);

        assertTrue(isBreached);
    }

    @Test
    @DisplayName("Should not detect breach when deadline not passed")
    void testCheckSlaBreached_DeadlineNotPassed() {
        testRequest.setSlaDeadline(OffsetDateTime.now().plusHours(1));

        boolean isBreached = slaService.checkSlaBreached(testRequest);

        assertFalse(isBreached);
    }

    @Test
    @DisplayName("Should return false when no deadline set")
    void testCheckSlaBreached_NoDeadline() {
        testRequest.setSlaDeadline(null);

        boolean isBreached = slaService.checkSlaBreached(testRequest);

        assertFalse(isBreached);
    }

    @Test
    @DisplayName("Should update breach status to true when deadline exceeded")
    void testDetectAndUpdateBreachStatus_SetToTrue() {

        testRequest.setSlaDeadline(OffsetDateTime.now().minusHours(2));
        testRequest.setIsSlaBreached(false);

        when(serviceRequestRepository.findRequestsPastDeadline(any()))
                .thenReturn(List.of(testRequest));

        when(serviceRequestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        slaService.detectAndUpdateBreachStatus();

        assertTrue(testRequest.getIsSlaBreached());

        verify(serviceRequestRepository)
                .save(testRequest);
    }

    @Test
    @DisplayName("Should not update breach status if already correct")
    void testDetectAndUpdateBreachStatus_AlreadyCorrect() {

        testRequest.setSlaDeadline(OffsetDateTime.now().minusHours(1));
        testRequest.setIsSlaBreached(true);

        when(serviceRequestRepository.findRequestsPastDeadline(any()))
                .thenReturn(List.of(testRequest));

        slaService.detectAndUpdateBreachStatus();

        verify(serviceRequestRepository, never())
                .save(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("Should calculate response time correctly")
    void testCalculateResponseTimeHours() {
        OffsetDateTime createdAt = OffsetDateTime.now().minusHours(2);
        OffsetDateTime firstResponseAt = OffsetDateTime.now();

        testRequest.setCreatedAt(createdAt);
        testRequest.setFirstResponseAt(firstResponseAt);

        Double responseTime = slaService.calculateResponseTimeHours(testRequest);

        assertNotNull(responseTime);
        assertTrue(responseTime >= 1.9 && responseTime <= 2.1);
    }

    @Test
    @DisplayName("Should return null when no first response")
    void testCalculateResponseTimeHours_NoResponse() {
        testRequest.setFirstResponseAt(null);

        Double responseTime = slaService.calculateResponseTimeHours(testRequest);

        assertNull(responseTime);
    }

    @Test
    @DisplayName("Should calculate resolution time correctly")
    void testCalculateResolutionTimeHours() {
        OffsetDateTime createdAt = OffsetDateTime.now().minusHours(5);
        OffsetDateTime resolvedAt = OffsetDateTime.now();

        testRequest.setCreatedAt(createdAt);
        testRequest.setResolvedAt(resolvedAt);

        Double resolutionTime = slaService.calculateResolutionTimeHours(testRequest);

        assertNotNull(resolutionTime);
        assertTrue(resolutionTime >= 4.9 && resolutionTime <= 5.1);
    }

    @Test
    @DisplayName("Should return null when not resolved")
    void testCalculateResolutionTimeHours_NotResolved() {
        testRequest.setResolvedAt(null);

        Double resolutionTime = slaService.calculateResolutionTimeHours(testRequest);

        assertNull(resolutionTime);
    }
}
