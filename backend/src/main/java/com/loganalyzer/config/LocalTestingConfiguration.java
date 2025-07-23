package com.loganalyzer.config;

import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryRepository;
import com.loganalyzer.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Configuration for local testing without external dependencies.
 * This configuration provides mock services and sample data for testing.
 */
@Configuration
@Profile("local")
public class LocalTestingConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalTestingConfiguration.class);
    
    @Value("${test-data.enabled:true}")
    private boolean testDataEnabled;
    
    @Value("${test-data.generate-on-startup:true}")
    private boolean generateOnStartup;
    
    @Value("${test-data.log-count:1000}")
    private int logCount;
    
    /**
     * Mock Elasticsearch operations for local testing.
     */
    @Bean
    @Profile("local")
    public ElasticsearchOperations mockElasticsearchOperations() {
        logger.info("Using mock Elasticsearch operations for local testing");
        return new MockElasticsearchOperations();
    }
    
    /**
     * Mock Redis template for local testing.
     */
    @Bean
    @Profile("local")
    public RedisTemplate<String, Object> mockRedisTemplate() {
        logger.info("Using mock Redis template for local testing");
        return new MockRedisTemplate<>();
    }
    
    /**
     * Command line runner to generate sample data on startup.
     */
    @Bean
    @Profile("local")
    public CommandLineRunner generateSampleData(@Autowired LogEntryRepository logEntryRepository) {
        return args -> {
            if (testDataEnabled && generateOnStartup) {
                logger.info("Generating {} sample log entries for local testing...", logCount);
                
                List<LogEntry> sampleLogs = generateSampleLogEntries(logCount);
                logEntryRepository.saveAll(sampleLogs);
                
                logger.info("Successfully generated {} sample log entries", sampleLogs.size());
            }
        };
    }
    
    /**
     * Generates sample log entries for testing.
     */
    private List<LogEntry> generateSampleLogEntries(int count) {
        List<LogEntry> logs = new ArrayList<>();
        Random random = new Random();
        
        String[] levels = {"DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
        String[] applications = {"web-service", "auth-service", "payment-service", "notification-service", "analytics-service"};
        String[] hosts = {"server-1", "server-2", "server-3", "server-4", "server-5"};
        String[] environments = {"development", "staging", "production"};
        String[] sources = {"application.log", "access.log", "error.log", "security.log"};
        
        String[] messageTemplates = {
            "User %d logged in successfully",
            "Database query executed in %d ms",
            "HTTP request to %s completed with status %d",
            "Cache miss for key: user_%d",
            "Payment processing failed for transaction %d",
            "Memory usage: %d%% of available heap",
            "Connection timeout to external service %s",
            "Scheduled task %s completed successfully",
            "Invalid input received: %s",
            "System health check passed",
            "Authentication failed for user %s",
            "File upload completed: %s (%d bytes)",
            "Email notification sent to %s",
            "API rate limit exceeded for client %s",
            "Background job %s started",
            "Configuration updated: %s = %s",
            "Security alert: suspicious activity detected",
            "Performance warning: slow query detected (%d ms)",
            "Service %s is now available",
            "Backup operation completed successfully"
        };
        
        for (int i = 0; i < count; i++) {
            LogEntry log = new LogEntry();
            log.setId(String.valueOf(i + 1));
            log.setLevel(levels[random.nextInt(levels.length)]);
            log.setApplication(applications[random.nextInt(applications.length)]);
            log.setHost(hosts[random.nextInt(hosts.length)]);
            log.setEnvironment(environments[random.nextInt(environments.length)]);
            log.setSource(sources[random.nextInt(sources.length)]);
            
            // Generate realistic timestamp (last 7 days)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime timestamp = now.minusMinutes(random.nextInt(7 * 24 * 60));
            log.setTimestamp(timestamp);
            
            // Generate message based on template
            String template = messageTemplates[random.nextInt(messageTemplates.length)];
            String message = generateMessageFromTemplate(template, random);
            log.setMessage(message);
            
            // Set severity based on level
            log.setSeverity(getSeverityForLevel(log.getLevel()));
            
            // Add some metadata
            log.addMetadata("thread", "thread-" + random.nextInt(10));
            log.addMetadata("requestId", "req-" + random.nextInt(100000));
            log.addMetadata("userId", String.valueOf(random.nextInt(1000)));
            
            // Add tags
            log.addTag("generated");
            log.addTag("local-testing");
            if (random.nextBoolean()) {
                log.addTag("important");
            }
            
            log.setParsed(true);
            
            logs.add(log);
        }
        
        return logs;
    }
    
    /**
     * Generates a message from a template with random values.
     */
    private String generateMessageFromTemplate(String template, Random random) {
        try {
            return String.format(template, 
                random.nextInt(10000),           // %d - numbers
                random.nextInt(5000),            // %d - more numbers
                "/api/endpoint" + random.nextInt(10), // %s - endpoints
                200 + random.nextInt(400),       // %d - HTTP status
                "user" + random.nextInt(100),    // %s - usernames
                "service" + random.nextInt(5),   // %s - service names
                "job" + random.nextInt(20),      // %s - job names
                "config.property",               // %s - config keys
                "value" + random.nextInt(100),   // %s - config values
                "file" + random.nextInt(100) + ".txt", // %s - filenames
                random.nextInt(1000000)          // %d - file sizes
            );
        } catch (Exception e) {
            // Fallback for templates that don't match format specifiers
            return template.replaceAll("%[sd]", String.valueOf(random.nextInt(1000)));
        }
    }
    
    /**
     * Maps log level to severity number.
     */
    private int getSeverityForLevel(String level) {
        switch (level.toUpperCase()) {
            case "FATAL":
                return 5;
            case "ERROR":
                return 4;
            case "WARN":
                return 3;
            case "INFO":
                return 2;
            case "DEBUG":
                return 1;
            default:
                return 0;
        }
    }
    
    /**
     * Mock implementation of ElasticsearchOperations for local testing.
     */
    private static class MockElasticsearchOperations implements ElasticsearchOperations {
        // Implement minimal required methods for testing
        // This is a simplified mock - in a real implementation,
        // you would use a proper mocking framework like Mockito
        
        @Override
        public String index(Object entity) {
            return "mock-index-" + System.currentTimeMillis();
        }
        
        @Override
        public boolean exists(String id, Class<?> clazz) {
            return true; // Mock implementation
        }
        
        // Add other required method implementations as needed
        // For brevity, only showing key methods
    }
    
    /**
     * Mock implementation of RedisTemplate for local testing.
     */
    private static class MockRedisTemplate<K, V> extends RedisTemplate<K, V> {
        // Mock implementation that stores data in memory
        private final java.util.Map<K, V> mockStorage = new java.util.concurrent.ConcurrentHashMap<>();
        
        @Override
        public void opsForValue() {
            // Return mock value operations
        }
        
        // Add other required method implementations as needed
    }
}
