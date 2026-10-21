package com.fielddispatch.service;

import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Onboards haulage partners from the operations console. The carrier desk
 * sends over an onboarding sheet whose first two columns are the carrier code
 * and the shared gateway access key; the key is presented to the carrier
 * gateway so it knows which account the handshake belongs to.
 */
@Service
@RequiredArgsConstructor
public class CarrierIntegrationService {

    private final AuditService auditService;
    private final WebClient gatewayClient = WebClient.builder().build();

    public Mono<String> onboardCarrier(AppUserPrincipal actor, FilePart sheet) {
        return DataBufferUtils.join(sheet.content()).flatMap(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            String[] fields = new String(bytes, StandardCharsets.UTF_8).trim().split("[,;\\s]+");
            String carrier = fields.length > 0 ? fields[0] : "unnamed";
            String accessKey = fields.length > 1 ? fields[1] : "";
            return gatewayClient.post()
                    .uri("http://carrier-gateway.internal/v1/handshake")
                    .header("X-Api-Key", accessKey)
                    .bodyValue("{\"carrier\":\"" + carrier + "\"}")
                    .retrieve()
                    .bodyToMono(String.class)
                    .then(auditService.record(actor, "CARRIER_ONBOARD", "CARRIER", carrier,
                            null, "handshake-with-gateway", "dispatcher-partner-onboarding"))
                    .thenReturn(carrier);
        });
    }
}
