package com.bidly.authservice.initializer;

import com.bidly.authservice.entity.Role;
import com.bidly.authservice.entity.User;
import com.bidly.authservice.enums.Rolename;
import com.bidly.authservice.repository.RoleRepository;
import com.bidly.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting database initialization...");

        Role adminRole = roleRepository.findByName(Rolename.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Rolename.ADMIN).build()));

        Role userRole = roleRepository.findByName(Rolename.USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Rolename.USER).build()));

        if (!userRepository.existsByUsername("admin1")) {
            User admin1 = User.builder()
                    .firstName("Andrei")
                    .lastName("Admin")
                    .username("admin1")
                    .email("admin1@bidly.com")
                    .password(passwordEncoder.encode("adminpass123"))
                    .phoneNumber("0711111111")
                    .enabled(true)
                    .kycApproved(true)
                    .rolename(Set.of(adminRole))
                    .build();
            userRepository.save(admin1);
            log.info("Default Admin 1 created.");
        }

        if (!userRepository.existsByUsername("admin2")) {
            User admin2 = User.builder()
                    .firstName("Maria")
                    .lastName("Boss")
                    .username("admin2")
                    .email("admin2@bidly.com")
                    .password(passwordEncoder.encode("adminpass456"))
                    .phoneNumber("0722222222")
                    .enabled(true)
                    .kycApproved(true)
                    .rolename(Set.of(adminRole))
                    .build();
            userRepository.save(admin2);
            log.info("Default Admin 2 created.");
        }

        // USER
        if (!userRepository.existsByUsername("user1")) {
            User user1 = User.builder()
                    .firstName("Ionut")
                    .lastName("Popescu")
                    .username("user1")
                    .email("user1@gmail.com")
                    .password(passwordEncoder.encode("userpass123"))
                    .phoneNumber("0733333333")
                    .enabled(true)
                    .kycApproved(false)
                    .rolename(Set.of(userRole))
                    .build();
            userRepository.save(user1);
            log.info("Default User 1 created.");
        }

        if (!userRepository.existsByUsername("user2")) {
            User user2 = User.builder()
                    .firstName("Elena")
                    .lastName("Ionescu")
                    .username("user2")
                    .email("user2@gmail.com")
                    .password(passwordEncoder.encode("userpass123"))
                    .phoneNumber("0744444444")
                    .enabled(true)
                    .kycApproved(true)
                    .rolename(Set.of(userRole))
                    .build();
            userRepository.save(user2);
            log.info("Default User 2 created.");
        }

        if (!userRepository.existsByUsername("user3")) {
            User user3 = User.builder()
                    .firstName("Vlad")
                    .lastName("Nistor")
                    .username("user3")
                    .email("user3@gmail.com")
                    .password(passwordEncoder.encode("userpass123"))
                    .phoneNumber("0755555555")
                    .enabled(true)
                    .kycApproved(false)
                    .rolename(Set.of(userRole))
                    .build();
            userRepository.save(user3);
            log.info("Default User 3 created.");
        }

        log.info("Database initialization completed successfully.");
    }
}
