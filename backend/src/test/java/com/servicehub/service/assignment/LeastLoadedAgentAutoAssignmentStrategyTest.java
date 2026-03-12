package com.servicehub.service.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.servicehub.model.Department;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import java.time.LocalDateTime;
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
class LeastLoadedAgentAutoAssignmentStrategyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private LeastLoadedAgentAutoAssignmentStrategy strategy;

    @Test
    @DisplayName("selectAssignee returns the active agent with the fewest open tickets")
    void selectAssigneeShouldReturnLeastLoadedAgent() {
        ServiceRequest request = requestForDepartment("IT", RequestCategory.IT_SUPPORT);
        User firstAgent = agent("first@amalitech.com", LocalDateTime.of(2026, 3, 1, 8, 0));
        User secondAgent = agent("second@amalitech.com", LocalDateTime.of(2026, 3, 2, 8, 0));

        when(userRepository.findAllByRoleAndDepartmentIgnoreCaseAndIsActiveTrueOrderByCreatedAtAsc(Role.AGENT, "IT"))
                .thenReturn(List.of(firstAgent, secondAgent));
        when(serviceRequestRepository.countByAssignedToAndStatusNotIn(firstAgent, List.of(
                com.servicehub.model.enums.RequestStatus.RESOLVED,
                com.servicehub.model.enums.RequestStatus.CLOSED))).thenReturn(4L);
        when(serviceRequestRepository.countByAssignedToAndStatusNotIn(secondAgent, List.of(
                com.servicehub.model.enums.RequestStatus.RESOLVED,
                com.servicehub.model.enums.RequestStatus.CLOSED))).thenReturn(1L);

        Optional<User> selected = strategy.selectAssignee(request);

        assertTrue(selected.isPresent());
        assertEquals(secondAgent, selected.get());
    }

    @Test
    @DisplayName("selectAssignee breaks ties using earliest creation time")
    void selectAssigneeShouldBreakTiesUsingCreatedAt() {
        ServiceRequest request = requestForDepartment("Facilities", RequestCategory.FACILITIES);
        User firstAgent = agent("first@amalitech.com", LocalDateTime.of(2026, 3, 1, 8, 0));
        User secondAgent = agent("second@amalitech.com", LocalDateTime.of(2026, 3, 2, 8, 0));

        when(userRepository.findAllByRoleAndDepartmentIgnoreCaseAndIsActiveTrueOrderByCreatedAtAsc(Role.AGENT, "Facilities"))
                .thenReturn(List.of(firstAgent, secondAgent));
        when(serviceRequestRepository.countByAssignedToAndStatusNotIn(firstAgent, List.of(
                com.servicehub.model.enums.RequestStatus.RESOLVED,
                com.servicehub.model.enums.RequestStatus.CLOSED))).thenReturn(2L);
        when(serviceRequestRepository.countByAssignedToAndStatusNotIn(secondAgent, List.of(
                com.servicehub.model.enums.RequestStatus.RESOLVED,
                com.servicehub.model.enums.RequestStatus.CLOSED))).thenReturn(2L);

        Optional<User> selected = strategy.selectAssignee(request);

        assertTrue(selected.isPresent());
        assertEquals(firstAgent, selected.get());
    }

    @Test
    @DisplayName("selectAssignee returns empty when no active matching agents exist")
    void selectAssigneeShouldReturnEmptyWhenNoMatchingAgentsExist() {
        ServiceRequest request = requestForDepartment("HR", RequestCategory.HR_REQUEST);

        when(userRepository.findAllByRoleAndDepartmentIgnoreCaseAndIsActiveTrueOrderByCreatedAtAsc(Role.AGENT, "HR"))
                .thenReturn(List.of());

        Optional<User> selected = strategy.selectAssignee(request);

        assertTrue(selected.isEmpty());
        verify(userRepository).findAllByRoleAndDepartmentIgnoreCaseAndIsActiveTrueOrderByCreatedAtAsc(Role.AGENT, "HR");
    }

    @Test
    @DisplayName("selectAssignee returns empty when request has no department")
    void selectAssigneeShouldReturnEmptyWhenRequestHasNoDepartment() {
        Optional<User> selected = strategy.selectAssignee(new ServiceRequest());

        assertTrue(selected.isEmpty());
    }

    private ServiceRequest requestForDepartment(String name, RequestCategory category) {
        ServiceRequest request = new ServiceRequest();
        Department department = new Department();
        department.setId(UUID.randomUUID());
        department.setName(name);
        department.setCategory(category);
        request.setDepartment(department);
        return request;
    }

    private User agent(String email, LocalDateTime createdAt) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName(email);
        user.setRole(Role.AGENT);
        user.setDepartment("IT");
        user.setCreatedAt(createdAt);
        user.setActive(true);
        return user;
    }
}
