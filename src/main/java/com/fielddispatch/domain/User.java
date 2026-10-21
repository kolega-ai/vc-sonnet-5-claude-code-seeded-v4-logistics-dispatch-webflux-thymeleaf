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
@Table("users")
public class User {
    @Id
    private Long id;
    private String email;
    private String passwordHash;
    private String fullName;
    /** Stored as plain text (matches DB CHECK constraint); use {@link #roleEnum()} for typed access. */
    private String role;
    private String phone;
    private boolean enabled;
    private Long regionId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Role roleEnum() {
        return Role.valueOf(role);
    }
}
