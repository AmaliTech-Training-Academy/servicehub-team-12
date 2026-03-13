package com.servicehub.service;

import com.servicehub.dto.UserPageQuery;
import com.servicehub.dto.UserDTO;
import com.servicehub.model.enums.Role;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface UserService {

    /** Return all users, optionally filtered by name/email query and/or role. */
    List<UserDTO> findAll(String query, Role role);

    Page<UserDTO> findPage(UserPageQuery query);

    /** Return a single user by id, throws 404 if not found. */
    UserDTO findById(UUID id);

    /** Change the role of a user. Throws 404 if not found. */
    void changeRole(UUID id, Role newRole);

    /** Toggle the isActive flag for a user. Throws 404 if not found. */
    void toggleActive(UUID id);

    /** Hard-delete a user. Throws 404 if not found. */
    void delete(UUID id);
}
