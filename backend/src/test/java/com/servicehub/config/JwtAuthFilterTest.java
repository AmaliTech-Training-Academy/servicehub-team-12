package com.servicehub.config;

import com.servicehub.repository.TokenBlacklistRepository;
import com.servicehub.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter")
class JwtAuthFilterTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private FilterChain filterChain;

    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter(jwtService, userDetailsService, tokenBlacklistRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh"
    })
    @DisplayName("skips JWT validation on public auth endpoints even with a bad bearer token")
    void skipsJwtValidationOnPublicAuthEndpoints(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.addHeader("Authorization", "Bearer malformed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verifyNoInteractions(jwtService, userDetailsService, tokenBlacklistRepository);
        assertThat(response.getStatus()).isEqualTo(MockHttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("skips JWT validation on login when a stale jwt cookie is present")
    void skipsJwtValidationOnLoginWithJwtCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setCookies(new Cookie("jwt", "stale-cookie-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verifyNoInteractions(jwtService, userDetailsService, tokenBlacklistRepository);
        assertThat(response.getStatus()).isEqualTo(MockHttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("rejects malformed JWT on protected API endpoints")
    void rejectsMalformedJwtOnProtectedApiEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dashboard/me");
        request.addHeader("Authorization", "Bearer malformed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistRepository.existsByToken("malformed-token")).thenReturn(false);
        when(jwtService.extractEmail("malformed-token")).thenThrow(new RuntimeException("bad token"));

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(tokenBlacklistRepository).existsByToken("malformed-token");
        verify(jwtService).extractEmail("malformed-token");
        assertThat(response.getStatus()).isEqualTo(MockHttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains("Token is invalid or has expired");
    }
}

