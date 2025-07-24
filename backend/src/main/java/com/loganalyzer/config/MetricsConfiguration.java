package com.loganalyzer.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application metrics and monitoring.
 */
@Configuration
public class MetricsConfiguration {
    
    /**
     * Timer for search operations
     */
    @Bean
    public Timer searchTimer(MeterRegistry meterRegistry) {
        return Timer.builder("log.search.duration")
            .description("Time taken to execute log searches")
            .tag("operation", "search")
            .register(meterRegistry);
    }
    
    /**
     * Counter for search requests
     */
    @Bean
    public Counter searchCounter(MeterRegistry meterRegistry) {
        return Counter.builder("log.search.requests")
            .description("Number of search requests")
            .tag("operation", "search")
            .register(meterRegistry);
    }
    
    /**
     * Counter for search errors
     */
    @Bean
    public Counter searchErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("log.search.errors")
            .description("Number of search errors")
            .tag("operation", "search")
            .register(meterRegistry);
    }
    
    /**
     * Timer for log ingestion operations
     */
    @Bean
    public Timer ingestionTimer(MeterRegistry meterRegistry) {
        return Timer.builder("log.ingestion.duration")
            .description("Time taken to ingest log entries")
            .tag("operation", "ingestion")
            .register(meterRegistry);
    }
    
    /**
     * Counter for ingested logs
     */
    @Bean
    public Counter ingestionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("log.ingestion.count")
            .description("Number of logs ingested")
            .tag("operation", "ingestion")
            .register(meterRegistry);
    }
    
    /**
     * Counter for cache hits
     */
    @Bean
    public Counter cacheHitCounter(MeterRegistry meterRegistry) {
        return Counter.builder("log.cache.hits")
            .description("Number of cache hits")
            .tag("operation", "cache")
            .register(meterRegistry);
    }
    
    /**
     * Counter for cache misses
     */
    @Bean
    public Counter cacheMissCounter(MeterRegistry meterRegistry) {
        return Counter.builder("log.cache.misses")
            .description("Number of cache misses")
            .tag("operation", "cache")
            .register(meterRegistry);
    }
}
