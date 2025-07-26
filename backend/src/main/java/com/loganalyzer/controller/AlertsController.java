package com.loganalyzer.controller;

import com.loganalyzer.repository.LogEntryJpaRepository;
import com.loganalyzer.service.AlertGenerationService;
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
    
    @Autowired
    private AlertGenerationService alertGenerationService;

    /**
     * Get all alerts
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            // Get alerts from the service (which auto-generates based on current logs)
            List<Map<String, Object>> alerts = alertGenerationService.getAllAlerts();
            
            // Paginate results
            int start = (page - 1) * size;
            int end = Math.min(start + size, alerts.size());
            List<Map<String, Object>> paginatedAlerts = start < alerts.size() 
                ? alerts.subList(start, end) 
                : new ArrayList<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("alerts", paginatedAlerts);
            response.put("totalElements", alerts.size());
            response.put("totalPages", (int) Math.ceil((double) alerts.size() / size));
            response.put("currentPage", page);
            response.put("size", size);
            response.put("hasNext", end < alerts.size());
            response.put("hasPrevious", page > 1);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get open alerts
     */
    @GetMapping("/open")
    public ResponseEntity<List<Map<String, Object>>> getOpenAlerts() {
        try {
            List<Map<String, Object>> openAlerts = alertGenerationService.getOpenAlerts();
            
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
            List<Map<String, Object>> severityAlerts = alertGenerationService.getAllAlerts().stream()
                .filter(alert -> severity.equalsIgnoreCase((String) alert.get("severity")))
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
            String acknowledgedBy = (String) request.get("acknowledgedBy");
            if (acknowledgedBy == null) {
                acknowledgedBy = "system-user";
            }
            
            boolean success = alertGenerationService.acknowledgeAlert(alertId, acknowledgedBy);
            
            if (!success) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert acknowledged successfully");
            
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
            String resolvedBy = (String) request.get("resolvedBy");
            String notes = (String) request.get("notes");
            
            if (resolvedBy == null) {
                resolvedBy = "system-user";
            }
            if (notes == null) {
                notes = "Alert resolved via API";
            }
            
            boolean success = alertGenerationService.resolveAlert(alertId, resolvedBy, notes);
            
            if (!success) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alert resolved successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get alert statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAlertStats() {
        try {
            Map<String, Object> stats = alertGenerationService.getAlertStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Force regeneration of all alerts based on current log data
     */
    @PostMapping("/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateAlerts() {
        try {
            alertGenerationService.regenerateAllAlerts();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Alerts regenerated successfully");
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
