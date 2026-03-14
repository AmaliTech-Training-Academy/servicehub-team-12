package com.servicehub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * Data transfer object for authentication credentials.
 */

@Data
public class AuthRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
}
