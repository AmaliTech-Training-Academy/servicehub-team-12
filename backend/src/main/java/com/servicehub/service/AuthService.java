package com.servicehub.service;

import com.servicehub.dto.AuthRequest;
import com.servicehub.dto.AuthResponse;
import com.servicehub.dto.RefreshTokenRequest;
import com.servicehub.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
/**
 * Defines authentication and token-management operations.
 */

public interface AuthService {

    /** Register a new local user and return tokens. */
    AuthResponse register(RegisterRequest request);

    /** Authenticate with email + password and return tokens. */
    AuthResponse login(AuthRequest request);

    /**
     * Rotate a refresh token: validate the old one, revoke it,
     * issue a new access token + refresh token pair.
     */
    AuthResponse refresh(RefreshTokenRequest request);

    /**
     * Blacklist the current access token and revoke all refresh tokens
     * for the authenticated user.
     */
    void logout(HttpServletRequest request);

    /** Writes a secure HttpOnly JWT cookie onto the HTTP response. */
    void issueJwtCookie(AuthResponse authResponse, HttpServletResponse response);
}
