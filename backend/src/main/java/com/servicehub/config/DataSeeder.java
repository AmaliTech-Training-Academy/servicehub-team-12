package com.servicehub.config;

import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL    = "admin@amalitech.com";
    private static final String ADMIN_PASSWORD = "password123";
    private static final String ADMIN_NAME     = "System Administrator";

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @SuppressWarnings("NullableProblems")
    public void run(String... args) {
        seedAdmin();
    }

    private void seedAdmin() {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            log.info("Admin user already exists — skipping seed.");
            return;
        }

        User admin = User.builder()
                .email(ADMIN_EMAIL)
                .fullName(ADMIN_NAME)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .provider("local")
                .isActive(true)
                .build();

        userRepository.save(admin);
        log.info("Admin user created: {}", ADMIN_EMAIL);
    }
}

