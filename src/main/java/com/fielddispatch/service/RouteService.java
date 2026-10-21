package com.fielddispatch.service;

import com.fielddispatch.domain.Route;
import com.fielddispatch.domain.RouteStop;
import com.fielddispatch.domain.Worker;
import com.fielddispatch.repository.RouteRepository;
import com.fielddispatch.repository.RouteStopRepository;
import com.fielddispatch.repository.WorkerRepository;
import com.fielddispatch.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final WorkerRepository workerRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    @Value("${app.uploads.base-dir}")
    private String uploadsBase;

    /**
     * Writes the day's stop sequence for a route to the shared driver-inbox
     * folder so the depot printer can pick it up. The sheet keeps the name the
     * dispatcher assigns it in the board's save dialog.
     */
    public Mono<Path> saveRouteSheet(AppUserPrincipal actor, Long routeId, String sheetName) {
        return routeRepository.findById(routeId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)))
                .flatMap(route -> authorizationService.assertDispatcherRegion(actor, route.getRegionId())
                        .then(routeStopRepository.findByRouteIdOrderBySequenceAsc(routeId).collectList())
                        .flatMap(stops -> Mono.fromCallable(() -> {
                            Path sheetDir = Path.of(uploadsBase, "route-sheets");
                            Files.createDirectories(sheetDir);
                            File target = sheetDir.resolve(sheetName).toFile();
                            try (FileWriter out = new FileWriter(target)) {
                                out.write("sequence,job_id\n");
                                for (RouteStop stop : stops) {
                                    out.write(stop.getSequence() + "," + stop.getJobId() + "\n");
                                }
                            }
                            return target.toPath();
                        })));
    }

    public Flux<RouteStop> stopsForRoute(Long routeId) {
        return routeStopRepository.findByRouteIdOrderBySequenceAsc(routeId);
    }

    public Mono<Route> todaysRouteForWorker(Long workerId) {
        return routeRepository.findByWorkerIdAndRouteDate(workerId, LocalDate.now());
    }

    public Flux<Route> routesForRegions(List<Long> regionIds) {
        return Flux.fromIterable(regionIds).flatMap(routeRepository::findByRegionId);
    }

    /**
     * Reorders the stops of a route to the given job-id sequence. Only a
     * dispatcher scoped to the route's region (or ops_admin) may reorder.
     * The change is written to the audit/activity history per the spec.
     */
    public Mono<Route> reorderStops(AppUserPrincipal dispatcher, Long routeId, List<Long> orderedJobIds) {
        return routeRepository.findById(routeId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)))
                .flatMap(route -> authorizationService.assertDispatcherRegion(dispatcher, route.getRegionId())
                        .then(routeStopRepository.findByRouteIdOrderBySequenceAsc(routeId).collectList())
                        .flatMap(stops -> {
                            String before = stops.stream().map(s -> String.valueOf(s.getJobId())).reduce("", (a, b) -> a + "," + b);
                            return Flux.fromIterable(stops)
                                    .flatMap(stop -> {
                                        int newSeq = orderedJobIds.indexOf(stop.getJobId()) + 1;
                                        if (newSeq <= 0) newSeq = stop.getSequence();
                                        stop.setSequence(newSeq);
                                        return routeStopRepository.save(stop);
                                    })
                                    .collectList()
                                    .flatMap(saved -> auditService.record(dispatcher, "ROUTE_REORDER", "ROUTE", String.valueOf(route.getId()),
                                            before, orderedJobIds.toString(), "dispatcher-region-scope"))
                                    .thenReturn(route);
                        }));
    }

    public Mono<Worker> workerForUser(Long userId) {
        return workerRepository.findByUserId(userId);
    }
}
