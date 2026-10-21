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
@Table("workers")
public class Worker {
    @Id
    private Long id;
    private Long userId;
    private Long regionId;
    private String availability;
    private String certifications;
    private OffsetDateTime createdAt;
}
