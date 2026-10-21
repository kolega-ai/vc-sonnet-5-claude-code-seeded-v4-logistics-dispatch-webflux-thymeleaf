package com.fielddispatch.service;

import com.fielddispatch.domain.OutboxEmail;
import com.fielddispatch.repository.OutboxEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * Local mock mail sender. Real SMTP delivery is intentionally not wired up;
 * every outbound email is durably recorded in the outbox table so reviewers
 * can inspect password-reset links and notifications without an external
 * mail provider.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final OutboxEmailRepository outboxEmailRepository;

    public Mono<OutboxEmail> send(String toEmail, String subject, String body) {
        return outboxEmailRepository.save(OutboxEmail.builder()
                .toEmail(toEmail).subject(subject).body(body).sentAt(OffsetDateTime.now()).build());
    }
}
