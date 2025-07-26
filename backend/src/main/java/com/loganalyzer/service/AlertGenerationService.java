package com.loganalyzer.service;

import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for generating and managing real-time alerts based on log analysis
 */
@Service
public class AlertGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertGenerationService.class);
    
    @Autowired
    private LogEntryJpaRepository logEntryRepository;
    
    // In-memory alert storage - in production this would be a database
    private Map<Long, Map<String, Object>> alertStorage = new ConcurrentHashMap<>();
    private Long nextAlertId = 1L;
    
    // Alert thresholds
    private static final double ERROR_RATE_THRESHOLD = 5.0;
    private static final double WARNING_RATE_THRESHOLD = 20.0;
    private static final int HIGH_VOLUME_THRESHOLD = 10;
    private static final int RECENT_ERRORS_THRESHOLD = 3;
    
    /**
     * Analyze a new log entry and generate alerts if necessary
     */
    public void analyzeLogAndGenerateAlerts(LogEntry logEntry) {
        try {
            logger.debug("Analyzing log entry for alerts: {}", logEntry.getId());
            
            // Check for immediate critical alerts
            checkCriticalErrorAlert(logEntry);
            
            // Check system-wide alert conditions
            checkSystemWideAlerts();
            
            logger.debug("Alert analysis completed for log entry: {}", logEntry.getId());
            
        } catch (Exception e) {
            logger.error("Error analyzing log for alerts: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check for critical error patterns in individual log entry
     */
    private void checkCriticalErrorAlert(LogEntry logEntry) {
        if ("ERROR".equalsIgnoreCase(logEntry.getLevel())) {
            String message = logEntry.getMessage();
            
            // Check for critical error patterns
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                
                if (lowerMessage.contains("database") && lowerMessage.contains("connection")) {
                    createOrUpdateAlert(
                        "DATABASE_CONNECTION_ERROR",
                        "Database Connection Error",
                        "Critical database connection error detected",
                        "CRITICAL",
                        "database"
                    );
                }
                
                if (lowerMessage.contains("timeout")) {
                    createOrUpdateAlert(
                        "TIMEOUT_ERROR",
                        "Service Timeout Error",
                        "Service timeout errors detected",
                        "HIGH",
                        "performance"
                    );
                }
                
                if (lowerMessage.contains("out of memory") || lowerMessage.contains("memory")) {
                    createOrUpdateAlert(
                        "MEMORY_ERROR",
                        "Memory Error",
                        "Memory-related errors detected",
                        "CRITICAL",
                        "system"
                    );
                }
            }
        }
    }
    
    /**
     * Check system-wide alert conditions
     */
    private void checkSystemWideAlerts() {
        long totalLogs = logEntryRepository.count();
        
        if (totalLogs == 0) return;
        
        // Check error rate
        long errorCount = logEntryRepository.countByLevelIgnoreCase("ERROR");
        double errorRate = (errorCount * 100.0) / totalLogs;
        
        if (errorRate > ERROR_RATE_THRESHOLD) {
            createOrUpdateAlert(
                "HIGH_ERROR_RATE",
                "High Error Rate Detected",
                String.format("Error rate is %.1f%% (threshold: %.1f%%)", errorRate, ERROR_RATE_THRESHOLD),
                "HIGH",
                "system"
            );
        }
        
        // Check warning rate
        long warnCount = logEntryRepository.countByLevelIgnoreCase("WARN") + 
                        logEntryRepository.countByLevelIgnoreCase("WARNING");
        double warningRate = (warnCount * 100.0) / totalLogs;
        
        if (warningRate > WARNING_RATE_THRESHOLD) {
            createOrUpdateAlert(
                "HIGH_WARNING_RATE",
                "High Warning Rate",
                String.format("Warning rate is %.1f%% (threshold: %.1f%%)", warningRate, WARNING_RATE_THRESHOLD),
                "MEDIUM",
                "application"
            );
        }
        
        // Check recent errors (last 10 minutes)
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(10);
        long recentErrors = logEntryRepository.countByTimestampAfterAndLevelIgnoreCase(recentTime, "ERROR");
        
        if (recentErrors >= RECENT_ERRORS_THRESHOLD) {
            createOrUpdateAlert(
                "RECENT_ERROR_SPIKE",
                "Recent Error Spike",
                String.format("%d errors occurred in the last 10 minutes", recentErrors),
                "HIGH",
                "monitoring"
            );
        }
        
        // Check for high log volume
        if (totalLogs > HIGH_VOLUME_THRESHOLD) {
            createOrUpdateAlert(
                "HIGH_LOG_VOLUME",
                "High Log Volume",
                String.format("Total log count is %d, indicating high system activity", totalLogs),
                "LOW",
                "monitoring"
            );
        }
        
        // Check multiple sources
        List<String> sources = logEntryRepository.findDistinctSources();
        if (sources.size() > 3) {
            createOrUpdateAlert(
                "MULTIPLE_SOURCES",
                "Multiple Log Sources Active",
                String.format("%d different log sources detected", sources.size()),
                "LOW",
                "infrastructure"
            );
        }
    }
    
    /**
     * Create or update an alert
     */
    private void createOrUpdateAlert(String alertKey, String title, String description, 
                                   String severity, String category) {
        
        // Check if alert already exists
        Map<String, Object> existingAlert = findAlertByKey(alertKey);
        
        if (existingAlert != null) {
            // Update existing alert
            existingAlert.put("description", description);
            existingAlert.put("lastOccurrence", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Increment occurrence count
            int count = (Integer) existingAlert.getOrDefault("occurrenceCount", 1);
            existingAlert.put("occurrenceCount", count + 1);
            
            logger.debug("Updated existing alert: {}", alertKey);
        } else {
            // Create new alert
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", nextAlertId++);
            alert.put("key", alertKey);
            alert.put("title", title);
            alert.put("description", description);
            alert.put("severity", severity);
            alert.put("category", category);
            alert.put("status", "OPEN");
            alert.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            alert.put("lastOccurrence", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            alert.put("occurrenceCount", 1);
            alert.put("acknowledgedBy", null);
            alert.put("resolvedBy", null);
            
            alertStorage.put((Long) alert.get("id"), alert);
            
            logger.info("Created new alert: {} - {}", alertKey, title);
        }
    }
    
    /**
     * Find alert by key
     */
    private Map<String, Object> findAlertByKey(String alertKey) {
        return alertStorage.values().stream()
            .filter(alert -> alertKey.equals(alert.get("key")))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get all alerts
     */
    public List<Map<String, Object>> getAllAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>(alertStorage.values());
        
        // Sort by timestamp (newest first)
        alerts.sort((a, b) -> {
            String timestampA = (String) a.get("timestamp");
            String timestampB = (String) b.get("timestamp");
            return timestampB.compareTo(timestampA);
        });
        
        return alerts;
    }
    
    /**
     * Get open alerts
     */
    public List<Map<String, Object>> getOpenAlerts() {
        return alertStorage.values().stream()
            .filter(alert -> "OPEN".equals(alert.get("status")))
            .sorted((a, b) -> {
                String timestampA = (String) a.get("timestamp");
                String timestampB = (String) b.get("timestamp");
                return timestampB.compareTo(timestampA);
            })
            .toList();
    }
    
    /**
     * Acknowledge an alert
     */
    public boolean acknowledgeAlert(Long alertId, String acknowledgedBy) {
        Map<String, Object> alert = alertStorage.get(alertId);
        if (alert != null && "OPEN".equals(alert.get("status"))) {
            alert.put("status", "ACKNOWLEDGED");
            alert.put("acknowledgedBy", acknowledgedBy);
            alert.put("acknowledgedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            logger.info("Alert {} acknowledged by {}", alertId, acknowledgedBy);
            return true;
        }
        return false;
    }
    
    /**
     * Resolve an alert
     */
    public boolean resolveAlert(Long alertId, String resolvedBy, String notes) {
        Map<String, Object> alert = alertStorage.get(alertId);
        if (alert != null && !"RESOLVED".equals(alert.get("status"))) {
            alert.put("status", "RESOLVED");
            alert.put("resolvedBy", resolvedBy);
            alert.put("resolvedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            alert.put("resolutionNotes", notes);
            logger.info("Alert {} resolved by {}: {}", alertId, resolvedBy, notes);
            return true;
        }
        return false;
    }
    
    /**
     * Get alert statistics
     */
    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long total = alertStorage.size();
        long open = alertStorage.values().stream()
            .mapToLong(alert -> "OPEN".equals(alert.get("status")) ? 1 : 0)
            .sum();
        long acknowledged = alertStorage.values().stream()
            .mapToLong(alert -> "ACKNOWLEDGED".equals(alert.get("status")) ? 1 : 0)
            .sum();
        long resolved = alertStorage.values().stream()
            .mapToLong(alert -> "RESOLVED".equals(alert.get("status")) ? 1 : 0)
            .sum();
        
        // Count by severity
        long critical = alertStorage.values().stream()
            .mapToLong(alert -> "CRITICAL".equals(alert.get("severity")) ? 1 : 0)
            .sum();
        long high = alertStorage.values().stream()
            .mapToLong(alert -> "HIGH".equals(alert.get("severity")) ? 1 : 0)
            .sum();
        long medium = alertStorage.values().stream()
            .mapToLong(alert -> "MEDIUM".equals(alert.get("severity")) ? 1 : 0)
            .sum();
        long low = alertStorage.values().stream()
            .mapToLong(alert -> "LOW".equals(alert.get("severity")) ? 1 : 0)
            .sum();
        
        stats.put("total", total);
        stats.put("open", open);
        stats.put("acknowledged", acknowledged);
        stats.put("resolved", resolved);
        stats.put("critical", critical);
        stats.put("high", high);
        stats.put("medium", medium);
        stats.put("low", low);
        
        return stats;
    }
    
    /**
     * Force regeneration of all alerts based on current log data
     */
    public void regenerateAllAlerts() {
        logger.info("Regenerating all alerts based on current log data");
        alertStorage.clear();
        nextAlertId = 1L;
        checkSystemWideAlerts();
    }
}
