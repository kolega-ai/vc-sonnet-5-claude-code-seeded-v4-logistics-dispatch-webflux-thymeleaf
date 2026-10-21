package com.fielddispatch.web;

import com.fielddispatch.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CurrentUser currentUser;

    @GetMapping("/dashboard")
    public Mono<String> dashboard() {
        return currentUser.get().map(user -> switch (user.getRole()) {
            case OPS_ADMIN -> "redirect:/admin/overview";
            case DISPATCHER -> "redirect:/dispatch/board";
            case FIELD_WORKER -> "redirect:/worker/dashboard";
            case CUSTOMER -> "redirect:/customer/portal";
        });
    }

    @GetMapping("/")
    public Mono<String> home() {
        return Mono.just("redirect:/dashboard");
    }
}
