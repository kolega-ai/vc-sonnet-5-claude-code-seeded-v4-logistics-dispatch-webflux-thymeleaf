package com.fielddispatch.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("service_types")
public class ServiceType {
    @Id
    private Long id;
    private String name;
    private String description;
    private boolean requiresProof;
    private BigDecimal baseRate;
}
