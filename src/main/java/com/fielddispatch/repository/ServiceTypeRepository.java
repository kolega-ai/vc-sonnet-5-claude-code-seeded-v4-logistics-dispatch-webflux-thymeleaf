package com.fielddispatch.repository;

import com.fielddispatch.domain.ServiceType;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface ServiceTypeRepository extends R2dbcRepository<ServiceType, Long> {
    Flux<ServiceType> findAllByOrderByNameAsc();
}
