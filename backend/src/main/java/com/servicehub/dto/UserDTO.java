package com.servicehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID          id;
    private String        firstName;
    private String        lastName;
    private String        email;
    private String        department;
    private String        role;
    private boolean       active;
    private String        provider;
    private LocalDateTime createdAt;
}
