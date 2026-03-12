package com.servicehub.service.impl;

import com.servicehub.dto.UserDTO;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findAll(String query, Role role) {
        List<User> users = (query == null || query.isBlank()) && role == null
                ? userRepository.findAllByOrderByCreatedAtDesc()
                : userRepository.search(
                        query == null ? "" : query.trim().toLowerCase(),
                        role);
        return users.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findById(UUID id) {
        return toDTO(getOrThrow(id));
    }

    // ── Mutations ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void changeRole(UUID id, Role newRole) {
        User user = getOrThrow(id);
        user.setRole(newRole);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void toggleActive(UUID id) {
        User user = getOrThrow(id);
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        User user = getOrThrow(id);
        userRepository.delete(user);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User getOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    /**
     * Maps a {@link User} entity to a {@link UserDTO}.
     * Splits {@code fullName} on the first space so Thymeleaf templates
     * can access {@code u.firstName} and {@code u.lastName} independently.
     */
    private UserDTO toDTO(User user) {
        String full  = user.getFullName() == null ? "" : user.getFullName().trim();
        int    space = full.indexOf(' ');
        String first = space > 0 ? full.substring(0, space) : full;
        String last  = space > 0 ? full.substring(space + 1) : "";

        return UserDTO.builder()
                .id(user.getId())
                .firstName(first)
                .lastName(last)
                .email(user.getEmail())
                .department(user.getDepartment())
                .role(user.getRole() == null ? "" : user.getRole().name())
                .active(user.isActive())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                // Workload stats: default to 0 until ETL pipeline wires live data
                .openTicketCount(0)
                .slaComplianceRatePct(0)
                .build();
    }
}

