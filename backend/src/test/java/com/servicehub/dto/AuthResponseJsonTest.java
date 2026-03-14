package com.servicehub.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests JSON serialization behavior for authentication responses.
 */

@DisplayName("AuthResponse JSON")
class AuthResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("serializes the auth response UUID as id")
    void serializesIdField() throws Exception {
        UUID id = UUID.randomUUID();
        AuthResponse response = AuthResponse.builder()
                .id(id)
                .token("access-token")
                .refreshToken("refresh-token")
                .email("user@example.com")
                .role("USER")
                .fullName("John Doe")
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"id\":\"" + id + "\"");
        assertThat(json).doesNotContain("userId");
    }
}

