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
@Table("outbox_emails")
public class OutboxEmail {
    @Id
    private Long id;
    private String toEmail;
    private String subject;
    private String body;
    private OffsetDateTime sentAt;
}
