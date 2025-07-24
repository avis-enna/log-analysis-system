package com.loganalyzer.service;

import com.loganalyzer.model.Alert;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service for managing alerts and alert rules.
 * Monitors log entries and triggers alerts based on configured rules.
 */
@Service
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private NotificationService notificationService;
    
    @Value("${alerting.enabled:true}")
    private boolean alertingEnabled;
    
    @Value("${alerting.check-interval:60000}")
    private long checkInterval;
    
    // Alert rules cache
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    
    // Alert suppression tracking
    private final Map<String, LocalDateTime> suppressedAlerts = new ConcurrentHashMap<>();
    
    /**
     * Initializes default alert rules.
     */
    public void initializeDefaultRules() {
        // Error rate alert
        AlertRule errorRateRule = new AlertRule(
            "high-error-rate",
            "High Error Rate",
            "Error rate exceeds threshold",
            AlertRule.RuleType.ERROR_RATE,
            Alert.AlertSeverity.HIGH,
            Map.of("threshold", 10.0, "timeWindow", 300) // 10 errors in 5 minutes
        );
        alertRules.put(errorRateRule.getId(), errorRateRule);
        
        // Critical error alert
        AlertRule criticalErrorRule = new AlertRule(
            "critical-error",
            "Critical Error",
            "Critical error detected in logs",
            AlertRule.RuleType.PATTERN_MATCH,
            Alert.AlertSeverity.CRITICAL,
            Map.of("pattern", "FATAL|OutOfMemoryError|StackOverflowError")
        );
        alertRules.put(criticalErrorRule.getId(), criticalErrorRule);
        
        // HTTP 5xx errors
        AlertRule httpErrorRule = new AlertRule(
            "http-5xx-errors",
            "HTTP 5xx Errors",
            "High rate of HTTP 5xx errors",
            AlertRule.RuleType.HTTP_ERROR,
            Alert.AlertSeverity.HIGH,
            Map.of("statusCodePattern", "5\\d\\d", "threshold", 5.0, "timeWindow", 300)
        );
        alertRules.put(httpErrorRule.getId(), httpErrorRule);
        
        // Slow response time
        AlertRule slowResponseRule = new AlertRule(
            "slow-response",
            "Slow Response Time",
            "Response time exceeds threshold",
            AlertRule.RuleType.PERFORMANCE,
            Alert.AlertSeverity.MEDIUM,
            Map.of("responseTimeThreshold", 5000.0) // 5 seconds
        );
        alertRules.put(slowResponseRule.getId(), slowResponseRule);
        
        logger.info("Initialized {} default alert rules", alertRules.size());
    }
    
    /**
     * Checks a log entry against all alert rules.
     */
    public void checkLogForAlerts(LogEntry logEntry) {
        if (!alertingEnabled) {
            return;
        }
        
        for (AlertRule rule : alertRules.values()) {
            try {
                if (evaluateRule(rule, logEntry)) {
                    triggerAlert(rule, logEntry);
                }
            } catch (Exception e) {
                logger.error("Error evaluating alert rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Evaluates if a log entry matches an alert rule.
     */
    private boolean evaluateRule(AlertRule rule, LogEntry logEntry) {
        switch (rule.getType()) {
            case PATTERN_MATCH:
                return evaluatePatternMatch(rule, logEntry);
            
            case ERROR_RATE:
                return evaluateErrorRate(rule, logEntry);
            
            case HTTP_ERROR:
                return evaluateHttpError(rule, logEntry);
            
            case PERFORMANCE:
                return evaluatePerformance(rule, logEntry);
            
            case CUSTOM:
                return evaluateCustomRule(rule, logEntry);
            
            default:
                return false;
        }
    }
    
    /**
     * Evaluates pattern match rule.
     */
    private boolean evaluatePatternMatch(AlertRule rule, LogEntry logEntry) {
        String pattern = (String) rule.getParameters().get("pattern");
        if (pattern == null) return false;
        
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        
        return regex.matcher(logEntry.getMessage()).find() ||
               (logEntry.getStackTrace() != null && regex.matcher(logEntry.getStackTrace()).find());
    }
    
    /**
     * Evaluates error rate rule.
     */
    private boolean evaluateErrorRate(AlertRule rule, LogEntry logEntry) {
        if (!logEntry.isError()) return false;
        
        Double threshold = (Double) rule.getParameters().get("threshold");
        Integer timeWindow = (Integer) rule.getParameters().get("timeWindow");
        
        if (threshold == null || timeWindow == null) return false;
        
        // Count errors in time window
        LocalDateTime windowStart = LocalDateTime.now().minusSeconds(timeWindow);
        long errorCount = countErrorsSince(windowStart, logEntry.getSource());
        
        return errorCount >= threshold;
    }
    
    /**
     * Evaluates HTTP error rule.
     */
    private boolean evaluateHttpError(AlertRule rule, LogEntry logEntry) {
        if (!logEntry.isHttpLog()) return false;
        
        String statusPattern = (String) rule.getParameters().get("statusCodePattern");
        if (statusPattern == null) return false;
        
        Integer httpStatus = logEntry.getHttpStatus();
        if (httpStatus == null) return false;
        
        return Pattern.matches(statusPattern, httpStatus.toString());
    }
    
    /**
     * Evaluates performance rule.
     */
    private boolean evaluatePerformance(AlertRule rule, LogEntry logEntry) {
        Double responseTimeThreshold = (Double) rule.getParameters().get("responseTimeThreshold");
        if (responseTimeThreshold == null) return false;
        
        Long responseTime = logEntry.getResponseTime();
        return responseTime != null && responseTime > responseTimeThreshold;
    }
    
    /**
     * Evaluates custom rule (placeholder for extensibility).
     */
    private boolean evaluateCustomRule(AlertRule rule, LogEntry logEntry) {
        // Custom rule evaluation logic would go here
        return false;
    }
    
    /**
     * Triggers an alert for a matched rule.
     */
    private void triggerAlert(AlertRule rule, LogEntry logEntry) {
        String alertKey = generateAlertKey(rule, logEntry);
        
        // Check if alert is suppressed
        if (isAlertSuppressed(alertKey)) {
            return;
        }
        
        // Check for existing open alert
        Optional<Alert> existingAlert = findExistingAlert(rule, logEntry);
        
        if (existingAlert.isPresent()) {
            // Update existing alert
            Alert alert = existingAlert.get();
            alert.incrementTriggerCount();
            alertRepository.save(alert);
            
            logger.debug("Updated existing alert: {}", alert.getId());
        } else {
            // Create new alert
            Alert alert = createAlert(rule, logEntry);
            Alert savedAlert = alertRepository.save(alert);
            
            // Send real-time notification (if WebSocket is available)
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/alerts", savedAlert);
            }
            
            // Send external notifications
            notificationService.sendAlertNotification(savedAlert);
            
            logger.info("Created new alert: {} for rule: {}", savedAlert.getId(), rule.getId());
        }
        
        // Suppress similar alerts for a period
        suppressAlert(alertKey, rule.getSuppressDuration());
    }
    
    /**
     * Creates a new alert from rule and log entry.
     */
    private Alert createAlert(AlertRule rule, LogEntry logEntry) {
        Alert alert = new Alert();
        alert.setTitle(rule.getName());
        alert.setDescription(generateAlertDescription(rule, logEntry));
        alert.setSeverity(rule.getSeverity());
        alert.setRuleId(Long.valueOf(rule.getId().hashCode()));
        alert.setRuleName(rule.getName());
        alert.setTriggeredBy(logEntry.getSource());
        
        // Set metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("logId", logEntry.getId());
        metadata.put("source", logEntry.getSource());
        metadata.put("host", logEntry.getHost());
        metadata.put("application", logEntry.getApplication());
        metadata.put("environment", logEntry.getEnvironment());
        alert.setMetadata(metadata);
        
        // Set tags
        Set<String> tags = new HashSet<>();
        tags.add(rule.getType().name().toLowerCase());
        tags.add(logEntry.getLevel().toLowerCase());
        if (logEntry.getApplication() != null) {
            tags.add("app:" + logEntry.getApplication());
        }
        alert.setTags(tags);
        
        return alert;
    }
    
    /**
     * Generates alert description from rule and log entry.
     */
    private String generateAlertDescription(AlertRule rule, LogEntry logEntry) {
        StringBuilder description = new StringBuilder();
        description.append(rule.getDescription()).append("\n\n");
        description.append("Log Details:\n");
        description.append("- Timestamp: ").append(logEntry.getTimestamp()).append("\n");
        description.append("- Level: ").append(logEntry.getLevel()).append("\n");
        description.append("- Source: ").append(logEntry.getSource()).append("\n");
        description.append("- Host: ").append(logEntry.getHost()).append("\n");
        description.append("- Message: ").append(logEntry.getMessage()).append("\n");
        
        if (logEntry.isHttpLog()) {
            description.append("- HTTP Status: ").append(logEntry.getHttpStatus()).append("\n");
            description.append("- Response Time: ").append(logEntry.getResponseTime()).append("ms\n");
        }
        
        return description.toString();
    }
    
    /**
     * Generates unique key for alert suppression.
     */
    private String generateAlertKey(AlertRule rule, LogEntry logEntry) {
        return String.format("%s:%s:%s", rule.getId(), logEntry.getSource(), logEntry.getHost());
    }
    
    /**
     * Checks if alert is currently suppressed.
     */
    private boolean isAlertSuppressed(String alertKey) {
        LocalDateTime suppressedUntil = suppressedAlerts.get(alertKey);
        if (suppressedUntil != null && LocalDateTime.now().isBefore(suppressedUntil)) {
            return true;
        }
        
        // Clean up expired suppressions
        suppressedAlerts.remove(alertKey);
        return false;
    }
    
    /**
     * Suppresses alert for specified duration.
     */
    private void suppressAlert(String alertKey, int durationMinutes) {
        LocalDateTime suppressUntil = LocalDateTime.now().plusMinutes(durationMinutes);
        suppressedAlerts.put(alertKey, suppressUntil);
    }
    
    /**
     * Finds existing open alert for the same rule and context.
     */
    private Optional<Alert> findExistingAlert(AlertRule rule, LogEntry logEntry) {
        return alertRepository.findOpenAlertByRuleAndSource(
            Long.valueOf(rule.getId().hashCode()),
            logEntry.getSource()
        );
    }
    
    /**
     * Counts errors since specified time for a source.
     */
    private long countErrorsSince(LocalDateTime since, String source) {
        // This would query the log repository
        // For now, returning a mock count
        return 1;
    }
    
    /**
     * Acknowledges an alert.
     */
    public Alert acknowledgeAlert(Long alertId, String acknowledgedBy) {
        Optional<Alert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isPresent()) {
            Alert alert = alertOpt.get();
            alert.acknowledge(acknowledgedBy);
            Alert savedAlert = alertRepository.save(alert);
            
            // Send real-time update (if WebSocket is available)
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/alerts/acknowledged", savedAlert);
            }
            
            logger.info("Alert {} acknowledged by {}", alertId, acknowledgedBy);
            return savedAlert;
        }
        
        throw new RuntimeException("Alert not found: " + alertId);
    }
    
    /**
     * Resolves an alert.
     */
    public Alert resolveAlert(Long alertId, String resolvedBy, String notes) {
        Optional<Alert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isPresent()) {
            Alert alert = alertOpt.get();
            alert.resolve(resolvedBy, notes);
            Alert savedAlert = alertRepository.save(alert);
            
            // Send real-time update (if WebSocket is available)
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/alerts/resolved", savedAlert);
            }
            
            logger.info("Alert {} resolved by {}", alertId, resolvedBy);
            return savedAlert;
        }
        
        throw new RuntimeException("Alert not found: " + alertId);
    }
    
    /**
     * Gets all open alerts.
     */
    public List<Alert> getOpenAlerts() {
        return alertRepository.findByStatus(Alert.AlertStatus.OPEN);
    }
    
    /**
     * Gets alerts by severity.
     */
    public List<Alert> getAlertsBySeverity(Alert.AlertSeverity severity) {
        return alertRepository.findBySeverity(severity);
    }
    
    /**
     * Scheduled task to clean up old suppressions.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupSuppressions() {
        LocalDateTime now = LocalDateTime.now();
        suppressedAlerts.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
    
    /**
     * Gets alert statistics.
     */
    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAlerts", alertRepository.count());
        stats.put("openAlerts", alertRepository.countByStatus(Alert.AlertStatus.OPEN));
        stats.put("criticalAlerts", alertRepository.countBySeverity(Alert.AlertSeverity.CRITICAL));
        stats.put("suppressedAlerts", suppressedAlerts.size());
        stats.put("activeRules", alertRules.size());
        
        return stats;
    }
    
    /**
     * Inner class representing an alert rule.
     */
    public static class AlertRule {
        private String id;
        private String name;
        private String description;
        private RuleType type;
        private Alert.AlertSeverity severity;
        private Map<String, Object> parameters;
        private boolean enabled = true;
        private int suppressDuration = 15; // minutes
        
        public AlertRule(String id, String name, String description, RuleType type, 
                        Alert.AlertSeverity severity, Map<String, Object> parameters) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.severity = severity;
            this.parameters = parameters;
        }
        
        public enum RuleType {
            PATTERN_MATCH, ERROR_RATE, HTTP_ERROR, PERFORMANCE, CUSTOM
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public RuleType getType() { return type; }
        public Alert.AlertSeverity getSeverity() { return severity; }
        public Map<String, Object> getParameters() { return parameters; }
        public boolean isEnabled() { return enabled; }
        public int getSuppressDuration() { return suppressDuration; }
        
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setSuppressDuration(int suppressDuration) { this.suppressDuration = suppressDuration; }
    }
}
