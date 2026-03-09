package com.servicehub.dto;

import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceRequestResponse {

    private UUID id;
    private String title;
    private String description;
    private RequestCategory category;
    private RequestPriority priority;
    private RequestStatus status;
    private UUID departmentId;
    private UUID assignedToId;
    private UUID requesterId;
    private OffsetDateTime slaDeadline;
    private OffsetDateTime firstResponseAt;
    private OffsetDateTime resolvedAt;
    private OffsetDateTime closedAt;
    private Boolean isSlaBreached;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
