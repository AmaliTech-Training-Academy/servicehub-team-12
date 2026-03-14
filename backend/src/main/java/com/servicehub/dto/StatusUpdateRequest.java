package com.servicehub.dto;

import lombok.*;
/**
 * Data transfer object for service request status updates.
 */

@Data
public class StatusUpdateRequest {
    private String newStatus;
    private String comment;
}
