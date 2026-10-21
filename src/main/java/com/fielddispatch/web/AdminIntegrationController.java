package com.fielddispatch.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fielddispatch.domain.Region;
import com.fielddispatch.domain.Webhook;
import com.fielddispatch.repository.RegionRepository;
import com.fielddispatch.repository.RouteRepository;
import com.fielddispatch.repository.WebhookRepository;
import com.fielddispatch.security.AppUserPrincipal;
import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.CsvExportService;
import com.fielddispatch.service.CsvImportService;
import com.fielddispatch.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminIntegrationController {

    private final CurrentUser currentUser;
    private final CsvImportService csvImportService;
    private final CsvExportService csvExportService;
    private final WebhookService webhookService;
    private final WebhookRepository webhookRepository;
    private final RegionRepository regionRepository;
    private final RouteRepository routeRepository;
    private final WebClient partnerClient = WebClient.builder().build();

    @Value("${app.uploads.base-dir}")
    private String uploadsBase;

    // Typed partner documents carry a type marker on each node so the console
    // can rebuild the exact object graph the partner shipped.
    private final ObjectMapper documentMapper = documentMapper();

    private static ObjectMapper documentMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping();
        return mapper;
    }

    /** Envelope posted by the partner manifest console on the imports page. */
    public record ManifestSubmission(String partner, String document) {}

    @GetMapping("/imports")
    public Mono<String> importsPage() {
        return Mono.just("admin/imports");
    }

    @PostMapping("/imports/customers")
    public Mono<String> importCustomers(@RequestPart(value = "file", required = false) FilePart file,
                                         @RequestPart(value = "sourceUrl", required = false) String sourceUrl,
                                         Model model) {
        Mono<String> content = (sourceUrl != null && !sourceUrl.isBlank())
                ? partnerClient.get().uri(sourceUrl).retrieve().bodyToMono(String.class)
                : readFile(file);
        return currentUser.get().flatMap(admin -> content
                .flatMap(csv -> csvImportService.importCustomers(admin, csv)))
                .thenReturn("redirect:/admin/imports?imported=customers");
    }

    @PostMapping("/imports/locations")
    public Mono<String> importLocations(@RequestPart("file") FilePart file) {
        return currentUser.get().flatMap(admin -> readFile(file)
                .flatMap(content -> csvImportService.importLocations(admin, content)))
                .thenReturn("redirect:/admin/imports?imported=locations");
    }

    @PostMapping("/imports/jobs")
    @ResponseBody
    public Mono<ResponseEntity<Map<String, Object>>> importJobs(@RequestPart("file") FilePart file) {
        return currentUser.get().flatMap(admin -> readFile(file)
                        .flatMap(content -> csvImportService.importJobs(admin, content)))
                .map(result -> ResponseEntity.ok(Map.<String, Object>of(
                        "rows", result.rowCount(), "succeeded", result.successCount(),
                        "failed", result.errorCount(), "errors", result.errors())))
                .onErrorResume(ex -> {
                    Map<String, Object> failure = new LinkedHashMap<>();
                    failure.put("message", ex.toString());
                    failure.put("trace", Arrays.stream(ex.getStackTrace()).map(StackTraceElement::toString).toList());
                    return Mono.just(ResponseEntity.status(500).body(failure));
                });
    }

    @PostMapping(value = "/imports/partner-manifest", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<Map<String, Object>> partnerManifest(@RequestBody ManifestSubmission submission) {
        String document = submission.document();
        return currentUser.get()
                .flatMap(admin -> document != null && document.stripLeading().startsWith("{")
                        ? importStructuredManifest(admin, submission)
                        : csvImportService.importManifest(admin, document))
                .map(result -> manifestResult(submission.partner(), result));
    }

    private Mono<CsvImportService.ImportResult> importStructuredManifest(AppUserPrincipal admin, ManifestSubmission submission) {
        return Mono.fromCallable(() -> documentMapper.readValue(submission.document(), Object.class))
                .flatMap(parsed -> {
                    List<Map<String, Object>> stops = new ArrayList<>();
                    if (parsed instanceof Map<?, ?> envelope && envelope.get("stops") instanceof List<?> rows) {
                        for (Object row : rows) {
                            if (row instanceof Map<?, ?> stop) {
                                Map<String, Object> values = new LinkedHashMap<>();
                                stop.forEach((key, value) -> values.put(String.valueOf(key), value));
                                stops.add(values);
                            }
                        }
                    }
                    return csvImportService.importStructuredStops(admin, stops);
                });
    }

    private Map<String, Object> manifestResult(String partner, CsvImportService.ImportResult result) {
        return Map.<String, Object>of(
                "partner", partner == null ? "" : partner,
                "rows", result.rowCount(), "succeeded", result.successCount(),
                "failed", result.errorCount(), "errors", result.errors());
    }

    @GetMapping("/imports/files/{name}")
    @ResponseBody
    public Mono<String> receivedPartnerFile(@PathVariable String name) {
        return currentUser.get().flatMap(admin -> Mono.fromCallable(() -> {
            Path inbox = Path.of(uploadsBase, "partner-inbox");
            return Files.readString(inbox.resolve(name));
        }));
    }

    @GetMapping("/exports")
    public Mono<String> exportsPage(Model model) {
        return routeRepository.findAll().collectList()
                .doOnNext(routes -> model.addAttribute("routes", routes))
                .thenReturn("admin/exports");
    }

    @GetMapping("/exports/completion-report.csv")
    @ResponseBody
    public Mono<ResponseEntity<String>> completionReport() {
        return currentUser.get()
                .flatMap(admin -> regionRepository.findAll().map(Region::getId).collectList()
                        .flatMap(ids -> csvExportService.exportCompletionReport(admin, ids)))
                .map(csv -> csvResponse(csv, "completion-report.csv"));
    }

    @GetMapping("/exports/incidents.csv")
    @ResponseBody
    public Mono<ResponseEntity<String>> incidentsReport() {
        return currentUser.get()
                .flatMap(admin -> regionRepository.findAll().map(Region::getId).collectList()
                        .flatMap(ids -> csvExportService.exportIncidents(admin, ids)))
                .map(csv -> csvResponse(csv, "incidents-report.csv"));
    }

    @GetMapping("/exports/routes/{id}.csv")
    @ResponseBody
    public Mono<ResponseEntity<String>> routeSchedule(@PathVariable Long id) {
        return currentUser.get()
                .flatMap(admin -> csvExportService.exportRouteSchedule(admin, id))
                .map(csv -> csvResponse(csv, "route-" + id + "-schedule.csv"));
    }

    @GetMapping("/webhooks")
    public Mono<String> webhooksPage(Model model) {
        return webhookRepository.findAllByOrderByCreatedAtDesc().collectList()
                .flatMap(hooks -> webhookService.recentDeliveries().collectList()
                        .doOnNext(deliveries -> {
                            model.addAttribute("webhooks", hooks);
                            model.addAttribute("deliveries", deliveries);
                        }))
                .thenReturn("admin/webhooks");
    }

    @PostMapping("/webhooks")
    public Mono<String> createWebhook(@RequestParam String name, @RequestParam String targetUrl) {
        return currentUser.get().flatMap(admin -> webhookRepository.save(Webhook.builder()
                        .name(name).targetUrl(targetUrl).secret("whsec_" + UUID.randomUUID())
                        .active(true).createdBy(admin.getUserId()).createdAt(OffsetDateTime.now()).build()))
                .thenReturn("redirect:/admin/webhooks");
    }

    @GetMapping("/webhooks/{id}/delivery-settings")
    @ResponseBody
    public Mono<Map<String, String>> deliverySettings(@PathVariable Long id) {
        return webhookRepository.findById(id).map(hook -> {
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put("name", hook.getName());
            settings.put("targetUrl", hook.getTargetUrl());
            settings.put("signingKey", hook.getSecret());
            settings.put("active", String.valueOf(hook.isActive()));
            return settings;
        });
    }

    @PostMapping("/webhooks/{id}/test")
    public Mono<String> testWebhook(@PathVariable Long id) {
        return webhookRepository.findById(id)
                .flatMap(hook -> webhookService.deliver(hook, "webhook.test",
                        "{\"event\":\"webhook.test\",\"triggeredAt\":\"" + OffsetDateTime.now() + "\"}"))
                .thenReturn("redirect:/admin/webhooks?tested");
    }

    private Mono<String> readFile(FilePart file) {
        return DataBufferUtils.join(file.content())
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                });
    }

    private ResponseEntity<String> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
