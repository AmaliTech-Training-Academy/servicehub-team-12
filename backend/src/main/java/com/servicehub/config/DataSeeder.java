package com.servicehub.config;

import com.servicehub.model.Department;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.model.enums.UserDepartment;
import com.servicehub.repository.DepartmentRepository;
import com.servicehub.repository.UserRepository;
import java.util.List;
import java.util.UUID;
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
    private static final String AGENT_PASSWORD = "password123";
    
    private static final List<AgentSeed> AGENT_SEEDS = List.of(
            new AgentSeed("ama.boateng@amalitech.com", "Ama Boateng", UserDepartment.IT),
            new AgentSeed("kwame.asare@amalitech.com", "Kwame Asare", UserDepartment.IT),
            new AgentSeed("efua.owusu@amalitech.com", "Efua Owusu", UserDepartment.IT),
            new AgentSeed("kojo.mensah@amalitech.com", "Kojo Mensah", UserDepartment.IT),
            new AgentSeed("abena.agyeman@amalitech.com", "Abena Agyeman", UserDepartment.HR),
            new AgentSeed("yaw.antwi@amalitech.com", "Yaw Antwi", UserDepartment.HR),
            new AgentSeed("akosua.darko@amalitech.com", "Akosua Darko", UserDepartment.HR),
            new AgentSeed("nana.quaye@amalitech.com", "Nana Quaye", UserDepartment.FACILITIES),
            new AgentSeed("adwoa.sarpong@amalitech.com", "Adwoa Sarpong", UserDepartment.FACILITIES),
            new AgentSeed("kofi.bonsu@amalitech.com", "Kofi Bonsu", UserDepartment.FACILITIES)); 
            
    private static final String USER_PASSWORD  = "password123";
    private static final List<SeedUser> REGULAR_USERS = List.of(
            new SeedUser("Daniel Agyeman", "daniel.agyeman@amalitech.com"),
            new SeedUser("Priscilla Tetteh", "priscilla.tetteh@amalitech.com"),
            new SeedUser("Samuel Frimpong", "samuel.frimpong@amalitech.com"),
            new SeedUser("Belinda Kwarteng", "belinda.kwarteng@amalitech.com"),
            new SeedUser("Emmanuel Sarpong", "emmanuel.sarpong@amalitech.com"),
            new SeedUser("Gloria Darko", "gloria.darko@amalitech.com"),
            new SeedUser("Richard Nartey", "richard.nartey@amalitech.com"),
            new SeedUser("Doreen Badu", "doreen.badu@amalitech.com"),
            new SeedUser("Michael Koranteng", "michael.koranteng@amalitech.com"),
            new SeedUser("Patience Aidoo", "patience.aidoo@amalitech.com")
    );

    private final DepartmentRepository departmentRepository;
    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @SuppressWarnings("NullableProblems")
    public void run(String... args) {
        seedDepartments();
        seedAdmin();
        seedAgents();
    }

    private void seedDepartments() {
        seedDepartment(UserDepartment.IT);
        seedDepartment(UserDepartment.HR);
        seedDepartment(UserDepartment.FACILITIES);
    }

    private void seedDepartment(UserDepartment departmentDefinition) {
        if (departmentRepository.findByNameIgnoreCase(departmentDefinition.getDisplayName()).isPresent()) {
            return;
        }

        Department department = new Department();
        department.setId(UUID.randomUUID());
        department.setName(departmentDefinition.getDisplayName());
        department.setCategory(departmentDefinition.getRequestCategory());
        departmentRepository.save(department);
        log.info("Department seeded: {}", departmentDefinition.getDisplayName());
        seedRegularUsers();
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

    private void seedAgents() {
        AGENT_SEEDS.forEach(this::seedAgent);
    }

    private void seedAgent(AgentSeed agentSeed) {
        if (userRepository.existsByEmail(agentSeed.email())) {
            log.info("Agent user already exists — skipping seed for {}.", agentSeed.email());
            return;
        }

        Department department = departmentRepository.findByNameIgnoreCase(agentSeed.department().getDisplayName())
                .orElseThrow(() -> new IllegalStateException(
                        "Department not found for agent seed: " + agentSeed.department().getDisplayName()));

        User agent = User.builder()
                .email(agentSeed.email())
                .fullName(agentSeed.fullName())
                .password(passwordEncoder.encode(AGENT_PASSWORD))
                .role(Role.AGENT)
                .department(agentSeed.department().getDisplayName())
                .departmentEntity(department)
                .provider("local")
                .isActive(true)
                .build();

        userRepository.save(agent);
        log.info("Agent user created: {}", agentSeed.email());
    }

    private record AgentSeed(String email, String fullName, UserDepartment department) {
    }
    
    private void seedRegularUsers() {
        for (SeedUser seedUser : REGULAR_USERS) {
            String email = seedUser.email();

            if (userRepository.findByEmail(email).isPresent()) {
                log.info("Regular user already exists — skipping seed for {}", email);
                continue;
            }

            User user = User.builder()
                    .email(email)
                    .fullName(seedUser.fullName())
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .role(Role.USER)
                    .provider("local")
                    .isActive(true)
                    .build();

            userRepository.save(user);
            log.info("Regular user created: {}", email);
        }
    }

    private record SeedUser(String fullName, String email) {}
}
