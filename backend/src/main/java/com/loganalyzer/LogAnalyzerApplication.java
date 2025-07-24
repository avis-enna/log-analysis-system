package com.loganalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
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
 * - H2 Database for local development (PostgreSQL for production)
 * - JPA repositories for local development (Elasticsearch for production)
 * - In-memory processing for local development (Kafka for production)
 * - Local caching for development (Redis for production)
 *
 * External services are disabled by default for local development.
 * Enable them in production by setting the appropriate properties.
 *
 * @author Enterprise Log Analysis Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication(exclude = {
    // Disable external service auto-configurations for local development
    ElasticsearchRestClientAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class,
    KafkaAutoConfiguration.class
})
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableScheduling
public class LogAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogAnalyzerApplication.class, args);
    }
}
