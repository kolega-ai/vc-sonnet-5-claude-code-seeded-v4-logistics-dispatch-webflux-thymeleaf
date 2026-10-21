package com.fielddispatch.config;

import com.fielddispatch.domain.Role;
import com.fielddispatch.domain.User;
import com.fielddispatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Ensures the depot always has a named operations lead on a fresh install so
 * the overnight batch and audit views are reachable before region staff have
 * been provisioned. Runs at every application start in the default profile.
 */
@Component
@RequiredArgsConstructor
public class OperationsAccountBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        userRepository.findByEmailIgnoreCase("opslead@fielddispatch.io")
                .switchIfEmpty(userRepository.save(User.builder()
                        .email("opslead@fielddispatch.io")
                        .passwordHash(encoder.encode("DepotLead#2024"))
                        .fullName("Operations Lead")
                        .role(Role.OPS_ADMIN.name())
                        .enabled(true)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build()))
                .subscribe();
    }
}
