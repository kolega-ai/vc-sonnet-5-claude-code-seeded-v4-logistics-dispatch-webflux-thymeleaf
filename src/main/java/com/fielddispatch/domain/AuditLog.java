package com.fielddispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

/**
 * Append-only audit record. No service or repository method updates or deletes
 * rows in this table; only inserts are ever issued (see AuditService).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_log")
public class AuditLog {
    @Id
    private Long id;
    private Long actorId;
    private String actorEmail;
    private String action;
    private String entityType;
    private String entityId;
    private String beforeValue;
    private String afterValue;
    private String authorizationBasis;
    private OffsetDateTime createdAt;
}
