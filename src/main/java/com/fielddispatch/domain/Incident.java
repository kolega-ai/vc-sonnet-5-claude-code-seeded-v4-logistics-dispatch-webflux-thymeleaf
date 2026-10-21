package com.fielddispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("incidents")
public class Incident {
    @Id
    private Long id;
    private Long jobId;
    private Long reportedBy;
    private Long assignedTo;
    private String severity;
    private String status;
    private String internalNotes;
    private String customerSummary;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
}
