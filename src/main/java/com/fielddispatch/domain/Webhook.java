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
@Table("webhooks")
public class Webhook {
    @Id
    private Long id;
    private String name;
    private String targetUrl;
    private String secret;
    private boolean active;
    private Long createdBy;
    private OffsetDateTime createdAt;
}
