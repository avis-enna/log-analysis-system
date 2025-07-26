package com.loganalyzer.controller;

import com.loganalyzer.repository.LogEntryJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Alerts controller for managing system alerts and notifications
 */
@RestController
@RequestMapping("/alerts")
@CrossOrigin(origins = "*")
public class AlertsController {

    @Autowired
    private LogEntryJpaRepository logEntryRepository;

    // In-memory alert storage for demo purposes
    private Map<Long, Map<String, Object>> alertStorage = new HashMap<>();
    private Long nextAlertId = 1L;

    /**
     * Get all alerts
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            // Generate alerts based on current log data
            generateAlertsFromLogs();
            
            List<Map<String, Object>> alerts = new ArrayList<>(alertStorage.values());
            
            // Sort by timestamp (newest first)
            alerts.sort((a, b) -> {
                String timestampA = (String) a.get("timestamp");
                String timestampB = (String) b.get("timestamp");
                return timestampB.compareTo(timestampA);
            });
            
            // Paginate results
            int start = (page - 1) * size;
            int end = Math.min(start + size, alerts.size());
            List<Map<String, Object>> paginatedAlerts = start < alerts.size() 
                ? alerts.subList(start, end) 
                : new ArrayList<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("alerts", paginatedAlerts);
            response.put("totalCount", alerts.size());
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) alerts.size() / size));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get open alerts only
     */
    @GetMapping("/open")
    public ResponseEntity<List<Map<String, Object>>> getOpenAlerts() {
        try {
            generateAlertsFromLogs();
            
            List<Map<String, Object>> openAlerts = alertStorage.values().stream()
                .filter(alert -> "OPEN".equals(alert.get("status")))
                .sorted((a, b) -> {
                    String timestampA = (String) a.get("timestamp");
                    String timestampB = (String) b.get("timestamp");
                    return timestampB.compareTo(timestampA);
                })
                .toList();
            
            return ResponseEntity.ok(openAlerts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of(Map.of("error", e.getMessage())));
        }
    }

    /**
     * Get alerts by severity
     */
    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<Map<String, Object>>> getAlertsBySeverity(@PathVariable String severity) {
        try {
            generateAlertsFromLogs();
            
            List<Map<String, Object>> severityAlerts = alertStorage.values().stream()
                .filter(alert -> severity.equalsIgnoreCase((String) alert.get("severity")))
                .sorted((a, b) -> {
                    String timestampA = (String) a.get("timestamp");
                    String timestampB = (String) b.get("timestamp");
                    return timestampB.compareTo(timestampA);
                })
                .toList();
            
            return ResponseEntity.ok(severityAlerts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of(Map.of("error", e.getMessage())));
        }
    }

    /**
     * Acknowledge an alert
     */
    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(
            @PathVariable Long alertId,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> alert = alertStorage.get(alertId);
            if (alert == null) {
                return ResponseEntity.notFound().build();
            }
            
            String acknowledgedBy = (String) request.get("acknowledgedBy");
            
            alert.put("status", "ACKNOWLEDGED");
            alert.put("acknowledgedBy", acknowledgedBy);
            alert.put("acknowledgedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert acknowledged successfully");
            response.put("alert", alert);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Resolve an alert
     */
    @PostMapping("/{alertId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(
            @PathVariable Long alertId,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> alert = alertStorage.get(alertId);
            if (alert == null) {
                return ResponseEntity.notFound().build();
            }
            
            String resolvedBy = (String) request.get("resolvedBy");
            String notes = (String) request.get("notes");
            
            alert.put("status", "RESOLVED");
            alert.put("resolvedBy", resolvedBy);
            alert.put("resolvedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            alert.put("resolutionNotes", notes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert resolved successfully");
            response.put("alert", alert);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete an alert
     */
    @DeleteMapping("/{alertId}")
    public ResponseEntity<Map<String, Object>> deleteAlert(@PathVariable Long alertId) {
        try {
            Map<String, Object> alert = alertStorage.remove(alertId);
            if (alert == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generate alerts based on current log data
     */
    private void generateAlertsFromLogs() {
        try {
            long totalLogs = logEntryRepository.count();
            long errorCount = logEntryRepository.countByLevelIgnoreCase("ERROR");
            long warnCount = logEntryRepository.countByLevelIgnoreCase("WARN") + 
                           logEntryRepository.countByLevelIgnoreCase("WARNING");
            
            // Clear existing alerts (for demo purposes)
            if (alertStorage.isEmpty()) {
                
                // Generate error rate alert if error rate > 5%
                if (totalLogs > 0 && (errorCount * 100.0 / totalLogs) > 5) {
                    createAlert(
                        "High Error Rate Detected",
                        String.format("Error rate is %.1f%% (threshold: 5%%)", (errorCount * 100.0 / totalLogs)),
                        "HIGH",
                        "OPEN",
                        "system"
                    );
                }
                
                // Generate alert for high log volume
                if (totalLogs > 50) {
                    createAlert(
                        "High Log Volume",
                        String.format("Total log count is %d, indicating high system activity", totalLogs),
                        "MEDIUM",
                        "OPEN",
                        "monitoring"
                    );
                }
                
                // Generate warning alert if warnings > 20% of total logs
                if (totalLogs > 0 && (warnCount * 100.0 / totalLogs) > 20) {
                    createAlert(
                        "High Warning Rate",
                        String.format("Warning rate is %.1f%% (threshold: 20%%)", (warnCount * 100.0 / totalLogs)),
                        "MEDIUM",
                        "OPEN",
                        "application"
                    );
                }
                
                // Generate info alert for multiple sources
                List<String> sources = logEntryRepository.findDistinctSources();
                if (sources.size() > 3) {
                    createAlert(
                        "Multiple Log Sources Active",
                        String.format("%d different log sources detected", sources.size()),
                        "LOW",
                        "OPEN",
                        "infrastructure"
                    );
                }
                
                // Add a resolved alert for demo purposes
                createAlert(
                    "Database Connection Issue",
                    "Database connection timeout resolved after restart",
                    "HIGH",
                    "RESOLVED",
                    "database"
                );
            }
            
        } catch (Exception e) {
            // Silently handle errors in alert generation
        }
    }

    /**
     * Helper method to create an alert
     */
    private void createAlert(String title, String message, String severity, String status, String source) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("id", nextAlertId++);
        alert.put("title", title);
        alert.put("message", message);
        alert.put("severity", severity);
        alert.put("status", status);
        alert.put("source", source);
        alert.put("timestamp", LocalDateTime.now().minusMinutes((long)(Math.random() * 60)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        alert.put("count", 1);
        
        if ("RESOLVED".equals(status)) {
            alert.put("resolvedBy", "system");
            alert.put("resolvedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            alert.put("resolutionNotes", "Auto-resolved by system");
        }
        
        alertStorage.put((Long) alert.get("id"), alert);
    }
}
