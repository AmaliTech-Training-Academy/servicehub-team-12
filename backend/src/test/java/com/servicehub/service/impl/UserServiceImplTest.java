package com.servicehub.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.servicehub.dto.UserDTO;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("calls findAllByOrderByCreatedAtDesc when query is null and role is null")
        void shouldCallFindAllWhenNoFiltersProvided() {
            User user = user(UUID.randomUUID(), "Alice Johnson", Role.USER);
            when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(user));

            List<UserDTO> result = userService.findAll(null, null);

            assertEquals(1, result.size());
            verify(userRepository).findAllByOrderByCreatedAtDesc();
            verify(userRepository, never()).search(any(), any());
        }

        @Test
        @DisplayName("calls findAllByOrderByCreatedAtDesc when query is blank and role is null")
        void shouldCallFindAllWhenQueryIsBlankAndRoleIsNull() {
            when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

            userService.findAll("   ", null);

            verify(userRepository).findAllByOrderByCreatedAtDesc();
            verify(userRepository, never()).search(any(), any());
        }

        @Test
        @DisplayName("delegates to search when query is provided")
        void shouldCallSearchWhenQueryIsProvided() {
            User user = user(UUID.randomUUID(), "Bob Mensah", Role.AGENT);
            when(userRepository.search("bob", null)).thenReturn(List.of(user));

            List<UserDTO> result = userService.findAll("Bob", null);

            assertEquals(1, result.size());
            verify(userRepository).search("bob", null);
        }

        @Test
        @DisplayName("delegates to search when role is provided")
        void shouldCallSearchWhenRoleIsProvided() {
            User user = user(UUID.randomUUID(), "Admin User", Role.ADMIN);
            when(userRepository.search("", Role.ADMIN)).thenReturn(List.of(user));

            List<UserDTO> result = userService.findAll(null, Role.ADMIN);

            assertEquals(1, result.size());
            verify(userRepository).search("", Role.ADMIN);
        }

        @Test
        @DisplayName("delegates to search when both query and role are provided")
        void shouldCallSearchWhenBothFiltersProvided() {
            User user = user(UUID.randomUUID(), "Kwame Boateng", Role.AGENT);
            when(userRepository.search("kwame", Role.AGENT)).thenReturn(List.of(user));

            List<UserDTO> result = userService.findAll("Kwame", Role.AGENT);

            assertEquals(1, result.size());
            verify(userRepository).search("kwame", Role.AGENT);
        }

        @Test
        @DisplayName("trims and lowercases query before passing to search")
        void shouldTrimAndLowercaseQueryBeforeSearch() {
            when(userRepository.search("alice", null)).thenReturn(List.of());

            userService.findAll("  Alice  ", null);

            verify(userRepository).search("alice", null);
        }

        @Test
        @DisplayName("returns empty list when no users match")
        void shouldReturnEmptyListWhenNoUsersFound() {
            when(userRepository.search("ghost", null)).thenReturn(List.of());

            List<UserDTO> result = userService.findAll("ghost", null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("maps all returned users to DTOs")
        void shouldMapAllUsersToDTO() {
            List<User> users = List.of(
                user(UUID.randomUUID(), "Alice Johnson",  Role.USER),
                user(UUID.randomUUID(), "Bob Mensah",     Role.AGENT),
                user(UUID.randomUUID(), "Kwame Boateng",  Role.ADMIN)
            );
            when(userRepository.findAllByOrderByCreatedAtDesc()).thenReturn(users);

            List<UserDTO> result = userService.findAll(null, null);

            assertEquals(3, result.size());
        }
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns mapped DTO when user exists")
        void shouldReturnMappedDTOWhenUserExists() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Alice Johnson", Role.USER);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserDTO result = userService.findById(id);

            assertNotNull(result);
            assertEquals(id,              result.getId());
            assertEquals("Alice",         result.getFirstName());
            assertEquals("Johnson",       result.getLastName());
            assertEquals("user@test.com", result.getEmail());
            assertEquals("USER",          result.getRole());
        }

        @Test
        @DisplayName("throws NOT_FOUND when user does not exist")
        void shouldThrowNotFoundWhenUserMissing() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> userService.findById(id));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertTrue(ex.getMessage().contains(id.toString()));
        }
    }

    // ── changeRole ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("changeRole")
    class ChangeRole {

        @Test
        @DisplayName("updates role to ADMIN and saves")
        void shouldUpdateRoleToAdminAndSave() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Alice Johnson", Role.USER);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.changeRole(id, Role.ADMIN);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals(Role.ADMIN, captor.getValue().getRole());
        }

        @Test
        @DisplayName("updates role to AGENT and saves")
        void shouldUpdateRoleToAgentAndSave() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Bob Mensah", Role.USER);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.changeRole(id, Role.AGENT);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals(Role.AGENT, captor.getValue().getRole());
        }

        @Test
        @DisplayName("downgrades role from ADMIN to USER and saves")
        void shouldDowngradeRoleFromAdminToUser() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Admin User", Role.ADMIN);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.changeRole(id, Role.USER);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals(Role.USER, captor.getValue().getRole());
        }

        @Test
        @DisplayName("throws NOT_FOUND and never saves when user does not exist")
        void shouldThrowNotFoundAndNeverSave() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> userService.changeRole(id, Role.AGENT));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            verify(userRepository, never()).save(any());
        }
    }

    // ── toggleActive ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toggleActive")
    class ToggleActive {

        @Test
        @DisplayName("deactivates an active user")
        void shouldDeactivateActiveUser() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Alice Johnson", Role.USER);
            user.setActive(true);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.toggleActive(id);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertFalse(captor.getValue().isActive());
        }

        @Test
        @DisplayName("reactivates an inactive user")
        void shouldReactivateInactiveUser() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Bob Mensah", Role.AGENT);
            user.setActive(false);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.toggleActive(id);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertTrue(captor.getValue().isActive());
        }

        @Test
        @DisplayName("throws NOT_FOUND and never saves when user does not exist")
        void shouldThrowNotFoundAndNeverSave() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> userService.toggleActive(id));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            verify(userRepository, never()).save(any());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes the user when found")
        void shouldDeleteWhenUserExists() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Alice Johnson", Role.USER);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            userService.delete(id);

            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("throws NOT_FOUND and never calls delete when user is missing")
        void shouldThrowNotFoundAndSkipDeleteWhenUserMissing() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> userService.delete(id));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            verify(userRepository, never()).delete(any(User.class));
        }
    }

    // ── DTO mapping (toDTO) ───────────────────────────────────────────────────

    @Nested
    @DisplayName("DTO mapping")
    class DtoMapping {

        @Test
        @DisplayName("splits full name on first space into firstName and lastName")
        void shouldSplitFullNameOnFirstSpace() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id))
                .thenReturn(Optional.of(user(id, "Alice Johnson", Role.USER)));

            UserDTO dto = userService.findById(id);

            assertEquals("Alice",   dto.getFirstName());
            assertEquals("Johnson", dto.getLastName());
        }

        @Test
        @DisplayName("sets lastName to empty string when full name has no space")
        void shouldSetEmptyLastNameWhenFullNameHasNoSpace() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Alice", Role.USER);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserDTO dto = userService.findById(id);

            assertEquals("Alice", dto.getFirstName());
            assertEquals("",      dto.getLastName());
        }

        @Test
        @DisplayName("handles null full name — both names are empty strings")
        void shouldHandleNullFullName() {
            UUID id = UUID.randomUUID();
            User user = user(id, null, Role.USER);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserDTO dto = userService.findById(id);

            assertEquals("", dto.getFirstName());
            assertEquals("", dto.getLastName());
        }

        @Test
        @DisplayName("handles blank full name — both names are empty strings")
        void shouldHandleBlankFullName() {
            UUID id = UUID.randomUUID();
            User user = user(id, "   ", Role.USER);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserDTO dto = userService.findById(id);

            assertEquals("", dto.getFirstName());
            assertEquals("", dto.getLastName());
        }

        @Test
        @DisplayName("maps role enum to its string name")
        void shouldMapRoleEnumToString() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id))
                .thenReturn(Optional.of(user(id, "Alice Johnson", Role.AGENT)));

            UserDTO dto = userService.findById(id);

            assertEquals("AGENT", dto.getRole());
        }

        @Test
        @DisplayName("maps null role to empty string")
        void shouldMapNullRoleToEmptyString() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Alice Johnson", null);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserDTO dto = userService.findById(id);

            assertEquals("", dto.getRole());
        }

        @Test
        @DisplayName("maps isActive flag correctly for inactive user")
        void shouldMapIsActiveFlagForInactiveUser() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Bob Mensah", Role.USER);
            user.setActive(false);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserDTO dto = userService.findById(id);

            assertFalse(dto.isActive());
        }

        @Test
        @DisplayName("maps isActive flag correctly for active user")
        void shouldMapIsActiveFlagForActiveUser() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Bob Mensah", Role.USER);
            user.setActive(true);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserDTO dto = userService.findById(id);

            assertTrue(dto.isActive());
        }

        @Test
        @DisplayName("defaults workload stats to zero")
        void shouldDefaultWorkloadStatsToZero() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id))
                .thenReturn(Optional.of(user(id, "Kwame Boateng", Role.AGENT)));

            UserDTO dto = userService.findById(id);

            assertEquals(0, dto.getOpenTicketCount());
            assertEquals(0, dto.getSlaComplianceRatePct());
        }

        @Test
        @DisplayName("maps department and provider fields")
        void shouldMapDepartmentAndProvider() {
            UUID id = UUID.randomUUID();
            User user = user(id, "Alice Johnson", Role.USER);
            user.setDepartment("IT Support");
            user.setProvider("google");
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserDTO dto = userService.findById(id);

            assertEquals("IT Support", dto.getDepartment());
            assertEquals("google",     dto.getProvider());
        }

        @Test
        @DisplayName("maps createdAt timestamp")
        void shouldMapCreatedAtTimestamp() {
            UUID id = UUID.randomUUID();
            LocalDateTime created = LocalDateTime.of(2026, 3, 11, 10, 0);
            User user = user(id, "Alice Johnson", Role.USER);
            user.setCreatedAt(created);
            when(userRepository.findById(id)).thenReturn(Optional.of(user));

            UserDTO dto = userService.findById(id);

            assertEquals(created, dto.getCreatedAt());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User user(UUID id, String fullName, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@test.com");
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);
        user.setProvider("local");
        return user;
    }
}

