package com.fielddispatch.web;

import com.auth0.jwt.JWT;
import com.fielddispatch.repository.RouteRepository;
import com.fielddispatch.repository.RouteStopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only handoff console for haulage partners. The carrier gateway returns
 * the partner to this page with a handoff context attached as a cookie; the
 * console reads the carrier and region out of that context and lists the
 * routes the depot has released to that partner for the day.
 */
@RestController
@RequiredArgsConstructor
public class CarrierHandoffController {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    @GetMapping("/carriers/handoff")
    public Mono<Map<String, Object>> handoff(ServerWebExchange exchange) {
        var context = exchange.getRequest().getCookies().getFirst("carrier_ctx");
        if (context == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "No handoff context supplied"));
        }
        return Mono.fromCallable(() -> JWT.decode(context.getValue()))
                .flatMap(handoff -> {
                    String carrier = handoff.getClaim("carrier").asString();
                    Long regionId = handoff.getClaim("region").asLong();
                    return routeRepository.findByRegionId(regionId)
                            .flatMap(route -> routeStopRepository.findByRouteIdOrderBySequenceAsc(route.getId())
                                    .map(stop -> Map.<String, Object>of(
                                            "routeId", route.getId(),
                                            "jobId", stop.getJobId(),
                                            "sequence", stop.getSequence()))
                                    .collectList())
                            .collectList()
                            .map(rows -> {
                                Map<String, Object> result = new LinkedHashMap<>();
                                result.put("carrier", carrier);
                                result.put("region", regionId);
                                result.put("shipments", rows);
                                return result;
                            });
                });
    }
}
