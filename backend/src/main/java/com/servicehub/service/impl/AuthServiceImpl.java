package com.servicehub.service.impl;

import com.servicehub.config.JwtService;
import com.servicehub.dto.AuthRequest;
import com.servicehub.dto.AuthResponse;
import com.servicehub.dto.RefreshTokenRequest;
import com.servicehub.dto.RegisterRequest;
import com.servicehub.exception.EmailAlreadyExistsException;
import com.servicehub.exception.InvalidCredentialsException;
import com.servicehub.exception.InvalidRefreshTokenException;
import com.servicehub.exception.PasswordMismatchException;
import com.servicehub.model.RefreshToken;
import com.servicehub.model.TokenBlacklist;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.RefreshTokenRepository;
import com.servicehub.repository.TokenBlacklistRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFirstName() + " " + request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .department(request.getDepartment())
                .provider("local")
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        return buildAuthResponse(savedUser);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new InvalidRefreshTokenException();
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        log.info("Refresh token rotated for user: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null) return;

        try {
            Instant expiry = jwtService.extractExpiration(token);
            if (expiry.isAfter(Instant.now()) && !tokenBlacklistRepository.existsByToken(token)) {
                tokenBlacklistRepository.save(
                        TokenBlacklist.builder()
                                .token(token)
                                .expiresAt(expiry)
                                .build()
                );
            }
            String email = jwtService.extractEmail(token);
            userRepository.findByEmail(email).ifPresent(user -> {
                refreshTokenRepository.revokeAllByUser(user);
                log.info("User logged out, all tokens revoked: {}", email);
            });
        } catch (Exception ex) {
            log.warn("Logout called with unparseable token, ignoring: {}", ex.getMessage());
        }
    }

    // ── Cookie ────────────────────────────────────────────────────────────────

    @Override
    public void issueJwtCookie(AuthResponse authResponse, HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", authResponse.getToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String rawRefresh  = UUID.randomUUID().toString();

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .token(rawRefresh)
                        .user(user)
                        .expiresAt(Instant.now().plusMillis(refreshExpiration))
                        .revoked(false)
                        .build()
        );

        return AuthResponse.builder()
                .id(user.getId())
                .token(accessToken)
                .refreshToken(rawRefresh)
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .build();
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> "jwt".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
