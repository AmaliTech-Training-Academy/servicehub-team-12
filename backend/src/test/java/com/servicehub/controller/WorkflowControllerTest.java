package com.servicehub.controller;

import com.servicehub.config.JwtService;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.mapper.ServiceRequestMapper;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.TokenBlacklistRepository;
import com.servicehub.service.CustomUserDetailsService;
import com.servicehub.service.ServiceRequestService;
import com.servicehub.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private ServiceRequestService serviceRequestService;

    @MockitoBean
    private ServiceRequestMapper serviceRequestMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private TokenBlacklistRepository tokenBlacklistRepository;

    private UUID testRequestId;
    private ServiceRequest testServiceRequest;
    private ServiceRequestResponse testResponse;

    @BeforeEach
    void setUp() {
        testRequestId = UUID.randomUUID();

        testServiceRequest = new ServiceRequest();
        testServiceRequest.setId(testRequestId);
        testServiceRequest.setTitle("Test Request");
        testServiceRequest.setDescription("Test Description");
        testServiceRequest.setCategory(RequestCategory.IT_SUPPORT);
        testServiceRequest.setPriority(RequestPriority.HIGH);
        testServiceRequest.setStatus(RequestStatus.OPEN);
        testServiceRequest.setCreatedAt(OffsetDateTime.now());
        testServiceRequest.setUpdatedAt(OffsetDateTime.now());

        testResponse = new ServiceRequestResponse();
        testResponse.setId(testRequestId);
        testResponse.setTitle("Test Request");
        testResponse.setDescription("Test Description");
        testResponse.setCategory(RequestCategory.IT_SUPPORT);
        testResponse.setPriority(RequestPriority.HIGH);
        testResponse.setStatus(RequestStatus.OPEN);
    }

    @Test
    @DisplayName("Should transition status successfully")
    void testTransitionStatus_Success() throws Exception {

        ServiceRequestResponse beforeTransition = new ServiceRequestResponse();
        beforeTransition.setId(testRequestId);
        beforeTransition.setStatus(RequestStatus.OPEN);

        ServiceRequestResponse afterTransition = new ServiceRequestResponse();
        afterTransition.setId(testRequestId);
        afterTransition.setStatus(RequestStatus.ASSIGNED);

        when(serviceRequestService.findById(testRequestId)).thenReturn(beforeTransition);
        when(serviceRequestMapper.toEntity(beforeTransition)).thenReturn(testServiceRequest);
        doNothing().when(workflowService).transitionStatus(testServiceRequest);
        when(serviceRequestMapper.toDto(testServiceRequest)).thenReturn(afterTransition);

        mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testRequestId.toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(serviceRequestService).findById(testRequestId);
        verify(serviceRequestMapper).toEntity(beforeTransition);
        verify(workflowService).transitionStatus(testServiceRequest);
        verify(serviceRequestMapper).toDto(testServiceRequest);
    }

    @Test
    @DisplayName("Should return 404 when request not found")
    void testTransitionStatus_RequestNotFound() throws Exception {

        UUID nonExistentId = UUID.randomUUID();

        when(serviceRequestService.findById(nonExistentId))
                .thenThrow(new RuntimeException("Request not found"));

        mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        verify(serviceRequestService).findById(nonExistentId);
        verify(workflowService, never()).transitionStatus(any());
    }

    @Test
    @DisplayName("Should handle transition from CLOSED state")
    void testTransitionStatus_FromClosedState() throws Exception {

        ServiceRequestResponse closedRequest = new ServiceRequestResponse();
        closedRequest.setId(testRequestId);
        closedRequest.setStatus(RequestStatus.CLOSED);

        ServiceRequest closedEntity = new ServiceRequest();
        closedEntity.setId(testRequestId);
        closedEntity.setStatus(RequestStatus.CLOSED);

        when(serviceRequestService.findById(testRequestId)).thenReturn(closedRequest);
        when(serviceRequestMapper.toEntity(closedRequest)).thenReturn(closedEntity);

        doThrow(new IllegalStateException("No valid transition from status: CLOSED"))
                .when(workflowService).transitionStatus(closedEntity);

        mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        verify(workflowService).transitionStatus(closedEntity);
    }

    @Test
    @DisplayName("Should transition through complete workflow")
    void testTransitionStatus_CompleteWorkflow() throws Exception {

        ServiceRequestResponse openResponse = new ServiceRequestResponse();
        openResponse.setId(testRequestId);
        openResponse.setStatus(RequestStatus.OPEN);

        ServiceRequestResponse assignedResponse = new ServiceRequestResponse();
        assignedResponse.setId(testRequestId);
        assignedResponse.setStatus(RequestStatus.ASSIGNED);

        when(serviceRequestService.findById(testRequestId)).thenReturn(openResponse);
        when(serviceRequestMapper.toEntity(openResponse)).thenReturn(testServiceRequest);
        when(serviceRequestMapper.toDto(testServiceRequest)).thenReturn(assignedResponse);

        mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        ServiceRequestResponse inProgressResponse = new ServiceRequestResponse();
        inProgressResponse.setId(testRequestId);
        inProgressResponse.setStatus(RequestStatus.IN_PROGRESS);

        when(serviceRequestService.findById(testRequestId)).thenReturn(assignedResponse);
        when(serviceRequestMapper.toDto(testServiceRequest)).thenReturn(inProgressResponse);

        mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(workflowService, times(2)).transitionStatus(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("Should return full request response")
    void testTransitionStatus_ReturnsCompleteResponse() throws Exception {

        ServiceRequestResponse beforeTransition = new ServiceRequestResponse();
        beforeTransition.setId(testRequestId);
        beforeTransition.setTitle("Test Request");
        beforeTransition.setDescription("Test Description");
        beforeTransition.setCategory(RequestCategory.IT_SUPPORT);
        beforeTransition.setPriority(RequestPriority.HIGH);
        beforeTransition.setStatus(RequestStatus.OPEN);

        ServiceRequestResponse afterTransition = new ServiceRequestResponse();
        afterTransition.setId(testRequestId);
        afterTransition.setTitle("Test Request");
        afterTransition.setDescription("Test Description");
        afterTransition.setCategory(RequestCategory.IT_SUPPORT);
        afterTransition.setPriority(RequestPriority.HIGH);
        afterTransition.setStatus(RequestStatus.ASSIGNED);

        when(serviceRequestService.findById(testRequestId)).thenReturn(beforeTransition);
        when(serviceRequestMapper.toEntity(beforeTransition)).thenReturn(testServiceRequest);
        when(serviceRequestMapper.toDto(testServiceRequest)).thenReturn(afterTransition);

        mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testRequestId.toString()))
                .andExpect(jsonPath("$.title").value("Test Request"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.category").value("IT_SUPPORT"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("Should handle multiple rapid transitions")
    void testTransitionStatus_ConcurrentRequests() throws Exception {

        ServiceRequestResponse response = new ServiceRequestResponse();
        response.setId(testRequestId);
        response.setStatus(RequestStatus.ASSIGNED);

        when(serviceRequestService.findById(testRequestId)).thenReturn(response);
        when(serviceRequestMapper.toEntity(any())).thenReturn(testServiceRequest);
        when(serviceRequestMapper.toDto(testServiceRequest)).thenReturn(response);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        verify(workflowService, times(3)).transitionStatus(any(ServiceRequest.class));
    }
}
