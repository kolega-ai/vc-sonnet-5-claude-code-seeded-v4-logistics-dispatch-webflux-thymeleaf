package com.fielddispatch.repository;

import com.fielddispatch.domain.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationRepository extends R2dbcRepository<Notification, Long> {
    Flux<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    Mono<Long> countByUserIdAndIsReadFalse(Long userId);
}
