package com.fielddispatch.web;

import com.fielddispatch.domain.IncidentStatus;
import com.fielddispatch.security.CurrentUser;
import com.fielddispatch.service.AuthorizationService;
import com.fielddispatch.service.IncidentService;
import com.fielddispatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final CurrentUser currentUser;
    private final IncidentService incidentService;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;

    @GetMapping
    public Mono<String> queue(Model model) {
        return currentUser.get().flatMap(user -> {
            if (!authorizationService.isStaff(user)) {
                return Mono.error(AuthorizationService.forbidden());
            }
            return incidentService.queueForStaff().collectList()
                    .doOnNext(list -> model.addAttribute("incidents", list))
                    .thenReturn("incident/queue");
        });
    }

    @PostMapping("/{id}/assign")
    public Mono<String> assign(@PathVariable Long id, @RequestParam Long assigneeUserId) {
        return currentUser.get()
                .flatMap(user -> incidentService.assign(user, id, assigneeUserId))
                .thenReturn("redirect:/incidents")
                .onErrorResume(ex -> Mono.just("redirect:/incidents?error"));
    }

    @PostMapping("/{id}/status")
    public Mono<String> updateStatus(@PathVariable Long id, @RequestParam String status,
                                      @RequestParam(required = false) String internalNotes,
                                      @RequestParam(required = false) String customerSummary) {
        return currentUser.get()
                .flatMap(user -> incidentService.updateStatus(user, id, IncidentStatus.valueOf(status), internalNotes, customerSummary))
                .thenReturn("redirect:/incidents")
                .onErrorResume(ex -> Mono.just("redirect:/incidents?error"));
    }
}
