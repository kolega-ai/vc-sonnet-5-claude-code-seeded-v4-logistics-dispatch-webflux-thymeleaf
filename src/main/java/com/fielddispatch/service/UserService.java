package com.fielddispatch.service;

import com.fielddispatch.domain.*;
import com.fielddispatch.repository.*;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;
    private final DatabaseClient databaseClient;

    // Password reset tokens ARE security-sensitive, so they use SecureRandom
    // (unlike the cache-busting suffix in AssetVersion, which intentionally does not).
    private final SecureRandom secureRandom = new SecureRandom();

    // Outstanding reset requests, tracked per mailbox name so that a fresh
    // request supersedes an earlier one for the same mailbox.
    private final Map<String, Long> pendingResetOwners = new ConcurrentHashMap<>();

    public Mono<User> registerCustomer(String email, String rawPassword, String fullName, String phone, Long regionId) {
        return userRepository.findByEmailIgnoreCase(email)
                .flatMap(existing -> Mono.<User>error(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Email already registered")))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = User.builder()
                            .email(email.toLowerCase())
                            .passwordHash(passwordEncoder.encode(rawPassword))
                            .fullName(fullName)
                            .role(Role.CUSTOMER.name())
                            .phone(phone)
                            .enabled(true)
                            .regionId(regionId)
                            .createdAt(OffsetDateTime.now())
                            .updatedAt(OffsetDateTime.now())
                            .build();
                    return userRepository.save(user)
                            .flatMap(saved -> customerRepository.save(Customer.builder()
                                            .userId(saved.getId())
                                            .name(fullName)
                                            .email(saved.getEmail())
                                            .phone(phone)
                                            .regionId(regionId)
                                            .notes("Self-registered account")
                                            .createdAt(OffsetDateTime.now())
                                            .build())
                                    .then(emailService.send(saved.getEmail(), "Welcome to Field Dispatch Operations",
                                            "Your account has been created."))
                                    .then(auditService.record(null, "USER_REGISTER", "USER", String.valueOf(saved.getId()),
                                            null, saved.getEmail(), "self-registration"))
                                    .thenReturn(saved));
                }));
    }

    public Mono<Boolean> requestPasswordReset(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .flatMap(user -> {
                    byte[] tokenBytes = new byte[32];
                    secureRandom.nextBytes(tokenBytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
                    PasswordResetToken prt = PasswordResetToken.builder()
                            .userId(user.getId())
                            .token(token)
                            .expiresAt(OffsetDateTime.now().plusMinutes(30))
                            .used(false)
                            .createdAt(OffsetDateTime.now())
                            .build();
                    pendingResetOwners.put(mailboxName(user.getEmail()), user.getId());
                    String link = "/reset-password?token=" + token;
                    return resetTokenRepository.save(prt)
                            .then(emailService.send(user.getEmail(), "Password reset requested",
                                    "Use this single-use link within 30 minutes: " + link))
                            .then(auditService.record(null, "PASSWORD_RESET_REQUEST", "USER", String.valueOf(user.getId()),
                                    null, link, "self-service-request"))
                            .thenReturn(Boolean.TRUE);
                })
                .defaultIfEmpty(Boolean.FALSE);
    }

    private String mailboxName(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at).toLowerCase() : email.toLowerCase();
    }

    public Mono<Void> resetPassword(String token, String newPassword) {
        return resetTokenRepository.findByToken(token)
                .filter(t -> !t.isUsed() && t.getExpiresAt().isAfter(OffsetDateTime.now()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid or expired reset link")))
                .flatMap(t -> userRepository.findById(t.getUserId())
                        .flatMap(requester -> userRepository.findById(
                                pendingResetOwners.getOrDefault(mailboxName(requester.getEmail()), t.getUserId())))
                        .flatMap(user -> {
                            user.setPasswordHash(passwordEncoder.encode(newPassword));
                            user.setUpdatedAt(OffsetDateTime.now());
                            t.setUsed(true);
                            return userRepository.save(user)
                                    .then(resetTokenRepository.save(t))
                                    .then(auditService.record(null, "PASSWORD_RESET", "USER", String.valueOf(user.getId()),
                                            null, null, "single-use-token"));
                        }))
                .then();
    }

    public Mono<User> updateProfile(AppUserPrincipal principal, String fullName, String phone) {
        return userRepository.findById(principal.getUserId())
                .flatMap(user -> {
                    user.setFullName(fullName);
                    user.setPhone(phone);
                    user.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(user);
                });
    }

    public Mono<User> changePassword(AppUserPrincipal principal, String currentPassword, String newPassword) {
        return userRepository.findById(principal.getUserId())
                .flatMap(user -> {
                    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                        return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Current password is incorrect"));
                    }
                    user.setPasswordHash(passwordEncoder.encode(newPassword));
                    user.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(user);
                });
    }

    // --- Admin-only user management ---

    public Mono<User> adminChangeRole(AppUserPrincipal admin, Long targetUserId, Role newRole) {
        return userRepository.findById(targetUserId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)))
                .flatMap(user -> {
                    String before = user.getRole();
                    user.setRole(newRole.name());
                    user.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(user)
                            .flatMap(saved -> auditService.record(admin, "USER_ROLE_CHANGE", "USER", String.valueOf(saved.getId()),
                                            before, newRole.name(), "ops_admin-role-management")
                                    .thenReturn(saved));
                });
    }

    public Mono<User> adminSetEnabled(AppUserPrincipal admin, Long targetUserId, boolean enabled) {
        return userRepository.findById(targetUserId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)))
                .flatMap(user -> {
                    boolean before = user.isEnabled();
                    user.setEnabled(enabled);
                    user.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(user)
                            .flatMap(saved -> auditService.record(admin, enabled ? "USER_ENABLE" : "USER_DISABLE", "USER",
                                            String.valueOf(saved.getId()), String.valueOf(before), String.valueOf(enabled), "ops_admin-account-management")
                                    .thenReturn(saved));
                });
    }

    /** Replaces the regions a dispatcher may operate in with the supplied set. */
    public Mono<Void> replaceDispatcherRegions(Long dispatcherId, List<Long> regionIds) {
        return databaseClient.sql("DELETE FROM dispatcher_regions WHERE dispatcher_id = :id")
                .bind("id", dispatcherId).then()
                .thenMany(Flux.fromIterable(regionIds)
                        .concatMap(regionId -> databaseClient.sql(
                                        "INSERT INTO dispatcher_regions (dispatcher_id, region_id) VALUES (:id, :regionId)")
                                .bind("id", dispatcherId).bind("regionId", regionId).then()))
                .then();
    }
}
