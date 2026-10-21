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
@Table("customers")
public class Customer {
    @Id
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private Long regionId;
    private String notes;
    private OffsetDateTime createdAt;
}
