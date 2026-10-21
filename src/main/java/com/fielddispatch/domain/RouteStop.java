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
@Table("route_stops")
public class RouteStop {
    @Id
    private Long id;
    private Long routeId;
    private Long jobId;
    private int sequence;
    private OffsetDateTime createdAt;
}
