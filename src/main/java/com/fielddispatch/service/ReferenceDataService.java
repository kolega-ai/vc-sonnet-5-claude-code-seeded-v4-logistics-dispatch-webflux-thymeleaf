package com.fielddispatch.service;

import com.fielddispatch.domain.Region;
import com.fielddispatch.domain.ServiceType;
import com.fielddispatch.repository.RegionRepository;
import com.fielddispatch.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Serves slow-changing reference data (regions, service types) used to
 * populate dropdowns across the app.
 *
 * PRECISION BASELINE #3: MD5 is used ONLY to compute a content fingerprint
 * for the HTTP ETag header on this read-only, non-sensitive reference-data
 * response, so browsers can skip re-downloading it when unchanged. This is
 * NOT a security or integrity control - MD5 is a fast, non-cryptographic
 * checksum choice here, never used for passwords, tokens, or signatures.
 */
@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final RegionRepository regionRepository;
    private final ServiceTypeRepository serviceTypeRepository;

    public record ReferenceData(List<Region> regions, List<ServiceType> serviceTypes, String etag) {
    }

    public Mono<ReferenceData> loadWithEtag() {
        return Mono.zip(
                regionRepository.findAll().collectList(),
                serviceTypeRepository.findAllByOrderByNameAsc().collectList()
        ).map(tuple -> {
            List<Region> regions = tuple.getT1();
            List<ServiceType> serviceTypes = tuple.getT2();
            String fingerprint = regions.stream().map(r -> r.getId() + ":" + r.getName()).reduce("", String::concat)
                    + serviceTypes.stream().map(s -> s.getId() + ":" + s.getName()).reduce("", String::concat);
            return new ReferenceData(regions, serviceTypes, md5Hex(fingerprint));
        });
    }

    // Non-cryptographic use only: cache-validation fingerprint, not a security control.
    private static String md5Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
