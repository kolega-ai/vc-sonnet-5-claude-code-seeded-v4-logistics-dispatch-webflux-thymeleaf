package com.fielddispatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Field Dispatch Operations - reactive Spring WebFlux + Thymeleaf logistics dispatch app.
 */
@SpringBootApplication
@EnableScheduling
public class FieldDispatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(FieldDispatchApplication.class, args);
    }
}
