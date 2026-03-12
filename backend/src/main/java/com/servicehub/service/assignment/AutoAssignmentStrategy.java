package com.servicehub.service.assignment;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import java.util.Optional;

public interface AutoAssignmentStrategy {

    Optional<User> selectAssignee(ServiceRequest request);
}
