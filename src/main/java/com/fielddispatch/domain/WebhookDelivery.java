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
@Table("webhook_deliveries")
public class WebhookDelivery {
    @Id
    private Long id;
    private Long webhookId;
    private String eventType;
    private String payload;
    private Integer responseStatus;
    private boolean success;
    private OffsetDateTime attemptedAt;
}
