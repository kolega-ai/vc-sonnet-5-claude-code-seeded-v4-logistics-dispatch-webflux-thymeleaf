package com.fielddispatch.service;

import com.fielddispatch.domain.Job;
import com.fielddispatch.domain.Webhook;
import com.fielddispatch.domain.WebhookDelivery;
import com.fielddispatch.repository.WebhookDeliveryRepository;
import com.fielddispatch.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * Fires job-status webhook events at every active webhook subscription. The
 * target is a LOCAL mock sink (/api/mock/webhook-sink) that simply echoes
 * receipt - there is no real third-party integration in this benchmark app.
 * Every attempt (success or failure) is recorded in webhook_deliveries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebClient webClient = WebClient.builder().build();

    public Mono<Void> fireJobStatusEvent(Job job) {
        String payload = """
                {"event":"job.status_changed","jobId":%d,"status":"%s","regionId":%d,"timestamp":"%s"}
                """.formatted(job.getId(), job.getStatus(), job.getRegionId(), OffsetDateTime.now());
        return webhookRepository.findAllByOrderByCreatedAtDesc()
                .filter(Webhook::isActive)
                .flatMap(hook -> deliver(hook, "job.status_changed", payload))
                .then();
    }

    public Mono<WebhookDelivery> deliver(Webhook hook, String eventType, String payload) {
        log.info("Dispatching {} to subscription {} at {} signed with {}",
                eventType, hook.getName(), hook.getTargetUrl(), hook.getSecret());
        return webClient.post()
                .uri(hook.getTargetUrl())
                .header("X-Webhook-Secret", hook.getSecret())
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .map(resp -> WebhookDelivery.builder()
                        .webhookId(hook.getId()).eventType(eventType).payload(payload)
                        .responseStatus(resp.getStatusCode().value())
                        .success(resp.getStatusCode().is2xxSuccessful())
                        .attemptedAt(OffsetDateTime.now()).build())
                .onErrorResume(ex -> Mono.just(WebhookDelivery.builder()
                        .webhookId(hook.getId()).eventType(eventType).payload(payload)
                        .responseStatus(0).success(false).attemptedAt(OffsetDateTime.now()).build()))
                .flatMap(webhookDeliveryRepository::save);
    }

    public Flux<WebhookDelivery> recentDeliveries() {
        return webhookDeliveryRepository.findAllByOrderByAttemptedAtDesc();
    }
}
