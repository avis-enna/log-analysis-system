package com.loganalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Enterprise Log Analysis System.
 * 
 * This application provides comprehensive log analysis capabilities including:
 * - Real-time log ingestion and processing
 * - Advanced search and query capabilities
 * - Pattern detection and anomaly analysis
 * - Interactive dashboards and visualizations
 * - Intelligent alerting system
 * - RESTful API for external integrations
 * 
 * Technology Stack:
 * - Spring Boot 3.2 with Java 17
 * - Elasticsearch for search and indexing
 * - Apache Kafka for real-time data streaming
 * - InfluxDB for time-series data storage
 * - PostgreSQL for metadata and configuration
 * - Redis for caching and session management
 * 
 * @author Enterprise Log Analysis Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class LogAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogAnalyzerApplication.class, args);
    }
}
