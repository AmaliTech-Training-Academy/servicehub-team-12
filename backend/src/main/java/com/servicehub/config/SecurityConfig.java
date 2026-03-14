package com.servicehub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehub.service.CustomUserDetailsService;
import com.servicehub.service.OAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * Configures Spring Security authentication and authorization rules.
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final OAuth2UserService oAuth2UserService;

    // ── HTTP filter chain ─────────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configure(http))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/logout", "/assets/**", "/error").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/agent/**").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/dashboard/admin").hasRole("ADMIN")
                .requestMatchers("/dashboard/agent").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/dashboard/user").hasAnyRole("ADMIN", "AGENT", "USER")
                .requestMatchers("/requests/assigned", "/requests/open").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/requests/**").hasAnyRole("ADMIN", "AGENT", "USER")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            // For API paths: return 401 JSON — never redirect to login HTML
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    apiAuthenticationEntryPoint(),
                    req -> req.getRequestURI().startsWith("/api/")
                )
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                .defaultSuccessUrl("/oauth2/success", true)
                .failureUrl("/login?error")
            )
            .logout(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // Returns a JSON 401 body instead of a 302 redirect to /login
    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            String body = new ObjectMapper().writeValueAsString(Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", "Authentication required. Please provide a valid Bearer token.",
                "timestamp", LocalDateTime.now().toString()
            ));
            response.getWriter().write(body);
        };
    }

    // ── Auth infrastructure beans ─────────────────────────────────────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
