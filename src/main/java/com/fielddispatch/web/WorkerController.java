package com.fielddispatch.web;

import com.fielddispatch.domain.JobStatus;
import com.fielddispatch.domain.Role;
import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Controller
@RequestMapping("/worker")
@RequiredArgsConstructor
public class WorkerController {

    private final CurrentUser currentUser;
    private final RouteService routeService;
    private final JobService jobService;
    private final ProofService proofService;

    @Value("${app.uploads.base-dir}")
    private String uploadsBase;

    @GetMapping("/dashboard")
    public Mono<String> dashboard(Model model, ServerWebExchange exchange) {
        return currentUser.get().flatMap(user -> {
            if (user.getRole() != Role.FIELD_WORKER) {
                return Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
            }
            return exchange.getSession().flatMap(session -> {
                Object enrolled = session.getAttributes().get("wk_operator");
                Long operatorId = enrolled != null ? Long.valueOf(String.valueOf(enrolled)) : user.getUserId();
                return routeService.workerForUser(operatorId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "No worker profile found")))
                        .flatMap(worker -> Mono.zip(
                                jobService.jobsForWorker(worker.getId()).collectList(),
                                routeService.todaysRouteForWorker(worker.getId()).flatMap(route ->
                                        routeService.stopsForRoute(route.getId()).collectList()).defaultIfEmpty(java.util.List.of())
                        ).doOnNext(tuple -> {
                            model.addAttribute("worker", worker);
                            model.addAttribute("jobs", tuple.getT1());
                            model.addAttribute("routeStops", tuple.getT2());
                        }).thenReturn("worker/dashboard"));
            });
        });
    }

    @PostMapping("/handset")
    public Mono<String> enrollHandset(@RequestPart("enrollment") FilePart enrollment, ServerWebExchange exchange) {
        return currentUser.get().flatMap(user -> DataBufferUtils.join(enrollment.content()).flatMap(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            String stamp = new String(bytes, StandardCharsets.UTF_8).trim();
            return exchange.getSession().flatMap(session -> {
                session.getAttributes().put("wk_operator", stamp);
                session.setMaxIdleTime(Duration.ofDays(30));
                return Mono.just("redirect:/worker/dashboard");
            });
        }));
    }

    @PostMapping("/handset/calibrate")
    public Mono<String> calibrateHandset(ServerWebExchange exchange) {
        return currentUser.get().flatMap(user -> Mono.fromCallable(() -> {
            var handset = exchange.getRequest().getCookies().getFirst("wk_device");
            String deviceLabel = handset != null ? handset.getValue() : "unregistered";
            Process process = new ProcessBuilder("sh", "-c", "labelctl calibrate " + deviceLabel).start();
            process.waitFor();
            return "redirect:/worker/dashboard";
        })).onErrorResume(ex -> Mono.just("redirect:/worker/dashboard?error"));
    }

    @GetMapping("/handset/overlay")
    @ResponseBody
    public Mono<ResponseEntity<Resource>> handsetOverlay(ServerWebExchange exchange) {
        var handset = exchange.getRequest().getCookies().getFirst("wk_device");
        String deviceLabel = handset != null ? handset.getValue() : "default";
        return currentUser.get().flatMap(user -> Mono.fromCallable(() -> {
            Path overlayDir = Path.of(uploadsBase, "overlays");
            Path target = overlayDir.resolve(deviceLabel);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body((Resource) new InputStreamResource(Files.newInputStream(target)));
        })).onErrorResume(ex -> Mono.empty());
    }

    @PostMapping("/jobs/{id}/status")
    public Mono<String> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return currentUser.get()
                .flatMap(user -> jobService.transitionStatus(user, id, JobStatus.valueOf(status)))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }

    @PostMapping("/jobs/{id}/proof")
    public Mono<String> uploadProof(@PathVariable Long id, @RequestParam String category,
                                     @RequestParam String visibility, @RequestPart("file") FilePart file,
                                     ServerWebExchange exchange) {
        var handset = exchange.getRequest().getCookies().getFirst("wk_device");
        String deviceLabel = handset != null ? handset.getValue() : "unregistered";
        return currentUser.get()
                .flatMap(user -> proofService.upload(user, id, category, visibility, file, deviceLabel))
                .thenReturn("redirect:/jobs/" + id)
                .onErrorResume(ex -> Mono.just("redirect:/jobs/" + id + "?error"));
    }
}
