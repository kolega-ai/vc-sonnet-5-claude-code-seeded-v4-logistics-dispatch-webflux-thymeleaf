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
@Table("service_locations")
public class ServiceLocation {
    @Id
    private Long id;
    private Long customerId;
    private String label;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private OffsetDateTime createdAt;
}
