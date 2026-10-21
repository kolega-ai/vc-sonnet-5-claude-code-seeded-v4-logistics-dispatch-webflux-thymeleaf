package com.fielddispatch.repository;

import com.fielddispatch.domain.Webhook;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface WebhookRepository extends R2dbcRepository<Webhook, Long> {
    Flux<Webhook> findAllByOrderByCreatedAtDesc();
}
