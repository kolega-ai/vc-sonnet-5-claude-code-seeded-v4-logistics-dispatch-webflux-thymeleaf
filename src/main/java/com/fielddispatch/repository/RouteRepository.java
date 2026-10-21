package com.fielddispatch.repository;

import com.fielddispatch.domain.Route;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface RouteRepository extends R2dbcRepository<Route, Long> {
    Flux<Route> findByWorkerId(Long workerId);
    Mono<Route> findByWorkerIdAndRouteDate(Long workerId, LocalDate routeDate);
    Flux<Route> findByRegionId(Long regionId);
}
