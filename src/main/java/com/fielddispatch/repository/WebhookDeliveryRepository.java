package com.fielddispatch.repository;

import com.fielddispatch.domain.WebhookDelivery;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface WebhookDeliveryRepository extends R2dbcRepository<WebhookDelivery, Long> {
    Flux<WebhookDelivery> findByWebhookIdOrderByAttemptedAtDesc(Long webhookId);
    Flux<WebhookDelivery> findAllByOrderByAttemptedAtDesc();
}
