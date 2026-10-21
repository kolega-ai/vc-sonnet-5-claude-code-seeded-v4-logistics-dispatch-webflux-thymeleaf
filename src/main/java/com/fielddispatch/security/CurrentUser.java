package com.fielddispatch.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Resolves the authenticated principal from the reactive security context.
 * Every controller/service must obtain the acting user THIS way (never from a
 * request parameter) so the identity used for authorization always matches
 * the session that Spring Security has already verified.
 */
@Component
public class CurrentUser {

    public Mono<AppUserPrincipal> get() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (AppUserPrincipal) ctx.getAuthentication().getPrincipal());
    }
}
