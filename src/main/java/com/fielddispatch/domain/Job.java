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
@Table("jobs")
public class Job {
    @Id
    private Long id;
    private Long customerId;
    private Long locationId;
    private Long serviceTypeId;
    private Long regionId;
    private Long assignedWorkerId;
    private String status;
    private String priority;
    private OffsetDateTime scheduledStart;
    private OffsetDateTime scheduledEnd;
    private String description;
    private int rescheduleCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
