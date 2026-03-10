package com.servicehub.controller.api;

import com.servicehub.dto.AuthRequest;
import com.servicehub.dto.AuthResponse;
import com.servicehub.dto.RefreshTokenRequest;
import com.servicehub.dto.RegisterRequest;
import com.servicehub.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh and logout")
public class AuthController {

    private final AuthService authService;

    // ── Register ─────────────────────────────────────────────────────────────

    @Operation(summary = "Register a new user account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registered successfully — access + refresh tokens returned"),
        @ApiResponse(responseCode = "400", description = "Validation error or passwords do not match"),
        @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        authService.issueJwtCookie(auth, response);
        return ResponseEntity.ok(auth);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Login with email and password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — access + refresh tokens returned"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                               HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        authService.issueJwtCookie(auth, response);
        return ResponseEntity.ok(auth);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Operation(
        summary = "Rotate tokens using a refresh token",
        description = "Validates the supplied refresh token, revokes it (single-use rotation), " +
                      "and returns a fresh access token + new refresh token pair."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New token pair issued"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, or already revoked")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                 HttpServletResponse response) {
        AuthResponse auth = authService.refresh(request);
        authService.issueJwtCookie(auth, response);
        return ResponseEntity.ok(auth);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Logout — blacklist access token and revoke all refresh tokens",
        description = "Reads the Bearer token from the Authorization header (or the jwt cookie), " +
                      "adds it to the blacklist, and revokes all stored refresh tokens for the user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logged out successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                        HttpServletResponse response) {
        authService.logout(request);

        // Clear the jwt cookie on the browser side as well
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }
}
