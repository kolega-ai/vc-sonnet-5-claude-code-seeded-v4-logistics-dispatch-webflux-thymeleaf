package com.fielddispatch.repository;

import com.fielddispatch.domain.ServiceLocation;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface ServiceLocationRepository extends R2dbcRepository<ServiceLocation, Long> {
    Flux<ServiceLocation> findByCustomerId(Long customerId);
}
