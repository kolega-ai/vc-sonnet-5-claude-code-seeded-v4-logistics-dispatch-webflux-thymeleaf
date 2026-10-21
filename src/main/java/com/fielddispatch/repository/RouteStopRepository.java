package com.fielddispatch.repository;

import com.fielddispatch.domain.RouteStop;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface RouteStopRepository extends R2dbcRepository<RouteStop, Long> {
    Flux<RouteStop> findByRouteIdOrderBySequenceAsc(Long routeId);
    Flux<RouteStop> findByJobId(Long jobId);
}
