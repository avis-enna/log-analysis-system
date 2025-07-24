package com.loganalyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for comprehensive API documentation.
 */
@Configuration
public class OpenApiConfiguration {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Enterprise Log Analysis System API")
                .description("""
                    A comprehensive log analysis and monitoring system that provides:
                    
                    ## Features
                    - **Real-time log ingestion** from multiple sources
                    - **Advanced search capabilities** with Splunk-like syntax
                    - **Intelligent alerting** with customizable rules
                    - **Performance monitoring** and analytics
                    - **Dashboard visualizations** for insights
                    
                    ## Getting Started
                    1. Use the `/api/v1/logs/ingest` endpoint to send log data
                    2. Search logs using `/api/v1/logs/search` with powerful query syntax
                    3. Set up alerts via `/api/v1/alerts` endpoints
                    4. Monitor system health at `/actuator/health`
                    
                    ## Authentication
                    Currently using basic authentication for development.
                    Production deployments should use OAuth2 or JWT tokens.
                    
                    ## Rate Limits
                    - Search API: 100 requests/minute per IP
                    - Ingestion API: 1000 requests/minute per IP
                    - Dashboard API: 200 requests/minute per IP
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Log Analysis Team")
                    .email("support@loganalyzer.com")
                    .url("https://github.com/enterprise/log-analyzer"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.loganalyzer.com")
                    .description("Production Server"),
                new Server()
                    .url("https://staging-api.loganalyzer.com")
                    .description("Staging Server")))
            .tags(List.of(
                new Tag()
                    .name("Search")
                    .description("Log search and query operations"),
                new Tag()
                    .name("Ingestion")
                    .description("Log data ingestion endpoints"),
                new Tag()
                    .name("Alerts")
                    .description("Alert management and configuration"),
                new Tag()
                    .name("Dashboard")
                    .description("Dashboard data and analytics"),
                new Tag()
                    .name("System")
                    .description("System health and monitoring"),
                new Tag()
                    .name("Configuration")
                    .description("System configuration and settings")));
    }
}
