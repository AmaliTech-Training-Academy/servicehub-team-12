package com.servicehub.service.assignment;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.servicehub.model.Department;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RandomAgentAutoAssignmentStrategyTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RandomAgentAutoAssignmentStrategy strategy;

    @Test
    @DisplayName("selectAssignee returns one of the matching department agents")
    void selectAssigneeShouldReturnOneOfTheMatchingAgents() {
        ServiceRequest request = new ServiceRequest();
        Department department = new Department();
        department.setId(UUID.randomUUID());
        department.setName("IT");
        department.setCategory(RequestCategory.IT_SUPPORT);
        request.setDepartment(department);

        User firstAgent = agent("first@amalitech.com");
        User secondAgent = agent("second@amalitech.com");

        when(userRepository.findAllByRoleAndDepartmentIgnoreCaseOrderByCreatedAtAsc(Role.AGENT, "IT"))
                .thenReturn(List.of(firstAgent, secondAgent));

        Optional<User> selected = strategy.selectAssignee(request);

        assertTrue(selected.isPresent());
        assertTrue(selected.get().equals(firstAgent) || selected.get().equals(secondAgent));
        verify(userRepository).findAllByRoleAndDepartmentIgnoreCaseOrderByCreatedAtAsc(Role.AGENT, "IT");
    }

    @Test
    @DisplayName("selectAssignee returns empty when no matching department agents exist")
    void selectAssigneeShouldReturnEmptyWhenNoMatchingAgentsExist() {
        ServiceRequest request = new ServiceRequest();
        Department department = new Department();
        department.setName("HR");
        department.setCategory(RequestCategory.HR_REQUEST);
        request.setDepartment(department);

        when(userRepository.findAllByRoleAndDepartmentIgnoreCaseOrderByCreatedAtAsc(Role.AGENT, "HR"))
                .thenReturn(List.of());

        Optional<User> selected = strategy.selectAssignee(request);

        assertTrue(selected.isEmpty());
    }

    @Test
    @DisplayName("selectAssignee returns empty when request has no department")
    void selectAssigneeShouldReturnEmptyWhenRequestHasNoDepartment() {
        ServiceRequest request = new ServiceRequest();

        Optional<User> selected = strategy.selectAssignee(request);

        assertTrue(selected.isEmpty());
    }

    private User agent(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName(email);
        user.setRole(Role.AGENT);
        user.setDepartment("IT");
        return user;
    }
}
