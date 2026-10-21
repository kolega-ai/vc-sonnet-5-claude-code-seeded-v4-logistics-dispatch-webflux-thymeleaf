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
@Table("password_reset_tokens")
public class PasswordResetToken {
    @Id
    private Long id;
    private Long userId;
    private String token;
    private OffsetDateTime expiresAt;
    private boolean used;
    private OffsetDateTime createdAt;
}
