package com.fielddispatch.service;

import com.fielddispatch.domain.AuditLog;
import com.fielddispatch.repository.AuditLogRepository;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * Writes append-only audit events. No method here (or anywhere in the
 * codebase) issues an UPDATE or DELETE against audit_log.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public Mono<AuditLog> record(AppUserPrincipal actor, String action, String entityType, String entityId,
                                  String beforeValue, String afterValue, String authorizationBasis) {
        AuditLog log = AuditLog.builder()
                .actorId(actor != null ? actor.getUserId() : null)
                .actorEmail(actor != null ? actor.getEmail() : "system")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .authorizationBasis(authorizationBasis)
                .createdAt(OffsetDateTime.now())
                .build();
        return auditLogRepository.save(log);
    }

    /** One event per request naming the resource class and count returned - never per-record. */
    public Mono<AuditLog> recordRead(AppUserPrincipal actor, String entityType, int count, String authorizationBasis) {
        return record(actor, "READ_LIST", entityType, null, null, "count=" + count, authorizationBasis);
    }
}
