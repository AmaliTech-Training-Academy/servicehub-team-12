package com.servicehub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @Email @NotBlank
    private String email;
    @NotBlank @Size(min = 8)
    private String password;
    @NotBlank
    private String confirmPassword;
    @NotBlank
    @Pattern(regexp = "IT|HR|Facilities", message = "Department must be one of: IT, HR, Facilities")
    private String department;
}
