package com.fielddispatch.service;

import com.fielddispatch.domain.ServiceType;
import com.fielddispatch.repository.ServiceTypeRepository;
import com.fielddispatch.security.AppUserPrincipal;
import com.fielddispatch.web.dto.RateCardOverride;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;

/**
 * Applies the carrier-published rate card overrides to the service-type
 * catalogue. The partner desk drops a YAML document listing each service type
 * and its negotiated rate; the console applies the matching rows.
 */
@Service
@RequiredArgsConstructor
public class RateCardService {

    private final ServiceTypeRepository serviceTypeRepository;
    private final AuditService auditService;

    public Mono<Integer> applyRateCard(AppUserPrincipal actor, FilePart card) {
        return DataBufferUtils.join(card.content()).flatMap(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            Yaml yaml = new Yaml(new Constructor(RateCardOverride.class, new LoaderOptions()));
            RateCardOverride override = yaml.load(new ByteArrayInputStream(bytes));
            if (override == null || override.getName() == null) {
                return Mono.just(0);
            }
            return serviceTypeRepository.findAllByOrderByNameAsc()
                    .filter(type -> type.getName().equalsIgnoreCase(override.getName()))
                    .flatMap(type -> apply(type, override))
                    .count()
                    .defaultIfEmpty(0L)
                    .map(Long::intValue)
                    .flatMap(applied -> auditService.record(actor, "RATE_CARD_APPLY", "SERVICE_TYPE",
                                    override.getName(), null, String.valueOf(override.getBaseRate()),
                                    "dispatcher-rate-card")
                            .thenReturn(applied));
        });
    }

    private Mono<ServiceType> apply(ServiceType type, RateCardOverride override) {
        type.setBaseRate(override.getBaseRate());
        return serviceTypeRepository.save(type);
    }
}
