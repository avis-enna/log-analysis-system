package com.sivareddy.loganalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Enterprise Log Analysis System - Like Splunk/QRadar
 * Features: Real-time ingestion, correlation, threat detection, analytics
 * 
 * @author Siva Reddy
 */
@SpringBootApplication
@EnableScheduling
public class LogAnalysisSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogAnalysisSystemApplication.class, args);
    }
}

// Data Models
class LogEvent {
    private String id;
    private LocalDateTime timestamp;
    private String level;
    private String source;
    private String message;
    private String host;
    private String category;
    private Map<String, Object> metadata;
    private String rawLog;
    
    public LogEvent(String id, LocalDateTime timestamp, String level, String source, 
                   String message, String host, String category) {
        this.id = id;
        this.timestamp = timestamp;
        this.level = level;
        this.source = source;
        this.message = message;
        this.host = host;
        this.category = category;
        this.metadata = new HashMap<>();
        this.rawLog = String.format("[%s] %s %s: %s", timestamp, level, source, message);
    }
    
    // Getters and setters
    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getSource() { return source; }
    public String getMessage() { return message; }
    public String getHost() { return host; }
    public String getCategory() { return category; }
    public Map<String, Object> getMetadata() { return metadata; }
    public String getRawLog() { return rawLog; }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}

class SecurityAlert {
    private String id;
    private LocalDateTime createdAt;
    private String severity;
    private String type;
    private String message;
    private String source;
    private boolean resolved;
    private List<String> relatedEvents;
    private String riskScore;
    private Map<String, Object> details;
    
    public SecurityAlert(String id, String severity, String type, String message, String source) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.severity = severity;
        this.type = type;
        this.message = message;
        this.source = source;
        this.resolved = false;
        this.relatedEvents = new ArrayList<>();
        this.details = new HashMap<>();
        this.riskScore = calculateRiskScore(severity);
    }
    
    private String calculateRiskScore(String severity) {
        switch (severity) {
            case "CRITICAL": return "95";
            case "HIGH": return "75";
            case "MEDIUM": return "50";
            case "LOW": return "25";
            default: return "10";
        }
    }
    
    // Getters
    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getSeverity() { return severity; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getSource() { return source; }
    public boolean isResolved() { return resolved; }
    public List<String> getRelatedEvents() { return relatedEvents; }
    public String getRiskScore() { return riskScore; }
    public Map<String, Object> getDetails() { return details; }
    
    public void resolve() { this.resolved = true; }
    public void addRelatedEvent(String eventId) { this.relatedEvents.add(eventId); }
}

// Services
@Service
class LogIngestionService {
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final List<LogEvent> logBuffer = Collections.synchronizedList(new ArrayList<>());
    private final Random random = new Random();
    
    @Scheduled(fixedRate = 2000) // Simulate real-time log ingestion every 2 seconds
    public void ingestLogs() {
        generateSampleLogs();
    }
    
