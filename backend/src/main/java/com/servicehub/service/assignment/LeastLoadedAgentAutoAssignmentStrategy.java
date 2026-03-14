package com.servicehub.service.assignment;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
/**
 * Assigns requests to the eligible agent with the lowest active workload.
 */

@Component
@RequiredArgsConstructor
public class LeastLoadedAgentAutoAssignmentStrategy implements AutoAssignmentStrategy {

    private static final List<RequestStatus> DONE_STATUSES =
            List.of(RequestStatus.RESOLVED, RequestStatus.CLOSED);

    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    @Override
    public Optional<User> selectAssignee(ServiceRequest request) {
        if (request.getDepartment() == null) {
            return Optional.empty();
        }

        return userRepository.findAllByRoleAndDepartmentIgnoreCaseAndIsActiveTrueOrderByCreatedAtAsc(
                        Role.AGENT, request.getDepartment().getName())
                .stream()
                .min(Comparator
                        .comparingLong(this::countActiveAssignedTickets)
                        .thenComparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private long countActiveAssignedTickets(User agent) {
        return serviceRequestRepository.countByAssignedToAndStatusNotIn(agent, DONE_STATUSES);
    }
}
