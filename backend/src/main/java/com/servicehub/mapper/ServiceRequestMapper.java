package com.servicehub.mapper;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.model.ServiceRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceRequestMapper {

    ServiceRequest toEntity(ServiceRequestResponse serviceRequestResponse);
}
