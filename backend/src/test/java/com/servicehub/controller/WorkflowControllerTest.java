package com.servicehub.controller;

import com.servicehub.exception.InvalidTransitionException;
import com.servicehub.exception.GlobalExceptionHandler;
import com.servicehub.service.WorkflowService;
import com.servicehub.service.ServiceRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private ServiceRequestService serviceRequestService;

    @InjectMocks
    private WorkflowController workflowController;

    private UUID testRequestId;

    @BeforeEach
    void setUp() {

        // Register the application's GlobalExceptionHandler so controller exceptions
        // (like InvalidTransitionException) are translated to proper HTTP responses
        mockMvc = MockMvcBuilders
                .standaloneSetup(workflowController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testRequestId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should transition status successfully")
    void transitionStatus_Success() throws Exception {

        doNothing().when(workflowService)
                .transitionStatus(testRequestId);

        mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(workflowService).transitionStatus(testRequestId);
    }

    @Test
    @DisplayName("Should handle request not found")
    void transitionStatus_RequestNotFound() throws Exception {

        doThrow(new RuntimeException("Request not found"))
                .when(workflowService)
                .transitionStatus(testRequestId);

        mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        verify(workflowService).transitionStatus(testRequestId);
    }

    @Test
    @DisplayName("Should handle transition from CLOSED state")
    void transitionStatus_FromClosed() throws Exception {

        doThrow(new InvalidTransitionException("No valid transition from status: CLOSED"))
                .when(workflowService)
                .transitionStatus(testRequestId);

        mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                        .contentType(MediaType.APPLICATION_JSON))
                // The application's GlobalExceptionHandler maps InvalidTransitionException -> 400 Bad Request
                .andExpect(status().isBadRequest());

        verify(workflowService).transitionStatus(testRequestId);
    }

    @Test
    @DisplayName("Should handle multiple rapid transitions")
    void transitionStatus_MultipleRequests() throws Exception {

        doNothing().when(workflowService)
                .transitionStatus(testRequestId);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/workflow/requests/{requestId}/transition", testRequestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        verify(workflowService, times(3)).transitionStatus(testRequestId);
    }
}