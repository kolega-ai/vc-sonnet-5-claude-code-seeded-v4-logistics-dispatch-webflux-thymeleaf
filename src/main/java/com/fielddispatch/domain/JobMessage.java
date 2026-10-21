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
@Table("job_messages")
public class JobMessage {
    @Id
    private Long id;
    private Long jobId;
    private Long senderId;
    private String senderRole;
    private String body;
    private OffsetDateTime createdAt;
}
