package com.servicehub.controller.api;

import com.servicehub.dto.UserDTO;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type User controller.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management — list, inspect, change role, toggle active status and delete users")
public class UserController {

    private final UserService userService;

    // ── List ──────────────────────────────────────────────────────────────────

    /**
     * Find all response entity.
     *
     * @param query the query
     * @param role  the role
     * @return the response entity
     */
    @Operation(
        summary = "List all users",
        description = "Returns every user. Optionally filter by a name/email search term and/or role. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User list returned"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserDTO>> findAll(
            @Parameter(description = "Optional name or email search term")
            @RequestParam(required = false) String query,
            @Parameter(description = "Optional role filter — USER | AGENT | ADMIN")
            @RequestParam(required = false) Role role) {
        return ResponseEntity.ok(userService.findAll(query, role));
    }

    // ── Get one ───────────────────────────────────────────────────────────────

    /**
     * Find by id response entity.
     *
     * @param id        the id
     * @param principal the principal
     * @return the response entity
     */
    @Operation(
        summary = "Get a user by id",
        description = "ADMIN can retrieve any user. An authenticated user can retrieve their own profile."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "403", description = "Non-admin caller requested another user's profile"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User principal) {
        if (principal != null && principal.getRole() != Role.ADMIN
                && !principal.getId().equals(id)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userService.findById(id));
    }

    // ── Change role ───────────────────────────────────────────────────────────

    /**
     * Change role response entity.
     *
     * @param id   the id
     * @param role the role
     * @return the response entity
     */
    @Operation(
        summary = "Change a user's role",
        description = "Assigns a new role (USER | AGENT | ADMIN) to the specified user. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Role updated"),
        @ApiResponse(responseCode = "400", description = "Invalid role value"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable UUID id,
            @Parameter(description = "New role — USER | AGENT | ADMIN", required = true)
            @RequestParam Role role) {
        userService.changeRole(id, role);
        return ResponseEntity.noContent().build();
    }

    // ── Toggle active ─────────────────────────────────────────────────────────

    /**
     * Toggle active response entity.
     *
     * @param id the id
     * @return the response entity
     */
    @Operation(
        summary = "Toggle a user's active status",
        description = "Flips the isActive flag — activates an inactive user or deactivates an active one. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Active status toggled"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> toggleActive(@PathVariable UUID id) {
        userService.toggleActive(id);
        return ResponseEntity.noContent().build();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Delete response entity.
     *
     * @param id the id
     * @return the response entity
     */
    @Operation(
        summary = "Delete a user",
        description = "Permanently removes the user record. ADMIN only."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN role"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

