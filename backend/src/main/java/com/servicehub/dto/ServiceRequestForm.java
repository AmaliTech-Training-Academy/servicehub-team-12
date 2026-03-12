package com.servicehub.dto;

import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceRequestForm {

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    @NotNull(message = "category is required")
    private RequestCategory category;

    @NotNull(message = "priority is required")
    private RequestPriority priority;
}
