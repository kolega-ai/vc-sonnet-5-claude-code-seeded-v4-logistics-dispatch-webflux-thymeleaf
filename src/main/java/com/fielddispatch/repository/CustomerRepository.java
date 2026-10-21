package com.fielddispatch.repository;

import com.fielddispatch.domain.Customer;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerRepository extends R2dbcRepository<Customer, Long> {
    Mono<Customer> findByUserId(Long userId);
    Flux<Customer> findAllByOrderByNameAsc();
}
