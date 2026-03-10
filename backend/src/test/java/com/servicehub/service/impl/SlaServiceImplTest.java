package com.servicehub.service.impl;

import java.time.OffsetDateTime;
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

import com.servicehub.exception.SlaPolicyNotFoundException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.SlaPolicy;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.SlaPolicyRepository;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaServiceImplTest {

  @Mock
  private SlaPolicyRepository slaPolicyRepository;

  @Mock
  private ServiceRequestRepository serviceRequestRepository;

  @InjectMocks
  private SlaServiceImpl slaService;

  private ServiceRequest testRequest;
  private SlaPolicy testPolicy;
  private UUID testRequestId;

  @BeforeEach
  void setUp() {
      testRequestId = UUID.randomUUID();

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
  }

  @Test
  @DisplayName("Should calculate SLA deadline correctly for IT_SUPPORT CRITICAL")
  void testCalculateAndSetSlaDeadline_Success() {
      when(slaPolicyRepository.findByCategoryAndPriority(
          RequestCategory.IT_SUPPORT, RequestPriority.CRITICAL))
          .thenReturn(Optional.of(testPolicy));

      OffsetDateTime expectedDeadline = testRequest.getCreatedAt().plusHours(4);

      OffsetDateTime actualDeadline = slaService.calculateAndSetSlaDeadline(testRequest);

      assertNotNull(actualDeadline);
      assertEquals(expectedDeadline, actualDeadline);
      assertEquals(expectedDeadline, testRequest.getSlaDeadline());
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

      OffsetDateTime expectedDeadline = testRequest.getCreatedAt().plusHours(16);

      OffsetDateTime actualDeadline = slaService.calculateAndSetSlaDeadline(testRequest);

      assertEquals(expectedDeadline, actualDeadline);
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

      OffsetDateTime expectedDeadline = testRequest.getCreatedAt().plusHours(168);

      OffsetDateTime actualDeadline = slaService.calculateAndSetSlaDeadline(testRequest);

      assertEquals(expectedDeadline, actualDeadline);
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

      when(serviceRequestRepository.findById(testRequestId))
          .thenReturn(Optional.of(testRequest));
      when(serviceRequestRepository.save(any(ServiceRequest.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

       ServiceRequest result = slaService.detectAndUpdateBreachStatus(testRequestId);

      // Then
      assertTrue(result.getIsSlaBreached());
      verify(serviceRequestRepository).save(any(ServiceRequest.class));
  }

  @Test
  @DisplayName("Should not update breach status if already correct")
  void testDetectAndUpdateBreachStatus_AlreadyCorrect() {
      // Given
      testRequest.setSlaDeadline(OffsetDateTime.now().minusHours(1));
      testRequest.setIsSlaBreached(true);

      when(serviceRequestRepository.findById(testRequestId))
          .thenReturn(Optional.of(testRequest));

      // When
      ServiceRequest result = slaService.detectAndUpdateBreachStatus(testRequestId);

      // Then
      assertTrue(result.getIsSlaBreached());
      verify(serviceRequestRepository, never()).save(any(ServiceRequest.class));
  }

  @Test
  @DisplayName("Should calculate response time correctly")
  void testCalculateResponseTimeHours() {
      // Given
      OffsetDateTime createdAt = OffsetDateTime.now().minusHours(2);
      OffsetDateTime firstResponseAt = OffsetDateTime.now();

      testRequest.setCreatedAt(createdAt);
      testRequest.setFirstResponseAt(firstResponseAt);

      // When
      Double responseTime = slaService.calculateResponseTimeHours(testRequest);

      // Then
      assertNotNull(responseTime);
      assertTrue(responseTime >= 1.9 && responseTime <= 2.1);
  }

  @Test
  @DisplayName("Should return null when no first response")
  void testCalculateResponseTimeHours_NoResponse() {
      // Given
      testRequest.setFirstResponseAt(null);

      // When
      Double responseTime = slaService.calculateResponseTimeHours(testRequest);

      // Then
      assertNull(responseTime);
  }

  @Test
  @DisplayName("Should calculate resolution time correctly")
  void testCalculateResolutionTimeHours() {
      // Given
      OffsetDateTime createdAt = OffsetDateTime.now().minusHours(5);
      OffsetDateTime resolvedAt = OffsetDateTime.now();

      testRequest.setCreatedAt(createdAt);
      testRequest.setResolvedAt(resolvedAt);

      // When
      Double resolutionTime = slaService.calculateResolutionTimeHours(testRequest);

      // Then
      assertNotNull(resolutionTime);
      assertTrue(resolutionTime >= 4.9 && resolutionTime <= 5.1);
  }

  @Test
  @DisplayName("Should return null when not resolved")
  void testCalculateResolutionTimeHours_NotResolved() {
      // Given
      testRequest.setResolvedAt(null);

      // When
      Double resolutionTime = slaService.calculateResolutionTimeHours(testRequest);

      // Then
      assertNull(resolutionTime);
  }
}