    private void generateSampleLogs() {
        String[] levels = {"INFO", "WARNING", "ERROR", "DEBUG", "FATAL"};
        String[] sources = {"nginx", "application", "database", "security", "firewall", "mail-server", "auth-service"};
        String[] hosts = {"web-01", "web-02", "db-01", "app-01", "proxy-01"};
        String[] categories = {"authentication", "network", "application", "security", "system"};
        
        String[][] logTemplates = {
            {"INFO", "User login successful for user: %s", "authentication"},
            {"WARNING", "High CPU usage detected: %d%%", "system"},
            {"ERROR", "Failed to connect to database: %s", "application"},
            {"INFO", "HTTP request processed in %dms", "network"},
            {"ERROR", "Authentication failed for user: %s", "security"},
            {"WARNING", "Memory usage threshold exceeded: %d%%", "system"},
            {"INFO", "Backup completed successfully", "system"},
            {"ERROR", "SQL injection attempt detected from IP: %s", "security"},
            {"WARNING", "Disk space low on partition /var: %d%% used", "system"},
            {"INFO", "User logout for session: %s", "authentication"},
            {"FATAL", "Critical system failure in module: %s", "system"},
            {"DEBUG", "Processing request from %s", "network"},
            {"ERROR", "Connection refused to %s:%d", "network"},
            {"WARNING", "Suspicious activity detected from %s", "security"}
        };
        
        for (int i = 0; i < 5; i++) {
            String[] template = logTemplates[random.nextInt(logTemplates.length)];
            String level = template[0];
            String messageTemplate = template[1];
            String category = template[2];
            
            String message;
            if (messageTemplate.contains("%s") && messageTemplate.contains("%d")) {
                message = String.format(messageTemplate, "192.168.1." + random.nextInt(255), random.nextInt(100));
            } else if (messageTemplate.contains("%s")) {
                String[] values = {"user" + random.nextInt(100), "192.168.1." + random.nextInt(255), 
                                 "session-" + random.nextInt(1000), "service-" + random.nextInt(10)};
                message = String.format(messageTemplate, values[random.nextInt(values.length)]);
            } else if (messageTemplate.contains("%d")) {
                message = String.format(messageTemplate, random.nextInt(100) + 1);
            } else {
                message = messageTemplate;
            }
            
            LogEvent event = new LogEvent(
                "log-" + System.currentTimeMillis() + "-" + random.nextInt(1000),
                LocalDateTime.now().minusSeconds(random.nextInt(300)),
                level,
                sources[random.nextInt(sources.length)],
                message,
                hosts[random.nextInt(hosts.length)],
                category  // Use the category from the template
            );
            
            event.addMetadata("thread", "thread-" + random.nextInt(10));
            event.addMetadata("requestId", "req-" + random.nextInt(10000));
            event.addMetadata("userAgent", "Mozilla/5.0 (compatible; LogBot/1.0)");
            
            logBuffer.add(event);
            counters.computeIfAbsent(level, k -> new AtomicLong()).incrementAndGet();
        }
        
        // Keep only last 1000 logs in buffer
        if (logBuffer.size() > 1000) {
            logBuffer.subList(0, logBuffer.size() - 1000).clear();
        }
    }
    
    // Method to ingest external logs (from file upload or manual entry)
    public void ingestLog(LogEvent logEvent) {
        logBuffer.add(logEvent);
        counters.computeIfAbsent(logEvent.getLevel(), k -> new AtomicLong()).incrementAndGet();
        
        // Keep only last 1000 logs in buffer
        if (logBuffer.size() > 1000) {
            logBuffer.subList(0, logBuffer.size() - 1000).clear();
        }
    }
    
    public List<LogEvent> getRecentLogs(int limit) {
        return logBuffer.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public List<LogEvent> searchLogs(String query, String level, String source, int limit) {
        return logBuffer.stream()
                .filter(log -> query == null || log.getMessage().toLowerCase().contains(query.toLowerCase()))
                .filter(log -> level == null || log.getLevel().equals(level))
                .filter(log -> source == null || log.getSource().equals(source))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public Map<String, Long> getLogCounts() {
        Map<String, Long> counts = new HashMap<>();
        counters.forEach((k, v) -> counts.put(k, v.get()));
        return counts;
    }
}

@Service
class ThreatDetectionService {
    private final List<SecurityAlert> alerts = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> failedLogins = new ConcurrentHashMap<>();
    private final LogIngestionService logService;
    
    public ThreatDetectionService(LogIngestionService logService) {
        this.logService = logService;
    }
    
    @Scheduled(fixedRate = 5000) // Check for threats every 5 seconds
    public void detectThreats() {
        List<LogEvent> recentLogs = logService.getRecentLogs(100);
        
        detectBruteForceAttacks(recentLogs);
        detectSqlInjection(recentLogs);
        detectSystemAnomalies(recentLogs);
    }
    
    private void detectBruteForceAttacks(List<LogEvent> logs) {
        Map<String, Long> failedAuthAttempts = logs.stream()
                .filter(log -> log.getMessage().contains("Authentication failed"))
                .collect(Collectors.groupingBy(
                    log -> extractIP(log.getMessage()),
                    Collectors.counting()
                ));
        
        failedAuthAttempts.forEach((ip, count) -> {
            if (count >= 5) { // 5 failed attempts = brute force
                createAlert("CRITICAL", "Brute Force Attack", 
                    "Multiple failed login attempts from IP: " + ip + " (" + count + " attempts)", 
                    "security");
            }
        });
    }
    
    private void detectSqlInjection(List<LogEvent> logs) {
        Pattern sqlPattern = Pattern.compile("(union|select|insert|update|delete|drop|exec|script)", Pattern.CASE_INSENSITIVE);
        
        logs.stream()
                .filter(log -> sqlPattern.matcher(log.getMessage()).find())
                .forEach(log -> {
                    createAlert("HIGH", "SQL Injection Attempt", 
                        "Potential SQL injection detected: " + log.getMessage(), 
                        log.getSource());
                });
    }
    
    private void detectSystemAnomalies(List<LogEvent> logs) {
        long errorCount = logs.stream()
                .filter(log -> log.getLevel().equals("ERROR"))
                .count();
        
        if (errorCount > 10) { // High error rate
            createAlert("MEDIUM", "System Anomaly", 
                "High error rate detected: " + errorCount + " errors in recent logs", 
                "system");
        }
    }
    
    private String extractIP(String message) {
        // Simple IP extraction - in real implementation, use regex
        return "192.168.1." + new Random().nextInt(255);
    }
    
    private void createAlert(String severity, String type, String message, String source) {
        SecurityAlert alert = new SecurityAlert(
            "alert-" + System.currentTimeMillis(),
            severity, type, message, source
        );
        alerts.add(alert);
        
        // Keep only last 100 alerts
        if (alerts.size() > 100) {
            alerts.subList(0, alerts.size() - 100).clear();
        }
    }
    
    public List<SecurityAlert> getRecentAlerts(int limit) {
        return alerts.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    public long getUnresolvedAlertsCount() {
        return alerts.stream().filter(alert -> !alert.isResolved()).count();
    }
}

@Controller
class DashboardController {
    
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("title", "Enterprise Security Operations Center - Dashboard");
        return "enterprise-dashboard";
    }
    
    @GetMapping("/search")
    public String search(Model model) {
        model.addAttribute("title", "Advanced Log Search & Analytics");
        return "search";
    }
    
    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("title", "Security Alerts & Incident Management");
        return "alerts";
    }
    
    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("title", "Threat Analytics & Intelligence");
        return "analytics";
    }
}

