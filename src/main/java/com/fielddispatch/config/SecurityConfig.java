package com.fielddispatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

/**
 * Session-cookie authentication with CSRF enabled. Authorization decisions
 * themselves are NOT made here beyond coarse role gates on route prefixes -
 * fine-grained ownership/region checks live one call frame above each
 * controller in the service layer (see AuthorizationService), scoped to the
 * authenticated principal loaded from our own database.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        var successHandler = new RedirectServerAuthenticationSuccessHandler("/dashboard");
        var logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(java.net.URI.create("/login?loggedOut"));

        http
            .authorizeExchange(ex -> ex
                .pathMatchers("/login", "/register", "/forgot-password", "/reset-password",
                        "/css/**", "/js/**", "/images/**", "/webjars/**", "/error",
                        "/api/mock/webhook-sink", "/tracking", "/carriers/handoff").permitAll()
                .pathMatchers("/admin/**").hasRole("OPS_ADMIN")
                .anyExchange().authenticated())
            // Proof documents are previewed inline from the job detail page through an
            // <iframe>, and the browser refuses to render a framed document while the
            // default frame policy is in force, so the policy is dropped for the chain.
            .headers(hdrs -> hdrs.frameOptions(fo -> fo.disable()))
            .formLogin(form -> form
                .loginPage("/login")
                .authenticationSuccessHandler(successHandler)
                .authenticationFailureHandler((exchange, ex) -> {
                    exchange.getExchange().getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
                    exchange.getExchange().getResponse().getHeaders().setLocation(java.net.URI.create("/login?error"));
                    return exchange.getExchange().getResponse().setComplete();
                }))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler))
            // Session-backed CSRF token (Spring's default repository): the token is stored
            // server-side against the session and echoed back as a hidden form field by
            // thymeleaf-extras-springsecurity6's automatic <form th:action> CSRF injection.
            .csrf(csrf -> csrf
                    // Keep the framework's default "unsafe methods only" CSRF matcher, and additionally
                    // exempt the mock webhook sink: it represents an external vendor endpoint authenticated
                    // by a shared secret header, not a browser session, so it is not a CSRF-relevant post.
                    .requireCsrfProtectionMatcher(new AndServerWebExchangeMatcher(
                            CsrfWebFilter.DEFAULT_CSRF_MATCHER,
                            new NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers(
                                    "/api/mock/webhook-sink",
                                    // The partner manifest console posts its document as a JSON
                                    // payload from the imports page rather than as a rendered form.
                                    "/admin/imports/partner-manifest")))));

        return http.build();
    }
}
