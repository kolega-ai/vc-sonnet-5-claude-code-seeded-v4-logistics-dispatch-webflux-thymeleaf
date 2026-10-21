package com.fielddispatch.repository;

import com.fielddispatch.domain.Region;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface RegionRepository extends R2dbcRepository<Region, Long> {
}
