package com.fielddispatch.repository;

import com.fielddispatch.domain.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface AuditLogRepository extends R2dbcRepository<AuditLog, Long> {
    Flux<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Flux<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);
}
