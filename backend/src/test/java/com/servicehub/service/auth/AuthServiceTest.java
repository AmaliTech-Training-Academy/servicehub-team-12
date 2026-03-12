package com.servicehub.service.auth;

import com.servicehub.config.JwtService;
import com.servicehub.dto.AuthRequest;
import com.servicehub.dto.AuthResponse;
import com.servicehub.dto.RefreshTokenRequest;
import com.servicehub.dto.RegisterRequest;
import com.servicehub.exception.EmailAlreadyExistsException;
import com.servicehub.exception.InvalidCredentialsException;
import com.servicehub.exception.InvalidRefreshTokenException;
import com.servicehub.exception.PasswordMismatchException;
import com.servicehub.model.Department;
import com.servicehub.model.RefreshToken;
import com.servicehub.model.TokenBlacklist;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.DepartmentRepository;
import com.servicehub.repository.RefreshTokenRepository;
import com.servicehub.repository.TokenBlacklistRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.impl.AuthServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceTest {


    @Mock private UserRepository              userRepository;
    @Mock private DepartmentRepository        departmentRepository;
    @Mock private RefreshTokenRepository      refreshTokenRepository;
    @Mock private TokenBlacklistRepository    tokenBlacklistRepository;
    @Mock private PasswordEncoder             passwordEncoder;
    @Mock private JwtService                  jwtService;
    @Mock private AuthenticationManager       authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final String EMAIL        = "user@example.com";
    private static final String PASSWORD     = "secret123";
    private static final String ENCODED_PWD  = "$2a$10$encodedpassword";
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.access";
    private static final long   REFRESH_EXP  = 604_800_000L; // 7 days ms

    private User sampleUser;
    private RegisterRequest validRegisterRequest;
    private Department itDepartment;

    @BeforeEach
    void setUp() {
        // inject @Value field that is not set by Mockito
        ReflectionTestUtils.setField(authService, "refreshExpiration", REFRESH_EXP);

        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .fullName("John Doe")
                .password(ENCODED_PWD)
                .role(Role.USER)
                .provider("local")
                .isActive(true)
                .build();

        itDepartment = new Department();
        itDepartment.setId(UUID.randomUUID());
        itDepartment.setName("IT");
        itDepartment.setCategory(RequestCategory.IT_SUPPORT);

        validRegisterRequest = new RegisterRequest(
                "John", "Doe", EMAIL, PASSWORD, PASSWORD, "IT");
    }

    // =========================================================================
    // register()
    // =========================================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("saves user and returns token pair on valid request")
        void register_validRequest_savesUserAndReturnsTokens() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(departmentRepository.findByNameIgnoreCase("IT")).thenReturn(Optional.of(itDepartment));
            when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PWD);
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);
            when(jwtService.generateToken(any(User.class))).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.register(validRegisterRequest);

            assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getFullName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("saves user with correct field values")
        void register_validRequest_userHasCorrectFields() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(departmentRepository.findByNameIgnoreCase("IT")).thenReturn(Optional.of(itDepartment));
            when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PWD);
            when(userRepository.save(any(User.class))).thenReturn(sampleUser);
            when(jwtService.generateToken(any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.register(validRegisterRequest);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getFullName()).isEqualTo("John Doe");
            assertThat(saved.getPassword()).isEqualTo(ENCODED_PWD);
            assertThat(saved.getRole()).isEqualTo(Role.USER);
            assertThat(saved.getDepartment()).isEqualTo("IT");
            assertThat(saved.getDepartmentEntity()).isEqualTo(itDepartment);
            assertThat(saved.getProvider()).isEqualTo("local");
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("saves a refresh token with correct expiry and not revoked")
        void register_validRequest_refreshTokenSavedCorrectly() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(departmentRepository.findByNameIgnoreCase("IT")).thenReturn(Optional.of(itDepartment));
            when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PWD);
            when(userRepository.save(any())).thenReturn(sampleUser);
            when(jwtService.generateToken(any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Instant before = Instant.now();
            authService.register(validRegisterRequest);
            Instant after = Instant.now();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken rt = captor.getValue();

            assertThat(rt.isRevoked()).isFalse();
            assertThat(rt.getToken()).isNotBlank();
            assertThat(rt.getExpiresAt())
                    .isAfter(before.plusMillis(REFRESH_EXP - 1000))
                    .isBefore(after.plusMillis(REFRESH_EXP + 1000));
        }

        @Test
        @DisplayName("throws PasswordMismatchException when passwords do not match")
        void register_passwordMismatch_throwsPasswordMismatchException() {
            RegisterRequest req = new RegisterRequest(
                    "John", "Doe", EMAIL, PASSWORD, "different123", "IT");

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(PasswordMismatchException.class)
                    .hasMessageContaining("do not match");

            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("throws EmailAlreadyExistsException when email is taken")
        void register_duplicateEmail_throwsEmailAlreadyExistsException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> authService.register(validRegisterRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining(EMAIL);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("full name is firstName + space + lastName")
        void register_fullNameConcatenated() {
            RegisterRequest req = new RegisterRequest(
                    "Alice", "Smith", EMAIL, PASSWORD, PASSWORD, "HR");
            Department hrDepartment = new Department();
            hrDepartment.setId(UUID.randomUUID());
            hrDepartment.setName("HR");
            hrDepartment.setCategory(RequestCategory.HR_REQUEST);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(departmentRepository.findByNameIgnoreCase("HR")).thenReturn(Optional.of(hrDepartment));
            when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PWD);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.register(req);

            verify(userRepository).save(argThat(u -> "Alice Smith".equals(u.getFullName())));
        }
    }

    // =========================================================================
    // login()
    // =========================================================================

    @Nested
    @DisplayName("login()")
    class Login {

        private AuthRequest validLoginRequest;

        @BeforeEach
        void setUp() {
            validLoginRequest = new AuthRequest();
            validLoginRequest.setEmail(EMAIL);
            validLoginRequest.setPassword(PASSWORD);
        }

        @Test
        @DisplayName("returns token pair on valid credentials")
        void login_validCredentials_returnsTokens() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser));
            when(jwtService.generateToken(sampleUser)).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.login(validLoginRequest);

            assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            assertThat(response.getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("calls AuthenticationManager with correct credentials")
        void login_validCredentials_authenticatesWithManager() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser));
            when(jwtService.generateToken(any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.login(validLoginRequest);

            verify(authenticationManager).authenticate(
                    argThat(auth ->
                            auth instanceof UsernamePasswordAuthenticationToken token &&
                            EMAIL.equals(token.getPrincipal()) &&
                            PASSWORD.equals(token.getCredentials())
                    )
            );
        }

        @Test
        @DisplayName("throws InvalidCredentialsException on bad password")
        void login_badPassword_throwsInvalidCredentialsException() {
            doThrow(new BadCredentialsException("bad"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid email or password");

            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when user not found after auth")
        void login_userNotFound_throwsInvalidCredentialsException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(validLoginRequest))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    // =========================================================================
    // refresh()
    // =========================================================================

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        private RefreshToken validStoredToken;
        private RefreshTokenRequest refreshRequest;

        @BeforeEach
        void setUp() {
            validStoredToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token("valid-refresh-token")
                    .user(sampleUser)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            refreshRequest = new RefreshTokenRequest();
            refreshRequest.setRefreshToken("valid-refresh-token");
        }

        @Test
        @DisplayName("returns new token pair on valid refresh token")
        void refresh_validToken_returnsNewTokenPair() {
            when(refreshTokenRepository.findByToken("valid-refresh-token"))
                    .thenReturn(Optional.of(validStoredToken));
            when(jwtService.generateToken(sampleUser)).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.refresh(refreshRequest);

            assertThat(response.getToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("revokes the old refresh token during rotation")
        void refresh_validToken_revokesOldToken() {
            when(refreshTokenRepository.findByToken("valid-refresh-token"))
                    .thenReturn(Optional.of(validStoredToken));
            when(jwtService.generateToken(any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.refresh(refreshRequest);

            assertThat(validStoredToken.isRevoked()).isTrue();
            // first save = revoke old, second save = new refresh token
            verify(refreshTokenRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("new refresh token is different from the old one")
        void refresh_validToken_newTokenIsDifferent() {
            when(refreshTokenRepository.findByToken("valid-refresh-token"))
                    .thenReturn(Optional.of(validStoredToken));
            when(jwtService.generateToken(any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.refresh(refreshRequest);

            assertThat(response.getRefreshToken()).isNotEqualTo("valid-refresh-token");
        }

        @Test
        @DisplayName("throws InvalidRefreshTokenException when token not found")
        void refresh_tokenNotFound_throwsInvalidRefreshTokenException() {
            when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(refreshRequest))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("Invalid or expired refresh token");
        }

        @Test
        @DisplayName("throws InvalidRefreshTokenException and deletes when token is revoked")
        void refresh_revokedToken_throwsAndDeletes() {
            validStoredToken.setRevoked(true);
            when(refreshTokenRepository.findByToken("valid-refresh-token"))
                    .thenReturn(Optional.of(validStoredToken));

            assertThatThrownBy(() -> authService.refresh(refreshRequest))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            verify(refreshTokenRepository).delete(validStoredToken);
        }

        @Test
        @DisplayName("throws InvalidRefreshTokenException and deletes when token is expired")
        void refresh_expiredToken_throwsAndDeletes() {
            validStoredToken.setExpiresAt(Instant.now().minusSeconds(3600)); // expired 1h ago
            when(refreshTokenRepository.findByToken("valid-refresh-token"))
                    .thenReturn(Optional.of(validStoredToken));

            assertThatThrownBy(() -> authService.refresh(refreshRequest))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            verify(refreshTokenRepository).delete(validStoredToken);
        }
    }

    // =========================================================================
    // logout()
    // =========================================================================

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("blacklists access token and revokes all refresh tokens")
        void logout_withBearerToken_blacklistsAndRevokesAll() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + ACCESS_TOKEN);

            Instant futureExpiry = Instant.now().plusSeconds(3600);
            when(jwtService.extractExpiration(ACCESS_TOKEN)).thenReturn(futureExpiry);
            when(tokenBlacklistRepository.existsByToken(ACCESS_TOKEN)).thenReturn(false);
            when(jwtService.extractEmail(ACCESS_TOKEN)).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser));

            authService.logout(request);

            // token was blacklisted
            ArgumentCaptor<TokenBlacklist> blCaptor = ArgumentCaptor.forClass(TokenBlacklist.class);
            verify(tokenBlacklistRepository).save(blCaptor.capture());
            assertThat(blCaptor.getValue().getToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(blCaptor.getValue().getExpiresAt()).isEqualTo(futureExpiry);

            // all refresh tokens revoked
            verify(refreshTokenRepository).revokeAllByUser(sampleUser);
        }

        @Test
        @DisplayName("does nothing when no token is present")
        void logout_noToken_doesNothing() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(null);

            authService.logout(request);

            verifyNoInteractions(jwtService, tokenBlacklistRepository, refreshTokenRepository);
        }

        @Test
        @DisplayName("does not blacklist token that is already blacklisted")
        void logout_alreadyBlacklisted_skipsBlacklisting() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + ACCESS_TOKEN);
            when(jwtService.extractExpiration(ACCESS_TOKEN))
                    .thenReturn(Instant.now().plusSeconds(3600));
            when(tokenBlacklistRepository.existsByToken(ACCESS_TOKEN)).thenReturn(true);
            when(jwtService.extractEmail(ACCESS_TOKEN)).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser));

            authService.logout(request);

            verify(tokenBlacklistRepository, never()).save(any());
            verify(refreshTokenRepository).revokeAllByUser(sampleUser);
        }

        @Test
        @DisplayName("does not blacklist already-expired token")
        void logout_expiredToken_skipsBlacklisting() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer " + ACCESS_TOKEN);
            when(jwtService.extractExpiration(ACCESS_TOKEN))
                    .thenReturn(Instant.now().minusSeconds(10)); // already expired
            when(jwtService.extractEmail(ACCESS_TOKEN)).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser));

            authService.logout(request);

            verify(tokenBlacklistRepository, never()).save(any());
        }

        @Test
        @DisplayName("extracts token from jwt cookie when Authorization header is absent")
        void logout_cookieToken_usedWhenNoHeader() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            Cookie jwtCookie = new Cookie("jwt", ACCESS_TOKEN);
            when(request.getHeader("Authorization")).thenReturn(null);
            when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

            when(jwtService.extractExpiration(ACCESS_TOKEN))
                    .thenReturn(Instant.now().plusSeconds(3600));
            when(tokenBlacklistRepository.existsByToken(ACCESS_TOKEN)).thenReturn(false);
            when(jwtService.extractEmail(ACCESS_TOKEN)).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser));

            authService.logout(request);

            verify(tokenBlacklistRepository).save(argThat(bl -> ACCESS_TOKEN.equals(bl.getToken())));
        }

        @Test
        @DisplayName("silently swallows unparseable token without throwing")
        void logout_unparseableToken_doesNotThrow() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("Authorization")).thenReturn("Bearer malformed.token.here");
            when(jwtService.extractExpiration(anyString()))
                    .thenThrow(new RuntimeException("JWT parse error"));

            // should not propagate exception
            authService.logout(request);

            verify(tokenBlacklistRepository, never()).save(any());
        }
    }

    // =========================================================================
    // issueJwtCookie()
    // =========================================================================

    @Nested
    @DisplayName("issueJwtCookie()")
    class IssueJwtCookie {

        @Test
        @DisplayName("adds HttpOnly jwt cookie to response")
        void issueJwtCookie_addsCorrectCookieToResponse() {
            HttpServletResponse response = mock(HttpServletResponse.class);
            AuthResponse authResponse = AuthResponse.builder()
                    .token(ACCESS_TOKEN)
                    .email(EMAIL)
                    .role("USER")
                    .build();

            authService.issueJwtCookie(authResponse, response);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());

            Cookie cookie = captor.getValue();
            assertThat(cookie.getName()).isEqualTo("jwt");
            assertThat(cookie.getValue()).isEqualTo(ACCESS_TOKEN);
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60);
        }
    }
}
