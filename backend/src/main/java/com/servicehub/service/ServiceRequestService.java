package com.servicehub.service;

import com.servicehub.dto.ServiceRequestResponse;
import com.servicehub.dto.ServiceRequestUpsertRequest;
import java.util.List;
import java.util.UUID;

public interface ServiceRequestService {
    ServiceRequestResponse create(ServiceRequestUpsertRequest request);

    List<ServiceRequestResponse> findAll();

    ServiceRequestResponse findById(UUID id);

    ServiceRequestResponse update(UUID id, ServiceRequestUpsertRequest request);

    void delete(UUID id);
}
