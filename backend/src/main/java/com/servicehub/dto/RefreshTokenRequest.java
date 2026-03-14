package com.servicehub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * Data transfer object for refresh token submission.
 */

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token must not be blank")
    private String refreshToken;
}

