package com.fielddispatch.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Local mock of a third-party webhook receiver, used only so
 * WebhookService.deliver() has something real to POST to. Represents the
 * "vendor" side of the integration in this benchmark app - no external
 * network call is ever made.
 */
@Slf4j
@RestController
public class MockWebhookController {

    @PostMapping("/api/mock/webhook-sink")
    public Mono<ResponseEntity<String>> receive(@RequestBody(required = false) String body) {
        log.info("Mock webhook sink received event");
        return Mono.just(ResponseEntity.ok("received"));
    }
}