@RestController
@RequestMapping("/api")
class EnterpriseApiController {
    
    private final LogIngestionService logService;
    private final ThreatDetectionService threatService;
    private final Random random = new Random();
    
    public EnterpriseApiController(LogIngestionService logService, ThreatDetectionService threatService) {
        this.logService = logService;
        this.threatService = threatService;
    }
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "Enterprise Log Analysis System");
        health.put("version", "2.0.0");
        health.put("features", Arrays.asList("Real-time Ingestion", "Threat Detection", "Advanced Analytics", "Correlation Engine"));
        return health;
    }
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Long> logCounts = logService.getLogCounts();
        long totalLogs = logCounts.values().stream().mapToLong(Long::longValue).sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", totalLogs);
        stats.put("errorLogs", logCounts.getOrDefault("ERROR", 0L));
        stats.put("warningLogs", logCounts.getOrDefault("WARNING", 0L));
        stats.put("infoLogs", logCounts.getOrDefault("INFO", 0L));
        stats.put("unresolvedAlerts", threatService.getUnresolvedAlertsCount());
        stats.put("criticalAlerts", threatService.getRecentAlerts(100).stream()
                .filter(alert -> "CRITICAL".equals(alert.getSeverity()) && !alert.isResolved())
                .count());
        stats.put("sources", Arrays.asList("nginx", "application", "database", "security", "firewall", "mail-server", "auth-service"));
        stats.put("uptime", "Running - Real-time Processing");
        stats.put("lastUpdated", LocalDateTime.now().toString());
        stats.put("eventsPerSecond", 150 + random.nextInt(50));
        stats.put("storageUsed", "2.4 TB");
        stats.put("indexedEvents", "45.2M");
        return stats;
    }
    
    @GetMapping("/logs")
    public Map<String, Object> getLogs(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source) {
        
        List<LogEvent> logs = logService.searchLogs(query, level, source, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("logs", logs.stream().map(this::logToMap).collect(Collectors.toList()));
        response.put("total", logService.getLogCounts().values().stream().mapToLong(Long::longValue).sum());
        response.put("filtered", logs.size());
        response.put("query", query);
        response.put("level", level);
        response.put("source", source);
        return response;
    }
    
    @GetMapping("/alerts")
    public Map<String, Object> getAlerts(@RequestParam(defaultValue = "10") int size) {
        List<SecurityAlert> alerts = threatService.getRecentAlerts(size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alerts.stream().map(this::alertToMap).collect(Collectors.toList()));
        response.put("total", alerts.size());
        response.put("unresolved", threatService.getUnresolvedAlertsCount());
        return response;
    }
    
    @PostMapping("/alerts/{id}/resolve")
    public Map<String, Object> resolveAlert(@PathVariable String id) {
        // In real implementation, would resolve the alert
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Alert " + id + " has been resolved");
        return response;
    }
    
    @GetMapping("/search")
    public Map<String, Object> advancedSearch(
            @RequestParam String query,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String[] sources,
            @RequestParam(defaultValue = "50") int limit) {
        
        // Advanced search with query parsing (simplified)
        List<LogEvent> results = logService.searchLogs(query, null, null, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("results", results.stream().map(this::logToMap).collect(Collectors.toList()));
        response.put("query", query);
        response.put("executionTime", random.nextInt(500) + "ms");
        response.put("totalHits", results.size());
        response.put("facets", generateFacets(results));
        return response;
    }
    
    @GetMapping("/charts/timeline")
    public Map<String, Object> getTimelineChart() {
        Map<String, Object> response = new HashMap<>();
        List<Object[]> data = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            LocalDateTime time = now.minusHours(i);
            data.add(new Object[]{time.toString(), "ERROR", random.nextInt(20) + 5});
            data.add(new Object[]{time.toString(), "WARNING", random.nextInt(50) + 10});
            data.add(new Object[]{time.toString(), "INFO", random.nextInt(200) + 100});
            data.add(new Object[]{time.toString(), "DEBUG", random.nextInt(300) + 200});
        }
        
        response.put("data", data);
        response.put("type", "timeline");
        return response;
    }
    
    @GetMapping("/charts/sources")
    public Map<String, Object> getSourcesChart() {
        Map<String, Object> response = new HashMap<>();
        List<Object[]> data = Arrays.asList(
            new Object[]{"nginx", 4500 + random.nextInt(500)},
            new Object[]{"application", 3800 + random.nextInt(400)},
            new Object[]{"database", 2900 + random.nextInt(300)},
            new Object[]{"security", 1200 + random.nextInt(200)},
            new Object[]{"firewall", 800 + random.nextInt(200)},
            new Object[]{"mail-server", 600 + random.nextInt(150)}
        );
        
        response.put("data", data);
        response.put("type", "sources");
        return response;
    }
    
    @GetMapping("/charts/threats")
    public Map<String, Object> getThreatChart() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Integer> threatData = new HashMap<>();
        threatData.put("Brute Force", 15 + random.nextInt(10));
        threatData.put("SQL Injection", 8 + random.nextInt(5));
        threatData.put("Malware", 3 + random.nextInt(3));
        threatData.put("DDoS", 5 + random.nextInt(4));
        threatData.put("Data Exfiltration", 2 + random.nextInt(2));
        
        response.put("data", threatData);
        response.put("type", "threats");
        return response;
    }
    
    @GetMapping("/analytics/top-errors")
    public Map<String, Object> getTopErrors() {
        List<Map<String, Object>> topErrors = Arrays.asList(
            createErrorStat("Database connection timeout", 156),
            createErrorStat("Authentication failed", 89),
            createErrorStat("Memory allocation error", 67),
            createErrorStat("File not found", 45),
            createErrorStat("Network unreachable", 34)
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("topErrors", topErrors);
        return response;
    }
    
    @GetMapping("/analytics/performance")
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("avgResponseTime", 250 + random.nextInt(100) + "ms");
        metrics.put("throughput", 1500 + random.nextInt(300) + " req/sec");
        metrics.put("errorRate", String.format("%.2f%%", 1.5 + random.nextDouble() * 2));
        metrics.put("cpuUsage", 45 + random.nextInt(30) + "%");
        metrics.put("memoryUsage", 62 + random.nextInt(25) + "%");
        metrics.put("diskIOPS", 850 + random.nextInt(200));
        return metrics;
    }
    
    @PostMapping("/logs/bulk-ingest")
    public Map<String, Object> bulkIngestLogs(@RequestBody Map<String, Object> payload) {
        // Simulate bulk log ingestion
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("processed", random.nextInt(1000) + 500);
        response.put("errors", random.nextInt(5));
        response.put("processingTime", random.nextInt(2000) + 500 + "ms");
        return response;
    }
    
    @GetMapping("/analytics/threat-intelligence")
    public Map<String, Object> getThreatIntelligence() {
        Map<String, Object> intelligence = new HashMap<>();
        
        // Threat indicators
        List<Map<String, Object>> indicators = Arrays.asList(
            createThreatIndicator("IP", "192.168.1.45", "Malicious", "Command & Control", "High"),
            createThreatIndicator("Domain", "malicious-site.com", "Suspicious", "Phishing", "Medium"),
            createThreatIndicator("Hash", "d41d8cd98f00b204e9800998ecf8427e", "Known Malware", "Trojan", "Critical"),
            createThreatIndicator("IP", "10.0.0.42", "Brute Force Source", "Authentication Attack", "High"),
            createThreatIndicator("URL", "/admin/config.php", "Web Attack", "Directory Traversal", "Medium")
        );
        
        // Threat feeds
        Map<String, Object> feeds = new HashMap<>();
        feeds.put("totalFeeds", 15);
        feeds.put("activeFeeds", 12);
        feeds.put("lastUpdate", LocalDateTime.now().minusMinutes(random.nextInt(30)).toString());
        feeds.put("sources", Arrays.asList("MISP", "VirusTotal", "IBM X-Force", "Recorded Future", "Internal Intel"));
        
        // IOC statistics
        Map<String, Object> iocStats = new HashMap<>();
        iocStats.put("totalIOCs", 15640 + random.nextInt(1000));
        iocStats.put("newToday", 45 + random.nextInt(20));
        iocStats.put("highConfidence", 8920 + random.nextInt(500));
        iocStats.put("categories", Arrays.asList("Malware", "Phishing", "C&C", "Exploit", "Suspicious"));
        
        intelligence.put("indicators", indicators);
        intelligence.put("feeds", feeds);
        intelligence.put("iocStats", iocStats);
        intelligence.put("lastAnalysis", LocalDateTime.now().toString());
        
        return intelligence;
    }
    
    @GetMapping("/incident-response/dashboard")
    public Map<String, Object> getIncidentResponseDashboard() {
        Map<String, Object> response = new HashMap<>();
        
        // Active incidents
        List<Map<String, Object>> incidents = Arrays.asList(
            createIncident("INC-2025-001", "Critical", "Data Exfiltration Attempt", "In Progress", "John Doe"),
            createIncident("INC-2025-002", "High", "Malware Detection", "Investigating", "Jane Smith"),
            createIncident("INC-2025-003", "Medium", "Unauthorized Access", "Contained", "Mike Johnson"),
            createIncident("INC-2025-004", "Low", "Policy Violation", "Resolved", "Sarah Wilson")
        );
        
        // Response metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("avgResponseTime", "12 minutes");
        metrics.put("avgResolutionTime", "2.4 hours");
        metrics.put("escalationRate", "15%");
        metrics.put("containmentRate", "98%");
        
        // Team status
        Map<String, Object> teamStatus = new HashMap<>();
        teamStatus.put("analystOnDuty", 5);
        teamStatus.put("availableAnalysts", 3);
        teamStatus.put("currentWorkload", "Medium");
        teamStatus.put("lastEscalation", LocalDateTime.now().minusHours(2).toString());
        
        response.put("activeIncidents", incidents);
        response.put("metrics", metrics);
        response.put("teamStatus", teamStatus);
        response.put("playbooks", Arrays.asList("Malware Response", "Data Breach", "DDoS Mitigation", "Insider Threat"));
        
        return response;
    }
    
    @GetMapping("/correlation/engine-status")
    public Map<String, Object> getCorrelationEngineStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Engine statistics
        Map<String, Object> engineStats = new HashMap<>();
        engineStats.put("status", "Running");
        engineStats.put("uptime", "15d 8h 23m");
        engineStats.put("rulesLoaded", 247);
        engineStats.put("activeRules", 235);
        engineStats.put("eventsProcessed", 2847392 + random.nextInt(10000));
        engineStats.put("correlationsFound", 1543 + random.nextInt(100));
        
        // Active correlation rules
        List<Map<String, Object>> activeRules = Arrays.asList(
            createCorrelationRule("R001", "Failed Login Sequence", "Authentication", "High", true),
            createCorrelationRule("R002", "Port Scan Detection", "Network", "Medium", true),
            createCorrelationRule("R003", "SQL Injection Pattern", "Application", "Critical", true),
            createCorrelationRule("R004", "Data Exfiltration", "Data Loss", "High", true),
            createCorrelationRule("R005", "Privilege Escalation", "System", "High", false)
        );
        
        // Recent correlations
        List<Map<String, Object>> recentCorrelations = Arrays.asList(
            createCorrelation("CORR-001", "Multiple failed logins from 192.168.1.45", "Authentication Attack", 8),
            createCorrelation("CORR-002", "Unusual data transfer pattern", "Data Exfiltration", 5),
            createCorrelation("CORR-003", "Port scanning activity detected", "Reconnaissance", 12)
        );
        
        status.put("engineStats", engineStats);
        status.put("activeRules", activeRules);
        status.put("recentCorrelations", recentCorrelations);
        status.put("performance", createPerformanceMetrics());
        
        return status;
    }
    
    @GetMapping("/timeline/events")
    public Map<String, Object> getEventTimeline(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(required = false) String eventType) {
        
        Map<String, Object> timeline = new HashMap<>();
        List<Map<String, Object>> events = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        
        // Generate timeline events for the specified period
        for (int i = hours * 60; i >= 0; i -= 5) { // Every 5 minutes
            LocalDateTime eventTime = now.minusMinutes(i);
            
            // Generate multiple event types
            if (random.nextDouble() < 0.7) { // 70% chance of log event
                events.add(createTimelineEvent(eventTime, "Log Event", 
                    "System log entry: " + generateRandomLogMessage(), "info"));
            }
            
            if (random.nextDouble() < 0.15) { // 15% chance of security event
                events.add(createTimelineEvent(eventTime, "Security Event", 
                    "Security alert: " + generateRandomSecurityEvent(), "warning"));
            }
            
            if (random.nextDouble() < 0.05) { // 5% chance of critical event
                events.add(createTimelineEvent(eventTime, "Critical Event", 
                    "Critical incident: " + generateRandomCriticalEvent(), "danger"));
            }
            
            if (random.nextDouble() < 0.10) { // 10% chance of system event
                events.add(createTimelineEvent(eventTime, "System Event", 
                    "System status: " + generateRandomSystemEvent(), "success"));
            }
        }
        
        // Sort events by timestamp (newest first)
        events.sort((a, b) -> {
            String timeA = (String) a.get("timestamp");
            String timeB = (String) b.get("timestamp");
            return timeB.compareTo(timeA);
        });
        
        // Timeline statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", events.size());
        stats.put("timeRange", hours + " hours");
        stats.put("criticalEvents", events.stream().filter(e -> "danger".equals(e.get("severity"))).count());
        stats.put("securityEvents", events.stream().filter(e -> "warning".equals(e.get("severity"))).count());
        
        timeline.put("events", events);
        timeline.put("statistics", stats);
        timeline.put("generatedAt", now.toString());
        
        return timeline;
    }
    
    @PostMapping("/incident-response/create")
    public Map<String, Object> createIncident(@RequestBody Map<String, Object> incidentData) {
        Map<String, Object> response = new HashMap<>();
        String incidentId = "INC-" + System.currentTimeMillis();
        
        response.put("success", true);
        response.put("incidentId", incidentId);
        response.put("message", "Incident created successfully");
        response.put("assignedAnalyst", "Auto-assigned to available analyst");
        response.put("estimatedResolution", "2-4 hours based on severity");
        
        return response;
    }
    
    @PostMapping("/correlation/rules/toggle/{ruleId}")
    public Map<String, Object> toggleCorrelationRule(@PathVariable String ruleId) {
        Map<String, Object> response = new HashMap<>();
        boolean newStatus = random.nextBoolean();
        
        response.put("success", true);
        response.put("ruleId", ruleId);
        response.put("status", newStatus ? "enabled" : "disabled");
        response.put("message", "Rule " + ruleId + " has been " + (newStatus ? "enabled" : "disabled"));
        
        return response;
    }
    
    // Helper methods for new functionality
    private Map<String, Object> createThreatIndicator(String type, String value, String classification, String category, String confidence) {
        Map<String, Object> indicator = new HashMap<>();
        indicator.put("type", type);
        indicator.put("value", value);
        indicator.put("classification", classification);
        indicator.put("category", category);
        indicator.put("confidence", confidence);
        indicator.put("firstSeen", LocalDateTime.now().minusDays(random.nextInt(30)).toString());
        indicator.put("lastSeen", LocalDateTime.now().minusHours(random.nextInt(24)).toString());
        indicator.put("count", random.nextInt(100) + 1);
        return indicator;
    }
    
    private Map<String, Object> createIncident(String id, String severity, String title, String status, String analyst) {
        Map<String, Object> incident = new HashMap<>();
        incident.put("id", id);
        incident.put("severity", severity);
        incident.put("title", title);
        incident.put("status", status);
        incident.put("assignedAnalyst", analyst);
        incident.put("created", LocalDateTime.now().minusHours(random.nextInt(48)).toString());
        incident.put("lastUpdate", LocalDateTime.now().minusMinutes(random.nextInt(120)).toString());
        incident.put("category", Arrays.asList("Malware", "Network", "Data Loss", "Insider Threat").get(random.nextInt(4)));
        return incident;
    }
    
    private Map<String, Object> createCorrelationRule(String id, String name, String category, String priority, boolean enabled) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("id", id);
        rule.put("name", name);
        rule.put("category", category);
        rule.put("priority", priority);
        rule.put("enabled", enabled);
        rule.put("matches", random.nextInt(500));
        rule.put("lastTriggered", LocalDateTime.now().minusMinutes(random.nextInt(180)).toString());
        return rule;
    }
    
    private Map<String, Object> createCorrelation(String id, String description, String type, int eventCount) {
        Map<String, Object> correlation = new HashMap<>();
        correlation.put("id", id);
        correlation.put("description", description);
        correlation.put("type", type);
        correlation.put("eventCount", eventCount);
        correlation.put("timestamp", LocalDateTime.now().minusMinutes(random.nextInt(120)).toString());
        correlation.put("risk", Arrays.asList("Low", "Medium", "High", "Critical").get(random.nextInt(4)));
        return correlation;
    }
    
    private Map<String, Object> createPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("avgProcessingTime", (50 + random.nextInt(200)) + "ms");
        metrics.put("eventsPerSecond", 150 + random.nextInt(100));
        metrics.put("memoryUsage", (60 + random.nextInt(30)) + "%");
        metrics.put("cpuUsage", (25 + random.nextInt(40)) + "%");
        return metrics;
    }
    
    private Map<String, Object> createTimelineEvent(LocalDateTime timestamp, String type, String description, String severity) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", timestamp.toString());
        event.put("type", type);
        event.put("description", description);
        event.put("severity", severity);
        event.put("source", Arrays.asList("System", "Network", "Application", "Security", "User").get(random.nextInt(5)));
        event.put("id", "evt-" + System.currentTimeMillis() + "-" + random.nextInt(1000));
        return event;
    }
    
    private String generateRandomLogMessage() {
        String[] messages = {
            "User authentication successful",
            "Database connection established",
            "File upload completed",
            "Cache cleared successfully",
            "Backup process initiated",
            "Service restart completed"
        };
        return messages[random.nextInt(messages.length)];
    }
    
    private String generateRandomSecurityEvent() {
        String[] events = {
            "Suspicious login attempt detected",
            "Unusual file access pattern",
            "Port scan detected from external IP",
            "Failed authentication threshold exceeded",
            "Privileged account usage outside hours"
        };
        return events[random.nextInt(events.length)];
    }
    
    private String generateRandomCriticalEvent() {
        String[] events = {
            "System compromise detected",
            "Data exfiltration attempt",
            "Malware execution blocked",
            "Critical service failure",
            "Security policy violation"
        };
        return events[random.nextInt(events.length)];
    }
    
    private String generateRandomSystemEvent() {
        String[] events = {
            "System health check passed",
            "Performance optimization completed",
            "Scheduled maintenance finished",
            "Security update applied",
            "Monitoring threshold adjusted"
        };
        return events[random.nextInt(events.length)];
    }
    
    // Log Upload Endpoints
    @PostMapping("/logs/upload")
    public ResponseEntity<Map<String, Object>> uploadLogFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Please select a file to upload");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            int processedLines = 0;
            int errors = 0;
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    
                    try {
                        LogEvent logEvent = parseLogLine(line);
                        logService.ingestLog(logEvent);
                        processedLines++;
                    } catch (Exception e) {
                        errors++;
                        System.err.println("Error parsing log line: " + line + " - " + e.getMessage());
                    }
                }
            }
            
            response.put("success", true);
            response.put("message", "Log file uploaded successfully");
            response.put("processedLines", processedLines);
            response.put("errors", errors);
            response.put("filename", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error reading file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/logs/manual")
    public ResponseEntity<Map<String, Object>> addManualLog(@RequestBody Map<String, String> logData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String level = logData.getOrDefault("level", "INFO");
            String source = logData.getOrDefault("source", "manual");
            String message = logData.getOrDefault("message", "Manual log entry");
            String host = logData.getOrDefault("host", "localhost");
            String category = logData.getOrDefault("category", "application");
            
            LogEvent logEvent = new LogEvent(
                "manual-" + System.currentTimeMillis(),
                LocalDateTime.now(),
                level,
                source,
                message,
                host,
                category
            );
            
            logService.ingestLog(logEvent);
            
            response.put("success", true);
            response.put("message", "Manual log entry added successfully");
            response.put("logId", logEvent.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error adding manual log: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // Parse log line into LogEvent
    private LogEvent parseLogLine(String line) {
        // Basic log parsing - can be enhanced for specific formats
        String[] parts = line.split("\\s+", 6);
        
        String timestamp = parts.length > 0 ? parts[0] : LocalDateTime.now().toString();
        String level = parts.length > 1 ? parts[1].toUpperCase() : "INFO";
        String source = parts.length > 2 ? parts[2] : "unknown";
        String host = parts.length > 3 ? parts[3] : "localhost";
        String message = parts.length > 4 ? String.join(" ", Arrays.copyOfRange(parts, 4, parts.length)) : "No message";
        
        // Clean up level if it has brackets
        level = level.replaceAll("[\\[\\]()]", "");
        
        // Map common log levels
        if (level.contains("ERR") || level.contains("ERROR")) level = "ERROR";
        else if (level.contains("WARN")) level = "WARNING";
        else if (level.contains("INFO")) level = "INFO";
        else if (level.contains("DEBUG")) level = "DEBUG";
        else if (level.contains("FATAL") || level.contains("CRIT")) level = "CRITICAL";
        
        return new LogEvent(
            "upload-" + System.currentTimeMillis() + "-" + random.nextInt(1000),
            LocalDateTime.now(),
            level,
            source,
            message,
            host,
            "uploaded"
        );
    }
    
    // Utility methods for API controller
    private Map<String, Object> logToMap(LogEvent log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getId());
        map.put("timestamp", log.getTimestamp().toString());
        map.put("level", log.getLevel());
        map.put("source", log.getSource());
        map.put("message", log.getMessage());
        map.put("host", log.getHost());
        map.put("category", log.getCategory());
        map.put("metadata", log.getMetadata());
        map.put("rawLog", log.getRawLog());
        return map;
    }
    
    private Map<String, Object> alertToMap(SecurityAlert alert) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", alert.getId());
        map.put("createdAt", alert.getCreatedAt().toString());
        map.put("severity", alert.getSeverity());
        map.put("type", alert.getType());
        map.put("message", alert.getMessage());
        map.put("source", alert.getSource());
        map.put("resolved", alert.isResolved());
        map.put("relatedEvents", alert.getRelatedEvents());
        map.put("riskScore", alert.getRiskScore());
        map.put("details", alert.getDetails());
        return map;
    }
    
    private Map<String, Object> generateFacets(List<LogEvent> logs) {
        Map<String, Object> facets = new HashMap<>();
        
        // Level facets
        Map<String, Long> levelCounts = logs.stream()
            .collect(Collectors.groupingBy(LogEvent::getLevel, Collectors.counting()));
        facets.put("levels", levelCounts);
        
        // Source facets
        Map<String, Long> sourceCounts = logs.stream()
            .collect(Collectors.groupingBy(LogEvent::getSource, Collectors.counting()));
        facets.put("sources", sourceCounts);
        
        // Host facets
        Map<String, Long> hostCounts = logs.stream()
            .collect(Collectors.groupingBy(LogEvent::getHost, Collectors.counting()));
        facets.put("hosts", hostCounts);
        
        return facets;
    }
    
    private Map<String, Object> createErrorStat(String errorType, int count) {
        Map<String, Object> stat = new HashMap<>();
        stat.put("type", errorType);
        stat.put("count", count);
        stat.put("percentage", String.format("%.1f%%", (count / 1000.0) * 100));
        return stat;
    }
}
