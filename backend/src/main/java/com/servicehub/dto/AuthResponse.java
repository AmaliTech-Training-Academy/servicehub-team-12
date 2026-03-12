package com.servicehub.dto;

import lombok.*;

import java.util.UUID;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class AuthResponse {
    private UUID id;
    private String token;
    private String refreshToken;
    private String email;
    private String role;
    private String fullName;
}
