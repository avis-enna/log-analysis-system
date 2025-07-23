package com.sivareddy.loganalysis.controller;

import com.sivareddy.loganalysis.model.LogEntry;
import com.sivareddy.loganalysis.model.Alert;
import com.sivareddy.loganalysis.repository.LogEntryRepository;
import com.sivareddy.loganalysis.repository.AlertRepository;
import com.sivareddy.loganalysis.service.LogProcessingService;
import com.sivareddy.loganalysis.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API controller for log analysis operations
 * 
 * @author Siva Reddy
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LogAnalysisApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogAnalysisApiController.class);
    
    @Autowired
    private LogEntryRepository logEntryRepository;
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Autowired
    private LogProcessingService logProcessingService;
    
    @Autowired
    private AlertService alertService;
    
    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            
            // Log statistics
            long totalLogs = logEntryRepository.count();
            long logsLast24Hours = logEntryRepository.countByLevelSince("INFO", last24Hours) +
                                  logEntryRepository.countByLevelSince("WARNING", last24Hours) +
                                  logEntryRepository.countByLevelSince("ERROR", last24Hours);
            long errorLogs = logEntryRepository.countByLevelSince("ERROR", last24Hours);
            long warningLogs = logEntryRepository.countByLevelSince("WARNING", last24Hours);
            
            // Alert statistics
            long totalAlerts = alertRepository.count();
            long unresolvedAlerts = alertRepository.countUnresolvedAlerts();
            long criticalAlerts = alertRepository.countBySeveritySince("CRITICAL", last24Hours);
            
            // Sources
            List<String> sources = logEntryRepository.findDistinctSources();
            
            stats.put("totalLogs", totalLogs);
            stats.put("logsLast24Hours", logsLast24Hours);
            stats.put("errorLogs", errorLogs);
            stats.put("warningLogs", warningLogs);
            stats.put("totalAlerts", totalAlerts);
            stats.put("unresolvedAlerts", unresolvedAlerts);
            stats.put("criticalAlerts", criticalAlerts);
            stats.put("sources", sources);
            stats.put("uptime", "Running");
            stats.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error getting dashboard stats", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get paginated log entries
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String search) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            Page<LogEntry> logPage;
            
            if (search != null && !search.trim().isEmpty()) {
                logPage = logEntryRepository.findByMessageContaining(search, pageable);
            } else if (level != null && source != null) {
                logPage = logEntryRepository.findByLevelAndSource(level, source, pageable);
            } else if (level != null) {
                logPage = logEntryRepository.findByLevel(level, pageable);
            } else if (source != null) {
                logPage = logEntryRepository.findBySource(source, pageable);
            } else {
                logPage = logEntryRepository.findAll(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logPage.getContent());
            response.put("totalPages", logPage.getTotalPages());
            response.put("totalElements", logPage.getTotalElements());
            response.put("currentPage", logPage.getNumber());
            response.put("pageSize", logPage.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting logs", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get recent alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean resolved) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Alert> alertPage;
            
            if (severity != null && resolved != null) {
                if (resolved) {
                    alertPage = alertRepository.findByResolvedTrue(pageable);
                } else {
                    alertPage = alertRepository.findByResolvedFalse(pageable);
                }
            } else if (severity != null) {
                alertPage = alertRepository.findBySeverity(severity, pageable);
            } else if (resolved != null) {
                if (resolved) {
                    alertPage = alertRepository.findByResolvedTrue(pageable);
                } else {
                    alertPage = alertRepository.findByResolvedFalse(pageable);
                }
            } else {
                alertPage = alertRepository.findAll(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("alerts", alertPage.getContent());
            response.put("totalPages", alertPage.getTotalPages());
            response.put("totalElements", alertPage.getTotalElements());
            response.put("currentPage", alertPage.getNumber());
            response.put("pageSize", alertPage.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting alerts", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Upload and process log file
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadLogFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
            }
            
            // Create temporary file
            File tempFile = File.createTempFile("log_upload_", ".log");
            file.transferTo(tempFile);
            
            // Process the file asynchronously
            CompletableFuture<Integer> processingResult = logProcessingService.processLogFile(tempFile.getAbsolutePath());
            
            // Clean up temp file
            tempFile.deleteOnExit();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "File uploaded and processing started");
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("status", "processing");
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            logger.error("Error uploading file", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }
    
    /**
     * Get timeline chart data
     */
    @GetMapping("/charts/timeline")
    public ResponseEntity<Map<String, Object>> getTimelineChart(@RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<Object[]> data = logEntryRepository.getHourlyLogCounts(since);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("generated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting timeline chart", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get sources distribution chart data
     */
    @GetMapping("/charts/sources")
    public ResponseEntity<Map<String, Object>> getSourcesChart(@RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<Object[]> data = logEntryRepository.getLogStatsBySource(since);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("generated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting sources chart", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get top IP addresses
     */
    @GetMapping("/analytics/top-ips")
    public ResponseEntity<Map<String, Object>> getTopIpAddresses(@RequestParam(defaultValue = "24") int hours) {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<Object[]> data = logEntryRepository.getTopIpAddresses(since, 10);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            response.put("generated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting top IP addresses", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Acknowledge an alert
     */
    @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        try {
            String acknowledgedBy = request.get("acknowledgedBy");
            if (acknowledgedBy == null || acknowledgedBy.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "acknowledgedBy is required"));
            }
            
            Alert alert = alertService.acknowledgeAlert(id, acknowledgedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Alert acknowledged successfully");
            response.put("alert", alert);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error acknowledging alert", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Resolve an alert
     */
    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveAlert(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        
        try {
            String resolvedBy = request.get("resolvedBy");
            String resolutionNotes = request.get("resolutionNotes");
            
            if (resolvedBy == null || resolvedBy.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "resolvedBy is required"));
            }
            
            Alert alert = alertService.resolveAlert(id, resolvedBy, resolutionNotes);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Alert resolved successfully");
            response.put("alert", alert);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error resolving alert", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        health.put("service", "Log Analysis System");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }
}
