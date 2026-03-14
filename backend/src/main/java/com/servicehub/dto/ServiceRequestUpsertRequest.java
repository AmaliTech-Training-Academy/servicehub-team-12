package com.servicehub.dto;

import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestPriority;
import com.servicehub.model.enums.RequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
/**
 * Data transfer object for service request create/update operations.
 */

@Getter
@Setter
public class ServiceRequestUpsertRequest{

    public interface Create {
    }

    @NotBlank(groups = Create.class, message = "title is required")
    private String title;
    private String description;
    @NotNull(groups = Create.class, message = "category is required")
    private RequestCategory category;
    @NotNull(groups = Create.class, message = "priority is required")
    private RequestPriority priority;
    private RequestStatus status;
    private UUID departmentId;
    private UUID assignedToId;
    @NotNull(groups = Create.class, message = "requesterId is required")
    private UUID requesterId;
}
