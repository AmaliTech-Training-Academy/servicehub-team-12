package com.servicehub.mapper;

import com.servicehub.dto.ServiceRequestForm;
import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import com.servicehub.model.ServiceRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ServiceRequestMapper {

    ServiceRequest toEntity(ServiceRequestResponse serviceRequestResponse);

    @Mapping(target = "requesterId", source = "requesterId")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "departmentId", ignore = true)
    @Mapping(target = "assignedToId", ignore = true)
    ServiceRequestUpsertRequest toCreateRequest(ServiceRequestForm form, UUID requesterId);
}
