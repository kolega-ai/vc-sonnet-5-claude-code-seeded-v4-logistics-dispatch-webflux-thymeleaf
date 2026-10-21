package com.fielddispatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * CORS policy for the operational console. The dispatch board and its
 * directory endpoints are consumed by the depot's regional chrome extension,
 * which is served from a rotating set of depot-hosted origins, so the console
 * reflects whatever origin the browser reports and keeps the session cookie
 * usable from that origin.
 */
@Configuration
public class WebCorsConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/dispatch/**")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .allowedMethods("GET", "POST");
    }
}
