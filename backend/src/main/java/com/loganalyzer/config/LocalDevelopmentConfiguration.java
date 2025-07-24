package com.loganalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for local development.
 * Provides beans that are needed for the application to run locally
 * without external services like email servers, message brokers, etc.
 */
@Configuration
public class LocalDevelopmentConfiguration {

    /**
     * Provides a RestTemplate bean for HTTP client operations.
     * Used by NotificationService for webhook notifications.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
