package com.fielddispatch.config;

import com.fielddispatch.security.AppUserPrincipal;
import com.fielddispatch.util.AssetVersion;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import reactor.core.publisher.Mono;

/**
 * Adds shared, non-sensitive attributes (asset cache-busting suffix, current
 * role for nav rendering) to every Thymeleaf model. Reactive WebFlux
 * Thymeleaf does not support sec:authorize/hasRole() expressions, so the
 * navbar template branches on this plain "currentRole" string instead. This
 * is display-only - it never substitutes for a real authorization check.
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalModelAdvice {

    @ModelAttribute("assetVersion")
    public String assetVersion() {
        return AssetVersion.suffix();
    }

    @ModelAttribute("currentRole")
    public Mono<String> currentRole() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (AppUserPrincipal) ctx.getAuthentication().getPrincipal())
                .map(p -> p.getRole().name())
                .defaultIfEmpty("");
    }
}
