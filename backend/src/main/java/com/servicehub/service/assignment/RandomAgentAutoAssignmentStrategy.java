package com.servicehub.service.assignment;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RandomAgentAutoAssignmentStrategy implements AutoAssignmentStrategy {

    private final UserRepository userRepository;

    @Override
    public Optional<User> selectAssignee(ServiceRequest request) {
        if (request.getDepartment() == null) {
            return Optional.empty();
        }

        List<User> agents = userRepository.findAllByRoleAndDepartmentIgnoreCaseOrderByCreatedAtAsc(
                Role.AGENT, request.getDepartment().getName());

        if (agents.isEmpty()) {
            return Optional.empty();
        }

        int selectedIndex = ThreadLocalRandom.current().nextInt(agents.size());
        return Optional.of(agents.get(selectedIndex));
    }
}
