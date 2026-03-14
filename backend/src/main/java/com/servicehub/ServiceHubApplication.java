package com.servicehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
/**
 * Entry point for the ServiceHub Spring Boot application.
 */

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ServiceHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceHubApplication.class, args);
    }
}
