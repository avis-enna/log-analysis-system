package com.loganalyzer.config;

import com.loganalyzer.model.LogEntry;
import com.loganalyzer.model.User;
import com.loganalyzer.model.Role;
import com.loganalyzer.repository.LogEntryJpaRepository;
import com.loganalyzer.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired 
    private PasswordEncoder passwordEncoder;

    /**
     * Initialize demo users with different roles and data scenarios
     */
    @Bean
    @Profile("local")
    public CommandLineRunner initializeDemoUsers() {
        return args -> {
            if (testDataEnabled) {
                logger.info("🎭 Creating demo user accounts with different scenarios...");
                createDemoUsers();
                logger.info("✅ Demo users created successfully!");
            }
        };
    }
    
    /**
     * Generate role-specific sample data for demo scenarios
     */
    @Bean
    @Profile("local")
    public CommandLineRunner generateRoleSpecificData(@Autowired LogEntryJpaRepository logEntryRepository) {
        return args -> {
            if (testDataEnabled && generateOnStartup) {
                logger.info("🚀 Generating role-specific demo data...");
                
                // Clear existing data
                logEntryRepository.deleteAll();
                
                List<LogEntry> allLogs = new ArrayList<>();
                
                // Generate admin scenario - production issues
                allLogs.addAll(generateAdminScenarioLogs());
                
                // Generate developer scenario - deployment and debug logs  
                allLogs.addAll(generateDeveloperScenarioLogs());
                
                // Generate QA scenario - testing and monitoring logs
                allLogs.addAll(generateQAScenarioLogs());
                
                // Generate general application logs
                allLogs.addAll(generateGeneralLogs(200));
                
                logEntryRepository.saveAll(allLogs);
                logger.info("✅ Generated {} demo log entries with role-specific scenarios", allLogs.size());
            }
        };
    }
    
    private void createDemoUsers() {
        createDemoUserIfNotExists("admin", "admin123", "admin@company.com", 
            "Sarah Johnson - System Administrator", Set.of(Role.ADMIN),
            "Complete system access. Monitors production, manages users, handles critical incidents.");
            
        createDemoUserIfNotExists("dev", "dev123", "dev@company.com",
            "Alex Chen - Lead Developer", Set.of(Role.DEVELOPER), 
            "Development team lead. Handles deployments, debugging, and code reviews.");
            
        createDemoUserIfNotExists("qa", "qa123", "qa@company.com",
            "Maria Rodriguez - QA Engineer", Set.of(Role.QA),
            "Quality assurance lead. Manages testing, monitoring, and release validation.");
            
        createDemoUserIfNotExists("devops", "devops123", "devops@company.com",
            "Jordan Kim - DevOps Engineer", Set.of(Role.ADMIN, Role.DEVELOPER),
            "Infrastructure and deployment specialist. Full admin access with development focus.");
            
        createDemoUserIfNotExists("qaread", "qaread123", "qaread@company.com", 
            "Sam Wilson - QA Analyst", Set.of(Role.QA),
            "Junior QA analyst. Read-only access for monitoring and basic testing.");
            
        // Additional demo users for different scenarios
        createDemoUserIfNotExists("john.developer", "dev456", "john@company.com",
            "John Smith - Frontend Developer", Set.of(Role.DEVELOPER),
            "Frontend specialist. Focuses on UI/UX issues and client-side debugging.");
            
        createDemoUserIfNotExists("lisa.qa", "qa456", "lisa@company.com", 
            "Lisa Zhang - Senior QA", Set.of(Role.QA),
            "Senior QA engineer. Automated testing and performance monitoring specialist.");
    }
    
    private void createDemoUserIfNotExists(String username, String password, String email, 
                                         String fullName, Set<Role> roles, String description) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, passwordEncoder.encode(password), email, fullName, roles);
            user.setEnabled(true);
            userRepository.save(user);
            logger.info("✨ Created demo user: {} ({}) - {}", username, roles, description);
        }
    }
    

    
    /**
     * Command line runner to generate sample data on startup.
     */
    // @Bean  // Commented out to disable sample data generation
    // @Profile("local")
    // public CommandLineRunner generateSampleData(@Autowired LogEntryJpaRepository logEntryRepository) {
    /*    return args -> {
            if (testDataEnabled && generateOnStartup) {
                logger.info("Generating {} sample log entries for local testing...", logCount);
                
                List<LogEntry> sampleLogs = generateSampleLogEntries(logCount);
                logEntryRepository.saveAll(sampleLogs);
                
                logger.info("Successfully generated {} sample log entries", sampleLogs.size());
            }
        };
    } */
    
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
     * Generate admin scenario logs - production issues, system monitoring
     */
    private List<LogEntry> generateAdminScenarioLogs() {
        List<LogEntry> logs = new ArrayList<>();
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();
        
        // Critical production issues (last 2 hours)
        String[] criticalMessages = {
            "🔥 CRITICAL: Database connection pool exhausted - 0 connections available",
            "💥 SYSTEM ALERT: Memory usage at 95% - immediate action required", 
            "⚠️ HIGH CPU: Server load average: 8.5, 7.2, 6.8 (threshold: 5.0)",
            "🚨 SECURITY: Multiple failed login attempts from IP 192.168.1.100",
            "❌ SERVICE DOWN: Payment gateway unreachable - 15 consecutive failures",
            "🔴 DISK SPACE: Root partition at 98% capacity on production-server-1",
            "⛔ SSL CERT: Certificate expires in 3 days - renewal required",
            "🚧 NETWORK: High latency detected to external API (timeout: 5000ms)"
        };
        
        for (int i = 0; i < 25; i++) {
            LogEntry log = new LogEntry();
            log.setTimestamp(now.minusHours(random.nextInt(2)));
            log.setLevel(i < 8 ? "ERROR" : (i < 15 ? "WARN" : "INFO"));
            log.setSeverity(getSeverityForLevel(log.getLevel()));
            log.setMessage(criticalMessages[random.nextInt(criticalMessages.length)]);
            log.setApplication("production-monitor");
            log.setHost("prod-server-" + (random.nextInt(3) + 1));
            log.setEnvironment("production");
            log.setSource("system.log");
            log.setLogger("com.system.monitor.AlertManager");
            log.setCategory("SYSTEM_ALERT");
            log.setUserId("admin");
            logs.add(log);
        }
        
        return logs;
    }
    
    /**
     * Generate developer scenario logs - deployment, debugging, code issues
     */
    private List<LogEntry> generateDeveloperScenarioLogs() {
        List<LogEntry> logs = new ArrayList<>();
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();
        
        String[] devMessages = {
            "🚀 DEPLOYMENT: Starting deployment of v2.1.3 to staging environment",
            "✅ BUILD SUCCESS: Maven build completed in 45.2 seconds",
            "🔧 DEBUG: SQL query execution time: 1247ms for getUserOrders()",
            "🐛 BUG FIX: NullPointerException in PaymentProcessor.validateCard()",
            "📦 DEPENDENCY: Updated Spring Boot from 3.1.0 to 3.2.0",
            "🔍 TRACE: Method entry: UserService.authenticateUser() with params [email=user@test.com]",
            "⚡ PERFORMANCE: Cache hit ratio: 87% (target: 90%)",
            "🛠️ CONFIG: Updated database connection pool size from 10 to 20",
            "📊 METRICS: API response time p95: 450ms, p99: 890ms",
            "🔄 ROLLBACK: Rolling back deployment due to integration test failures",
            "📝 CODE REVIEW: Security vulnerability fixed in authentication module",
            "🧪 UNIT TEST: Test coverage increased to 85% (+3% from last run)"
        };
        
        for (int i = 0; i < 30; i++) {
            LogEntry log = new LogEntry();
            log.setTimestamp(now.minusHours(random.nextInt(8)));
            log.setLevel(i < 3 ? "ERROR" : (i < 8 ? "WARN" : (i < 20 ? "INFO" : "DEBUG")));
            log.setSeverity(getSeverityForLevel(log.getLevel()));
            log.setMessage(devMessages[random.nextInt(devMessages.length)]);
            log.setApplication(random.nextBoolean() ? "api-service" : "web-app");
            log.setHost("dev-server-" + (random.nextInt(2) + 1));
            log.setEnvironment(random.nextBoolean() ? "development" : "staging");
            log.setSource("application.log");
            log.setLogger("com.app." + (random.nextBoolean() ? "service" : "controller"));
            log.setCategory("DEVELOPMENT");
            log.setUserId("dev");
            logs.add(log);
        }
        
        return logs;
    }
    
    /**
     * Generate QA scenario logs - testing, monitoring, validation
     */
    private List<LogEntry> generateQAScenarioLogs() {
        List<LogEntry> logs = new ArrayList<>();
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();
        
        String[] qaMessages = {
            "🧪 TEST START: Automated regression test suite initiated",
            "✅ TEST PASS: User authentication flow - 45/45 scenarios passed",
            "❌ TEST FAIL: Payment integration test failed - timeout after 30s",
            "📊 LOAD TEST: Simulating 1000 concurrent users on staging",
            "🔍 VALIDATION: API contract validation completed - 0 breaking changes",
            "📈 PERFORMANCE: Response time baseline established - avg: 245ms",
            "🚫 ASSERTION FAIL: Expected HTTP 200, got HTTP 500 for /api/users",
            "🎯 COVERAGE: Code coverage report generated - 82% overall",
            "🔄 REGRESSION: Re-running failed tests after bug fix deployment",
            "📋 REPORT: Test execution summary - 156 passed, 3 failed, 2 skipped",
            "🛡️ SECURITY TEST: Penetration testing completed - 2 low-risk findings",
            "📱 MOBILE TEST: Cross-platform testing on iOS/Android completed",
            "🌐 BROWSER TEST: Selenium tests passed on Chrome, Firefox, Safari",
            "⚙️ API TEST: Postman collection executed - all endpoints healthy"
        };
        
        for (int i = 0; i < 35; i++) {
            LogEntry log = new LogEntry();
            log.setTimestamp(now.minusHours(random.nextInt(12)));
            log.setLevel(i < 5 ? "ERROR" : (i < 12 ? "WARN" : "INFO"));
            log.setSeverity(getSeverityForLevel(log.getLevel()));
            log.setMessage(qaMessages[random.nextInt(qaMessages.length)]);
            log.setApplication("test-framework");
            log.setHost("qa-server-" + (random.nextInt(2) + 1));
            log.setEnvironment(random.nextBoolean() ? "testing" : "staging");
            log.setSource("test.log");
            log.setLogger("com.qa." + (random.nextBoolean() ? "automation" : "manual"));
            log.setCategory("TESTING");
            log.setUserId("qa");
            logs.add(log);
        }
        
        return logs;
    }
    
    /**
     * Generate general application logs for baseline
     */
    private List<LogEntry> generateGeneralLogs(int count) {
        List<LogEntry> logs = new ArrayList<>();
        Random random = new Random();
        LocalDateTime now = LocalDateTime.now();
        
        String[] generalMessages = {
            "User session started for user ID: {}",
            "HTTP {} request to {} completed in {}ms",
            "Database connection established to primary node",
            "Scheduled backup job completed successfully",
            "Configuration reloaded from application.properties",
            "Health check endpoint responding normally",
            "JWT token refreshed for user session",
            "Email notification sent to user@example.com",
            "File upload completed: document.pdf (2.1MB)",
            "Search query processed: 'order status' (15 results)"
        };
        
        String[] apps = {"user-service", "order-service", "notification-service", "file-service"};
        String[] environments = {"production", "staging", "development"};
        
        for (int i = 0; i < count; i++) {
            LogEntry log = new LogEntry();
            log.setTimestamp(now.minusHours(random.nextInt(24)));
            log.setLevel(i % 10 == 0 ? "WARN" : "INFO");
            log.setSeverity(getSeverityForLevel(log.getLevel()));
            log.setMessage(generalMessages[random.nextInt(generalMessages.length)]
                .replace("{}", String.valueOf(random.nextInt(1000))));
            log.setApplication(apps[random.nextInt(apps.length)]);
            log.setHost("app-server-" + (random.nextInt(5) + 1));
            log.setEnvironment(environments[random.nextInt(environments.length)]);
            log.setSource("application.log");
            log.setLogger("com.app.service.BaseService");
            log.setCategory("APPLICATION");
            logs.add(log);
        }
        
        return logs;
    }
    
}
