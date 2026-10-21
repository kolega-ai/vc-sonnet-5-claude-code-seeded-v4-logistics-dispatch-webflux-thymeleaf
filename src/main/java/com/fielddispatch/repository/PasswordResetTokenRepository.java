package com.fielddispatch.repository;

import com.fielddispatch.domain.PasswordResetToken;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface PasswordResetTokenRepository extends R2dbcRepository<PasswordResetToken, Long> {
    Mono<PasswordResetToken> findByToken(String token);
}
