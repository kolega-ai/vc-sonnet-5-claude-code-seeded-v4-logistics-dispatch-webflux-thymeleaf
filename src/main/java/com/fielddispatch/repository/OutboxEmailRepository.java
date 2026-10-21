package com.fielddispatch.repository;

import com.fielddispatch.domain.OutboxEmail;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface OutboxEmailRepository extends R2dbcRepository<OutboxEmail, Long> {
    Flux<OutboxEmail> findAllByOrderBySentAtDesc();
}
