package com.wego;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WeGo Application Entry Point.
 *
 * A collaborative travel planning platform for friends and groups.
 *
 * @contract
 *   - pre: Spring Boot environment is properly configured
 *   - post: Application context is initialized and running
 */
@SpringBootApplication
public class WegoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WegoApplication.class, args);
    }
}
