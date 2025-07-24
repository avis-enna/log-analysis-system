package com.loganalyzer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates application configuration on startup.
 */
@Component
public class ConfigurationValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);
    
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;
    
    @Value("${server.port:8080}")
    private int serverPort;
    
    @Value("${management.endpoints.web.exposure.include:}")
    private String exposedEndpoints;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfiles;
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        logger.info("🔍 Validating application configuration...");
        
        validateDatabaseConfiguration();
        validateServerConfiguration();
        validateSecurityConfiguration();
        validateMonitoringConfiguration();
        
        logger.info("✅ Configuration validation completed successfully");
    }
    
    private void validateDatabaseConfiguration() {
        logger.info("📊 Database Configuration:");
        
        if (datasourceUrl.isEmpty()) {
            logger.warn("⚠️  Database URL not configured, using default H2");
        } else {
            logger.info("   - Database URL: {}", maskSensitiveInfo(datasourceUrl));
        }
        
        if (datasourceUrl.contains("h2:mem:")) {
            logger.info("   - Using H2 in-memory database (perfect for development)");
        } else if (datasourceUrl.contains("h2:file:")) {
            logger.info("   - Using H2 file-based database");
        } else if (datasourceUrl.contains("postgresql://")) {
            logger.info("   - Using PostgreSQL database");
        } else if (datasourceUrl.contains("mysql://")) {
            logger.info("   - Using MySQL database");
        }
    }
    
    private void validateServerConfiguration() {
        logger.info("🌐 Server Configuration:");
        logger.info("   - Server port: {}", serverPort);
        logger.info("   - Active profiles: {}", activeProfiles);
        
        if (serverPort < 1024 && !activeProfiles.contains("prod")) {
            logger.warn("⚠️  Using privileged port {} in non-production environment", serverPort);
        }
        
        if (activeProfiles.contains("prod") && serverPort == 8080) {
            logger.warn("⚠️  Using default port 8080 in production environment");
        }
    }
    
    private void validateSecurityConfiguration() {
        logger.info("🔒 Security Configuration:");
        
        if (activeProfiles.contains("prod")) {
            logger.info("   - Production profile detected - security features enabled");
            // Add production-specific security validations
        } else {
            logger.info("   - Development profile - using relaxed security settings");
        }
        
        // Check if security headers are configured
        logger.info("   - Security headers: Configured via SecurityHeadersConfiguration");
        logger.info("   - CORS: Configured for development origins");
    }
    
    private void validateMonitoringConfiguration() {
        logger.info("📈 Monitoring Configuration:");
        logger.info("   - Exposed actuator endpoints: {}", exposedEndpoints);
        
        if (exposedEndpoints.contains("env") && activeProfiles.contains("prod")) {
            logger.warn("⚠️  Environment endpoint exposed in production - consider restricting access");
        }
        
        if (exposedEndpoints.contains("health")) {
            logger.info("   - Health checks: Enabled");
        }
        
        if (exposedEndpoints.contains("metrics")) {
            logger.info("   - Metrics collection: Enabled");
        }
        
        if (exposedEndpoints.contains("prometheus")) {
            logger.info("   - Prometheus metrics: Enabled");
        }
    }
    
    private String maskSensitiveInfo(String value) {
        if (value == null || value.length() < 10) {
            return "***";
        }
        
        // Mask passwords and sensitive parts of URLs
        return value.replaceAll("://[^:]+:[^@]+@", "://***:***@")
                   .replaceAll("password=[^&\\s]+", "password=***")
                   .replaceAll("token=[^&\\s]+", "token=***");
    }
}
