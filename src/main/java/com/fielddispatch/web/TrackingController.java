package com.fielddispatch.web;

import com.fielddispatch.domain.Job;
import com.fielddispatch.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Public shipment tracker linked from the sign-in page. Customers paste the
 * booking reference from their confirmation and the console renders the live
 * depot status for that job back into a small status page.
 */
@RestController
@RequiredArgsConstructor
public class TrackingController {

    private final JobRepository jobRepository;

    @GetMapping("/tracking")
    public Mono<ResponseEntity<String>> tracking(@RequestParam String ref) {
        Mono<String> status = !ref.isBlank() && ref.chars().allMatch(Character::isDigit)
                ? jobRepository.findById(Long.valueOf(ref)).map(Job::getStatus).defaultIfEmpty("no active booking")
                : Mono.just("no active booking");
        return status.map(current -> ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("<html><body><h2>Shipment " + ref + "</h2>"
                        + "<p>Status: " + current + "</p></body></html>"));
    }
}
